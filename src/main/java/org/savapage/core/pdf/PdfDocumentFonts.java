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
package org.savapage.core.pdf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.savapage.core.system.SystemInfo.Command;
import org.savapage.core.util.DateUtil;

import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;

/**
 * PDF Document Fonts.
 * <ul>
 * <li><a href=
 * "https://stackoverflow.com/questions/56173544/how-to-check-embedded-font-in-pdf-using-itext">How
 * to Check Embedded font in PDF using iText</a></li>
 * </ul>
 *
 * @author Rijk Ravestein
 *
 */
public final class PdfDocumentFonts {

    /** */
    private static final String FAMILY_TIMES = "Times";
    /** */
    private static final String STYLE_BOLD = "Bold";
    /** */
    private static final String STYLE_OBLIQUE = "Oblique";
    /** */
    private static final String STYLE_BOLDOBLIQUE = STYLE_BOLD + STYLE_OBLIQUE;
    /** */
    private static final String STYLE_ITALIC = "Italic";
    /** */
    private static final String STYLE_BOLDITALIC = STYLE_BOLD + STYLE_ITALIC;
    /** */
    private static final Set<String> STANDARD_FONTS;

    static {
        STANDARD_FONTS = new HashSet<>();
        STANDARD_FONTS.add(BaseFont.COURIER);
        STANDARD_FONTS.add(BaseFont.COURIER_BOLD);
        STANDARD_FONTS.add(BaseFont.COURIER + "-" + STYLE_BOLDOBLIQUE);
        STANDARD_FONTS.add(BaseFont.COURIER_OBLIQUE);
        STANDARD_FONTS.add(BaseFont.HELVETICA);
        STANDARD_FONTS.add(BaseFont.HELVETICA_BOLD);
        STANDARD_FONTS.add(BaseFont.HELVETICA + "-" + STYLE_BOLDOBLIQUE);
        STANDARD_FONTS.add(BaseFont.HELVETICA_OBLIQUE);
        STANDARD_FONTS.add(BaseFont.SYMBOL);
        STANDARD_FONTS.add(BaseFont.TIMES_ROMAN);
        STANDARD_FONTS.add(BaseFont.TIMES_BOLD);
        STANDARD_FONTS.add(FAMILY_TIMES + "-" + STYLE_BOLDITALIC);
        STANDARD_FONTS.add(BaseFont.TIMES_ITALIC);
        STANDARD_FONTS.add(BaseFont.ZAPFDINGBATS);
    }

    /** */
    private static final PdfName[] FONTFILE_ARRAY = new PdfName[] {
            PdfName.FONTFILE, PdfName.FONTFILE2, PdfName.FONTFILE3 };
    /** */
    private static final PdfName[] SUBTYPE_ARRAY = new PdfName[] {
            PdfName.TYPE0, PdfName.TYPE1, PdfName.MMTYPE1, PdfName.CIDFONTTYPE0,
            PdfName.CIDFONTTYPE2, PdfName.TRUETYPE };

    /** */
    public enum FontType {
        /** */
        TYPE_1("Type 1"),
        /** aka Compact Font Format (CFF). */
        TYPE_1C("Type 1C"),
        /** */
        TYPE_2("Type 3"),
        /** */
        TRUETYPE("TrueType"),
        /** 16-bit font with no specified type. */
        CID_0("CID Type 0"),
        /** 16-bit PostScript CFF font. */
        CID_0C("CID Type 0C"),
        /** */
        CID_2("CID Type 2"),
        /** 16-bit TrueType font. */
        CID_TT("CID TrueType"),
        /** Placeholder for unforeseen values. */
        UNKNOWN("");

        /** */
        private final String text;

        FontType(final String name) {
            this.text = name;
        }

        public String uiText() {
            return this.text;
        }
    }

    /** */
    public enum Encoding {
        /** */
        CUSTOM("Custom"),
        /** */
        MACROMAN("MacRoman"),
        /** */
        WINANSI("WinAnsi"),
        /** */
        CID_TT("CID TrueType"),
        /** Placeholder for unforeseen values. */
        UNKNOWN("");
        /** */
        private final String text;

