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

import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * SIGNED-INTEGER.
 *
 * @author Datraverse B.V.
 */
public class IppEnum extends AbstractIppAttrSyntax {

    // ------------------------------------------------------------------------
    // orientation
    // ------------------------------------------------------------------------

    /**
     * 'portait': The content will be imaged across the short edge of the
     * medium.
     */
    public static final int ORIENTATION_PORTRAIT = 3;

    /**
     * 'landscape': The content will be imaged across the long edge of the
     * medium. Landscape is defined to be a rotation of the print-stream page to
     * be imaged by +90 degrees with respect to the medium (i.e. anti-clockwise)
     * from the portrait orientation. Note: The +90 direction was chosen because
     * simple finishing on the long edge is the same edge whether portrait or
     * landscape
     */
    public static final int ORIENTATION_LANDSCAPE = 4;

    /**
     * 'reverse-landscape': The content will be imaged across the long edge of
     * the medium. Reverse-landscape is defined to be a rotation of the
     * print-stream page to be imaged by - 90 degrees with respect to the medium
     * (i.e. clockwise) from the portrait orientation. Note: The 'reverse-
     * landscape' value was added because some applications rotate landscape -90
     * degrees from portrait, rather than +90 degrees.
     */
    public static final int ORIENTATION_REVERSE_LANDSCAPE = 5;

    /**
     * 'reverse-portrait': The content will be imaged across the short edge of
     * the medium. Reverse-portrait is defined to be a rotation of the
     * print-stream page to be imaged by 180 degrees with respect to the medium
     * from the portrait orientation. Note: The 'reverse-portrait' value was
     * added for use with the "finishings" attribute in cases where the opposite
     * edge is desired for finishing a portrait document on simple finishing
     * devices that have only one finishing position. Thus a 'text'/plain'
     * portrait document can be stapled "on the right" by a simple finishing
     * device as is common use with some middle eastern languages such as
     * Hebrew.
     */
    public static final int ORIENTATION_REVERSE_PORTRAIT = 6;

    // ------------------------------------------------------------------------
    // print-quality
    // ------------------------------------------------------------------------
    /**
     * Lowest quality available on the printer.
     */
    public static final int PRINT_QUALITY_DRAFT = 3;

    /**
     * Normal or intermediate quality on the printer.
     */
    public static final int PRINT_QUALITY_NORMAL = 4;

    /**
     * Highest quality available on the printer.
     */
    public static final int PRINT_QUALITY_HIGH = 5;

    // ------------------------------------------------------------------------
    // finishings
    // ------------------------------------------------------------------------

    /**
     * 'none': Perform no finishing
     */
    public static final int FINISHING_NONE = 3;

    /**
     * 'staple': Bind the document(s) with one or more staples. The exact number
     * and placement of the staples is site- defined.
     */
    public static final int FINISHING_STAPLE = 4;

    /**
     * 'punch': This value indicates that holes are required in the finished
     * document. The exact number and placement of the holes is site-defined The
     * punch specification MAY be satisfied (in a site- and
     * implementation-specific manner) either by drilling/punching, or by
     * substituting pre- drilled media.
     */
    public static final int FINISHING_PUNCH = 5;

    /**
     * 'cover': This value is specified when it is desired to select a
     * non-printed (or pre-printed) cover for the document. This does not
     * supplant the specification of a printed cover (on cover stock medium) by
     * the document itself.
     */
    public static final int FINISHING_COVER = 6;

    /**
     * 'bind': This value indicates that a binding is to be applied to the
     * document; the type and placement of the binding is site-defined.
     */
    public static final int FINISHING_BIND = 7;

    /**
     * 'saddle-stitch': Bind the document(s) with one or more staples (wire
     * stitches) along the middle fold. The exact number and placement of the
     * staples and the middle fold is implementation and/or site-defined.
     */
    public static final int FINISHING_SADDLE_STITCH = 8;

    /**
     * 'edge-stitch': Bind the document(s) with one or more staples (wire
     * stitches) along one edge. The exact number and placement of the staples
     * is implementation and/or site- defined.
     */
    public static final int FINISHING_EDGE_STITCH = 9;

    // '10'-'19' reserved for future generic finishing enum values.

    // The following values are more specific; they indicate a corner or an
    // edge as if the document were a portrait document (see below):

    /**
     * 'staple-top-left': Bind the document(s) with one or more staples in the
     * top left corner.
     */
    public static final int FINISHING_STAPLE_TOP_LEFT = 20;

    /**
     * 'staple-bottom-left': Bind the document(s) with one or more staples in
     * the bottom left corner.
     */
    public static final int FINISHING_STAPLE_BOTTOM_LEFT = 21;

    /**
     * 'staple-top-right': Bind the document(s) with one or more staples in the
     * top right corner.
     */
    public static final int FINISHING_STAPLE_TOP_RIGHT = 22;

    /**
     * 'staple-bottom-right': Bind the document(s) with one or more staples in
     * the bottom right corner.
     */
    public static final int FINISHING_STAPLE_BOTTOM_RIGHT = 23;

    /**
     * 'edge-stitch-left': Bind the document(s) with one or more staples (wire
     * stitches) along the left edge. The exact number and placement of the
     * staples is implementation and/or site-defined.
     */
    public static final int FINISHING_STAPLE_EDGE_STITCH_LEFT = 24;

    /**
     * 'edge-stitch-top': Bind the document(s) with one or more staples (wire
     * stitches) along the top edge. The exact number and placement of the
     * staples is implementation and/or site-defined.
     */
    public static final int FINISHING_STAPLE_EDGE_STITCH_TOP = 25;

    /**
     * 'edge-stitch-right': Bind the document(s) with one or more staples (wire
     * stitches) along the right edge. The exact number and placement of the
     * staples is implementation and/or site-defined.
     */
    public static final int FINISHING_STAPLE_EDGE_STITCH_RIGHT = 26;

    /**
     * 'edge-stitch-bottom': Bind the document(s) with one or more staples (wire
     * stitches) along the bottom edge. The exact number and placement of the
     * staples is implementation and/or site-defined.
     */
    public static final int FINISHING_STAPLE_EDGE_STITCH_BOTTOM = 27;

    /**
     * 'staple-dual-left': Bind the document(s) with two staples (wire stitches)
     * along the left edge assuming a portrait document (see above).
     */
    public static final int FINISHING_STAPLE_DUAL_LEFT = 28;

    /**
     * 'staple-dual-top': Bind the document(s) with two staples (wire stitches)
     * along the top edge assuming a portrait document (see above).
     */
    public static final int FINISHING_STAPLE_DUAL_TOP = 29;

    /**
     * staple-dual-right': Bind the document(s) with two staples (wire stitches)
     * along the right edge assuming a portrait document (see above).
     */
    public static final int FINISHING_STAPLE_DUAL_RIGHT = 30;

    /**
     * 'staple-dual-bottom': Bind the document(s) with two staples (wire
     * stitches) along the bottom edge assuming a portrait document (see above).
     */
    public static final int FINISHING_STAPLE_DUAL_BOTTOM = 31;

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppEnum#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppEnum INSTANCE = new IppEnum();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static IppEnum instance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public final IppValueTag getValueTag() {
        return IppValueTag.ENUM;
    }

    @Override
    public final void write(final OutputStream ostr, final String value,
            final Charset charset) throws IOException {
        IppEncoder.writeInt16(ostr, 4);
        IppEncoder.writeInt32(ostr, Integer.parseInt(value));
    }

}
