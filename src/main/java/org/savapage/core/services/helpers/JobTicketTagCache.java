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
package org.savapage.core.services.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.JobTicketTagDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketTagCache {

    /** */
    private static final String TICKER_NUMBER_PREFIX_TAG_SEPARATOR = "/";

    /** */
    private static volatile SortedMap<String, JobTicketTagDto> tagsByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, JobTicketTagDto> tagsByWord =
            new TreeMap<>();

    /**
     * Static methods only.
     */
    private JobTicketTagCache() {
    }

    /**
     *
     * @return The sorted tags from cache, or empty when no tags are defined.
     */
    public static Collection<JobTicketTagDto> getTicketTagsByWord() {
        return tagsByWord.values();
    }

    /**
     * Returns tag by ID from cache.
     *
     * @param tagID
     *            The tag ID.
     * @return The {@link JobTicketTagDto} or null if this ID does not exist.
     */
    public static JobTicketTagDto getTicketTag(final String tagID) {
        return tagsByID.get(tagID);
    }

    /**
     * Sets the tags in the cache.
     *
     * @param tags
     *            The tag kist.
     */
    public static void setTicketTags(final List<JobTicketTagDto> tags) {

        final TreeMap<String, JobTicketTagDto> mapByID = new TreeMap<>();
        final TreeMap<String, JobTicketTagDto> mapByWord = new TreeMap<>();

        for (final JobTicketTagDto dto : tags) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getWord(), dto);
        }

        tagsByID = mapByID;
        tagsByWord = mapByWord;
    }

    /**
     * Parses formatted ticket tags (cache is <b>not</b> updated).
     *
     * @param formattedTags
     *            The formatted tags string.
     * @return The tag list.
     * @throws IllegalArgumentException
     *             When invalid tag format.
     */
    public static List<JobTicketTagDto>
            parseTicketTags(final String formattedTags) {

        final List<JobTicketTagDto> list = new ArrayList<>();

        final String regexKey = "^[A-Z0-9]+$";
        final int maxKeyLen = 5;

        final String tags = StringUtils
                .remove(StringUtils.remove(formattedTags, '\n'), '\r');

        for (final String tag : StringUtils.split(tags, ',')) {

            final String[] res =
                    StringUtils.split(tag, TICKER_NUMBER_PREFIX_TAG_SEPARATOR);

            if (res.length != 2) {
                throw new IllegalArgumentException(String
                        .format("Job Ticket tag [%s]: invalid format.", tag));
            }

            final String tagID = res[0];
            final String tagWord = res[1];

            if (!tagID.matches(regexKey)) {
                throw new IllegalArgumentException(String.format(
                        "Job Ticket tag [%s]: ID [%s] "
                                + "does not match regex [%s].",
                        tag, tagID, regexKey));
            }

            if (tagID.length() > maxKeyLen) {
                throw new IllegalArgumentException(String.format(
                        "Job Ticket tag [%s]: ID [%s] "
                                + "has more then %d characters.",
                        tag, tagID, maxKeyLen));
            }

            final JobTicketTagDto dto = new JobTicketTagDto();
            dto.setId(tagID);
            dto.setWord(tagWord);

            list.add(dto);
        }
        return list;
    }

}
