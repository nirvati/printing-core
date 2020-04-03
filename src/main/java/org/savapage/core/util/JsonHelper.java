/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonHelper {

    /**
     * {@link ObjectMapper} is thread-safe.
     */
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * .
     */
    private static JsonFactory jsonFactory = new JsonFactory();

    /**
     * Private constructor.
     */
    private JsonHelper() {

    }

    /**
     * Serializes an {@link EnumSet}.
     *
     * @param <E>
     *            The enum type.
     * @param enumSet
     *            The {@link EnumSet}.
     * @return The serialized JSON result.
     */
    public static <E extends Enum<E>> String
            serializeEnumSet(final EnumSet<E> enumSet) {

        final ArrayNode node = mapper.createArrayNode();
        for (final Object value : enumSet.toArray()) {
            node.add(value.toString());
        }

        return node.toString();
    }

    /**
     * De-serializes an {@link EnumSet}.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param json
     *            The serialized JSON string.
     * @return The {@link EnumSet}.
     * @throws IOException
     *             When JSON input is not valid.
     * @throws IllegalArgumentException
     *             When JSON string contains invalid enum value.
     */
    public static <E extends Enum<E>> EnumSet<E> deserializeEnumSet(
            final Class<E> enumClass, final String json) throws IOException {

        final List<String> result =
                mapper.readValue(json, new TypeReference<List<String>>() {
                });

        if (result.isEmpty()) {
            return EnumSet.noneOf(enumClass);
        }

        final Collection<E> collection = new HashSet<>();

        for (final String enumName : result) {
            collection.add(Enum.valueOf(enumClass, enumName));
        }

        return EnumSet.copyOf(collection);
    }

    /**
     * De-serializes an {@link Map} with enum key and boolean value.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map} or {@code null} when JSON input is invalid.
     */
    public static <E extends Enum<E>> Map<E, Boolean>
            createEnumBooleanMapOrNull(final Class<E> enumClass,
                    final String json) {
        try {
            return createEnumBooleanMap(enumClass, json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * De-serializes a {@link Map} with enum key and boolean value.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static <E extends Enum<E>> Map<E, Boolean> createEnumBooleanMap(
            final Class<E> enumClass, final String json) throws IOException {
        /*
         * NOTE: this trick to use an intermediary map is needed cause
         * TypeReference<Map<E, Boolean>> throws ClassCastException exception on
         * the enum key when traversing the map with entrySet.
         */
        final Map<String, Boolean> mapTemp = createStringBooleanMap(json);
        return asEnumBooleanMap(enumClass, mapTemp);
    }

    /**
     * De-serializes a {@link Map} with string key and integer value.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static Map<String, Integer> createStringIntegerMap(final String json)
            throws IOException {
        try {
            return mapper.readValue(json,
                    new TypeReference<Map<String, Integer>>() {
                    });
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * De-serializes a {@link Map} with string key and boolean value.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static Map<String, Boolean> createStringBooleanMap(final String json)
            throws IOException {
        try {
            return mapper.readValue(json,
                    new TypeReference<Map<String, Boolean>>() {
                    });
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * De-serializes a {@link Map} with string key and integer value.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map} or {@code null} when JSON input is invalid.
     */
    public static Map<String, Integer>
            createStringIntegerMapOrNull(final String json) {
        try {
            return createStringIntegerMap(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * De-serializes a {@link Map} with string key and string value.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static Map<String, String> createStringMap(final String json)
            throws IOException {
        try {
            return mapper.readValue(json,
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * De-serializes a {@link Set} of string keys.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Set}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static Set<String> createStringSet(final String json)
            throws IOException {
        try {
            return mapper.readValue(json, new TypeReference<Set<String>>() {
            });
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * De-serializes a {@link Set} of Long keys.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Set}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static Set<Long> createLongSet(final String json)
            throws IOException {
        try {
            return mapper.readValue(json, new TypeReference<Set<Long>>() {
            });
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * De-serializes a {@link Map} with string key and string value.
     *
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map} or {@code null} when JSON input is invalid.
     */
    public static Map<String, String> createStringMapOrNull(final String json) {
        try {
            return createStringMap(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates Enum - Integer map of intermediary string key map.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param mapTemp
     *            The intermediary map.
     * @return The map.
     */
    private static <E extends Enum<E>> Map<E, Integer> asEnumIntegerMap(
            final Class<E> enumClass, final Map<String, Integer> mapTemp) {

        final Map<E, Integer> map = new HashMap<>();

        for (final Entry<String, Integer> entry : mapTemp.entrySet()) {
            if (EnumUtils.isValidEnum(enumClass, entry.getKey())) {
                map.put(EnumUtils.getEnum(enumClass, entry.getKey()),
                        entry.getValue());
            }
        }
        return map;
    }

    /**
     * Creates Enum - Boolean map of intermediary string key map.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param mapTemp
     *            The intermediary map.
     * @return The map.
     */
    private static <E extends Enum<E>> Map<E, Boolean> asEnumBooleanMap(
            final Class<E> enumClass, final Map<String, Boolean> mapTemp) {

        final Map<E, Boolean> map = new HashMap<>();

        for (final Entry<String, Boolean> entry : mapTemp.entrySet()) {
            if (EnumUtils.isValidEnum(enumClass, entry.getKey())) {
                map.put(EnumUtils.getEnum(enumClass, entry.getKey()),
                        entry.getValue());
            }
        }
        return map;
    }

    /**
     * De-serializes a {@link Map} with enum key and integer value.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map}.
     * @throws IOException
     *             When JSON syntax is invalid.
     */
    public static <E extends Enum<E>> Map<E, Integer> createEnumIntegerMap(
            final Class<E> enumClass, final String json) throws IOException {
        /*
         * NOTE: this trick to use an intermediary map is needed cause
         * TypeReference<Map<E, Integer>> throws ClassCastException exception on
         * the enum key when traversing the map with entrySet.
         */
        final Map<String, Integer> mapTemp = createStringIntegerMap(json);
        return asEnumIntegerMap(enumClass, mapTemp);
    }

    /**
     * De-serializes a {@link Map} with enum key and integer value.
     *
     * @param <E>
     *            The enum type.
     * @param enumClass
     *            The enum class.
     * @param json
     *            The serialized JSON string.
     * @return The {@link Map} or {@code null} when JSON input is invalid.
     */
    public static <E extends Enum<E>> Map<E, Integer>
            createEnumIntegerMapOrNull(final Class<E> enumClass,
                    final String json) {
        /*
         * NOTE: this trick to use an intermediary map is needed cause
         * TypeReference<Map<E, Integer>> throws ClassCastException exception on
         * the enum key when traversing the map with entrySet.
         */
        final Map<String, Integer> mapTemp = createStringIntegerMapOrNull(json);
        if (mapTemp == null) {
            return null;
        }
        return asEnumIntegerMap(enumClass, mapTemp);
    }

    /**
     * Creates a bean object from a JSON string, when de-serialization fails
     * (due to syntax errors) {@code null} is returned.
     *
     * @param <E>
     *            The bean type.
     * @param clazz
     *            The bean class.
     * @param json
     *            The JSON String
     * @return The instance or {@code null} when an IO or syntax error occurs.
     */
    public static <E> E createOrNull(final Class<E> clazz, final String json) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates a JSON string from a {@link Map}. Note: the string does NOT
     * contain any whitespace.
     *
     * @param map
     *            The {@link Map}
     * @return The JSON String.
     */
    public static String stringifyStringMap(final Map<String, String> map) {
        try {
            return StringUtils.deleteWhitespace(mapper.writeValueAsString(map));
        } catch (JsonProcessingException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Creates a JSON string from a string {@link Set}. Note: the strings in the
     * set MUST NOT contain any whitespace.
     *
     * @param set
     *            The {@link Set}.
     * @return The JSON String.
     */
    public static String stringifyStringSet(final Set<String> set) {
        try {
            return StringUtils.deleteWhitespace(mapper.writeValueAsString(set));
        } catch (JsonProcessingException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Creates a JSON string from a Long{@link Set}.
     *
     * @param set
     *            The {@link Set}.
     * @return The JSON String.
     */
    public static String stringifyLongSet(final Set<Long> set) {
        try {
            return StringUtils.deleteWhitespace(mapper.writeValueAsString(set));
        } catch (JsonProcessingException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Creates a JSON string from a {@link Map}. Note: the string does NOT
     * contain any whitespace.
     *
     * @param map
     *            The {@link Map}.
     * @return The JSON String.
     * @throws IOException
     *             When serialization fails.
     */
    public static String stringifyObjectMap(final Map<String, Object> map)
            throws IOException {
        return StringUtils.deleteWhitespace(mapper.writeValueAsString(map));
    }

    /**
     * Creates a JSON string from a {@link Map}. Note: whitespace in the string
     * is preserved.
     *
     * @param map
     *            The {@link Map}.
     * @return The JSON String.
     * @throws IOException
     *             When serialization fails.
     */
    public static String objectMapAsString(final Map<String, Object> map)
            throws IOException {
        return mapper.writeValueAsString(map);
    }

    /**
     * Creates a JSON string from an {@link Object}. Note: the string does NOT
     * contain any whitespace.
     *
     * @param object
     *            The {@link Object}.
     * @return The JSON String.
     * @throws IOException
     *             When serialization fails.
     */
    public static String stringifyObject(final Object object)
            throws IOException {
        return StringUtils.deleteWhitespace(mapper.writeValueAsString(object));
    }

    /**
     * Writes POJO as pretty printed JSON.
     *
     * @param pojo
     *            The POJO.
     * @param writer
     *            The {@link Writer}.
     * @throws IOException
     *             When IO error.
     */
    public static void write(final Object pojo, final Writer writer)
            throws IOException {
        final JsonGenerator jg = jsonFactory.createGenerator(writer);
        jg.useDefaultPrettyPrinter();
        mapper.writeValue(jg, pojo);
    }

    /**
     * Creates a pretty printed JSON string without NULL values from an POJO
     * {@link Object}.
     *
     * @param pojo
     *            The POJO.
     * @return The JSON String.
     * @throws IOException
     *             When IO error.
     */
    public static String stringifyObjectPretty(final Object pojo)
            throws IOException {
        // Do not use static class mapper.
        final ObjectMapper mpr = new ObjectMapper();
        final StringWriter w = new StringWriter();
        final JsonGenerator jg = jsonFactory.createGenerator(w);
        jg.useDefaultPrettyPrinter();
        mpr.setSerializationInclusion(Include.NON_NULL);
        mpr.writeValue(jg, pojo);
        return w.toString();
    }

    /**
     * Creates a bean from a JSON file.
     *
     * @param <E>
     *            The bean class.
     * @param clazz
     *            The bean class.
     * @param file
     *            The JSON {@link File}.
     * @return The bean instance.
     * @throws IOException
     *             When IO error.
     */
    public static <E> E read(final Class<E> clazz, final File file)
            throws IOException {
        return mapper.readValue(file, clazz);
    }

}
