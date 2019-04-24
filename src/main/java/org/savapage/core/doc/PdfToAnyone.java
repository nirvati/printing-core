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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.pdf.ITextPdfCreator;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;

/**
 * Use {@link com.lowagie} to copy a PDF file.
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfToAnyone extends AbstractPdfConverter
        implements IPdfConverter {

    /**
     * A unique suffix to type the kind of PDF convert.
     */
    private static final String OUTPUT_FILE_SFX = "1T3XT";

    /**
     *
     */
    public PdfToAnyone() {
        super();
    }

    /**
     *
     * @param createDir
     *            The directory location of the created file.
     */
    public PdfToAnyone(final File createDir) {
        super(createDir);
    }

    @Override
    public File convert(final File pdfFile) throws IOException {

        final File pdfOut = getOutputFile(pdfFile);

        boolean exception = true;

        try (FileInputStream istr = new FileInputStream(pdfFile);
                FileOutputStream ostr = new FileOutputStream(pdfOut)) {

            final PdfReader reader = new PdfReader(istr);

            final Document document =
                    new Document(reader.getPageSizeWithRotation(1));
            final PdfCopy writer = new PdfCopy(document, ostr);

            @SuppressWarnings("unchecked")
            final Map<String, String> info = reader.getInfo();

            document.open();
            document.addCreationDate();

            String infoWlk;

            infoWlk = info.get(ITextPdfCreator.PDF_INFO_KEY_CREATOR);
            if (StringUtils.isNotBlank(infoWlk)) {
                document.addCreator(infoWlk);
            }

            infoWlk = info.get(ITextPdfCreator.PDF_INFO_KEY_AUTHOR);
            if (StringUtils.isNotBlank(infoWlk)) {
                document.addAuthor(infoWlk);
            }

            infoWlk = info.get(ITextPdfCreator.PDF_INFO_KEY_KEYWORDS);
            if (StringUtils.isNotBlank(infoWlk)) {
                document.addKeywords(infoWlk);
            }

            infoWlk = info.get(ITextPdfCreator.PDF_INFO_KEY_SUBJECT);
            if (StringUtils.isNotBlank(infoWlk)) {
                document.addSubject(infoWlk);
            }

            infoWlk = info.get(ITextPdfCreator.PDF_INFO_KEY_TITLE);
            if (StringUtils.isNotBlank(infoWlk)) {
                document.addTitle(infoWlk);
            }

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                writer.addPage(writer.getImportedPage(reader, i));
            }

            writer.close();
            reader.close();

            exception = false;

        } catch (DocumentException e) {
            throw new IOException(e.getMessage());
        } finally {
            if (exception) {
                pdfOut.delete();
            }
        }
        return pdfOut;
    }

    @Override
    protected String getOutputFileSfx() {
        return OUTPUT_FILE_SFX;
    }

}
