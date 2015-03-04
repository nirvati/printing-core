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

import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.Messages;

import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;

/**
 * Internal Fonts packaged with SavaPage.
 *
 * @author Datraverse B.V.
 *
 */
public enum InternalFontFamilyEnum {

    /**
     * The default font.
     */
    DEFAULT("DejaVu Sans", "dejavu/DejaVuSans.ttf"),

    /**
     * Chinese, Japanese and Korean.
     */
    CJK("Droid Sans Fallback", "droid/DroidSansFallbackFull.ttf"),

    /**
     * GNU Unifont.
     */
    UNIFONT("Unifont", "misc/unifont.ttf");

    /**
     * The font name.
     */
    private final String fontName;

    /**
     * File name relative to the font directory.
     */
    private final String fileName;

    /**
     *
     * @param fontName
     *            The font name.
     * @param fileName
     *            The file name relative to the font directory.
     */
    private InternalFontFamilyEnum(final String fontName, final String fileName) {
        this.fontName = fontName;
        this.fileName = fileName;
    }

    /**
     * Gets the Font Family name as defined in {@code fonts.xml} and to be used
     * as font name in the JasperReport API.
     * <p>
     * See e.g. {@link JRDesignStyle#setFontName(String)} or {
     * {@link JRDesignTextField#setFontName(String)}.
     * </p>
     *
     * @return The JasperReport font name.
     */
    public String getJrName() {
        return fontName;
    }

    /**
     * Gets the font name.
     *
     * @return The name of the font.
     */
    public String getName() {
        return Messages.getMessage(getClass(), ServiceContext.getLocale(),
                "FONT_" + this.toString());
    }

    /**
     *
     * @return The path of the font file.
     */
    public String fontFilePath() {
        return FontLocation.getClassPath() + this.fileName;
    }

}
