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
package org.savapage.core.fonts;

import java.io.File;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class FontLocation {

    private static final String TRUETYPE_DIRECTORY = "truetype";

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
     * @return The absolute classpath of the truetype font base directory with
     *         '/' appended.
     */
    public static String getClassPath() {
        return SingletonHolder.INSTANCE.createClassPath();
    }

    /**
     * @return The absolute classpath of the truetype font base directory with
     *         '/' appended.
     */
    private String createClassPath() {

        final Class<?> referenceClass = FontLocation.class;

        final StringBuilder path = new StringBuilder();

        path.append(File.separatorChar);
        path.append(referenceClass.getPackage().getName().replace('.',
                File.separatorChar));
        path.append(File.separatorChar);
        path.append(TRUETYPE_DIRECTORY);
        path.append(File.separatorChar);

        return path.toString();
    }

    /**
     * Checks if font file is present.
     *
     * @param font
     *            The {@link InternalFontFamilyEnum}.
     * @return {@code true} if the font file is present.
     */
    public static boolean isFontPresent(final InternalFontFamilyEnum font) {

        final StringBuilder path = new StringBuilder();

        path.append(TRUETYPE_DIRECTORY).append(File.separatorChar)
                .append(font.getRelativePath());

        return FontLocation.class.getResource(path.toString()) != null;
    }

}
