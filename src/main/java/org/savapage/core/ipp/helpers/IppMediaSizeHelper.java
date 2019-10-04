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
package org.savapage.core.ipp.helpers;

import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.util.MediaUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppMediaSizeHelper {

    /**
     * Number of hundredth in a mm.
     */
    private static final int HUNDREDTH_MM = 100;

    /** */
    private static final IppInteger IPP_INTEGER_ZERO = new IppInteger(0);

    /**
     * Utility class.
     */
    private IppMediaSizeHelper() {
    }

    /**
     * Creates a media-size collection of all IppMediaSizeEnum values.
     *
     * @return The {@link IppAttrCollection}.
     */
    public static IppAttrCollection createMediaSizeCollection() {

        final IppAttrCollection collectionMediaSize =
                new IppAttrCollection(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE);

        for (final IppMediaSizeEnum ippMediaSize : IppMediaSizeEnum.values()) {
            final MediaSizeName sizeName = ippMediaSize.getMediaSizeName();
            if (MediaSize.getMediaSizeForName(sizeName) == null) {
                continue;
            }
            addMediaSize(collectionMediaSize, sizeName);
        }
        return collectionMediaSize;
    }

    /**
     * Adds a media size to collection.
     *
     * @param collection
     *            The collection.
     * @param sizeName
     *            MediaSizeName.
     */
    private static void addMediaSize(final IppAttrCollection collection,
            final MediaSizeName sizeName) {

        final int[] array = MediaUtils.getMediaWidthHeight(sizeName);

        collection.add(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE_X_DIMENSION,
                IPP_INTEGER_ZERO, String.valueOf(array[0] * HUNDREDTH_MM));

        collection.add(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE_Y_DIMENSION,
                IPP_INTEGER_ZERO, String.valueOf(array[1] * HUNDREDTH_MM));
    }

    /**
     * Creates a media-size collection with one item.
     *
     * @param ippMediaValue
     *            The IPP "media" keyword value.
     * @return The {@link IppAttrCollection}.
     */
    public static IppAttrCollection
            createMediaSizeCollection(final String ippMediaValue) {

        final IppAttrCollection collectionMediaSize =
                new IppAttrCollection(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE);

        final MediaSizeName sizeName =
                IppMediaSizeEnum.findMediaSizeName(ippMediaValue);

        addMediaSize(collectionMediaSize, sizeName);

        return collectionMediaSize;
    }

}
