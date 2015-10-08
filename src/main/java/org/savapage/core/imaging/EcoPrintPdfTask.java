/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.imaging;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.doc.ImageToPdf;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.FileSystemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * A {@link Runnable} task for creating an Eco Print PDF.
 *
 * @author Rijk Ravestein
 *
 */
public final class EcoPrintPdfTask implements Runnable,
        Comparable<EcoPrintPdfTask> {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(EcoPrintPdfTask.class);

    private final ThreadPoolExecutor executor;

    private final Long pdfInFileLength;

    private final EcoPrintPdfTaskInfo taskInfo;

    private boolean stopRequest = false;

    /**
     *
     * @param taskInfo
     * @param executor
     *            The {@link ThreadPoolExecutor} this task is executed in.
     */
    public EcoPrintPdfTask(final EcoPrintPdfTaskInfo taskInfo,
            final ThreadPoolExecutor executor) {

        this.taskInfo = taskInfo;
        this.executor = executor;

        if (this.taskInfo.getPdfIn() != null) {
            this.pdfInFileLength =
                    Long.valueOf(this.taskInfo.getPdfIn().length());
        } else {
            this.pdfInFileLength = Long.valueOf(0L);
        }
    }

    @Override
    public void run() {

        try {
            this.checkExecutorTerminating();
            createEcoPdf();
        } catch (InterruptedException e) {
            // noop
        }
    }

    /**
     *
     * @param imageIn
     * @return
     * @throws IOException
     */
    private static BufferedImage createFilteredImage(
            final EcoImageFilter filter, final File imageIn) throws IOException {

        final BufferedImage imageOut = filter.filter(imageIn);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("%s : Tot %.2f | Read %.2f | "
                    + "Proc %.2f | Write %.2f secs", imageIn.getName(),
                    (double) filter.getTotalTime()
                            / DateUtil.DURATION_MSEC_SECOND,
                    (double) filter.getReadTime()
                            / DateUtil.DURATION_MSEC_SECOND,
                    (double) filter.getFilterTime()
                            / DateUtil.DURATION_MSEC_SECOND,
                    (double) filter.getWriteTime()
                            / DateUtil.DURATION_MSEC_SECOND));
        }
        return imageOut;
    }

    /**
     *
     * @param pdfFile
     * @param pageOrdinal
     * @param rotate2Apply
     * @param resolution
     * @param imgFile
     * @return
     */
    private static String createPdf2ImgCmd(final File pdfFile,
            final Integer pageOrdinal, final String rotate2Apply,
            final Integer resolution, final File imgFile) {

        final Pdf2ImgCommand cmd = new Pdf2PngPopplerCmd();
        return cmd.createCommand(pdfFile, imgFile, pageOrdinal, rotate2Apply,
                resolution);
    }

    /**
     *
     * @throws InterruptedException
     */
    private void checkExecutorTerminating() throws InterruptedException {
        if (this.stopRequest
                || (this.executor != null && this.executor.isTerminating())) {

            if (this.stopRequest && LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("[%s] %s : STOPPED", this.taskInfo
                        .getUuid().toString(), this.taskInfo.getPdfIn()
                        .getName()));
            }

            throw new InterruptedException();
        }
    }

    /**
     * .
     */
    private void createEcoPdf() {

        final String pathTmpDir = this.taskInfo.getPathTmpDir().toString();

        final long startTime = System.currentTimeMillis();

        final EcoImageFilter filter = new EcoImageFilterSquare();

        final Path pathPdfOutTemp =
                FileSystems.getDefault().getPath(
                        pathTmpDir,
                        String.format("%s.pdf.eco", UUID.randomUUID()
                                .toString()));

        /*
         * Create target document, but lazy open it when page size of first page
         * is known.
         */
        final Document targetDocument = new Document();

        File imageOut = null;

        boolean finished = false;

        int nPagesTot = 0;
        int nPagesMax = 0;

        try {

            final PdfReader readerWlk =
                    new PdfReader(new FileInputStream(this.taskInfo.getPdfIn()));

            nPagesMax = readerWlk.getNumberOfPages();

            if (LOGGER.isInfoEnabled()) {

                final StringBuilder msg = new StringBuilder();

                msg.append(String.format("%s | %d page(s) ...", taskInfo
                        .getPdfOut().getName(), nPagesMax));

                LOGGER.info(msg.toString());
            }

            PdfWriter.getInstance(targetDocument, new FileOutputStream(
                    pathPdfOutTemp.toFile()));

            for (int i = 0; i < nPagesMax; i++) {

                this.checkExecutorTerminating();

                /*
                 * Set page size and margins first.
                 */
                final boolean rotatePdfPage;

                if (StringUtils.defaultString(this.taskInfo.getRotation(), "0")
                        .equals("0")) {
                    rotatePdfPage = false;
                } else {
                    final int rotate =
                            Integer.valueOf(this.taskInfo.getRotation());
                    // Rotate when odd multiple of 90.
                    rotatePdfPage = (rotate / 90) % 2 != 0;
                }

                final Rectangle pageSize = readerWlk.getPageSize(i + 1);

                if (rotatePdfPage) {
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

                imageOut =
                        new File(String.format("%s/%s.png", pathTmpDir, UUID
                                .randomUUID().toString()));

                final String command =
                        createPdf2ImgCmd(taskInfo.getPdfIn(), i, "0",
                                taskInfo.getResolution(), imageOut);

                final ICommandExecutor exec =
                        CommandExecutor.createSimple(command);

                if (exec.executeCommand() != 0) {

                    final StringBuilder msg = new StringBuilder();
                    msg.append("image [")
                            .append(imageOut.getAbsolutePath())
                            .append("] could not be created. Command [")
                            .append(command)
                            .append("] Error [")
                            .append(exec.getStandardErrorFromCommand()
                                    .toString()).append("]");

                    throw new SpException(msg.toString());
                }

                this.checkExecutorTerminating();

                final com.itextpdf.text.Image image =
                        com.itextpdf.text.Image.getInstance(
                                createFilteredImage(filter, imageOut),
                                Color.WHITE);

                this.checkExecutorTerminating();

                ImageToPdf.addImagePage(targetDocument, 0, 0, image);
                nPagesTot++;

                imageOut.delete();
                imageOut = null;
            }

            targetDocument.close();

            // Atomic move
            FileSystemHelper.doAtomicFileMove(//
                    pathPdfOutTemp, this.getTaskInfo().getPdfOut().toPath());

            //
            finished = true;

        } catch (IOException | DocumentException | SpException e) {

            LOGGER.error(e.getMessage());

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

            if (!finished && this.taskInfo.getPdfOut().exists()) {
                this.taskInfo.getPdfOut().delete();
            }

            if (imageOut != null) {
                imageOut.delete();
            }
        }

        if (LOGGER.isInfoEnabled()) {

            final StringBuilder msg = new StringBuilder();

            msg.append(String.format("%s | %d page(s) | %.2f secs.", taskInfo
                    .getPdfOut().getName(), nPagesMax,
                    (double) (System.currentTimeMillis() - startTime)
                            / DateUtil.DURATION_MSEC_SECOND));

            if (!finished) {
                msg.append(" | ABORTED after ").append(nPagesTot)
                        .append(" page(s).");
            }

            LOGGER.info(msg.toString());
        }
    }

    @Override
    public int hashCode() {
        return this.taskInfo.getUuid().hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        return this.taskInfo.getUuid() != null
                && this.taskInfo.getUuid().equals(
                        ((EcoPrintPdfTask) object).taskInfo.getUuid());
    }

    @Override
    public int compareTo(EcoPrintPdfTask o) {
        /*
         * The compare works out so larger files have lower priority.
         */
        return this.pdfInFileLength.compareTo(o.pdfInFileLength);
    }

    public EcoPrintPdfTaskInfo getTaskInfo() {
        return taskInfo;
    }

    public void stop() {
        this.stopRequest = true;
    }
}
