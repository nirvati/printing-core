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
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractFileConverter implements IFileConverter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractFileConverter.class);

    protected static enum ExecMode {
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

    protected final ExecMode execMode;

    protected AbstractFileConverter(ExecMode execMode) {
        this.execMode = execMode;
    }

    /**
     * Returns the OS shell command to perform the conversion.
     *
     * @return The shell command.
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
    public static File
            getFileSibling(File file, DocContentTypeEnum contentType) {
        return new File(file.getParent() + File.separator
                + FilenameUtils.getBaseName(file.getAbsolutePath()) + "."
                + DocContent.getFileExtension(contentType));
    }

    /**
     *
     * @param fileIn
     * @return
     */
    protected abstract File getOutputFile(File fileIn);

    @Override
    public File convert(DocContentTypeEnum contentType, File fileIn)
            throws DocContentToPdfException {

        final File filePdf = getOutputFile(fileIn);

        final String pdfName = filePdf.getAbsolutePath();
        final String command = getOsCommand(contentType, fileIn, filePdf);

        LOGGER.debug(command);

        ICommandExecutor exec = CommandExecutor.createSimple(command);
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

                final String stdout =
                        exec.getStandardOutputFromCommand().toString();

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

                final String stderr =
                        exec.getStandardErrorFromCommand().toString();

                String reason = "";

                if (StringUtils.isNotBlank(stderr)) {
                    reason = " [" + stderr + "]";
                }

                LOGGER.error("Command [" + command + "] failed." + reason);
                throw new DocContentToPdfException("PDF could not be created"
                        + reason);
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
