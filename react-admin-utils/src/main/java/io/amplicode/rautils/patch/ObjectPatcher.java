/*
 * Copyright 2024 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.amplicode.rautils.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.util.LRUMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bean providing methods for object patching and validation.
 * <br/>
 * Object patching is required for implementing <code>update</code> and <code>updateMany</code>
 *  endpoints for React Admin resources that use immutable DTOs for REST API layer.
 * <br/>
 * Relies on Jackson and therefore considers all its annotations.
 */
public class ObjectPatcher {

    private final ObjectMapper objectMapper;
	private final Validator validator;
	private final LRUMap<Class<?>, BeanDescription> cachedDescriptions;

	public ObjectPatcher(ObjectMapper objectMapper,
						 Validator validator) {
        this.objectMapper = objectMapper;
		this.validator = validator;
        cachedDescriptions = new LRUMap<>(64, 2000);
    }

	/**
	 * Patches passed object with properties from passed json.
	 * Supports immutable DTOs and Java 17 records.
	 * May modify existing object or create a shallow copy.
	 * Then validates patched object using globally configured validator.
	 *
	 * @param patchJson request body JSON containing fields to update
	 * @param target target object (bean) that should be patched
	 * @return the same modified or another created object with patched properties
	 * @param <T> object class
	 */
	public <T> T patchAndValidate(T target, String patchJson) {
		T patchedTarget = patch(target, patchJson);
		validate(patchedTarget);
		return patchedTarget;
	}

	/**
	 * Patches passed object with properties from passed json.
	 * Supports immutable DTOs and Java 17 records.
	 * May modify existing object or create a shallow copy.
	 *
	 * @param patchJson request body JSON containing fields to update
	 * @param target target object (bean) that should be patched
	 * @return the same modified or another created object with patched properties
	 * @param <T> object class
	 */
    @SuppressWarnings("unchecked")
    public <T> T patch(T target, String patchJson) {
        // 1. deserialize json into DTO_2
        // 2. deserialize json into Set of patched property names
        // 3. gather Map of properties - take from target
        // 4. patch Map of properties - copy from DTO_2 only those that existed in json
        // 5. construct DTO_3 from patched properties.
        // 6. return DTO_3 as a result.

        BeanDescription beanDescription = getBeanDescription(target.getClass());
        List<BeanPropertyDefinition> beanProperties = beanDescription.findProperties();

        // optimisation for safely mutable beans
        if (canBePatchedInPlace(beanDescription)) {
            patchBeanInPlace(patchJson, target);
            return target;
        }

        // 1.
        T patchObject;
        try {
            patchObject = (T) objectMapper.readValue(patchJson, target.getClass());
        } catch (JsonProcessingException e) {
			throw new JsonConversionException("Failed to parse patch JSON", e);
        }

        // 2.
        Set<String> patchedPropertyNames = determinePatchedProperties(patchJson);

        // 3.
        Map<String, Object> allPropertyValues = extractPropertyValues(target, beanProperties);

        // 4.
        for (String patchedPropertyName: patchedPropertyNames) {
			BeanPropertyDefinition propertyDefinition = beanProperties.stream()
				.filter(p -> p.getName().equals(patchedPropertyName) && p.hasGetter())
				.findAny()
				.orElseThrow(() -> new IllegalStateException("Can't find getter for property " + patchedPropertyName));

			Object patchedValue =  propertyDefinition.getGetter().getValue(patchObject);
            allPropertyValues.put(patchedPropertyName, patchedValue);
        }

        // 5.
        T patchedObject = (T) constructBean(allPropertyValues, beanDescription);
        return patchedObject;
    }

    private <T> void patchBeanInPlace(String patchJson, T target) {
        try {
            objectMapper.readerForUpdating(target).readValue(patchJson);
        } catch (JsonProcessingException e) {
			throw new JsonConversionException("Failed to parse patch JSON", e);
        }
    }

