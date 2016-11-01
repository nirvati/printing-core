/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.doc.soffice;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeDocFormat {

    /**
     *
     */
    private String name;

    /**
     *
     */
    private String extension;

    /**
     *
     */
    private String mediaType;

    /**
     *
     */
    private SOfficeDocFamilyEnum inputFamily;

    /**
     *
     */
    private Map<String, Object> loadProps;

    /**
     *
     */
    private Map<SOfficeDocFamilyEnum, Map<String, Object>> storePropsByFamily;

    /**
     *
     */
    public SOfficeDocFormat() {
        // default
    }

    /**
     * Constructor.
     *
     * @param name
     * @param extension
     * @param mediaType
     */
    public SOfficeDocFormat(final String name, final String extension,
            final String mediaType) {
        this.name = name;
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public SOfficeDocFamilyEnum getInputFamily() {
        return inputFamily;
    }

    public void setInputFamily(SOfficeDocFamilyEnum documentFamily) {
        this.inputFamily = documentFamily;
    }

    public Map<String, Object> getLoadProperties() {
        return loadProps;
    }

    public void setLoadProperties(Map<String, Object> loadProperties) {
        this.loadProps = loadProperties;
    }

    public Map<SOfficeDocFamilyEnum, Map<String, Object>>
            getStorePropertiesByFamily() {
        return storePropsByFamily;
    }

    public void setStorePropertiesByFamily(
            Map<SOfficeDocFamilyEnum, Map<String, Object>> storePropMap) {
        this.storePropsByFamily = storePropMap;
    }

    public void putStoreProperties(SOfficeDocFamilyEnum family,
            Map<String, Object> storeProperties) {

        if (storePropsByFamily == null) {
            storePropsByFamily =
                    new HashMap<SOfficeDocFamilyEnum, Map<String, Object>>();
        }
        storePropsByFamily.put(family, storeProperties);
    }

    /**
     *
     * @param family
     * @return {@code null} when no properties found.
     */
    public Map<String, Object>
            getStoreProperties(final SOfficeDocFamilyEnum family) {

        if (storePropsByFamily == null) {
            return null;
        }
        return storePropsByFamily.get(family);
    }

}
