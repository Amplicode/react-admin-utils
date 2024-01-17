package com.amplicode.restutils.filter;

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
