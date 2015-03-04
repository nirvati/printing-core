/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.fonts;

import java.io.File;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class FontLocation {

    /**
     * The {@link FontLocation} is loaded on first access of
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        public static final FontLocation INSTANCE = new FontLocation();
    }

    /**
     * Prevent public instantiation.
     */
    private FontLocation() {

    }

    /**
     * @return The absolute classpath of the font base directory with '/'
     *         appended.
     */
    public static String getClassPath() {
        return SingletonHolder.INSTANCE.createClassPath();
    }

    /**
     * @return The absolute classpath of the font base directory with '/'
     *         appended.
     */
    private String createClassPath() {

        final Class<?> referenceClass = FontLocation.class;

        final StringBuilder buffer = new StringBuilder();

        buffer.append(File.separatorChar);
        buffer.append(referenceClass.getPackage().getName()
                .replace('.', File.separatorChar));
        buffer.append(File.separatorChar);
        buffer.append("truetype");
        buffer.append(File.separatorChar);

        return buffer.toString();
    }

}
