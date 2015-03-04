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
package org.savapage.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.print.PageFormat;
import java.awt.print.Paper;

import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.junit.Test;
import org.savapage.core.util.MediaUtils;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class MediaUtilsTest {

    /**
     * Creates a PageFormat for a MediaSizeName, either in portrait or landscape
     * orientation.
     *
     * @param mediaSizeName
     * @param landscape
     * @return
     */
    private PageFormat createPageFormat(MediaSizeName mediaSizeName,
            boolean landscape) {

        final MediaSize mediaSize =
                MediaSize.getMediaSizeForName(mediaSizeName);

        final double width = mediaSize.getX(MediaSize.INCH) * 72;
        final double height = mediaSize.getY(MediaSize.INCH) * 72;

        final Paper paper = new Paper();

        if (landscape) {
            paper.setSize(height, width);
        } else {
            paper.setSize(width, height);
        }

        final PageFormat pageFormat = new PageFormat();
        pageFormat.setPaper(paper);

        return pageFormat;
    }

    @Test
    public void testPortait() {
        assertEquals("A4 portrait", MediaSizeName.ISO_A4.toString(), MediaUtils
                .getMediaSize(createPageFormat(MediaSizeName.ISO_A4, false))
                .toString());
    }

    @Test
    public void testLandscape() {

        assertEquals(
                "A4 landscape",
                MediaSizeName.ISO_A4.toString(),
                MediaUtils.getMediaSize(
                        createPageFormat(MediaSizeName.ISO_A4, true))
                        .toString());
    }

    @Test
    public void testMediaSizeCompare() {

        assertTrue(MediaUtils.compareMediaSize(MediaSizeName.ISO_A4,
                MediaSizeName.ISO_A4) == 0);

        assertTrue(MediaUtils.compareMediaSize(MediaSizeName.ISO_A4,
                MediaSizeName.ISO_A3) == -1);

        assertTrue(MediaUtils.compareMediaSize(MediaSizeName.ISO_A3,
                MediaSizeName.ISO_A4) == 1);

    }

}
