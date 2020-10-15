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
package org.savapage.core.services.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.PrinterDao.MediaSourceAttr;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.util.NumberUtil;

/**
 * Encapsulation of Printer attribute map.
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterAttrLookup {

    /**
     * .
     */
    private final Map<String, String> lookup;

    /**
     *
     */
    private final boolean mediaSourcePresent;

    /**
     * Creates a lookup for Printer attribute values.
     *
     * @param printer
     *            The Printer.
     */
    public PrinterAttrLookup(final Printer printer) {

        this.lookup = new HashMap<>();

        boolean hasMediaSourceAttr = false;

        if (printer.getAttributes() != null) {

            for (final PrinterAttr attr : printer.getAttributes()) {

                lookup.put(attr.getName(), attr.getValue());

                if (attr.getName().startsWith(MediaSourceAttr.getKeyPrefix())) {
                    hasMediaSourceAttr = true;
                }
            }
        }
        this.mediaSourcePresent = hasMediaSourceAttr;
    }

    /**
     * Gets the attribute value.
     *
     * @param key
     *            The attribute {@link PrinterAttrEnum} key.
     * @return The attribute value, or {@code null} when not found.
     */
    public String get(final PrinterAttrEnum key) {
        return get(key.getDbName());
    }

    /**
     * Gets the attribute value.
     *
     * @param key
     *            The attribute key as String.
     * @return The attribute value, or {@code null} when not found.
     */
    public String get(final String key) {
        return lookup.get(key);
    }

    /**
     * Gets the attribute value of a {@link IppKeyword#MEDIA_SOURCE_MANUAL}.
     *
     * @return The {@link IppMediaSourceCostDto} object, or {@code null} when
     *         not found.
     */
    public IppMediaSourceCostDto getMediaSourceManual() {
        final PrinterDao.MediaSourceAttr attr =
                new PrinterDao.MediaSourceAttr(IppKeyword.MEDIA_SOURCE_MANUAL);
        return get(attr);
    }

    /**
     * Gets the attribute value of a {@link PrinterDao.MediaSourceAttr}.
     *
     * @param attr
     *            The {@link PrinterDao.MediaSourceAttr}.
     * @return The {@link IppMediaSourceCostDto} object, or {@code null} when
     *         not found.
     */
    public IppMediaSourceCostDto get(final PrinterDao.MediaSourceAttr attr) {

        IppMediaSourceCostDto dto = null;

        final String json = this.get(attr.getKey());

        if (json != null) {
            try {
                dto = IppMediaSourceCostDto.create(json);
            } catch (IOException e) {
                dto = null;
            }
        }

        return dto;
    }

    /**
     * Gets the attribute value.
     *
     * @param key
     *            The attribute {@link PrinterAttrEnum} key.
     * @param dfault
     *            The default value.
     * @return The attribute value, or the default when not found.
     */
    public String get(final PrinterAttrEnum key, final String dfault) {
        String value = this.get(key);
        if (value == null) {
            value = dfault;
        }
        return value;
    }

    /**
     * Checks if the the value of the attribute key represents "true" value.
     *
     * @param key
     *            The attribute {@link PrinterAttrEnum} key.
     * @param dfault
     *            The default value.
     * @return {@code true} when key value represents true.
     */
    public boolean isTrue(final PrinterAttrEnum key, final boolean dfault) {

        final boolean bValue;
        final String value = this.get(key);

        if (value == null) {
            bValue = dfault;
        } else {
            bValue = value.equals(IConfigProp.V_YES);
        }

        return bValue;
    }

    /**
     * Finds any matching printer {@link IppMediaSourceCostDto} for a
     * {@link IppMediaSizeEnum}. When there are more then one (1) candidates a
     * random candidate is returned.
     *
     * @param ippMediaSize
     *            The {@link IppMediaSizeEnum} to look for.
     * @return {@code null} when not found.
     */
    public IppMediaSourceCostDto
            findAnyMediaSourceForMedia(final IppMediaSizeEnum ippMediaSize) {

        final List<IppMediaSourceCostDto> mediaSourceCostList =
                findMediaSourcesForMedia(ippMediaSize);

        if (mediaSourceCostList.isEmpty()) {
            return null;
        }

        final int iMediaSource;

        if (mediaSourceCostList.size() == 1) {

            iMediaSource = 0;

        } else {
            /*
             * Pick a random media-source.
             */
            iMediaSource = NumberUtil.getRandomNumber(0,
                    mediaSourceCostList.size() - 1);
        }
        return mediaSourceCostList.get(iMediaSource);
    }

    /**
     * Finds the unique matching printer {@link IppMediaSourceCostDto} for a
     * {@link IppMediaSizeEnum}.
     *
     * @param ippMediaSize
     *            The {@link IppMediaSizeEnum}.
     * @return {@code null} when not exactly one (1) found.
     */
    public IppMediaSourceCostDto
            findUniqueMediaSourceForMedia(final IppMediaSizeEnum ippMediaSize) {

        final IppMediaSourceCostDto ippMediaSourceCost;

        if (ippMediaSize == null) {

            ippMediaSourceCost = null;

        } else {

            final List<IppMediaSourceCostDto> mediaSourceCostList =
                    findMediaSourcesForMedia(ippMediaSize);

            if (mediaSourceCostList.size() == 1) {
                ippMediaSourceCost = mediaSourceCostList.get(0);
            } else {
                ippMediaSourceCost = null;
            }
        }

        return ippMediaSourceCost;
    }

    /**
     * Finds the matching printer {@link IppMediaSourceCostDto} objects for a
     * {@link IppMediaSizeEnum}.
     *
     * @param ippMediaSize
     *            The {@link IppMediaSizeEnum}.
     * @return The list of matching {@link IppMediaSourceCostDto} objects.
     */
    public List<IppMediaSourceCostDto>
            findMediaSourcesForMedia(final IppMediaSizeEnum ippMediaSize) {

        final List<IppMediaSourceCostDto> mediaSourceCostList =
                new ArrayList<>();

        if (ippMediaSize == null) {
            return mediaSourceCostList;
        }

        for (final Entry<String, String> entry : this.lookup.entrySet()) {

            final PrinterDao.MediaSourceAttr mediaSourceAttr =
                    PrinterDao.MediaSourceAttr.createFromDbKey(entry.getKey());

            if (mediaSourceAttr != null) {

                final IppMediaSourceCostDto ippMediaSourceCostWlk =
                        this.get(mediaSourceAttr);

                /*
                 * Note: media-source 'auto' and 'manual' do NOT have cost.
                 */
                final IppMediaCostDto mediaCost =
                        ippMediaSourceCostWlk.getMedia();

                if (mediaCost != null && mediaCost.getMedia()
                        .equals(ippMediaSize.getIppKeyword())) {
                    mediaSourceCostList.add(ippMediaSourceCostWlk);
                }
            }
        }

        return mediaSourceCostList;
    }

    /**
     *
     * @return {@code true} if the lookup contains media-source attributes.
     */
    public boolean isMediaSourcePresent() {
        return mediaSourcePresent;
    }

}