        Encoding(final String name) {
            this.text = name;
        }

        public String uiText() {
            return this.text;
        }
    }

    /** */
    public static class Font {

        private static final Map<String, String> STANDARD_FONT_SUBST;

        static {
            STANDARD_FONT_SUBST = new TreeMap<>();
            for (final String font : STANDARD_FONTS) {
                STANDARD_FONT_SUBST.put(font,
                        matchWithSystemFontCmd(getMatchFontCmd(font)));
            }
        }

        private Font(final String name) {
            this.name = name;
        }

        /**
         * The font name, exactly as given in the PDF file (potentially
         * including a subset prefix)
         */
        private final String name;

        private FontType type;

        private String typeUnknown;

        private Encoding encoding;

        private String encodingUnknown;

        private boolean embedded;

        private boolean subset;

        private boolean toUnicodeMap;

        private String systemFontMatch;

        private void setType(FontType type) {
            this.type = type;
        }

        private void setTypeUnknown(String typeUnknown) {
            this.typeUnknown = typeUnknown;
        }

        private void setEncoding(Encoding encoding) {
            this.encoding = encoding;
        }

        private void setEncodingUnknown(String encodingUnknown) {
            this.encodingUnknown = encodingUnknown;
        }

        private void setEmbedded(boolean embedded) {
            this.embedded = embedded;
        }

        private void setSubset(boolean subset) {
            this.subset = subset;
        }

        private void setToUnicodeMap(boolean toUnicodeMap) {
            this.toUnicodeMap = toUnicodeMap;
        }

        public String getName() {
            return name;
        }

        public FontType getType() {
            return type;
        }

        public String getTypeUnknown() {
            return typeUnknown;
        }

        public Encoding getEncoding() {
            return encoding;
        }

        public String getEncodingUnknown() {
            return encodingUnknown;
        }

        public boolean isEmbedded() {
            return embedded;
        }

        public boolean isSubset() {
            return subset;
        }

        public boolean isToUnicodeMap() {
            return toUnicodeMap;
        }

        public String getSystemFontMatch() {
            return systemFontMatch;
        }

        public static String getUiHeader() {
            return "name                                 "
                    + "type              " + "encoding         " + "std "
                    + "emb " + "sub " + "uni" + "\n"
                    + "------------------------------------ "
                    + "----------------- " + "---------------- " + "--- "
                    + "--- " + "--- " + "---";
        }

        public String getUiLine() {
            final StringBuilder line = new StringBuilder();
            line.append(StringUtils.rightPad(this.getName(), 37, ' '));

            String val;

            if (this.getType() == FontType.UNKNOWN) {
                val = this.getTypeUnknown();
            } else {
                val = this.getType().uiText();
            }
            line.append(StringUtils.rightPad(val, 18, ' '));

            if (this.getEncoding() == Encoding.UNKNOWN) {
                val = this.getEncodingUnknown();
            } else {
                val = this.getEncoding().uiText();
            }
            line.append(StringUtils.rightPad(val, 17, ' '));

            if (this.isStandardFont()) {
                val = "yes";
            } else {
                val = "no";
            }
            line.append(StringUtils.rightPad(val, 4, ' '));

            if (this.isEmbedded()) {
                val = "yes";
            } else {
                val = "no";
            }
            line.append(StringUtils.rightPad(val, 4, ' '));

            if (this.isSubset()) {
                val = "yes";
            } else {
                val = "no";
            }
            line.append(StringUtils.rightPad(val, 4, ' '));

            if (this.isToUnicodeMap()) {
                val = "yes";
            } else {
                val = "no";
            }
            line.append(StringUtils.rightPad(val, 3, ' '));

            if (this.systemFontMatch != null) {
                line.append(" -> ").append(this.systemFontMatch);
            }

            //
            return line.toString();
        }

        public boolean isStandardFont() {
            return !this.embedded && STANDARD_FONTS.contains(this.name);
        }

