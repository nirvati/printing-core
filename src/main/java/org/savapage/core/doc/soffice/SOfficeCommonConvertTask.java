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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.sun.star.lang.XComponent;
import com.sun.star.util.XRefreshable;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeCommonConvertTask extends SOfficeConvertTask {

    /**
     * The format of the output document.
     */
    private final SOfficeDocFormat outputFormat;

    /**
     *
     */
    private Map<String, Object> defaultLoadProperties;

    /**
     * .
     */
    private SOfficeDocFormat inputFormat;

    /**
     *
     * @param inputFile
     *            The input file to convert.
     * @param outputFile
     *            The resulting file.
     * @param outputFormat
     *            The output format.
     */
    public SOfficeCommonConvertTask(final File inputFile, final File outputFile,
            final SOfficeDocFormat outputFormat) {
        super(inputFile, outputFile);
        this.outputFormat = outputFormat;
    }

    /**
     *
     * @param properties
     *            The default load properties.
     */
    public void setDefaultLoadProperties(final Map<String, Object> properties) {
        this.defaultLoadProperties = properties;
    }

    /**
     *
     * @param format
     *            The format of the input document.
     */
    public void setInputFormat(final SOfficeDocFormat format) {
        this.inputFormat = format;
    }

    @Override
    protected void modifyDocument(final XComponent document)
            throws SOfficeException {

        final XRefreshable refreshable =
                SOfficeHelper.unoCast(XRefreshable.class, document);

        if (refreshable != null) {
            refreshable.refresh();
        }
    }

    @Override
    protected Map<String, Object> getLoadProperties(final File inputFile) {

        final Map<String, Object> loadProperties =
                new HashMap<String, Object>();

        if (defaultLoadProperties != null) {
            loadProperties.putAll(defaultLoadProperties);
        }

        if (inputFormat != null && inputFormat.getLoadProperties() != null) {
            loadProperties.putAll(inputFormat.getLoadProperties());
        }

        return loadProperties;
    }

    @Override
    protected Map<String, Object> getStoreProperties(final File outputFile,
            final XComponent document) {

        final SOfficeDocFamilyEnum family =
                SOfficeHelper.getDocumentFamily(document);

        return outputFormat.getStoreProperties(family);
    }

}
