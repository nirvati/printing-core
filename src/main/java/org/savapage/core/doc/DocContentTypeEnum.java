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

/**
 * Content type of a document, file or data stream.
 * <p>
 * References:
 * <ul>
 * <li><a href=
 * "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx"
 * >2007 Office system file format MIME types</a></li>
 * </ul>
 * </p>
 *
 * @author Datraverse B.V.
 */
public enum DocContentTypeEnum {

    /** Bitmap. */
    BMP,

    /**
     * More info in the <a
     * href="http://www.cups.org/documentation.php/spec-command.html">Online
     * CUPS Documentation</a>.
     */
    CUPS_COMMAND,

    /** Microsoft Word 97/2000/XP/2003 document. */
    DOC,

    /** Microsoft Office Word 2007 document. */
    DOCX,

    /** */
    GIF,

    /** */
    HTML,

    /** */
    JPEG,

    /** ODF Presentation. */
    ODP,

    /** ODF Spreadsheet. */
    ODS,

    /** ODF Text Document. */
    ODT,

    /** */
    RTF,

    /** */
    PDF,

    /** */
    PNG,

    /** Microsoft PowerPoint 97/2000/XP/2003 presentation. */
    PPT,

    /** Microsoft Office PowerPoint 2007 presentation. */
    PPTX,

    /** PostScript. */
    PS,

    /** Scalable Vector Graphics. */
    SVG,

    /** OpenOffice.org 1.0 Spreadsheet. */
    SXC,

    /** OpenOffice.org 1.0 Presentation. */
    SXI,

    /** OpenOffice.org 1.0 Text Document. */
    SXW,

    /** */
    TIFF,

    /** */
    TXT,

    /** */
    UNIRAST,

    /** */
    VCARD,

    /** */
    WMF,

    /** Microsoft Excel 97/2000/XP/2003 workbook. */
    XLS,

    /** Microsoft Office Excel 2007 workbook. */
    XLSX,

    /** Microsoft XML Paper Specification. */
    XPS,

    /** Dummy format, not assigned to any mime type or file extension. */
    UNKNOWN
}