        /**
         * Matches with available system fonts.
         *
         * @return The matched font.
         */
        public String matchWithSystemFont() {
            if (isStandardFont()) {
                this.systemFontMatch = STANDARD_FONT_SUBST.get(this.getName());
            }
            this.systemFontMatch =
                    matchWithSystemFontCmd(getMatchFontCmd(this.getName()));
            return this.systemFontMatch;
        }

        /**
         *
         * @param pdfFontName
         *            PDF font name.
         * @return System command
         */
        private static String getMatchFontCmd(final String pdfFontName) {
            return Command.FC_MATCH.cmdLineExt("-f",
                    "\"%{family} %{style[0]}\"",
                    pdfFontName.replace('-', ':')
                            .replace(STYLE_BOLDITALIC,
                                    STYLE_BOLD + ":" + STYLE_ITALIC)
                            .replace(STYLE_BOLDOBLIQUE,
                                    STYLE_BOLD + ":" + STYLE_OBLIQUE));
        }

        /**
         * Matches with available system fonts.
         *
         * @param cmd
         *            The system command.
         * @return The matched font.
         */
        private static String matchWithSystemFontCmd(final String cmd) {

            final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

            try {
                int rc = exec.executeCommand();
                if (rc != 0) {
                    return null;
                }
                return exec.getStandardOutput();
            } catch (Exception e) {
                throw new SpException(e);
            }
        }

        /**
         * @return The substitutes of PDF standard fonts sorted by PDF name.
         */
        public static Map<String, String> getStandardFontSubst() {
            return STANDARD_FONT_SUBST;
        }

    }

    /**
     * Key/value name/Font.
     */
    private final Map<String, Font> fonts;

    /** */
    private PdfDocumentFonts() {
        this.fonts = new HashMap<>();
    }

    public Map<String, Font> getFonts() {
        return this.fonts;
    }

    private void addFont(final Font font) {
        this.fonts.put(font.getName(), font);
    }

    public void matchWithSystemFont() {
        for (final Font font : this.getFonts().values()) {
            if ((!font.isEmbedded() || font.isStandardFont())) {
                font.matchWithSystemFont();
            }
        }
    }

