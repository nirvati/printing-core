/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.ipp;

import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.standard.MediaSizeName;

/**
 *
 * A dictionary of IPP Media Sizes (RFC2911 and PWG5100.1).
 *
 * @author Rijk Ravestein
 *
 */
public enum IppMediaSizeEnum {
    //
    ISO_A0(MediaSizeName.ISO_A0, "iso_a0_841x1189mm"),
    ISO_A1(MediaSizeName.ISO_A1, "iso_a1_594x841mm"),
    ISO_A2(MediaSizeName.ISO_A2, "iso_a2_420x594mm"),
    ISO_A3(MediaSizeName.ISO_A3, "iso_a3_297x420mm"),
    ISO_A4(MediaSizeName.ISO_A4, "iso_a4_210x297mm"),
    ISO_A5(MediaSizeName.ISO_A5, "iso_a5_148x210mm"),
    ISO_A6(MediaSizeName.ISO_A6, "iso_a6_105x148mm"),
    ISO_A7(MediaSizeName.ISO_A7, "iso_a7_74x105mm"),
    ISO_A8(MediaSizeName.ISO_A8, "iso_a8_52x74mm"),
    ISO_A9(MediaSizeName.ISO_A9, "iso_a9_37x52mm"),
    ISO_A10(MediaSizeName.ISO_A10, "iso_a10_26x37mm"),

    A(MediaSizeName.A, "a"), //
    B(MediaSizeName.B, "b"), //
    C(MediaSizeName.C, "c"), //
    D(MediaSizeName.D, "d"), //
    E(MediaSizeName.E, "e"), //

    ISO_B0(MediaSizeName.ISO_B0, "iso_b0_1000x1414mm"),
    ISO_B1(MediaSizeName.ISO_B1, "iso_b1_707x1000mm"),
    ISO_B2(MediaSizeName.ISO_B2, "iso_b2_500x707mm"),
    ISO_B3(MediaSizeName.ISO_B3, "iso_b3_353x500mm"),
    ISO_B4(MediaSizeName.ISO_B4, "iso_b4_250x353mm"),
    ISO_B5(MediaSizeName.ISO_B5, "iso_b5_176x250mm"),
    ISO_B6(MediaSizeName.ISO_B6, "iso_b6_125x176mm"),
    ISO_B7(MediaSizeName.ISO_B7, "iso_b7_88x125mm"),
    ISO_B8(MediaSizeName.ISO_B8, "iso_b8_62x88mm"),
    ISO_B9(MediaSizeName.ISO_B9, "iso_b9_44x62mm"),
    ISO_B10(MediaSizeName.ISO_B10, "iso_b10_31x44mm"),

    ISO_C0(MediaSizeName.ISO_C0, "iso_c0_917x1297mm"),
    ISO_C1(MediaSizeName.ISO_C1, "iso_c1_648x917mm"),
    ISO_C2(MediaSizeName.ISO_C2, "iso_c2_458x648mm"),
    ISO_C3(MediaSizeName.ISO_C3, "iso_c3_324x458mm"),
    ISO_C4(MediaSizeName.ISO_C4, "iso_c4_229x324mm"),
    ISO_C5(MediaSizeName.ISO_C5, "iso_c5_162x229mm"),
    ISO_C6(MediaSizeName.ISO_C6, "iso_c6_114x162mm"),

    JIS_B0(MediaSizeName.JIS_B0, "jis_b0_1030x1456mm"),
    JIS_B1(MediaSizeName.JIS_B1, "jis_b1_728x1030mm"),
    JIS_B2(MediaSizeName.JIS_B2, "jis_b2_515x728mm"),
    JIS_B3(MediaSizeName.JIS_B3, "jis_b3_364x515mm"),
    JIS_B4(MediaSizeName.JIS_B4, "jis_b4_257x364mm"),
    JIS_B5(MediaSizeName.JIS_B5, "jis_b5_182x257mm"),
    JIS_B6(MediaSizeName.JIS_B6, "jis_b6_128x182mm"),
    JIS_B7(MediaSizeName.JIS_B7, "jis_b7_91x128mm"),
    JIS_B8(MediaSizeName.JIS_B8, "jis_b8_64x91mm"),
    JIS_B9(MediaSizeName.JIS_B9, "jis_b9_45x64mm"),
    JIS_B10(MediaSizeName.JIS_B10, "jis_b10_32x45mm"),

    NA_LETTER(MediaSizeName.NA_LETTER, "na_letter_8.5x11in"),
    NA_LEGAL(MediaSizeName.NA_LEGAL, "na_legal_8.5x14in"),

    // Identical to Tabloid (obsolete).
    LEDGER(MediaSizeName.LEDGER, "na_ledger_11x17in"),
    INVOICE(MediaSizeName.INVOICE, "na_invoice_5.5x8.5in"),
    FOLIO(MediaSizeName.FOLIO, "om_folio_210x330mm"), // TODO
    QUARTO(MediaSizeName.QUARTO, "na_quarto_8.5x10.83in"),

