/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * XML resource bundle based on XML property file.
 *
 * <p>
 * Copied from
 * <a href="http://www.java2s.com/Code/Java/JDK-6/XMLresourcebundle.htm">this
 * example</a>.
 * </p>
 *
 * @see The <a href=
 *      "http://docs.oracle.com/javase/7/docs/api/java/util/ResourceBundle.Control.html"
 *      >ResourceBundle.Control<a> API doc.
 *
 * @author Rijk Ravestein
 *
 */
class XMLResourceBundle extends ResourceBundle {
    private final Properties props;

    XMLResourceBundle(InputStream stream) throws IOException {
        props = new Properties();
        props.loadFromXML(stream);
    }

    @Override
    protected Object handleGetObject(String key) {
        return props.getProperty(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set<String> handleKeys = props.stringPropertyNames();
        return Collections.enumeration(handleKeys);
    }
}