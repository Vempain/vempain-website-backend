package fi.poltsi.vempain.website.api;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every request and response DTO of the contract is discovered by reflection, filled with a value in every field, serialised with the Jackson 3 mapper the
 * service uses, and read back: the JSON keys must be snake_case, nothing may be lost on the way and unknown properties sent by a newer frontend must be
 * ignored.
 */
class DtoRoundTripJTC {

    private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z0-9]+(_[a-z0-9]+)*$");
    private static final String API_PACKAGE = "fi.poltsi.vempain.website.api";

    private final JsonMapper mapper = JsonMapper.builder()
        .build();

    @Test
    void everyDtoSurvivesASnakeCaseRoundTrip() throws Exception {
        List<Class<?>> dtos = new ArrayList<>(contractClasses("request"));
        dtos.addAll(contractClasses("response"));
        assertFalse(dtos.isEmpty(), "no DTOs discovered");

        for (Class<?> dto : dtos) {
            assertTrue(dto.isAnnotationPresent(JsonIgnoreProperties.class), dto.getSimpleName() + " must ignore unknown properties");
            assertTrue(dto.getAnnotation(JsonIgnoreProperties.class)
                .ignoreUnknown(), dto.getSimpleName() + " must ignore unknown properties");

            Object filled = filled(dto, 1);
            String json = mapper.writeValueAsString(filled);
            JsonNode tree = mapper.readTree(json);

            assertTrue(tree.size() > 0, dto.getSimpleName() + " serialised nothing");
            tree.propertyNames()
                .forEach(key -> assertTrue(SNAKE_CASE.matcher(key)
                    .matches(), dto.getSimpleName() + " writes a non snake_case key: " + key));
            assertEquals(fieldCount(dto), tree.size(), dto.getSimpleName() + " does not serialise every field");

            Object read = mapper.readValue(json, dto);
            assertEquals(filled, read, dto.getSimpleName() + " changed during the round trip");

            Map<String, Object> withUnknown = new LinkedHashMap<>(mapper.readValue(json, Map.class));
            withUnknown.put("added_by_a_newer_frontend", "ignored");
            assertEquals(filled, mapper.readValue(mapper.writeValueAsString(withUnknown), dto), dto.getSimpleName() + " rejects unknown keys");
        }
    }

    @Test
    void emptyDtosSerialiseEveryFieldAsNullOrDefault() throws Exception {
        for (Class<?> dto : contractClasses("response")) {
            Object empty = dto.getDeclaredConstructor()
                .newInstance();
            JsonNode tree = mapper.readTree(mapper.writeValueAsString(empty));

            assertEquals(fieldCount(dto), tree.size(), dto.getSimpleName() + " hides fields when they are empty");
            assertEquals(empty, mapper.readValue(mapper.writeValueAsString(empty), dto));
        }
    }

    /**
     * The DTO classes of a contract package, found through the class path so that a new DTO is covered without listing it here.
     */
    private static List<Class<?>> contractClasses(String subPackage) throws Exception {
        String packageName = API_PACKAGE + "." + subPackage;
        Enumeration<URL> resources = Thread.currentThread()
            .getContextClassLoader()
            .getResources(packageName.replace('.', '/'));
        List<Class<?>> classes = new ArrayList<>();
        for (URL resource : Collections.list(resources)) {
            File[] files = new File(resource.toURI()).listFiles((dir, name) -> name.endsWith(".class")
                                                                               && !name.contains("$"));
            for (File file : Objects.requireNonNull(files)) {
                classes.add(Class.forName(packageName
                        + "."
                        + file.getName()
                            .replace(".class", "")));
            }
        }
        classes.sort((a, b) -> a.getSimpleName()
            .compareTo(b.getSimpleName()));
        assertFalse(classes.isEmpty(), "no classes found in " + packageName);

        return classes;
    }

    private static int fieldCount(Class<?> dto) {
        return (int) Arrays.stream(dto.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .count();
    }

    /**
     * Nesting depth after which nested DTOs stay empty; {@code DirectoryNodeResponse} nests itself through its children.
     */
    private static final int MAX_DEPTH = 3;

    private static Object filled(Class<?> type, int seed) throws Exception {
        Object instance = type.getDeclaredConstructor()
            .newInstance();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            field.set(instance, value(field.getGenericType(), seed));
        }

        return instance;
    }

    private static Object value(Type type, int seed) throws Exception {
        if (type instanceof TypeVariable<?>) {
            // A raw generic DTO such as PagedResponse<T> carries plain strings
            return "element-" + seed;
        }
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            if (List.class.isAssignableFrom(raw)) {
                return seed > MAX_DEPTH ? List.of() : List.of(value(parameterized.getActualTypeArguments()[0], seed + 1));
            }
            if (Map.class.isAssignableFrom(raw)) {
                return Map.of("key_" + seed, "value_" + seed);
            }
            return filledGeneric(raw, parameterized.getActualTypeArguments()[0], seed + 1);
        }
        if (type instanceof Class<?> clazz) {
            if (clazz == String.class) {
                return "text-" + seed;
            }
            if (clazz == Long.class
                || clazz == long.class) {
                return (long) seed;
            }
            if (clazz == Integer.class
                || clazz == int.class) {
                return seed;
            }
            if (clazz == Double.class
                || clazz == double.class) {
                return seed + 0.5;
            }
            if (clazz == Boolean.class
                || clazz == boolean.class) {
                return true;
            }
            if (clazz == BigDecimal.class) {
                return new BigDecimal(seed + ".25");
            }
            if (clazz == LocalDateTime.class) {
                return LocalDateTime.of(2024, 5, 15, 12, seed % 60);
            }
            if (clazz == OffsetDateTime.class) {
                return OffsetDateTime.of(2024, 5, 13, 16, seed % 60, 0, 0, ZoneOffset.UTC);
            }
            if (clazz.getName()
                .startsWith(API_PACKAGE)) {
                return filled(clazz, seed + 1);
            }
        }
        throw new IllegalArgumentException("No sample value for " + type + "; extend the test when a DTO gains a new field type");
    }

    /**
     * A generic DTO such as {@code PagedResponse<T>}: its {@code List<T>} fields receive an element of the actual type argument.
     */
    private static Object filledGeneric(Class<?> raw, Type argument, int seed) throws Exception {
        Object instance = raw.getDeclaredConstructor()
            .newInstance();
        for (Field field : raw.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Type fieldType = field.getGenericType();
            if (fieldType instanceof ParameterizedType parameterized
                && List.class.isAssignableFrom((Class<?>) parameterized.getRawType())) {
                field.set(instance, List.of(value(argument, seed + 1)));
            } else {
                field.set(instance, value(fieldType, seed));
            }
        }

        return instance;
    }

    @Test
    void sampleValueFactoryRefusesUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> value(Thread.class, 1));
    }
}
