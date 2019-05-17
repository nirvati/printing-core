/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * XML resource bundle control based on XML property file.
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
public final class XMLResourceBundleControl extends ResourceBundle.Control {

    /** */
    private static final String XML = "xml";

    @Override
    public List<String> getFormats(final String baseName) {
        return Collections.singletonList(XML);
    }

    @Override
    public ResourceBundle newBundle(final String baseName, final Locale locale,
            final String format, final ClassLoader loader, final boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {

        if (baseName == null || locale == null || format == null
                || loader == null) {
            throw new IllegalArgumentException();
        }

        if (!format.equals(XML)) {
            return null;
        }

        final String bundleName = toBundleName(baseName, locale);
        final String resourceName = toResourceName(bundleName, format);
        final URL url = loader.getResource(resourceName);
        if (url == null) {
            return null;
        }

        final URLConnection connection = url.openConnection();
        if (connection == null) {
            return null;
        }
        if (reload) {
            connection.setUseCaches(false);
        }

        final InputStream stream = connection.getInputStream();
        if (stream == null) {
            return null;
        }

        final BufferedInputStream bis = new BufferedInputStream(stream);
        final ResourceBundle bundle = new XMLResourceBundle(bis);
        bis.close();

        return bundle;
    }
}
