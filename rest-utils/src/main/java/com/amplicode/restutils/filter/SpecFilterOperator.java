package com.amplicode.restutils.filter;

/**
 * Defines filter condition operator.
 * @see SpecFilterCondition
 */
public enum SpecFilterOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    LESS,
    LESS_OR_EQUALS,
    GREATER,
    GREATER_OR_EQUALS,
    IN,
    NOT_IN,
    IS_SET,
    IS_NOT_SET
}
