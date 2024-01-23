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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.amplicode.rautils.ReactAdminUtilsTestConfiguration;
import jakarta.validation.constraints.Digits;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest
@ContextConfiguration(classes = ReactAdminUtilsTestConfiguration.class)
public class ObjectPatcherTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ObjectPatcher objectPatcher;

    @Test
    void checkRecord() {
        String json = """
            {"id": 1, "telephone": 155, "city": null}
        """;

		RecordDto dto = new RecordDto(1, "109", "Samara", 7927714);

		dto = objectPatcher.patch(dto, json);

        System.out.println(dto);

		assertEquals(155, dto.telephone()); // changed
		assertNull(dto.city()); // reset to null
		assertEquals("109", dto.address()); // not touched
    }

	@Disabled("postpone investigating problem for a while")
	@Test
	void checkMixedDto() {
		String json = """
            {"id": 1, "telephone": 155, "city": null, "name": "Alex"}
        """;

		MixedDto dto = new MixedDto(1, "109", 79277);
		dto.setCity("Samara");

		dto = objectPatcher.patch(dto, json);

		System.out.println(dto);

		assertEquals(155, dto.getTelephone()); // changed via constructor
		assertNull(dto.getCity()); // reset to null
		assertEquals("109", dto.getAddress()); // not touched
		assertEquals("Alex", dto.getName()); // changed via setter
	}

	@Test
	void checkMutableDto() {
		String json = """
            {"id": 1, "telephone": 155, "city": null}
        """;

		MutableDto dto = new MutableDto();
		dto.setId(1);
		dto.setAddress("109");
		dto.setTelephone(79277);
		dto.setCity("Samara");

		dto = objectPatcher.patch(dto, json);

		System.out.println(dto);

		assertEquals(155, dto.getTelephone()); // changed
		assertNull(dto.getCity()); // reset to null
		assertEquals("109", dto.getAddress()); // not touched
	}

	@Test
	void checkValidate() {
		ProductDto dto = new ProductDto(1, "12347");

		// invalid code value
		String json1 = """
            {"id": 1, "code": "12345678"}
        """;

		assertThrows(PatchValidationException.class, () -> {
			objectPatcher.patchAndValidate(dto, json1);
		});

		String json2 = """
            {"id": 1, "code": "12345"}
        """;
		ProductDto patched = objectPatcher.patchAndValidate(dto, json2);
		assertEquals("12345", patched.code());
	}

	public record ProductDto(Integer id, @Digits(integer = 5, fraction = 0) String code) {
	}

	public record RecordDto(Integer id, String address,
						   String city, Integer telephone) {
	}

	public static final class MixedDto {
		private final Integer id;
		private final String address;
		private String name;
		private String city;
		private final Integer telephone;

		public MixedDto(Integer id, String address, Integer telephone) {
			this.id = id;
			this.address = address;
			this.telephone = telephone;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public Integer getId() {
			return id;
		}

		public String getAddress() {
			return address;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCity() {
			return city;
		}

		public Integer getTelephone() {
			return telephone;
		}

		@Override
		public String toString() {
			return "MyOwnerDto{" +
				"id=" + id +
				", address='" + address + '\'' +
				", city='" + city + '\'' +
				", name='" + name + '\'' +
				", telephone='" + telephone + '\'' +
				'}';
		}
	}

	public static final class MutableDto {
		private Integer id;
		private String address;
		private String city;
		private Integer telephone;

		public void setCity(String city) {
			this.city = city;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public void setTelephone(Integer telephone) {
			this.telephone = telephone;
		}

		public Integer getId() {
			return id;
		}

		public String getAddress() {
			return address;
		}

		public String getCity() {
			return city;
		}

		public Integer getTelephone() {
			return telephone;
		}

		@Override
		public String toString() {
			return "MyOwnerDto{" +
				"id=" + id +
				", address='" + address + '\'' +
				", city='" + city + '\'' +
				", telephone='" + telephone + '\'' +
				'}';
		}
	}
}
