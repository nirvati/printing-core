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
package org.savapage.core.ipp.attribute.syntax;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.savapage.core.ipp.encoding.IppValueTag;

/**
 *
 * @author Datraverse B.V.
 */
public class IppKeyword extends AbstractIppAttrSyntax {

    // ------------------------------------------------------------------------
    // compression: type3 keyword
    // ------------------------------------------------------------------------

    /**
     * No compression is used.
     */
    public static final String COMPRESSION_NONE = "none";

    /**
     * ZIP public domain inflate/deflate) compression technology in RFC1951.
     */
    public static final String COMPRESSION_DEFLATE = "deflate";

    /**
     * GNU zip compression technology described in RFC 1952
     */
    public static final String COMPRESSION_GZIP = "gzip";

    /**
     * UNIX compression technology in RFC1977.
     */
    public static final String COMPRESSION_COMPRESS = "compress";

    // ------------------------------------------------------------------------
    // print-color-mode
    // ------------------------------------------------------------------------
    /**
     * Automatic based on document REQUIRED
     */
    public static final String PRINT_COLOR_MODE_AUTO = "auto";

    /**
     * 1-colorant (typically black) threshold output OPTIONAL (note 1)
     */
    public static final String PRINT_COLOR_MODE_BI_LEVEL = "bi-level";

    /**
     * Full-color output CONDITIONALLY REQUIRED (note 2)
     */
    public static final String PRINT_COLOR_MODE_COLOR = "color";

    /**
     * 1-colorant + black output OPTIONAL
     */
    public static final String PRINT_COLOR_MODE_HIGHLIGHT = "highlight";

    /**
     * 1-colorant (typically black) shaded/grayscale output REQUIRED
     */
    public static final String PRINT_COLOR_MODE_MONOCHROME = "monochrome";

    /**
     * Process (2 or more colorants) threshold output OPTIONAL
     */
    public static final String PRINT_COLOR_MODE_PROCESS_BI_LEVEL =
            "process-bi-level";

    /**
     * Process (2 or more colorants) shaded/grayscale output
     */
    public static final String PRINT_COLOR_MODE_PROCESS_MONOCHROME =
            "process-monochrome";


    // ------------------------------------------------------------------------
    // media-source
    // ------------------------------------------------------------------------
    public static final String MEDIA_SOURCE_AUTO = "auto";
    public static final String MEDIA_SOURCE_MANUAL = "manual";

    // ------------------------------------------------------------------------
    // sides
    // ------------------------------------------------------------------------
    public static final String SIDES_ONE_SIDED = "one-sided";
    public static final String SIDES_TWO_SIDED_LONG_EDGE =
            "two-sided-long-edge";
    public static final String SIDES_TWO_SIDED_SHORT_EDGE =
            "two-sided-short-edge";

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppKeyword#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppKeyword INSTANCE = new IppKeyword();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppKeyword instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.KEYWORD;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        /*
         * Ignore the offered charset, use US_ASCII instead.
         */
        writeUsAscii(ostr, value);
    }

}