    JAPANESE_POSTCARD(MediaSizeName.JAPANESE_POSTCARD),
    JAPANESE_DOUBLE_POSTCARD(MediaSizeName.JAPANESE_DOUBLE_POSTCARD),
    ISO_DESIGNATED_LONG(MediaSizeName.ISO_DESIGNATED_LONG),
    ITALY_ENVELOPE(MediaSizeName.ITALY_ENVELOPE),
    MONARCH_ENVELOPE(MediaSizeName.MONARCH_ENVELOPE, "na_monarch_3.875x7.5in"),
    PERSONAL_ENVELOPE(MediaSizeName.PERSONAL_ENVELOPE,
            "na_personal_3.625x6.5in"),

    // <entry key="ipp-attr-media-oufuko-postcard"></entry> // ??

    NA_NUMBER_9_ENVELOPE(MediaSizeName.NA_NUMBER_9_ENVELOPE,
            "na_number-9_3.875x8.875in"),
    NA_NUMBER_10_ENVELOPE(MediaSizeName.NA_NUMBER_10_ENVELOPE,
            "na_number-10_4.125x9.5in"),
    NA_NUMBER_11_ENVELOPE(MediaSizeName.NA_NUMBER_11_ENVELOPE,
            "na_number-11_4.5x10.375in"),
    NA_NUMBER_12_ENVELOPE(MediaSizeName.NA_NUMBER_12_ENVELOPE,
            "na_number-12_4.75x11in"),
    NA_NUMBER_14_ENVELOPE(MediaSizeName.NA_NUMBER_14_ENVELOPE,
            "na_number-14_5x11.5in"),

    NA_6x9_ENVELOPE(MediaSizeName.NA_6X9_ENVELOPE, "na_6x9_6x9in"),
    NA_7x9_ENVELOPE(MediaSizeName.NA_7X9_ENVELOPE, "na_7x9_7x9in"),
    NA_9x11_ENVELOPE(MediaSizeName.NA_9X11_ENVELOPE, "na_9x11_9x11in"),
    NA_9x12_ENVELOPE(MediaSizeName.NA_9X12_ENVELOPE),
    NA_10x13_ENVELOPE(MediaSizeName.NA_10X13_ENVELOPE, "na_10x13_10x13in"),
    NA_10x14_ENVELOPE(MediaSizeName.NA_10X14_ENVELOPE, "na_10x14_10x14in"),
    NA_10x15_ENVELOPE(MediaSizeName.NA_10X15_ENVELOPE, "na_10x15_10x15in"),
    NA_5X7(MediaSizeName.NA_5X7, "na_5x7_5x7in"), //
    NA_8X10(MediaSizeName.NA_8X10);

    /** */
    private MediaSizeName mediaSizeName;
    /** */
    private String ippKeyword;

    /**
     *
     * @author rijk
     *
     */
    private static class Lookups {
        /** */
        private static Map<String, IppMediaSizeEnum> findByMediaSizeName =
                new HashMap<>();
        /** */
        private static Map<String, IppMediaSizeEnum> findByIppKeyword =
                new HashMap<>();
    }

    private IppMediaSizeEnum(final MediaSizeName sizeName,
            final String keyword) {
        init(sizeName, keyword);
    }

    private IppMediaSizeEnum(final MediaSizeName sizeName) {
        init(sizeName, sizeName.toString());
    }

    private void init(final MediaSizeName sizeName, final String keyword) {
        this.mediaSizeName = sizeName;
        this.ippKeyword = keyword;
        Lookups.findByMediaSizeName.put(sizeName.toString(), this);
        Lookups.findByIppKeyword.put(keyword, this);
    }

    public MediaSizeName getMediaSizeName() {
        return this.mediaSizeName;
    }

    public String getIppKeyword() {
        return this.ippKeyword;
    }

    public static IppMediaSizeEnum find(final MediaSizeName sizeName) {
        return Lookups.findByMediaSizeName.get(sizeName.toString());
    }

    /**
     *
     * @param ippKeyword
     *            The IPP PWG keyword value.
     * @return {@code null} when not found.
     */
    public static IppMediaSizeEnum find(final String ippKeyword) {
        return Lookups.findByIppKeyword.get(ippKeyword.toLowerCase());
    }

    /**
     *
     * @param ippKeyword
     *            The IPP PWG keyword value.
     * @return {@code null} when not found.
     */
    public static MediaSizeName findMediaSizeName(final String ippKeyword) {

        final IppMediaSizeEnum value =
                Lookups.findByIppKeyword.get(ippKeyword.toLowerCase());
        if (value == null) {
            return null;

        }
        return value.getMediaSizeName();
    }

}
