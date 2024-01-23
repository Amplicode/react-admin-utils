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

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Bean that converts filter object with simple declared conditions into JPA specification.
 */
public class SpecificationFilterConverter {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    /**
     * Convert filter object to JPA specification.
     * Use {@link SpecFilterCondition} annotations as declaration of filter conditions.
     */
    public <T> Specification<T> convert(@Nullable Object filter) {
        if (filter == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.and();
        }

        Map<Field, SpecFilterCondition> conditions = getDeclaredConditions(filter.getClass());
        List<PredicateFactory> predicateFactories = new ArrayList<>();
        for (Map.Entry<Field, SpecFilterCondition> entry: conditions.entrySet()) {
            Optional<PredicateFactory> predicate = convertToPredicate(entry.getKey(), entry.getValue(), filter);
            predicate.ifPresent(predicateFactories::add);
        }

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = predicateFactories.stream()
                    .map(pf -> pf.getLeafPredicate(root, criteriaBuilder))
                    .toList();

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<Field, SpecFilterCondition> getDeclaredConditions(Class<?> filterClass) {
        Map<Field, SpecFilterCondition> res = new HashMap<>();
        ReflectionUtils.doWithFields(filterClass,
                fc -> {
                    SpecFilterCondition annotation = fc.getAnnotation(SpecFilterCondition.class);
                    res.put(fc, annotation);
                }, f -> {
                    return f.isAnnotationPresent(SpecFilterCondition.class) && !Modifier.isStatic(f.getModifiers());
                }
        );
        return res;
    }

    private Optional<PredicateFactory> convertToPredicate(Field field, SpecFilterCondition condition, Object filter) {
        field.setAccessible(true);
        Object filterValue = ReflectionUtils.getField(field, filter);
        if (filterValue == null) {
            return Optional.empty();
        }

        String property = !condition.property().isEmpty() ? condition.property() : field.getName();
        String[] propertyParts;
        if (property.contains(".")) { // compound
            propertyParts = DOT_PATTERN.split(property);
        } else {
            propertyParts = new String[] {property};
        }

        PredicateFactory res = (root, criteriaBuilder) -> {
            SpecFilterOperator op = condition.operator();
            Expression<?> leftArg = buildLeftArgPath(condition, root, propertyParts);
            Object rightArg = filterValue;

            // handle lowerCase option
            if (op == SpecFilterOperator.EQUALS || op == SpecFilterOperator.NOT_EQUALS || op == SpecFilterOperator.CONTAINS
                    || op == SpecFilterOperator.STARTS_WITH || op == SpecFilterOperator.ENDS_WITH) {
                if (condition.ignoreCase() && filterValue instanceof String) {
                    leftArg = criteriaBuilder.lower((Expression<String>) leftArg);
                    rightArg = ((String) filterValue).toLowerCase();
                }
            }

            Predicate leaf = getPredicateByCondition(criteriaBuilder, op, leftArg, rightArg);
            return leaf;
        };
        return Optional.of(res);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate getPredicateByCondition(CriteriaBuilder cb, SpecFilterOperator op,
                                                     Expression<?> leftArg, Object rightArg) {
        Predicate leaf;
        switch (op) {
            case EQUALS -> {
                leaf = cb.equal(leftArg, rightArg);
            }
            case NOT_EQUALS -> {
                leaf = cb.notEqual(leftArg, rightArg);
            }
            case CONTAINS -> {
                leaf = cb.like((Expression<String>) leftArg, "%" + rightArg + "%");
            }
            case STARTS_WITH -> {
                leaf = cb.like((Expression<String>) leftArg, rightArg + "%");
            }
            case ENDS_WITH -> {
                leaf = cb.like((Expression<String>) leftArg, "%" + rightArg);
            }
            case LESS -> {
                leaf = cb.lessThan((Expression<? extends Comparable>) leftArg, (Comparable) rightArg);
            }
            case LESS_OR_EQUALS -> {
                leaf = cb.lessThanOrEqualTo((Expression<? extends Comparable>) leftArg, (Comparable) rightArg);
            }
            case GREATER -> {
                leaf = cb.greaterThan((Expression<? extends Comparable>) leftArg, (Comparable) rightArg);
            }
            case GREATER_OR_EQUALS -> {
                leaf = cb.greaterThanOrEqualTo((Expression<? extends Comparable>) leftArg, (Comparable) rightArg);
            }
            case IN -> {
                leaf = leftArg.in((Object[]) rightArg);
            }
            case NOT_IN -> {
                leaf = cb.not(leftArg.in((Object[]) rightArg));
            }
            case IS_SET -> {
                leaf = Boolean.TRUE.equals(rightArg) ? leftArg.isNotNull() : leftArg.isNull();
            }
            case IS_NOT_SET -> {
                leaf = Boolean.TRUE.equals(rightArg) ? leftArg.isNull() : leftArg.isNotNull();
            }
            default -> throw new UnsupportedOperationException("Not supported yet: " + op);
        }
        return leaf;
    }

    // build chain call like:
    //   root.join("collectionAttr").get("associationAttr").get("property")
    private static Expression<?> buildLeftArgPath(SpecFilterCondition condition, Root<?> root, String[] propertyParts) {
        Path<?> leftArgPath = condition.joinCollection().isEmpty() ? root : root.join(condition.joinCollection());
        for (String propertyPart : propertyParts) {
            leftArgPath = leftArgPath.get(propertyPart);
        }

        return leftArgPath;
    }

    @FunctionalInterface
    private interface PredicateFactory {
        Predicate getLeafPredicate(Root<?> root, CriteriaBuilder criteriaBuilder);
    }

}
