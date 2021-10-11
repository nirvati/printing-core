/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2021 Datraverse B.V. <info@datraverse.com>
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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.LabelDomainPartDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractLabelCache {

    /** */
    protected static final String LABEL_WORDS_SEPARATOR = "/";

    /** */
    protected static final String LABEL_ID_REGEX = "^[A-Z0-9]+$";

    /** */
    protected static final int LABEL_ID_MAX_LENGTH = 5;

    /** */
    protected static final int LABEL_SPLIT_IDX_ID = 0;
    /** */
    protected static final int LABEL_SPLIT_IDX_NAME = 1;

    /**
     *
     * @param items
     *            The formatted items.
     * @return Item array.
     */
    protected static String[] splitFormattedItems(final String items) {
        return StringUtils.split(
                StringUtils.remove(StringUtils.remove(items, '\n'), '\r'), ',');
    }

    /**
     *
     * @param item
     *            The formatted items.
     * @return Item word array.
     */
    protected static String[] splitItem(final String item) {

        final String[] res = StringUtils.split(item, LABEL_WORDS_SEPARATOR);

        if (res.length < 2) {
            throw new IllegalArgumentException(String
                    .format("[%s]: invalid format.", item));
        }

        final String labelID = res[LABEL_SPLIT_IDX_ID];

        if (!labelID.matches(LABEL_ID_REGEX)) {
            throw new IllegalArgumentException(String.format(
                    "[%s]: ID [%s] "
                            + "does not match regex [%s].",
                    item, labelID, LABEL_ID_REGEX));
        }

        if (labelID.length() > LABEL_ID_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "[%s]: ID [%s] "
                            + "has more then %d characters.",
                    item, labelID, LABEL_ID_MAX_LENGTH));
        }
        return res;
    }

    /**
     * Processes a formatted item.
     *
     * @param item
     *            Formatted item.
     * @param dto
     *            {@link LabelDomainPartDto} to put processed result
     *            on.
     */
    protected static void processFormattedItem(final String item,
            final LabelDomainPartDto dto) {
        final String[] res = splitItem(item);
        dto.setId(res[LABEL_SPLIT_IDX_ID]);
        dto.setName(res[LABEL_SPLIT_IDX_NAME]);
        for (int i = LABEL_SPLIT_IDX_NAME + 1; i < res.length; i++) {
            dto.addDomainID(res[i]);
        }
    }
}
