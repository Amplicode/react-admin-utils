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

package io.amplicode.autoconfigure.rautils;

import io.amplicode.rautils.ReactAdminUtilsConfiguration;
import io.amplicode.rautils.filter.SpecificationFilterConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

@AutoConfiguration
@Import({ReactAdminUtilsConfiguration.class})
public class ReactAdminUtilsAutoConfiguration {

    @Bean
    @ConditionalOnClass(Specification.class)
    public SpecificationFilterConverter specificationFilterConverter() {
        return new SpecificationFilterConverter();
    }
}
