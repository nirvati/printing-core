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
package org.savapage.core.doc;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.savapage.core.SpException;
import org.savapage.core.imaging.Pdf2ImgCairoCmd;
import org.savapage.core.imaging.Pdf2ImgCommand;
import org.savapage.core.pdf.PdfPageRotateHelper;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.savapage.core.util.FileSystemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToImagePdf extends AbstractPdfConverter
        implements IPdfConverter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PdfToImagePdf.class);

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "imaged";

    /**
     * Directory for temporary files.
     */
    private final File tempDir;

    /** */
    private final int resolutionDpi;

    /**
     * @param tmpDir
     *            Directory for temporary files.
     * @param dpi
     *            Resolution in DPI.
     */
    public PdfToImagePdf(final File tmpDir, final int dpi) {
        super();
        this.tempDir = tmpDir;
        this.resolutionDpi = dpi;
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     * @param tmpDir
     *            Directory for temporary files.
     * @param dpi
     *            Resolution in DPI.
     */
    public PdfToImagePdf(final File createDir, final File tmpDir,
            final int dpi) {
        super(createDir);
        this.tempDir = tmpDir;
        this.resolutionDpi = dpi;
    }

    @Override
    public File convert(final File pdfFile) throws IOException {
        /*
         * Create target document, but lazy open it when page size of first page
         * is known.
         */
        final Document targetDocument = new Document();

        File imageOut = null;

        boolean finished = false;

        int nPagesTot = 0;
        int nPagesMax = 0;

        final File pdfOut = getOutputFile(pdfFile);

        final Path pathPdfOutTemp = FileSystems.getDefault()
                .getPath(this.tempDir.toString(), String.format("%s.pdf.%s",
                        UUID.randomUUID().toString(), OUTPUT_FILE_SFX));

        try {
            final PdfReader readerWlk =
                    new PdfReader(new FileInputStream(pdfFile));

            nPagesMax = readerWlk.getNumberOfPages();

            PdfWriter.getInstance(targetDocument,
                    new FileOutputStream(pathPdfOutTemp.toFile()));

            for (int i = 0; i < nPagesMax; i++) {

                final int nPage = i + 1;

                final Rectangle pageSize = readerWlk.getPageSize(nPage);
                final int pageRotation = readerWlk.getPageRotation(nPage);

                final boolean pageLandscape =
                        PdfPageRotateHelper.isLandscapePage(pageSize);

                final boolean seenAsLandscape = PdfPageRotateHelper
                        .isSeenAsLandscape(pageLandscape, pageRotation);

                /*
                 * Set page size and margins first.
                 */
                if (pageLandscape && !seenAsLandscape) {

                    final PdfDictionary pageDict = readerWlk.getPageN(i + 1);

                    pageDict.put(PdfName.ROTATE, new PdfNumber(
                            PdfPageRotateHelper.PDF_ROTATION_90.intValue()));

                    targetDocument.setPageSize(pageSize.rotate());
                } else {
                    targetDocument.setPageSize(pageSize);
                }

                targetDocument.setMargins(0, 0, 0, 0);

                /*
                 * Open document or add new page.
                 */
                if (!targetDocument.isOpen()) {
                    targetDocument.open();
                } else {
                    targetDocument.newPage();
                }

                imageOut = new File(String.format("%s/%s.jpg",
                        this.tempDir.toString(), UUID.randomUUID().toString()));

                // final Pdf2ImgCommand cmd = new Pdf2PngPopplerCmd();
                final Pdf2ImgCommand cmd =
                        new Pdf2ImgCairoCmd(Pdf2ImgCairoCmd.ImgType.JPEG);

                final String command = cmd.createCommand(pdfFile, pageLandscape,
                        pageRotation, imageOut, i, resolutionDpi, 0);

                final ICommandExecutor exec =
                        CommandExecutor.createSimple(command);

                if (exec.executeCommand() != 0) {

                    final StringBuilder msg = new StringBuilder();
                    msg.append("image [").append(imageOut.getAbsolutePath())
                            .append("] could not be created. Command [")
                            .append(command).append("] Error [").append(exec
                                    .getStandardErrorFromCommand().toString())
                            .append("]");

                    throw new SpException(msg.toString());
                }

                final com.itextpdf.text.Image image = com.itextpdf.text.Image
                        .getInstance(ImageIO.read(imageOut), Color.WHITE);

                ImageToPdf.addImagePage(targetDocument, 0, 0, image);
                nPagesTot++;

                imageOut.delete();
                imageOut = null;
            }

            targetDocument.close();

            // Atomic move
            FileSystemHelper.doAtomicFileMove(//
                    pathPdfOutTemp, pdfOut.toPath());
            //
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

            if (imageOut != null) {
                imageOut.delete();
            }
        }
        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
