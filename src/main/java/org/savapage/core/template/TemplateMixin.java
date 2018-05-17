/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.template;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.savapage.core.template.dto.TemplateAppDto;
import org.savapage.core.template.dto.TemplateDto;
import org.savapage.core.util.Messages;
import org.stringtemplate.v4.DateRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.StringRenderer;

/**
 * Base class for all templates.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class TemplateMixin {

    /**
     * Date Renderer with our own locale (ignoring default locale).
     */
    private static class SpDateRenderer extends DateRenderer {

        /** */
        private final Locale myLocale;

        /**
         * Initialize with our own locale.
         *
         * @param locale
         *            The locale.
         */
        SpDateRenderer(final Locale locale) {
            this.myLocale = locale;
        }

        @Override
        public String toString(final Object o, final String formatString,
                final Locale locale) {
            // Ignore (default) locale parameter, use our own.
            return super.toString(o, formatString, this.myLocale);
        }
    }

    /**
     * Delimiter start character for placeholder.
     */
    protected static final char ST_DELIMITER_CHAR_START = '$';

    /**
     * Delimiter stop (end) character for placeholder.
     */
    protected static final char ST_DELIMITER_CHAR_STOP =
            ST_DELIMITER_CHAR_START;

    /**
     * Renders placeholder key/object (Java Bean) pairs.
     *
     * @param locale
     *            The {@link Locale}.
     * @return A map with key (attribute) and Java Bean Objects.
     *         <p>
     *         The template has $attribute.property$ placeholder objects.
     *         StringTemplate engine looks for accessor methods of Bean like
     *         getProperty() or isProperty() or hasProperty(). If that fails,
     *         StringTemplate looks for a raw field of the attribute called
     *         property. Evaluates to the empty string if no such property is
     *         found.
     *         </p>
     */
    protected abstract Map<String, TemplateDto> onRender(final Locale locale);

    /**
     * Renders placeholder key/value pairs.
     *
     * @param rcBundle
     *            The {@link ResourceBundle}.
     * @return A map with placeholder keys and values, or {@code null} when not
     *         applicable.
     */
    protected abstract Map<String, String>
            onRender(final ResourceBundle rcBundle);

    /**
     * Loads a {@link ResourceBundle} from a jar file.
     *
     * @param locale
     *            The {@link Locale}.
     * @return The {@link ResourceBundle}.
     */
    protected final ResourceBundle getResourceBundle(final Locale locale) {
        return Messages.loadXmlResource(this.getClass(),
                this.getClass().getSimpleName(), locale);
    }

    /**
     * Loads a {@link ResourceBundle} from the file system.
     *
     * @param directory
     *            The directory location of the XML resource.
     * @param resourceName
     *            The name of the XML resource without the locale suffix and
     *            file extension.
     * @param locale
     *            The {@link Locale}.
     * @return The {@link ResourceBundle}.
     */
    protected final ResourceBundle getResourceBundle(final File directory,
            final String resourceName, final Locale locale) {
        return Messages.loadXmlResource(directory, resourceName, locale);
    }

    /**
     * Renders single placeholder values.
     *
     * @param rcBundle
     *            The {@link ResourceBundle}.
     * @param tpl
     *            The template object.
     */
    private void render(final ResourceBundle rcBundle, final ST tpl) {

        final Map<String, String> map = this.onRender(rcBundle);

        if (map == null) {
            return;
        }

        for (final Entry<String, String> entry : map.entrySet()) {
            tpl.add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Renders a template.
     *
     * @param rcBundle
     *            The {@link ResourceBundle} for the
     *            {@link #onRender(ResourceBundle)} callback.
     * @param template
     *            The template.
     * @param locale
     *            The {@link Locale}.
     * @return The rendered template with placeholder objects are replaced by
     *         values.
     */
    protected final String render(final ResourceBundle rcBundle,
            final String template, final Locale locale) {

        final ST tpl = new ST(template, ST_DELIMITER_CHAR_START,
                ST_DELIMITER_CHAR_STOP);

        /*
         * The StringRenderer knows to perform a few format operations on String
         * objects: upper, lower, cap, url-encode and xml-encode.
         */
        tpl.groupThatCreatedThisInstance.registerRenderer(String.class,
                new StringRenderer());

        /*
         * The DateRenderer knows to perform a few format operations on Date
         * objects.
         */
        tpl.groupThatCreatedThisInstance.registerRenderer(Date.class,
                new SpDateRenderer(locale));

        final Map<String, TemplateDto> mapBean = this.onRender(locale);

        if (mapBean != null) {
            for (final Entry<String, TemplateDto> entry : mapBean.entrySet()) {
                tpl.add(entry.getKey(), entry.getValue());
            }
        }

        //
        this.render(rcBundle, tpl);

        //
        tpl.add(TemplateAttrEnum.APP.asAttr(), TemplateAppDto.create(locale));

        //
        return tpl.render();
    }

}
