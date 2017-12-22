/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.template.email;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.activation.DataSource;
import javax.activation.URLDataSource;

import org.apache.commons.lang3.EnumUtils;
import org.savapage.core.template.TemplateMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class EmailTemplateMixin extends TemplateMixin {

    protected static final String RESOURCE_KEY_STATIONARY = "stationary";
    protected static final String RESOURCE_KEY_SUBJECT = "subject";
    protected static final String RESOURCE_KEY_HEADER = "header";
    protected static final String RESOURCE_KEY_HTML = "html";
    protected static final String RESOURCE_KEY_TEXT = "text";

    private static final String RESOURCE_KEY_PFX_CID = "cid_";

    /**
    *
    */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailTemplateMixin.class);

    /**
     *
     */
    private static final String FILE_XML_EXT = "xml";

    /**
     * Content-ID map.
     */
    private ContentIdMap cidMap;

    /**
     * The directory with the custom Email template files.
     */
    private final File customDirectory;

    /**
     * @param customHome
     *            The directory with the custom Email template files.
     * @param locale
     */
    protected EmailTemplateMixin(final File customHome) {
        super();
        this.customDirectory = customHome;
    }

    /**
     *
     * @return {@code null} when not present.
     */
    private ContentIdMap getCidMap() {
        return this.cidMap;
    }

    /**
     * @return {@code true} when CID map is present.
     */
    private boolean hasCidMap() {
        return this.cidMap != null;
    }

    /**
     *
     */
    private void createCidMap() {
        this.cidMap = new ContentIdMap();
    }

    @Override
    protected final Map<String, String>
            onRender(final ResourceBundle rcBundle) {

        if (!hasCidMap()) {
            return null;
        }

        final Map<String, String> map = new HashMap<>();

        final Enumeration<String> keys = rcBundle.getKeys();

        while (keys.hasMoreElements()) {

            final String key = keys.nextElement();

            if (!key.startsWith(RESOURCE_KEY_PFX_CID)) {
                continue;
            }

            final String resourceName = rcBundle.getString(key);

            final DataSource dataSrc;

            final EmailCidEnum cidEnum =
                    EnumUtils.getEnum(EmailCidEnum.class, resourceName);

            if (cidEnum == null) {

                final File resourceFile =
                        Paths.get(this.customDirectory.getAbsolutePath(),
                                resourceName).toFile();

                if (resourceFile.exists()) {

                    DataSource dataSrcWrk = null;

                    try {
                        dataSrcWrk =
                                new URLDataSource(resourceFile.toURI().toURL());
                    } catch (MalformedURLException e) {
                        LOGGER.error(
                                String.format("URL for File [%s] is malformed.",
                                        resourceFile.getAbsolutePath()));
                    }

                    dataSrc = dataSrcWrk;

                } else {
                    LOGGER.error(String.format("File [%s] does not exist",
                            resourceFile.getAbsolutePath()));
                    dataSrc = null;
                }
            } else {
                dataSrc = cidEnum.getDataSource();
            }

            if (dataSrc != null) {
                map.put(key, this.cidMap.addSource(dataSrc));
            }
        }

        return map;
    }

    /**
     * Gets the custom ResourceBundle of a resource.
     *
     * @param resourceName
     *            The resource name.
     * @param locale
     *            The {@link Locale}.
     * @return {@code null} when not present.
     */
    private ResourceBundle getCustomResourceBundle(final String resourceName,
            final Locale locale) {

        if (Paths
                .get(this.customDirectory.getAbsolutePath(),
                        String.format("%s.%s", resourceName, FILE_XML_EXT))
                .toFile().exists()) {
            return this.getResourceBundle(this.customDirectory, resourceName,
                    locale);
        }
        return null;
    }

    /**
     *
     * @param rcBundle
     * @param locale
     *            The {@link Locale}.
     * @param asHtml
     * @return The rendered result.
     */
    private EmailRenderResult render(final ResourceBundle rcBundle,
            final Locale locale, final boolean asHtml) {

        final EmailRenderResult renderResult;

        final String key;

        if (asHtml) {
            key = RESOURCE_KEY_HTML;
            this.createCidMap();
        } else {
            key = RESOURCE_KEY_TEXT;
        }

        final String content =
                this.render(rcBundle, rcBundle.getString(key), locale);

        // Embed in stationary?
        if (rcBundle.containsKey(RESOURCE_KEY_STATIONARY)) {

            final String header;

            if (rcBundle.containsKey(RESOURCE_KEY_HEADER)) {
                header = this.render(rcBundle,
                        rcBundle.getString(RESOURCE_KEY_HEADER), locale);
            } else {
                header = "";
            }

            final EmailTemplateMixin tpl =
                    new EmailStationary(this.customDirectory, header, content);

            final String stationary =
                    rcBundle.getString(RESOURCE_KEY_STATIONARY);

            final ResourceBundle rbStationary =
                    this.getCustomResourceBundle(stationary, locale);

            if (rbStationary == null) {
                renderResult = tpl.render(locale, asHtml);
            } else {
                // recurse
                renderResult = tpl.render(rbStationary, locale, asHtml);
            }

            if (tpl.hasCidMap() && this.hasCidMap()) {
                this.getCidMap().addAll(tpl.getCidMap());
            }

        } else {
            renderResult = new EmailRenderResult();
            renderResult.setBody(content);
        }

        renderResult.setCidMap(getCidMap());

        if (rcBundle.containsKey(RESOURCE_KEY_SUBJECT)) {
            renderResult.setSubject(this.render(rcBundle,
                    rcBundle.getString(RESOURCE_KEY_SUBJECT), locale));
        }
        return renderResult;
    }

    /**
     * Renders the template.
     *
     * @param locale
     *            The {@link Locale}.
     * @param asHtml
     *            If {@code true} rendered as HTML, otherwise as plain text
     * @return The rendered template.
     */
    public final EmailRenderResult render(final Locale locale,
            final boolean asHtml) {

        ResourceBundle rb = this.getCustomResourceBundle(
                this.getClass().getSimpleName(), locale);

        if (rb == null) {
            rb = this.getResourceBundle(locale);
        }

        return this.render(rb, locale, asHtml);
    }

}
