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
package org.savapage.core.doc;

import java.io.File;

import org.savapage.core.SpException;
import org.savapage.core.UnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractDocFileConverter extends AbstractFileConverter
        implements IDocFileConverter {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractDocFileConverter.class);

    /**
     *
     * @param execMode
     *            The {@link ExecMode}.
     */
    protected AbstractDocFileConverter(final ExecMode execMode) {
        super(execMode);
    }

    @Override
    public final File convert(final DocContentTypeEnum contentType,
            final File fileIn)
            throws DocContentToPdfException, UnavailableException {

        final File filePdf = getOutputFile(fileIn);
        final String command = getOsCommand(contentType, fileIn, filePdf);

        if (command == null) {
            return convertWithService(contentType, fileIn, filePdf);
        }

        return convertWithOsCommand(contentType, fileIn, filePdf, command);
    }

    /**
     * Performs a custom conversion.
     *
     * @param contentType
     *            The type of input file.
     * @param fileIn
     *            The file to convert.
     * @param fileOut
     *            The output file.
     */
    protected void convertCustom(final DocContentTypeEnum contentType,
            final File fileIn, final File fileOut)
            throws DocContentToPdfException, UnavailableException {
        throw new SpException("Method not implemented");
    }

    /**
     * Performs a conversion using a service.
     *
     * @param contentType
     *            The type of input file.
     * @param fileIn
     *            The file to convert.
     * @param filePdf
     *            The output file.
     * @return The output file.
     * @throws DocContentToPdfException
     *             if error.
     * @throws UnavailableException
     *             When service is unavailable.
     */
    private File convertWithService(final DocContentTypeEnum contentType,
            final File fileIn, final File filePdf)
            throws DocContentToPdfException, UnavailableException {

        final String pdfName = filePdf.getAbsolutePath();

        boolean pdfCreated = false;

        try {
            if (this.getExecMode() == ExecMode.SINGLE_THREADED) {
                synchronized (this) {
                    convertCustom(contentType, fileIn, filePdf);
                }
            } else {
                convertCustom(contentType, fileIn, filePdf);
            }

            pdfCreated = true;

            if (filePdf.exists()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[" + pdfName + "] created.");
                }
            } else {
                LOGGER.error("[" + pdfName + "] NOT created.");
                throw new DocContentToPdfException("PDF is not created");
            }

        } finally {
            if (!pdfCreated) {
                File file2Delete = new File(pdfName);
                if (file2Delete.exists()) {
                    file2Delete.delete();
                }
            }
        }
        return filePdf;
    }

}
