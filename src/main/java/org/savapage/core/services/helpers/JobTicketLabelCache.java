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
package org.savapage.core.services.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.JobTicketDomainDto;
import org.savapage.core.dto.JobTicketLabelDomainPartDto;
import org.savapage.core.dto.JobTicketTagDto;
import org.savapage.core.dto.JobTicketUseDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketLabelCache {

    /** */
    private static final String LABEL_WORDS_SEPARATOR = "/";

    /** */
    private static final String LABEL_ID_REGEX = "^[A-Z0-9]+$";

    /** */
    private static final int LABEL_ID_MAX_LENGTH = 5;

    /** */
    private static final int LABEL_SPLIT_IDX_ID = 0;
    /** */
    private static final int LABEL_SPLIT_IDX_NAME = 1;

    /** */
    private static volatile SortedMap<String, JobTicketDomainDto> domainsByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, JobTicketDomainDto> //
    domainsByName = new TreeMap<>();

    /** */
    private static volatile SortedMap<String, JobTicketUseDto> usesByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, JobTicketUseDto> usesByName =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, JobTicketTagDto> tagsByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, JobTicketTagDto> tagsByName =
            new TreeMap<>();

    /**
     * Static methods only.
     */
    private JobTicketLabelCache() {
    }

    /**
     *
     * @return The domains sorted by name from cache, or empty when no uses are
     *         defined.
     */
    public static Collection<JobTicketDomainDto> getTicketDomainsByName() {
        return domainsByName.values();
    }

    /**
     * Returns domain by ID from cache.
     *
     * @param id
     *            The domain ID.
     * @return The {@link JobTicketDomainDto} or null if this ID does not exist.
     */
    public static JobTicketDomainDto getTicketDomain(final String id) {
        return domainsByID.get(id);
    }

    /**
     *
     * @return The uses sorted by name from cache, or empty when no uses are
     *         defined.
     */
    public static Collection<JobTicketUseDto> getTicketUsesByName() {
        return usesByName.values();
    }

    /**
     * Returns use by ID from cache.
     *
     * @param id
     *            The use ID.
     * @return The {@link JobTicketUseDto} or null if this ID does not exist.
     */
    public static JobTicketUseDto getTicketUse(final String id) {
        return usesByID.get(id);
    }

    /**
     *
     * @return The tags sorted by name from cache, or empty when no tags are
     *         defined.
     */
    public static Collection<JobTicketTagDto> getTicketTagsByName() {
        return tagsByName.values();
    }

    /**
     * Returns tag by ID from cache.
     *
     * @param id
     *            The tag ID.
     * @return The {@link JobTicketTagDto} or null if this ID does not exist.
     */
    public static JobTicketTagDto getTicketTag(final String id) {
        return tagsByID.get(id);
    }

    /**
     * Sets the domains in the cache.
     *
     * @param domains
     *            The domain list.
     */
    private static void
            setTicketDomains(final List<JobTicketDomainDto> domains) {

        final TreeMap<String, JobTicketDomainDto> mapByID = new TreeMap<>();
        final TreeMap<String, JobTicketDomainDto> mapByWord = new TreeMap<>();

        for (final JobTicketDomainDto dto : domains) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }

        domainsByID = mapByID;
        domainsByName = mapByWord;
    }

    /**
     * Sets the uses in the cache.
     *
     * @param uses
     *            The use list.
     */
    private static void setTicketUses(final List<JobTicketUseDto> uses) {

        final TreeMap<String, JobTicketUseDto> mapByID = new TreeMap<>();
        final TreeMap<String, JobTicketUseDto> mapByWord = new TreeMap<>();

        for (final JobTicketUseDto dto : uses) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }

        usesByID = mapByID;
        usesByName = mapByWord;
    }

    /**
     * Sets the tags in the cache.
     *
     * @param tags
     *            The tag list.
     */
    private static void setTicketTags(final List<JobTicketTagDto> tags) {

        final TreeMap<String, JobTicketTagDto> mapByID = new TreeMap<>();
        final TreeMap<String, JobTicketTagDto> mapByWord = new TreeMap<>();

        for (final JobTicketTagDto dto : tags) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }

        tagsByID = mapByID;
        tagsByName = mapByWord;
    }

    /**
     * Parses formatted ticket domains and sets the cache.
     *
     * @param formattedDomains
     *            The formatted domains string.
     * @throws IllegalArgumentException
     *             When invalid domain format.
     */
    public static void initTicketDomains(final String formattedDomains) {
        setTicketDomains(parseTicketDomains(formattedDomains));
    }

    /**
     * Parses formatted ticket uses and sets the cache.
     *
     * @param formattedUses
     *            The formatted uses string.
     * @throws IllegalArgumentException
     *             When invalid use format.
     */
    public static void initTicketUses(final String formattedUses) {
        setTicketUses(parseTicketUses(formattedUses));
    }

    /**
     * Parses formatted ticket tags and sets the cache.
     *
     * @param formattedTags
     *            The formatted tags string.
     * @throws IllegalArgumentException
     *             When invalid tag format.
     */
    public static void initTicketTags(final String formattedTags) {
        setTicketTags(parseTicketTags(formattedTags));
    }

    /**
     * Parses formatted ticket domains (cache is <b>not</b> updated).
     *
     * @param formattedDomains
     *            The formatted domains string.
     * @return The domain list.
     * @throws IllegalArgumentException
     *             When invalid tag format.
     */
    public static List<JobTicketDomainDto>
            parseTicketDomains(final String formattedDomains) {

        final List<JobTicketDomainDto> list = new ArrayList<>();

        for (final String item : splitFormattedItems(formattedDomains)) {
            final String[] res = splitItem(item);
            final JobTicketDomainDto dto = new JobTicketDomainDto();
            dto.setId(res[LABEL_SPLIT_IDX_ID]);
            dto.setName(res[LABEL_SPLIT_IDX_NAME]);
            list.add(dto);
        }
        return list;
    }

    /**
     * Parses formatted ticket uses (cache is <b>not</b> updated).
     *
     * @param formattedUses
     *            The formatted tags string.
     * @return The use list.
     * @throws IllegalArgumentException
     *             When invalid tag format.
     */
    public static List<JobTicketUseDto>
            parseTicketUses(final String formattedUses) {

        final List<JobTicketUseDto> list = new ArrayList<>();

        for (final String item : splitFormattedItems(formattedUses)) {
            final JobTicketUseDto dto = new JobTicketUseDto();
            processFormattedItem(item, dto);
            list.add(dto);
        }
        return list;
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

        for (final String item : splitFormattedItems(formattedTags)) {
            final JobTicketTagDto dto = new JobTicketTagDto();
            processFormattedItem(item, dto);
            list.add(dto);
        }
        return list;
    }

    /**
     * Processes a formatted item.
     *
     * @param item
     *            Formatted item.
     * @param dto
     *            {@link JobTicketLabelDomainPartDto} to put processed result
     *            on.
     */
    private static void processFormattedItem(final String item,
            final JobTicketLabelDomainPartDto dto) {
        final String[] res = splitItem(item);
        dto.setId(res[LABEL_SPLIT_IDX_ID]);
        dto.setName(res[LABEL_SPLIT_IDX_NAME]);
        for (int i = LABEL_SPLIT_IDX_NAME + 1; i < res.length; i++) {
            dto.addDomainID(res[i]);
        }
    }

    /**
     *
     * @param items
     *            The formatted items.
     * @return Item array.
     */
    private static String[] splitFormattedItems(final String items) {
        return StringUtils.split(
                StringUtils.remove(StringUtils.remove(items, '\n'), '\r'), ',');
    }

    /**
     *
     * @param item
     *            The formatted items.
     * @return Item word array.
     */
    private static String[] splitItem(final String item) {

        final String[] res = StringUtils.split(item, LABEL_WORDS_SEPARATOR);

        if (res.length < 2) {
            throw new IllegalArgumentException(String
                    .format("Job Ticket item [%s]: invalid format.", item));
        }

        final String labelID = res[LABEL_SPLIT_IDX_ID];

        if (!labelID.matches(LABEL_ID_REGEX)) {
            throw new IllegalArgumentException(String.format(
                    "Job Ticket item [%s]: ID [%s] "
                            + "does not match regex [%s].",
                    item, labelID, LABEL_ID_REGEX));
        }

        if (labelID.length() > LABEL_ID_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "Job Ticket item [%s]: ID [%s] "
                            + "has more then %d characters.",
                    item, labelID, LABEL_ID_MAX_LENGTH));
        }
        return res;
    }
}