    /**
     * Extracts the font information from page or XObject resources.
     *
     * @param collector
     *            Object to collect font info on.
     * @param fontNamesToDo
     *            Set of font names to get details for.
     * @param resource
     *            Resources dictionary.
     */
    private static void processResource(final PdfDocumentFonts collector,
            final PdfDictionary resource) {

        if (resource == null) {
            return;
        }

        final PdfDictionary xobjects = resource.getAsDict(PdfName.XOBJECT);
        if (xobjects != null) {
            for (PdfName key : xobjects.getKeys()) {
                // !!! Recurse !!!
                processResource(collector, xobjects.getAsDict(key));
            }
        }

        final PdfDictionary fonts = resource.getAsDict(PdfName.FONT);
        if (fonts == null) {
            return;
        }

        for (final PdfName key : fonts.getKeys()) {

            final PdfDictionary font = fonts.getAsDict(key);

            // Get base font name and skip leading '/' char.
            final String fontName =
                    font.getAsName(PdfName.BASEFONT).toString().substring(1);

            // Already got info?
            if (collector.getFonts().containsKey(fontName)) {
                return;
            }

            final PdfDictionary desc = font.getAsDict(PdfName.FONTDESCRIPTOR);

            final Font fontObj = new Font(fontName);

            //
            fontObj.setToUnicodeMap(font.contains(PdfName.TOUNICODE));

            //
            PdfName fontFile = null;

            if (desc != null) {
                for (final PdfName name : FONTFILE_ARRAY) {
                    if (desc.get(name) != null) {
                        fontFile = name;
                        break;
                    }
                }
            }

            final String fontSubType =
                    font.getAsName(PdfName.SUBTYPE).toString();

            PdfName subType = null;
            for (final PdfName name : SUBTYPE_ARRAY) {
                if (fontSubType.equals(name.toString())) {
                    subType = name;
                    break;
                }
            }

            FontType fontTypeEnum = FontType.UNKNOWN;

            if (subType == PdfName.TYPE0) {
                fontTypeEnum = FontType.CID_TT;
            } else if (subType == PdfName.TYPE1 || subType == PdfName.MMTYPE1) {
                if (fontFile == PdfName.FONTFILE3) {
                    fontTypeEnum = FontType.TYPE_1C;
                } else {
                    fontTypeEnum = FontType.TYPE_1;
                }
            } else if (subType == PdfName.TRUETYPE) {
                fontTypeEnum = FontType.TRUETYPE;
            } else if (subType == PdfName.CIDFONTTYPE0) {
                fontTypeEnum = FontType.CID_0;
            } else if (subType == PdfName.CIDFONTTYPE2) {
                fontTypeEnum = FontType.CID_2;
            }

            if (fontTypeEnum == FontType.UNKNOWN) {
                fontObj.setTypeUnknown(fontSubType);
            }

            fontObj.setType(fontTypeEnum);

            //
            final Encoding encodingEnum;

            if (font.contains(PdfName.ENCODING)) {

                final PdfName encodingName = font.getAsName(PdfName.ENCODING);

                if (encodingName == null) {
                    encodingEnum = Encoding.CUSTOM;
                } else {
                    final String encoding =
                            encodingName.toString().substring(1);

                    if (encoding.startsWith("WinAnsi")) {
                        encodingEnum = Encoding.WINANSI;
                    } else if (encoding.startsWith(BaseFont.MACROMAN)) {
                        encodingEnum = Encoding.MACROMAN;
                    } else {
                        encodingEnum = Encoding.UNKNOWN;
                        fontObj.setEncodingUnknown(encoding);
                    }
                }
            } else {
                // Cp1252 == WinAnsi
                if (FontFactory.defaultEncoding.equals(BaseFont.WINANSI)) {
                    encodingEnum = Encoding.WINANSI;
                } else {
                    encodingEnum = Encoding.UNKNOWN;
                    fontObj.setEncodingUnknown(FontFactory.defaultEncoding);
                }
            }

            fontObj.setEncoding(encodingEnum);

            //
            final boolean subset = fontName.contains("+");
            fontObj.setSubset(subset);

            //
            final boolean embedded = subset || fontFile != null;
            fontObj.setEmbedded(embedded);

            //
            collector.addFont(fontObj);

            // TODO Do we need to check descendant fonts?
            boolean checkDescendantFonts = false; // ??
            if (checkDescendantFonts) {
                final PdfArray descendant =
                        font.getAsArray(PdfName.DESCENDANTFONTS);

                if (descendant != null) {
                    for (int i = 0; i < descendant.size(); i++) {
                        PdfDictionary dic = descendant.getAsDict(i);
                        // !!! Recurse !!!
                        processResource(collector, dic);
                    }
                }
            }
        }

    }

    /**
     * Create info for PDF file.
     *
     * @param file
     *            PDF file.
     * @return info.
     * @throws IOException
     *             If file access error.
     */
    public static PdfDocumentFonts create(final File file) throws IOException {

        final PdfDocumentFonts info = new PdfDocumentFonts();
        final PdfReader reader = new PdfReader(file.getAbsolutePath());

        try {
            // Traverse pages to get font details.
            for (int k = 1; k <= reader.getNumberOfPages(); ++k) {
                processResource(info,
                        reader.getPageN(k).getAsDict(PdfName.RESOURCES));
            }

            info.matchWithSystemFont();

        } finally {
            reader.close();
        }
        return info;
    }

    /**
     *
     * @param args
     *            Files.
     */
    public static void main(final String[] args) {

        if (args.length == 0) {
            System.err.println("Enter files as argument.");
            return;
        }

        try {
            for (final String file : args) {

                final long startTime = System.currentTimeMillis();

                System.out.println("+---------------------------------");
                System.out.println("| " + file);
                System.out.println("+---------------------------------");

                final PdfDocumentFonts info =
                        PdfDocumentFonts.create(new File(file));

                System.out.println(PdfDocumentFonts.Font.getUiHeader());

                for (final PdfDocumentFonts.Font font : info.getFonts()
                        .values()) {
                    System.out.println(font.getUiLine());
                }

                System.out.println(DateUtil.formatDuration(
                        System.currentTimeMillis() - startTime));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
