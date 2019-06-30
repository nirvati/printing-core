/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Author: Rijk Ravestein.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.json;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract class for JSON objects.
 * <p>
 * References:
 * <ul>
 * <li><a
 * href="http://wiki.fasterxml.com/JacksonDocumentation">http://wiki.fasterxml
 * .com/JacksonDocumentation</a></li>
 * <li><a
 * href="http://wiki.fasterxml.com/JacksonInFiveMinutes">http://wiki.fasterxml
 * .com/JacksonInFiveMinutes</a></li>
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public abstract class JsonAbstractBase {

    /**
     * {@link ObjectMapper} is thread-safe.
     */
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     *
     */
    private static JsonFactory jsonFactory = new JsonFactory();

    /**
     *
     * @return The {@link ObjectMapper}.
     */
    protected static ObjectMapper getMapper() {
        return mapper;
    }

    /**
     *
     * @return The JSON string of this object.
     * @throws IOException
     *             When something goes wrong.
     */
    public final String stringify() throws IOException {
        return mapper.writeValueAsString(this);
    }

    /**
     * Generates a "pretty printed" JSON schema of this object.
     *
     * @return The JSON schema.
     * @throws IOException
     *             When serialization fails.
     */
    public final String generateJsonSchema() throws IOException {
        return prettyPrinted(mapper.generateJsonSchema(this.getClass()));
    }

    /**
     *
     * @return The pretty-printed JSON String.
     * @throws IOException
     *             When serialization fails.
     */
    public final String stringifyPrettyPrinted() throws IOException {
        return prettyPrinted(this);
    }

    /**
     * Returns a pretty printed version of a plain JSON string.
     *
     * @param plainJson
     *            The plain JSON string.
     * @return The pretty-printed JSON.
     * @throws IOException
     *             When serialization fails.
     */
    public static String prettyPrint(final String json) throws IOException {
        final Object obj = mapper.readValue(json, Object.class);
        return prettyPrinted(obj);
    }

    /**
     * Gets the pretty printed string of a POJO.
     *
     * @param pojo
     *            The POJO.
     * @return The pretty-printed JSON String.
     * @throws IOException
     *             When serialization fails.
     */
    protected static String prettyPrinted(final Object pojo) throws IOException {

        final StringWriter sw = new StringWriter();
        final JsonGenerator jg = jsonFactory.createJsonGenerator(sw);

        jg.useDefaultPrettyPrinter();
        mapper.writeValue(jg, pojo);
        return sw.toString();
    }

    /**
     * Creates a bean object from a JSON string.
     * <p>
     * An unchecked exception is thrown on IO and syntax errors.
     * </p>
     *
     * @param <E>
     *            The bean class.
     * @param clazz
     *            The bean class.
     * @param json
     *            The JSON String
     * @return The instance.
     */
    public static <E> E create(final Class<E> clazz, final String json) {
        try {
            return getMapper().readValue(json, clazz);
        } catch (IOException e) {
            throw new SpException(e);
        }
    }

    /**
     * Gets the truncated day of a {@link Long} date.
     *
     * @param date
     *            The milliseconds date as used in {@link Date#Date(long)}.
     * @return The {@link Date} value truncated as {@link Calendar#DAY_OF_MONTH
     *         }
     *         .
     */
    public final Date dayOfMonth(final Long date) {
        return DateUtils.truncate(new Date(date.longValue()),
                Calendar.DAY_OF_MONTH);
    }

}
