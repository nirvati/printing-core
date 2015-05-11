/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.reports;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;

import org.savapage.core.dto.JrPageLayoutDto;
import org.savapage.core.fonts.InternalFontFamilyEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractJrDesign {

    /**
     *
     */
    private final Locale locale;

    /**
    *
    */
    protected static final String JR_DEFAULT_BASE_STYLE_NAME = "Base";

    /**
     *
     */
    protected static final int HEADER_IMAGE_HEIGHT = 72;

    /**
     *
     */
    protected static final int HEADER_IMAGE_WIDTH = 72;

    /**
     *
     * @param locale
     *            The {@link Locale}.
     */
    protected AbstractJrDesign(final Locale locale) {
        this.locale = locale;
    }

    /**
     *
     * @return The name of the JasperReports resource bundle.
     */
    protected static String getResourceBundleBaseName() {
        return AbstractJrDesign.class.getPackage().getName()
                + ".JrMessages";
    }

    /**
     * Gets the {@link InputStream} for a Jasper Report JRXML template.
     *
     * @param templateName
     *            The base name of the template without the {@code .jrxml}
     *            suffix.
     * @return The {@link InputStream}.
     */
    public static InputStream getJrxmlAsStream(final String templateName) {

        final StringBuilder resource = new StringBuilder(64);

        resource.append(File.separatorChar)
                .append(AbstractJrDesign.class.getPackage().getName()
                        .replace('.', File.separatorChar))
                .append(File.separatorChar).append("template")
                .append(File.separatorChar).append(templateName)
                .append(".jrxml");

        return AbstractJrDesign.class.getResourceAsStream(resource.toString());
    }

    /**
     *
     * @return The layout.
     */
    protected abstract JrPageLayoutDto getLayout();

    /**
     *
     * @return The URL of the header image.
     */
    public static URL getHeaderImage() {
        return AbstractJrDesign.class.getResource("report-header.png");
    }

    /**
     *
     * @param defaultFontName
     *            The default font.
     * @return The {@link JRDesignStyle}.
     */
    protected static JRDesignStyle createDefaultBaseStyle(
            final InternalFontFamilyEnum defaultFontName) {

        JRDesignStyle baseStyle = new JRDesignStyle();

        baseStyle.setDefault(true);
        baseStyle.setName(JR_DEFAULT_BASE_STYLE_NAME);
        baseStyle.setFontName(defaultFontName.getJrName());

        return baseStyle;
    }

    /**
     * Adds a {@link JRDesignTextField} to the {@link JRDesignBand}.
     *
     * @param band
     *            The {@link JRDesignBand}.
     * @param expression
     * @param posX
     * @param posY
     * @param fieldWidth
     * @param fieldHeight
     * @param alignH
     * @param alignV
     * @return The {@link JRDesignTextField}.
     * @throws JRException
     */
    protected static JRDesignTextField addDesignTextField(
            final JRDesignBand band, final String expression, final int posX,
            final int posY, final int fieldWidth, final int fieldHeight,
            final HorizontalAlignEnum alignH, final VerticalAlignEnum alignV)
            throws JRException {

        final JRDesignTextField textField = new JRDesignTextField();

        textField.setX(posX);
        textField.setY(posY);

        textField.setWidth(fieldWidth);
        textField.setHeight(fieldHeight);

        textField.setHorizontalAlignment(alignH);
        textField.setVerticalAlignment(alignV);
        textField.setExpression(new JRDesignExpression(expression));

        band.addElement(textField);

        return textField;
    }

    /**
     * Adds {@link JRDesignParameter} objects to the {@link JasperDesign}.
     *
     * @param jasperDesign
     *            The the {@link JasperDesign}.
     * @param parmNames
     *            Array of parameter names.
     * @param clazz
     *            The class of the parameters.
     * @throws JRException
     *             When report errors.
     */
    protected static void addParameters(final JasperDesign jasperDesign,
            final String[] parmNames, final Class<?> clazz) throws JRException {

        for (String name : parmNames) {
            JRDesignParameter parameter = new JRDesignParameter();
            parameter.setName(name);
            parameter.setValueClass(clazz);
            jasperDesign.addParameter(parameter);
        }
    }

    /**
     * Adds {@link JRDesignField} objects to the {@link JasperDesign}.
     *
     * @param jasperDesign
     *            The the {@link JasperDesign}.
     * @param fieldNames
     *            Array of field names.
     * @param clazz
     *            The class of the fields.
     * @throws JRException
     *             When report errors.
     */
    protected static void addFields(final JasperDesign jasperDesign,
            final String[] fieldNames, final Class<?> clazz) throws JRException {
        for (String name : fieldNames) {
            JRDesignField field = new JRDesignField();
            field.setName(name);
            field.setValueClass(clazz);
            jasperDesign.addField(field);
        }

    }

    /**
     *
     * @return The {@link Locale}.
     */
    public final Locale getLocale() {
        return locale;
    }

}
