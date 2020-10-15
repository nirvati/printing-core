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
package org.savapage.core.json.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * <p>
 * Inspired by <a href=
 * "http://google-styleguide.googlecode.com/svn/trunk/jsoncstyleguide.xml#Reserved_Property_Names_for_Paging"
 * >Google JSON Style Guide</a>.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
@JsonPropertyOrder({ "currentItemCount", "itemsPerPage", "startIndex",
        "totalItems", "pageIndex", "totalPages", "items" })
public abstract class AbstractResultDataPaging extends JsonRpcResultDataMixin {

    /**
     * Convenience property for the number of items in this result set. It
     * should be equivalent to items.length.
     */
    @JsonProperty("currentItemCount")
    private Integer currentItemCount;

    /**
     * The number of items in the result. This is not necessarily the size of
     * the data.items array.
     * <p>
     * I.e. in the last page of items, the size of data.items may be less than
     * itemsPerPage. However the size of data.items should not exceed
     * itemsPerPage.
     * </p>
     */
    @JsonProperty("itemsPerPage")
    private Integer itemsPerPage;

    /**
     * 0-based index of the first item in data.items.
     */
    @JsonProperty("startIndex")
    private Integer startIndex;

    /**
     * The total number of items available in the set. For example, if 100 Users
     * are present in the system, the response may only contain 10 Users, but
     * the totalItems would be 100.
     */
    @JsonProperty("totalItems")
    private Integer totalItems;

    /**
     * The index of the current page of items. For consistency, pageIndex should
     * be 1-based. For example, the first page of items has a pageIndex of 1.
     * <p>
     * pageIndex can also be calculated from the item-based paging properties:
     * {@code pageIndex = floor(startIndex / itemsPerPage) + 1}.
     * </p>
     */
    @JsonProperty("pageIndex")
    private Integer pageIndex;

    /**
     * The total number of pages in the result set. totalPages can also be
     * calculated from the item-based paging properties above: totalPages =
     * ceiling(totalItems / itemsPerPage).
     */
    @JsonProperty("totalPages")
    private Integer totalPages;

    public Integer getCurrentItemCount() {
        return currentItemCount;
    }

    public void setCurrentItemCount(Integer currentItemCount) {
        this.currentItemCount = currentItemCount;
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

}
