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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.File;

import org.junit.Test;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class DocContentTest {

    /**
    *
    */
    @Test
    public final void fileExtentionTest() {
        assertEquals("PDF file extension", DocContent.FILENAME_EXT_PDF,
                DocContent.getFileExtension(DocContentTypeEnum.PDF));
        assertEquals("PNG file extension", DocContent.FILENAME_EXT_PNG,
                DocContent.getFileExtension(DocContentTypeEnum.PNG));
        assertEquals("PS file extension", DocContent.FILENAME_EXT_PS,
                DocContent.getFileExtension(DocContentTypeEnum.PS));
    }

    /**
    *
    */
    @Test
    public final void getContentTypeTest() {
        assertEquals("DOC content type from filename", DocContentTypeEnum.DOC,
                DocContent.getContentTypeFromFile("file.doc"));
        assertEquals("DOCX content type from filename",
                DocContentTypeEnum.DOCX,
                DocContent.getContentTypeFromFile("file.docx"));
        assertEquals("ODT content type from filename", DocContentTypeEnum.ODT,
                DocContent.getContentTypeFromFile("file.odt"));
        assertEquals("RTF content type from filename", DocContentTypeEnum.RTF,
                DocContent.getContentTypeFromFile("file.rtf"));
        assertEquals("SVG content type from filename", DocContentTypeEnum.SVG,
                DocContent.getContentTypeFromFile("file.svg"));
        assertEquals("SXW content type from filename", DocContentTypeEnum.SXW,
                DocContent.getContentTypeFromFile("file.sxw"));
        assertEquals("XPS content type from filename", DocContentTypeEnum.XPS,
                DocContent.getContentTypeFromFile("savapage-test.xps"));
    }

    /**
    *
    */
    @Test
    public final void getContentTypeByMimeTest() {
        assertEquals("PDF content type from mime", DocContentTypeEnum.PDF,
                DocContent.getContentTypeFromMime(DocContent.MIMETYPE_PDF));
        assertNotSame("PDF content type from mime", DocContentTypeEnum.DOC,
                DocContent.getContentTypeFromMime(DocContent.MIMETYPE_PDF));
    }

    /**
     *
     */
    @Test
    public final void fileSiblingTest() {

        final String dir = "/some/random/path/temp";
        final String pathIn = dir + "/x.doc";
        final String pathOut = dir + "/x." + DocContent.FILENAME_EXT_PDF;

        final File fileIn = new File(pathIn);

        final File fileOut =
                AbstractDocFileConverter.getFileSibling(fileIn,
                        DocContentTypeEnum.PDF);

        assertEquals("x.doc to x.pdf", fileOut.getAbsolutePath(), pathOut);
    }

}
