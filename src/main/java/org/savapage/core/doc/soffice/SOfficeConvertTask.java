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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.task.ErrorCodeIOException;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseable;
import com.sun.star.xml.dom.XDocument;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class SOfficeConvertTask implements SOfficeTask {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SOfficeConvertTask.class);

    /**
     * The input file.
     */
    private final File inputFile;

    /**
     * The output file.
     */
    private final File outputFile;

    /**
     * Constructor.
     *
     * @param fileIn
     *            The input file.
     * @param fileOut
     *            The output file.
     */
    public SOfficeConvertTask(final File fileIn, final File fileOut) {
        this.inputFile = fileIn;
        this.outputFile = fileOut;
    }

    /**
     * Gets the load properties of the input file.
     *
     * @param fileIn
     *            the input file.
     * @return The properties.
     */
    protected abstract Map<String, Object> getLoadProperties(final File fileIn);

    /**
     * Gets the store properties of the output file.
     *
     * @param fileOut
     *            The output file.
     * @param document
     *            The UNO document.
     * @return The properties.
     */
    protected abstract Map<String, Object>
            getStoreProperties(final File fileOut, final XComponent document);

    /**
     * Override to modify the document after it has been loaded and before it
     * gets saved in the new format.
     * <p>
     *
     * @param document
     *            The document to modify.
     * @throws SOfficeException
     *             if errors.
     */
    protected abstract void modifyDocument(final XComponent document)
            throws SOfficeException;

    @Override
    public final void execute(final SOfficeContext context)
            throws SOfficeException {

        XComponent document = null;

        try {
            document = loadDocument(context, inputFile);

            modifyDocument(document);

            storeDocument(document, outputFile);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Converted [%s] to [%s] ",
                        inputFile.getName(), outputFile.getName()));
            }
        } catch (SOfficeException e) {
            throw e;

        } catch (Exception e) {
            throw new SOfficeException("Conversion failed.", e);

        } finally {

            if (document != null) {

                final XCloseable closeable =
                        SOfficeHelper.unoCast(XCloseable.class, document);

                if (closeable != null) {
                    try {
                        closeable.close(true);
                    } catch (CloseVetoException closeVetoException) {
                        // whoever raised the veto should close the document
                    }
                } else {
                    document.dispose();
                }
            }
        }
    }

    /**
     * Loads an input document.
     *
     * @param context
     *            The context.
     * @param fileIn
     *            The input file.
     * @return The loaded document as UNO object.
     * @throws SOfficeException
     *             if document could not be loaded.
     */
    private XComponent loadDocument(final SOfficeContext context,
            final File fileIn) throws SOfficeException {

        if (!fileIn.exists()) {
            throw new SOfficeException("input document not found");
        }

        final XComponentLoader loader = SOfficeHelper.unoCast(
                XComponentLoader.class,
                context.getService(SOfficeHelper.UNO_SERVICE_FRAME_DESKTOP));

        final Map<String, Object> loadProperties =
                this.getLoadProperties(fileIn);

        XComponent document = null;

        try {
            document = loader.loadComponentFromURL(SOfficeHelper.toUrl(fileIn),
                    "_blank", 0, SOfficeHelper.toUnoProperties(loadProperties));

        } catch (IllegalArgumentException e) {
            throw new SOfficeException(String.format(
                    "Could not load document: %s", fileIn.getName()), e);

        } catch (ErrorCodeIOException e) {
            throw new SOfficeException(
                    String.format("Could not load document: %s (errorCode %d)",
                            fileIn.getName(), e.ErrCode),
                    e);
        } catch (IOException e) {
            throw new SOfficeException(String.format(
                    "Could not load document: %s", fileIn.getName()), e);
        }
        if (document == null) {
            throw new SOfficeException(String
                    .format("Could not load document: %s", fileIn.getName()));
        }
        return document;
    }

    /**
     * Stores the converted document to file.
     *
     * @param document
     *            The {@link XDocument}.
     * @param fileOut
     *            The file to store the document to.
     * @throws SOfficeException
     *             if error occurs.
     */
    private void storeDocument(final XComponent document, final File fileOut)
            throws SOfficeException {

        final Map<String, Object> storeProperties =
                getStoreProperties(fileOut, document);

        if (storeProperties == null) {
            throw new SOfficeException("Unsupported conversion.");
        }

        try {
            SOfficeHelper.unoCast(XStorable.class, document).storeToURL(
                    SOfficeHelper.toUrl(fileOut),
                    SOfficeHelper.toUnoProperties(storeProperties));

        } catch (ErrorCodeIOException errorCodeIOException) {
            throw new SOfficeException(
                    "Could not store document: " + fileOut.getName()
                            + ": errorCode: " + errorCodeIOException.ErrCode,
                    errorCodeIOException);
        } catch (IOException ioException) {
            throw new SOfficeException(
                    "could not store document: " + fileOut.getName(),
                    ioException);
        }
    }

}