    private boolean canBePatchedInPlace(BeanDescription beanDescription) {
        // Prefer to fill bean values through constructor parameters and setters.
		//
		// The bean can be patched in place if it does NOT contain properties
        //   that cannot be set via setter, but can be set via field or constructor.
		if (beanDescription.isRecordType()) {
			return false;
		}
        return beanDescription.findProperties().stream()
                .noneMatch(p -> (p.hasConstructorParameter() || p.hasField()) && !p.hasSetter());
    }

    private Object constructBean(Map<String, Object> propertyValues, BeanDescription beanDescription)  {
        try {
			DefaultDeserializationContext defaultContext = (DefaultDeserializationContext) objectMapper.getDeserializationContext();
			DeserializationContext ctxt = defaultContext.createInstance(objectMapper.getDeserializationConfig(), null, null);
			BeanDeserializer deser = (BeanDeserializer) ctxt.findRootValueDeserializer(beanDescription.getType());

            ValueInstantiator valueInstantiator = deser.getValueInstantiator();
            SettableBeanProperty[] creatorProps = valueInstantiator.getFromObjectArguments(ctxt.getConfig());

			if (creatorProps.length == 0) {
				throw new IllegalArgumentException("Bean " + beanDescription.getBeanClass() + " doesn't have appropriate constructor");
			}

			Object[] args = new Object[creatorProps.length];
			for (int i = 0; i < args.length; i++) {
				String propertyName = creatorProps[i].getName();
				args[i] = propertyValues.get(propertyName);
			}

			// create bean with preferred constructor
			Object bean = valueInstantiator.createFromObjectWith(ctxt, args);
			fillNonConstructorProperties(propertyValues, beanDescription, creatorProps, bean);
			return bean;
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error when constructing patched bean", e);
        }
    }

	private void fillNonConstructorProperties(Map<String, Object> propertyValues, BeanDescription beanDescription,
											  SettableBeanProperty[] creatorProps, Object bean) {
		Set<String> constructorPropertyNames = Arrays.stream(creatorProps)
			.map(p -> p.getName())
			.collect(Collectors.toSet());

		List<BeanPropertyDefinition> allBeanProperties = beanDescription.findProperties();

		for (Map.Entry<String, Object> property: propertyValues.entrySet()) {
			String propertyName = property.getKey();
			if (constructorPropertyNames.contains(propertyName)) {
				continue;
			}
			allBeanProperties.stream()
				.filter(p -> p.getName().equals(propertyName) && (p.hasSetter() || p.hasField()))
				.findAny()
				.ifPresent(def -> def.getNonConstructorMutator().setValue(bean, property.getValue()));
		}
	}

	private BeanDescription getBeanDescription(Class<?> targetClass) {
		BeanDescription description = cachedDescriptions.get(targetClass);
		if (description != null) {
			return description;
		}

        DeserializationConfig config = objectMapper.getDeserializationConfig();
        ClassIntrospector introspector = config.getClassIntrospector();
        description = introspector.forDeserialization(config, objectMapper.constructType(targetClass),
                config);

		cachedDescriptions.put(targetClass, description);
        return description;
    }

    private Set<String> determinePatchedProperties(String patchJson) {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(patchJson);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("Failed to parse patch JSON", e);
        }

        Set<String> propertyNames = new HashSet<>();
        for (var it = jsonNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            propertyNames.add(entry.getKey());
        }
        return propertyNames;
    }

    private <T> Map<String, Object> extractPropertyValues(T bean, List<BeanPropertyDefinition> beanProperties) {
		Map<String, Object> map = new HashMap<>();
		beanProperties.stream()
			.filter(def -> def.hasGetter())
			.forEach(def -> {
				Object value = def.getGetter().getValue(bean); // can be null, so can't use Collectors.toMap()
				map.put(def.getName(), value);
			});
		return map;
    }

	/**
	 * Validate patched object using globally configured {@link Validator}.
	 * @param target object to validate using bean validation rules
	 */
	public void validate(Object target) {
		DataBinder dataBinder = new DataBinder(target);
		dataBinder.setValidator(validator);
		dataBinder.validate();
		BindingResult bindResult = dataBinder.getBindingResult();
		if (bindResult.hasErrors()) {
			throw new PatchValidationException(bindResult);
		}
	}

}
