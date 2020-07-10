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
package org.savapage.core.ipp.helpers;

import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrCollectionSet;
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
     * Creates a 1setOf collection with 'media-size' collections each with
     * attributes 'x-dimension' and 'y-dimension' of {@link IppMediaSizeEnum}
     * values.
     *
     * @param keyword
     *            IPP keyword of the collection.
     * @param ippMediaValues
     *            {@link IppMediaSizeEnum} values.
     * @return The {@link IppAttrCollection}.
     */
    public static IppAttrCollection createMediaCollection(final String keyword,
            final IppMediaSizeEnum[] ippMediaValues) {

        final IppAttrCollection collection = new IppAttrCollection(keyword);
        return createMediaCollection(collection, keyword, ippMediaValues);
    }

    /**
     * Creates a 1setOf collection set of collections with attributes
     * 'x-dimension' and 'y-dimension' of {@link IppMediaSizeEnum} values.
     *
     * @param keyword
     *            IPP keyword of the collection.
     * @param ippMediaValues
     *            {@link IppMediaSizeEnum} values.
     * @return The {@link IppAttrCollectionSet}.
     */
    public static IppAttrCollectionSet createMediaCollectionSet(
            final String keyword, final IppMediaSizeEnum[] ippMediaValues) {

        final IppAttrCollectionSet collection =
                new IppAttrCollectionSet(keyword);
        createMediaCollection(collection, keyword, ippMediaValues);
        return collection;
    }

    /**
     * Creates a 1setOf collection set of collections with attributes
     * 'x-dimension' and 'y-dimension' of {@link IppMediaSizeEnum} values.
     *
     * @param collection
     *            The {@link IppAttrCollection} to append on.
     * @param keyword
     *            IPP keyword of the collection.
     * @param ippMediaValues
     *            {@link IppMediaSizeEnum} values.
     * @return The collection appended on.
     */
    private static IppAttrCollection createMediaCollection(
            final IppAttrCollection collection, final String keyword,
            final IppMediaSizeEnum[] ippMediaValues) {

        for (final IppMediaSizeEnum ippMediaSize : ippMediaValues) {

            final MediaSizeName sizeName = ippMediaSize.getMediaSizeName();

            if (MediaSize.getMediaSizeForName(sizeName) == null) {
                continue;
            }

            final IppAttrCollection mediaCollection;
            if (collection instanceof IppAttrCollectionSet) {
                mediaCollection = new IppAttrCollection(null);
            } else {
                mediaCollection = new IppAttrCollection(
                        IppDictJobTemplateAttr.ATTR_MEDIA_SIZE);
            }
            collection.addCollection(
                    createMediaSizeCollection(mediaCollection, sizeName));
        }
        return collection;
    }

    /**
     * Creates a media-size collection.
     *
     * @param collection
     *            The {@link IppAttrCollection} to append on.
     * @param sizeName
     *            {@link MediaSizeName}. The IPP "media" keyword value.
     * @return The collection appended on.
     */
    private static IppAttrCollection createMediaSizeCollection(
            final IppAttrCollection collection, final MediaSizeName sizeName) {

        final int[] array = MediaUtils.getMediaWidthHeight(sizeName);

        collection.add(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE_X_DIMENSION,
                IPP_INTEGER_ZERO, String.valueOf(array[0] * HUNDREDTH_MM));

        collection.add(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE_Y_DIMENSION,
                IPP_INTEGER_ZERO, String.valueOf(array[1] * HUNDREDTH_MM));

        return collection;
    }

    /**
     * Creates a 'media-size' collection with attributes 'x-dimension' and
     * 'y-dimension'.
     *
     * @param ippMediaValue
     *            The IPP "media" keyword value.
     * @return The {@link IppAttrCollection}.
     */
    public static IppAttrCollection
            createMediaSizeCollection(final String ippMediaValue) {

        return createMediaSizeCollection(
                new IppAttrCollection(IppDictJobTemplateAttr.ATTR_MEDIA_SIZE),
                IppMediaSizeEnum.findMediaSizeName(ippMediaValue));
    }

}
