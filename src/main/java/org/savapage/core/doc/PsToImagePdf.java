/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.pdf.AbstractPdfCreator;
import org.savapage.core.pdf.facade.PdfDocumentMPL;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.system.SystemInfo.ArgumentGS;
import org.savapage.core.util.FileSystemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Create a PDF file from PostScript with pages rendered as PNG images using
 * {@link com.lowagie.text.Document}.
 *
 * @author Rijk Ravestein
 *
 */
public final class PsToImagePdf extends AbstractPdfConverter
        implements IPostScriptConverter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PsToImagePdf.class);

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "ps-imaged";

    /**
     * Directory for temporary files.
     */
    private final File tempDir;

    /** */
    private final int resolutionDpi;

    /** */
    private final String pdfTitle;

    /** */
    private final String pdfAuthor;

    /**
     * @param tmpDir
     *            Directory for temporary files.
     * @param dpi
     *            Resolution in DPI.
     * @param title
     *            PDF title.
     * @param author
     *            PDF author.
     */
    public PsToImagePdf(final File tmpDir, final int dpi, final String title,
            final String author) {
        super();
        this.tempDir = tmpDir;
        this.resolutionDpi = dpi;
        this.pdfTitle = title;
        this.pdfAuthor = author;
    }

    /**
     * @param createDir
     *            The directory location of the created file.
     * @param tmpDir
     *            Directory for temporary files.
     * @param dpi
     *            Resolution in DPI.
     * @param title
     *            PDF title.
     * @param author
     *            PDF author.
     */
    public PsToImagePdf(final File createDir, final File tmpDir, final int dpi,
            final String title, final String author) {
        super(createDir);
        this.tempDir = tmpDir;
        this.resolutionDpi = dpi;
        this.pdfTitle = title;
        this.pdfAuthor = author;
    }

    /**
     *
     * @param psFile
     *            Input file.
     * @param pdfOut
     *            Output file.
     * @return List of image files.
     * @throws IOException
     *             If file error.
     * @throws InterruptedException
     *             If interrupted.
     */
    private List<File> createImageFiles(final File psFile, final Path pdfOut)
            throws IOException, InterruptedException {

        final List<File> imgFiles = new ArrayList<>();

        final StringBuilder cmd = new StringBuilder(128);

        final String imgBasePath = pdfOut.toString();
        final String imgOrdinalPfx = "-";
        final String imgOrdinal = "%d";
        final String imgOrdinalSfx = ".png";

        // try {
        cmd.append(SystemInfo.Command.GS.cmd()).append(" -sOutputFile=\"")
                .append(imgBasePath).append(imgOrdinalPfx).append(imgOrdinal)
                .append(imgOrdinalSfx)
                .append("\" -sDEVICE=" + "png16m"
                        + " -q -dSAFER -dNOPAUSE -dBATCH ")
                .append(" -r").append(this.resolutionDpi).append(" ")
                .append(ArgumentGS.STDOUT_TO_STDOUT.getArg()).append(" \"")
                .append(psFile.getCanonicalPath()).append("\"");

        final ICommandExecutor exec =
                CommandExecutor.createSimple(cmd.toString());

        if (exec.executeCommand() != 0) {

            final StringBuilder msg = new StringBuilder();
            msg.append("images from [").append(psFile.getCanonicalPath())
                    .append("] could not be created. Command [")
                    .append(cmd.toString()).append("] Error [")
                    .append(exec.getStandardError()).append("]");

            throw new SpException(msg.toString());
        }

        for (int i = 1;; i++) {
            final File file = new File(String.format("%s%s%d%s", imgBasePath,
                    imgOrdinalPfx, i, imgOrdinalSfx));
            if (file.exists()) {
                imgFiles.add(file);
                continue;
            }
            break;
        }

        return imgFiles;
    }

    @Override
    public File convert(final File psFile) throws IOException {
        /*
         * Create target document, but lazy open it when page size of first page
         * is known.
         */
        final Document targetDocument = new Document();

        //
        boolean finished = false;

        int nPagesTot = 0;

        final File pdfOut = getOutputFile(psFile);

        final Path pathPdfOutTemp = FileSystems.getDefault()
                .getPath(this.tempDir.toString(), String.format("%s.pdf.%s",
                        UUID.randomUUID().toString(), OUTPUT_FILE_SFX));

        List<File> imageFiles = null;

        try {

            imageFiles = this.createImageFiles(psFile, pathPdfOutTemp);

            PdfWriter.getInstance(targetDocument,
                    new FileOutputStream(pathPdfOutTemp.toFile()));

            final Rectangle docPageSize = targetDocument.getPageSize();

            final PdfDocumentMPL documentFacade =
                    new PdfDocumentMPL(targetDocument);

            for (final File imgFile : imageFiles) {

                final com.lowagie.text.Image image = com.lowagie.text.Image
                        .getInstance(ImageIO.read(imgFile), Color.WHITE);

                /*
                 * Set page size and margins first.
                 */
                targetDocument.setMargins(0, 0, 0, 0);

                if (image.getWidth() > image.getHeight()) {
                    targetDocument.setPageSize(docPageSize.rotate());
                } else {
                    targetDocument.setPageSize(docPageSize);
                }

                ImageToPdf.addImagePage(documentFacade,
                        documentFacade.create(image));
                nPagesTot++;
            }

            //
            targetDocument.addCreator(AbstractPdfCreator.getCreatorString());

            if (StringUtils.isNotBlank(this.pdfTitle)) {
                targetDocument.addTitle(this.pdfTitle);
            }
            if (StringUtils.isNotBlank(this.pdfAuthor)) {
                targetDocument.addAuthor(this.pdfAuthor);
            }

            targetDocument.close();

            // Atomic move
            FileSystemHelper.doAtomicFileMove(//
                    pathPdfOutTemp, pdfOut.toPath());

            finished = true;

        } catch (IOException | DocumentException | SpException e) {

            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);

        } catch (InterruptedException e) {

            finished = false;

        } finally {

            if (targetDocument != null && targetDocument.isOpen()
                    && nPagesTot > 0) {
                targetDocument.close();
            }

            if (pathPdfOutTemp.toFile().exists()) {
                pathPdfOutTemp.toFile().delete();
            }

            if (!finished && pdfOut.exists()) {
                pdfOut.delete();
            }

            if (imageFiles != null) {
                for (final File imgFile : imageFiles) {
                    imgFile.delete();
                }
            }
        }
        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
