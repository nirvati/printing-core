/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractFileConverter {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractFileConverter.class);

    /**
     * .
     */
    protected enum ExecMode {
        /**
         * ONE call to {@link ICommandExecutor#executeCommand()} can be
         * performed at a point in time.
         */
        SINGLE_THREADED,

        /**
         * MULTIPLE calls to {@link ICommandExecutor#executeCommand()} can be
         * performed concurrently.
         */
        MULTI_THREADED
    }

    /**
     * .
     */
    protected enum ExecType {
        /**
         * Uses separate threads for capturing stdout and stdin. This prevents
         * deadlock when stdin and stdout fills up input stream buffers to the
         * max.
         */
        ADVANCED,

        /**
         * Single thread capturing of stdout and stdin after OS process has
         * finished.
         * <p>
         * <b>CAUTION</b>: this might lead to deadlock. <i>Use if you're
         * absolutely sure stdin and stdout output is small.</i>
         * </p>
         */
        SIMPLE
    }

    /** */
    private final ExecMode execMode;

    /**
     *
     * @param emode
     *            The {@link ExecMode}.
     * @param etype
     *            The {@link ExecType}.
     */
    protected AbstractFileConverter(final ExecMode emode) {
        this.execMode = emode;
    }

    /**
     * @return {@link ExecMode}.
     */
    protected final ExecMode getExecMode() {
        return this.execMode;
    }

    /**
     * @return {@link ExecType}.
     */
    protected abstract ExecType getExecType();

    /**
     * Returns the OS shell command to perform the conversion.
     *
     * @param contentType
     *            The {@link DocContentTypeEnum} of the input file.
     * @param fileIn
     *            The input {@link File}.
     * @param filePdf
     *            The output PDF {@link File}.
     * @return The shell command, or {@code null} when no OS command is
     *         applicable and
     *         {@link #convertCustom(DocContentTypeEnum, File, File)} is used.
     */
    protected abstract String getOsCommand(DocContentTypeEnum contentType,
            File fileIn, File filePdf);

    /**
     * Creates a sibling of a File in the same directory with the file extension
     * of the offered {@link DocContentTypeEnum}.
     *
     * @param file
     *            The input file to create the sibling from.
     * @param contentType
     *            The content type if the sibling.
     * @return The sibling File object.
     */
    public static File getFileSibling(final File file,
            final DocContentTypeEnum contentType) {

        final StringBuilder builder = new StringBuilder(128);

        builder.append(file.getParent()).append(File.separator)
                .append(FilenameUtils.getBaseName(file.getAbsolutePath()))
                .append(".").append(DocContent.getFileExtension(contentType));

        return new File(builder.toString());
    }

    /**
     * Gets the output file.
     *
     * @param fileIn
     *            The input {@link File}.
     * @return The output {@link File}.
     */
    protected abstract File getOutputFile(File fileIn);

    /**
     * Performs a conversion using an OS Command.
     *
     * @param contentType
     *            The type of input file.
     * @param fileIn
     *            The file to convert.
     * @param filePdf
     *            The output file.
     * @param command
     *            The OS Command
     * @return The output file.
     * @throws DocContentToPdfException
     *             if error.
     */
    protected final File convertWithOsCommand(
            final DocContentTypeEnum contentType, final File fileIn,
            final File filePdf, final String command)
            throws DocContentToPdfException {

        final String pdfName = filePdf.getAbsolutePath();

        LOGGER.debug(command);

        final ICommandExecutor exec;

        if (this.getExecType() == ExecType.SIMPLE) {
            exec = CommandExecutor.createSimple(command);
        } else {
            exec = CommandExecutor.create(command);
        }

        boolean pdfCreated = false;

        try {
            int rc = 1;

            if (this.execMode == ExecMode.SINGLE_THREADED) {
                synchronized (this) {
                    rc = exec.executeCommand();
                }
            } else {
                rc = exec.executeCommand();
            }

            if (rc == 0) {

                pdfCreated = true;

                final String stdout = exec.getStandardOutput();

                if (StringUtils.isNotBlank(stdout)) {
                    LOGGER.debug(stdout);
                }

                if (filePdf.exists()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("[" + pdfName + "] created.");
                    }
                } else {
                    LOGGER.error("[" + pdfName + "] NOT created.");
                    throw new DocContentToPdfException("PDF is not created");
                }

            } else {

                final String stderr = exec.getStandardError();

                String reason = "";

                if (StringUtils.isNotBlank(stderr)) {
                    reason = " [" + stderr + "]";
                }

                LOGGER.error("Command [" + command + "] failed." + reason);

                throw new DocContentToPdfException(
                        "PDF could not be created" + reason);
            }

        } catch (IOException | InterruptedException e) {
            throw new SpException(e);
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
