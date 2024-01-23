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

package io.amplicode.rautils.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks attribute of a class as a filter condition attribute.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SpecFilterCondition {

    /**
     * Name of the property to filter by.
     * Can be compound property path, for singular associations.
     * If empty - assumed to be the same as filter field name.
     * */
    String property() default "";

    /** Name of the collection association attribute to join to access the property */
    String joinCollection() default "";

    SpecFilterOperator operator() default SpecFilterOperator.EQUALS;

    boolean ignoreCase() default false;
}
