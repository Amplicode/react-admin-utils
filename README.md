# Amplicode Utils for React Admin

This library provides several useful utilities helping to implement REST controllers for React Admin-based frontend.

## Compatibility
`react-admin-utils` works as a Spring Boot starter. Current version is compatible with Spring Boot **3.2.x**.

The library is a part of the [Amplicode](https://amplicode.io/) project, however it does not bring additional dependencies and can be used in any Spring Boot project. 

The implementation heavily depends on Jackson (version used by the Spring Web).

## Adding library to the project
Maven:
```
<dependency>
    <groupId>io.amplicode</groupId>
    <artifactId>react-admin-utils-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:
```
dependencies {
    implementation 'io.amplicode:react-admin-utils-starter:0.1.0'
}
```

## ObjectPatcher
#### Patching
The `io.amplicode.rautils.patch.ObjectPatcher` bean helps to implement `PATCH` REST endpoints.

The bean performs patching the passed object under the following set of conditions:
* Real [PATCH](https://datatracker.ietf.org/doc/html/rfc5789) semantics is needed: 
  * patch contains a subset of object attributes to modify
  * attributes not included into the patch are not modified
  * attributes can be reset to null
* Data transfer object is an **immutable** class (POJO with all-args constructor, or a Java 17 record).

Usage:
```java
import com.fasterxml.jackson.databind.JsonNode;
    
    @Autowired
    private ObjectPatcher objectPatcher;
    
    @PatchMapping("/{id}")
    public ResponseEntity<FooDto> patch(@PathVariable Integer id, @RequestBody JsonNode fooDtoPatch) {
        FooDto fooDto; // load current state of the entity
        FooDto patchedDto = objectPatcher.patch(fooDto, fooDtoPatch);

        // save patchedDto to the data store
    }
```

**Note** that if DTO used in the endpoint is **mutable** (e.g. has setters for all attributes), then an existing method from the Jackson library can be used instead of the `ObjectPatcher`:
```java
import com.fasterxml.jackson.databind.JsonNode;

    @Autowired
    private ObjectMapper objectMapper;

    @PatchMapping("/{id}")
    public ResponseEntity<FooDto> patch(@PathVariable Integer id, @RequestBody JsonNode fooDtoPatch) throws IOException {
        FooDto fooDto; // load current state of the entity
        FooDto patchedDto = objectMapper.readerForUpdating(fooDto).readValue(fooDtoPatch);

        // save patchedDto to the data store
    }
```

#### Validation
`PATCH` REST endpoints, unlike endpoints for other HTTP methods, cannot put `@Valid` on the request body argument, because the request body does not contain the full set of attributes that constitute the object to be validated.

Therefore, it is required to validate patched object in the endpoint code. Unfortunately, Spring doesn't provide one-liner API to perform such validation. As a shortcut to perform the validation, `ObjectPatcher` provides several methods:

```java
@Autowired
private ObjectMapper objectMapper;

// just validate
// public void ObjectPatcher#validate(Object target);
objectPatcher.validate(patchedDto);

// patch and validate right away
// public <T> T ObjectPatcher#patchAndValidate(T target, JsonNode patchJson)
patchedDto = objectPatcher.patchAndValidate(sourceDto, patchJsonNode);

// public <T> T ObjectPatcher#patchAndValidate(T target, String patchJson)
patchedDto = objectPatcher.patchAndValidate(sourceDto, patchString);
```

In case of validation failure, these methods throw non-checked exception `io.amplicode.rautils.patch.PatchValidationException` that contains validation errors and is automatically processed by the Spring `org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver` (resulting in `400 Bad Request` response code).
