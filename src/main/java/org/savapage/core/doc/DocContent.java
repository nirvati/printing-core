/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.soffice.SOfficeHelper;
import org.savapage.core.fonts.InternalFontFamilyEnum;

/**
 * Helper methods to determine content type of a document, file or data stream.
 * <p>
 * References:
 * <ul>
 * <li><a href="http://filext.com/">FILExt.com</a></li>
 * <li><a href= "http://reference.sitepoint.com/html/mime-types-full">reference.
 * sitepoint .com</a></li>
 * </ul>
 * </p>
 *
 * <p>
 * TODO:
 * <ul>
 * <li>Consider using {@link Files#probeContentType(java.nio.file.Path)}.</li>
 * </ul>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class DocContent {

    /**
     * The first bytes (signature) of {@link DocContentTypeEnum#PDF}.
     * <p>
     * Identifying characters Hex: {@code 25 50 44 46 2D 31 2E}
     * </p>
     * <p>
     * ASCII: "%PDF-1."
     * </p>
     * <p>
     * Reference: <a href="http://filext.com/file-extension/PDF">FILExt.com</a>
     * </p>
     */
    public static final String HEADER_PDF = "%PDF";

    /**
     * A request for a test page as e.g. defined in
     * {@code /usr/share/cups/data/testprint}.
     *
     * <pre>
     * #PDF-BANNER
     * Template default-testpage.pdf
     * Show printer-name printer-info printer-location printer-make-and-model printer-driver-name printer-driver-version paper-size imageable-area job-id options time-at-creation time-at-processing
     * </pre>
     */
    public static final String HEADER_PDF_BANNER = "#PDF";

    /**
     * The first bytes (signature) of {@link DocContentTypeEnum#PS}.
     * <p>
     * Identifying characters Hex: {@code 25 21 50 53}
     * </p>
     * <p>
     * ASCII: "%!PS"
     * </p>
     * <p>
     * Reference: <a href="http://filext.com/file-extension/PS">FILExt.com</a>
     * </p>
     */
    public static final String HEADER_PS = "%!PS";

    /**
     * The first bytes (signature) of {@link DocContentTypeEnum#UNIRAST}.
     */
    public static final String HEADER_UNIRAST = "UNIR";

    /** */
    public static final String FILENAME_EXT_PDF = "pdf";
    /** */
    public static final String FILENAME_EXT_PS = "ps";
    /** */
    public static final String FILENAME_EXT_PNG = "png";
    /** */
    public static final String FILENAME_EXT_JPG = "jpg";
    /** */
    public static final String FILENAME_EXT_PNM = "pnm";
    /** */
    public static final String FILENAME_EXT_XML = "xml";

    /**
     *
     */
    public static final String MIMETYPE_PDF = "application/pdf";

    /**
     * <b>Not</b> registered by IANA (yet): internal use for now.
     */
    public static final String MIMETYPE_PDF_PGP =
            "application/x-pdf+pgp-signature";

    /** */
    public static final String MIMETYPE_POSTSCRIPT = "application/postscript";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link DocContent#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final DocContent INSTANCE = new DocContent();
    }

    /**
     *
     */
    private final Map<DocContentTypeEnum, String> formatMime = new HashMap<>();
    private final Map<String, DocContentTypeEnum> mimeFormat = new HashMap<>();
    private final Map<String, DocContentTypeEnum> extFormat = new HashMap<>();
    private final Map<DocContentTypeEnum, String> formatExt = new HashMap<>();

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static DocContent getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private DocContent() {

        init(DocContentTypeEnum.BMP, "image/bmp", "bmp");

        init(DocContentTypeEnum.CUPS_COMMAND, "application/vnd.cups-command");

        init(DocContentTypeEnum.GIF, "image/gif", "gif");

        init(DocContentTypeEnum.HTML, "text/html", "html");

        init(DocContentTypeEnum.JPEG,
                new String[] { "image/jpg", "image/jpeg" },
                new String[] { "jpg", "jpeg", "jpe" });

        init(DocContentTypeEnum.PDF,
                new String[] { MIMETYPE_PDF, MIMETYPE_PDF_PGP,
                        "application/x-pdf", "application/acrobat",
                        "applications/vnd.pdf", "text/pdf", "text/x-pdf" },
                new String[] { FILENAME_EXT_PDF });

        init(DocContentTypeEnum.PNG, "image/png", FILENAME_EXT_PNG);

        init(DocContentTypeEnum.PS,
                new String[] { MIMETYPE_POSTSCRIPT, "application/ps",
                        "application/x-postscript", "application/x-ps",
                        "text/postscript", "application/x-postscript-not-eps" },
                new String[] { FILENAME_EXT_PS });

        init(DocContentTypeEnum.SVG, "image/svg+xml", "svg");

        init(DocContentTypeEnum.RTF, "application/rtf", "rtf");

        init(DocContentTypeEnum.TIFF, new String[] { "image/tiff" },
                new String[] { "tiff", "tif" });

        init(DocContentTypeEnum.TXT, "text/plain", "txt");

        init(DocContentTypeEnum.UNIRAST, "image/urf", "urf");

        init(DocContentTypeEnum.VCARD, "text/x-vcard", "vcf");

        init(DocContentTypeEnum.WMF,
                new String[] { "image/wmf", "application/x-msmetafile",
                        "application/wmf", "application/x-wmf", "image/x-wmf",
                        "image/x-win-metafile",
                        "zz-application/zz-winassoc-wmf" },
                new String[] { "wmf" });

        init(DocContentTypeEnum.XPS,
                new String[] { "application/oxps",
                        "application/vnd.ms-xpsdocument" },
                new String[] { "xps" });

        /*
         * ODF
         */
        init(DocContentTypeEnum.ODT,
                new String[] { "application/vnd.oasis.opendocument.text",
                        "application/x-vnd.oasis.opendocument.text" },
                new String[] { "odt" });

        init(DocContentTypeEnum.ODS,
                new String[] { "application/vnd.oasis.opendocument.spreadsheet",
                        "application/x-vnd.oasis.opendocument.spreadsheet" },
                new String[] { "ods" });

        init(DocContentTypeEnum.ODP,
                new String[] {
                        "application/vnd.oasis.opendocument.presentation",
                        "application/x-vnd.oasis.opendocument.presentation" },
                new String[] { "odp" });

        /*
         * OpenOffice.org 1.0.
         */
        init(DocContentTypeEnum.SXW, "application/vnd.sun.xml.writer", "sxw");
        init(DocContentTypeEnum.SXC, "application/vnd.sun.xml.calc", "sxc");
        init(DocContentTypeEnum.SXI, "application/vnd.sun.xml.impress", "sxi");

        /*
         * Microsoft Office 97
         */
        init(DocContentTypeEnum.DOC,
                new String[] { "application/msword", "application/doc",
                        "appl/text", "application/vnd.msword",
                        "application/vnd.ms-word", "application/winword",
                        "application/word", "application/x-msw6",
                        "application/x-msword" },
                new String[] { "doc" });

        init(DocContentTypeEnum.XLS,
                new String[] { "application/vnd.ms-excel",
                        "application/msexcel", "application/x-msexcel",
                        "application/x-ms-excel", "application/vnd.ms-excel",
                        "application/x-excel", "application/x-dos_ms_excel",
                        "application/xls" },
                new String[] { "xls" });

        init(DocContentTypeEnum.PPT,
                new String[] { "application/vnd.ms-powerpoint",
                        "application/mspowerpoint", "application/ms-powerpoint",
                        "application/mspowerpnt",
                        "application/vnd-mspowerpoint",
                        "application/powerpoint", "application/x-powerpoint" },
                new String[] { "ppt" });

        /*
         * Microsoft 2007.
         */
        init(DocContentTypeEnum.DOCX,
                "application/vnd.openxmlformats-officedocument."
                        + "wordprocessingml.document",
                "docx");

        init(DocContentTypeEnum.XLSX,
                "application/vnd.openxmlformats-officedocument."
                        + "spreadsheetml.sheet",
                "xlsx");

        init(DocContentTypeEnum.PPTX,
                "application/vnd.openxmlformats-officedocument."
                        + "presentationml.presentation",
                "pptx");
    }

    /**
     *
     * @param format
     * @param mimes
     * @param extensions
     */
    private void init(final DocContentTypeEnum format, final String[] mimes,
            String[] extensions) {

        for (final String mime : mimes) {
            formatMime.put(format, mime);
            mimeFormat.put(mime, format);
        }
        formatExt.put(format, extensions[0]);
        for (String ext : extensions) {
            extFormat.put(ext, format);
        }
    }

    /**
     *
     * @param format
     * @param mime
     * @param ext
     */
    private void init(final DocContentTypeEnum format, final String mime,
            final String ext) {
        formatMime.put(format, mime);
        mimeFormat.put(mime, format);
        extFormat.put(ext, format);
        formatExt.put(format, ext);
    }

    /**
     *
     * @param format
     * @param mime
     */
    private void init(final DocContentTypeEnum format, final String mime) {
        init(format, mime, null);
    }

    /**
     * @param excludeTypes
     *            The documents types to be excluded.
     * @return The list with supported documents. Entries on the list have key
     *         (description) and value ({@code true} for Open Standard and
     *         {@code false} for proprietary format).
     */
    public static List<AbstractMap.SimpleEntry<String, Boolean>>
            getSupportedDocsInfo(final Set<DocContentTypeEnum> excludeTypes) {

        final List<AbstractMap.SimpleEntry<String, Boolean>> list =
                new ArrayList<>();

        /*
         * Start with PDF as preferred format, and add other formats in
         * deliberate order.
         */
        for (DocContentTypeEnum contentType : new DocContentTypeEnum[] {
                DocContentTypeEnum.PDF, DocContentTypeEnum.ODT,
                DocContentTypeEnum.RTF, DocContentTypeEnum.HTML,
                DocContentTypeEnum.PS, DocContentTypeEnum.TXT,
                DocContentTypeEnum.XPS, DocContentTypeEnum.DOCX,
                DocContentTypeEnum.DOC }) {

            if (excludeTypes.contains(contentType)
                    || !isSupported(contentType)) {
                continue;
            }

            final String description;
            final Boolean isOpenStandard;

            switch (contentType) {
            case ODT:
                description = "Open Document Format";
                isOpenStandard = Boolean.TRUE;
                break;

            case PS:
                description = "PostScript";
                isOpenStandard = Boolean.TRUE;
                break;

            case DOCX:
                description = "OOXML";
                isOpenStandard = Boolean.FALSE;
                break;

            case DOC:
                description = "MS Office";
                isOpenStandard = Boolean.FALSE;
                break;

            case XPS:
                description = contentType.toString();
                isOpenStandard = Boolean.FALSE;
                break;

            default:
                description = contentType.toString();
                isOpenStandard = Boolean.TRUE;
                break;
            }

            list.add(new SimpleEntry<String, Boolean>(description,
                    isOpenStandard));
        }
        return list;
    }

    /**
     * @param excludeTypes
     *            The documents types to be excluded.
     * @return
     */
    public static String getSupportedGraphicsInfo(
            final Set<DocContentTypeEnum> excludeTypes) {

        final List<String> list = new ArrayList<>();

        /*
         * Note: the DocContentTypeEnum order is deliberate.
         */
        for (DocContentTypeEnum contentType : new DocContentTypeEnum[] {
                DocContentTypeEnum.JPEG, DocContentTypeEnum.PNG,
                DocContentTypeEnum.GIF, DocContentTypeEnum.SVG,
                DocContentTypeEnum.TIFF, DocContentTypeEnum.BMP }) {

            if (excludeTypes.contains(contentType)
                    || !isSupported(contentType)) {
                continue;
            }

            list.add(contentType.toString());
        }
        return getSupportedInfo(list);
    }

    /**
     *
     * @return
     */
    private static String getSupportedInfo(final List<String> list) {

        final StringBuilder supported = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                supported.append(", ");
            }
            supported.append(list.get(i));
        }
        return supported.toString();
    }

    /**
     * Checks if the DocContentType is supported. For every supported
     * DocContentType an {@link IStreamConverter} or {@link IDocFileConverter}
     * implementation MUST exist and MUST be assigned.
     * <p>
     * See {@link DocContent#createPdfStreamConverter(DocContentTypeEnum)} or
     * {@link DocContent#createPdfFileConverter(DocContentTypeEnum)}.
     * </p>
     *
     * @param contentType
     *            The content type.
     * @return {@code true} when supported.
     */
    public static boolean isSupported(DocContentTypeEnum contentType) {

        switch (contentType) {

        case PDF:
        case PS:
            return true;

        case BMP:
        case GIF:
        case JPEG:
        case PNG:
        case SVG:
        case TIFF:
            return true;

        case TXT:
        case HTML:
            return true;

        case RTF:
        case DOC:
        case XLS:
        case PPT:
        case DOCX:
        case XLSX:
        case PPTX:
        case ODT:
        case ODP:
        case ODS:
        case SXW:
        case SXC:
        case SXI:
            return ConfigManager.instance()
                    .isConfigValue(Key.DOC_CONVERT_LIBRE_OFFICE_ENABLED)
                    && SOfficeHelper.lazyIsInstalled();

        case XPS:
            return ConfigManager.instance()
                    .isConfigValue(Key.DOC_CONVERT_XPS_TO_PDF_ENABLED)
                    && XpsToPdf.lazyIsInstalled();

        case CUPS_PDF_BANNER:
            return true;

        case CUPS_COMMAND:
        case UNIRAST:
        case VCARD:
        case WMF:
        default:
            return false;
        }
    }

    /**
     * Creates a PDF stream converter for a content type.
     *
     * @param contentType
     *            The content type.
     * @param preferredOutputFont
     *            The preferred font for the PDF output. {@code null} when
     *            (user) preference is unknown or irrelevant.
     * @return {@code null} when NO stream converter is available.
     */
    public static IStreamConverter createPdfStreamConverter(
            final DocContentTypeEnum contentType,
            final InternalFontFamilyEnum preferredOutputFont) {

        final IStreamConverter converter;

        if (contentType == DocContentTypeEnum.SVG) {
            /*
             * Since SVG is an image, we make this exception: we have NO stream
             * converter (though we DO have a file converter).
             */
            converter = null;

        } else if (DocContent.isImage(contentType)) {
            converter = new ImageToPdf();

        } else if (contentType == DocContentTypeEnum.HTML) {
            converter = new HtmlToPdf();

        } else if (contentType == DocContentTypeEnum.CUPS_PDF_BANNER) {
            converter = new CupsPdfBannerToPdf();

        } else if (contentType == DocContentTypeEnum.TXT) {

            InternalFontFamilyEnum font = preferredOutputFont;

            if (font == null) {
                font = ConfigManager.getConfigFontFamily(
                        Key.REPORTS_PDF_INTERNAL_FONT_FAMILY);
            }
            converter = new TextToPdf(font);

        } else {
            converter = null;
        }
        return converter;
    }

    /**
     * Creates a PDF file converter for a content type.
     *
     * @param contentType
     *            The content type.
     * @return {@code null} when NO file converter is available.
     */
    public static IDocFileConverter
            createPdfFileConverter(DocContentTypeEnum contentType) {

        switch (contentType) {
        case PS:
            return new PsToPdf();
        case SVG:
            return new ConvertToPdf();
        case XPS:
            return new XpsToPdf();

        case RTF:
        case DOC:
        case DOCX:
        case ODT:
        case SXW:
            // Text
        case XLS:
        case XLSX:
        case ODS:
        case SXC:
            // Spreadsheet
        case PPT:
        case PPTX:
        case ODP:
        case SXI:
            // Presentation
            if (SOfficeToPdf.isAvailable()) {
                return new SOfficeToPdf();
            }
            return new OfficeToPdf();

        default:
            return null;
        }
    }

    /**
     * Gets the DocContentType of a Mime Type string.
     *
     * @param mime
     *            The mimetype string. E.g. "application/pdf"
     * @return {@code null} when not found.
     */
    public static DocContentTypeEnum getContentTypeFromMime(final String mime) {
        return getInstance().mimeFormat.get(mime.toLowerCase());
    }

    /**
     * Gets the DocContentType of a file extension (without leading point).
     *
     * @param ext
     *            The file extension.
     * @return {@code null} when not found.
     */
    public static DocContentTypeEnum getContentTypeFromExt(final String ext) {
        return getInstance().extFormat.get(ext.toLowerCase());
    }

    /**
     * Gets the DocContentType of a file based on its extension.
     *
     * @param file
     *            The file.
     * @return {@code null} when not found.
     */
    public static DocContentTypeEnum getContentTypeFromFile(final String file) {
        return getInstance().extFormat
                .get(FilenameUtils.getExtension(file).toLowerCase());
    }

    /**
     *
     * @param file
     * @return
     */
    public static DocContentTypeEnum getContentType(final File file) {
        return getContentTypeFromFile(file.getName());
    }

    /**
     *
     * @param file
     * @return
     */
    public static String getMimeType(final File file) {

        final DocContentTypeEnum format = getContentType(file);
        if (format == null) {
            return null;
        }
        return getMimeType(format);
    }

    /**
     *
     * @param format
     * @return
     */
    public static String getMimeType(final DocContentTypeEnum format) {
        return getInstance().formatMime.get(format);
    }

    /**
     *
     * @param format
     * @return
     */
    public static String getFileExtension(final DocContentTypeEnum format) {
        return getInstance().formatExt.get(format);
    }

    /**
     *
     * @param format
     *            The content type
     * @return The list of file extensions (without point prefix).
     */
    public static List<String>
            getFileExtensions(final DocContentTypeEnum format) {
        final List<String> list = new ArrayList<>();
        for (final Entry<String, DocContentTypeEnum> entry : getInstance().extFormat
                .entrySet()) {
            if (entry.getValue() == format) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    /**
     *
     * @param contentType
     * @return
     */
    public static boolean isImage(final DocContentTypeEnum contentType) {
        return StringUtils.defaultString(getMimeType(contentType))
                .startsWith("image");
    }
}
