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
