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

import org.apache.commons.io.FilenameUtils;
import org.savapage.core.services.SOfficeService;

import com.sun.star.document.UpdateDocMode;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeDocConverter {

    /**
     * The manager.
     */
    private final SOfficeService officeManager;

    /**
     * The document format registry.
     */
    private final SOfficeDocFormatRegistry formatRegistry;

    /**
     * The default load properties.
     */
    private Map<String, Object> defaultLoadProperties =
            createDefaultLoadProperties();

    /**
     * Constructor.
     *
     * @param manager
     *            The manager.
     */
    public SOfficeDocConverter(final SOfficeService manager) {
        this(manager, new SOfficeDocFormatRegistryDefault());
    }

    /**
     * Constructor.
     *
     * @param manager
     *            The manager.
     * @param registry
     *            The document format registry.
     */
    public SOfficeDocConverter(final SOfficeService manager,
            final SOfficeDocFormatRegistry registry) {
        this.officeManager = manager;
        this.formatRegistry = registry;
    }

    /**
     * Creates the default load properties.
     *
     * @return the properties.
     */
    private static Map<String, Object> createDefaultLoadProperties() {

        final Map<String, Object> loadProperties =
                new HashMap<String, Object>();
        loadProperties.put("Hidden", true);
        loadProperties.put("ReadOnly", true);
        loadProperties.put("UpdateDocMode", UpdateDocMode.QUIET_UPDATE);

        return loadProperties;
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
     * @return the format registry.
     */
    public SOfficeDocFormatRegistry getFormatRegistry() {
        return formatRegistry;
    }

    /**
     * Converts a file.
     *
     * @param inputFile
     *            input file.
     * @param outputFile
     *            output file
     * @throws SOfficeBusyException
     *             if service is too busy.
     * @throws SOfficeTaskTimeoutException
     *             When task did not complete within time.
     */
    public void convert(final File inputFile, final File outputFile)
            throws SOfficeBusyException, SOfficeTaskTimeoutException {

        final String outputExtension =
                FilenameUtils.getExtension(outputFile.getName());

        final SOfficeDocFormat outputFormat =
                formatRegistry.getFormatByExtension(outputExtension);

        convert(inputFile, outputFile, outputFormat);
    }

    /**
     *
     * @param inputFile
     *            input file.
     * @param outputFile
     *            output file
     * @param outputFormat
     *            the output format.
     * @throws SOfficeBusyException
     *             if service is too busy.
     * @throws SOfficeTaskTimeoutException
     *             When task did not complete within time.
     */
    public void convert(final File inputFile, final File outputFile,
            final SOfficeDocFormat outputFormat)
            throws SOfficeBusyException, SOfficeTaskTimeoutException {

        final String inputExtension =
                FilenameUtils.getExtension(inputFile.getName());

        final SOfficeDocFormat inputFormat =
                formatRegistry.getFormatByExtension(inputExtension);

        final SOfficeCommonConvertTask conversionTask =
                new SOfficeCommonConvertTask(inputFile, outputFile,
                        outputFormat);

        conversionTask.setDefaultLoadProperties(defaultLoadProperties);
        conversionTask.setInputFormat(inputFormat);

        officeManager.execute(conversionTask);
    }

}
