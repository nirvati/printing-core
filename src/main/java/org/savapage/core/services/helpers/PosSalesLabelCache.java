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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.savapage.core.dto.JobTicketDomainDto;
import org.savapage.core.dto.PosSalesItemDto;
import org.savapage.core.dto.PosSalesLocationDto;
import org.savapage.core.dto.PosSalesPriceDto;
import org.savapage.core.dto.PosSalesShopDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PosSalesLabelCache extends AbstractLabelCache {

    /** */
    private static volatile SortedMap<String, PosSalesLocationDto> //
    locationsByID = new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesLocationDto> //
    locationsByName = new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesShopDto> shopsByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesShopDto> shopsByName =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesItemDto> itemsByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesItemDto> itemsByName =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesPriceDto> pricesByID =
            new TreeMap<>();

    /** */
    private static volatile SortedMap<String, PosSalesPriceDto> pricesByName =
            new TreeMap<>();

    /**
     * Static methods only.
     */
    private PosSalesLabelCache() {
    }

    /**
     *
     * @return The locations sorted by name from cache, or empty when no uses
     *         are defined.
     */
    public static Collection<PosSalesLocationDto> getSalesLocationsByName() {
        return locationsByName.values();
    }

    /**
     * Returns location by ID from cache.
     *
     * @param id
     *            The location ID.
     * @return The {@link JobTicketDomainDto} or null if this ID does not exist.
     */
    public static PosSalesLocationDto getSalesLocation(final String id) {
        return locationsByID.get(id);
    }

    /**
     *
     * @return The shops sorted by name from cache, or empty when no shops are
     *         defined.
     */
    public static Collection<PosSalesShopDto> getSalesShopsByName() {
        return shopsByName.values();
    }

    /**
     * Returns shop by ID from cache.
     *
     * @param id
     *            The shop ID.
     * @return The {@link PosSalesShopDto} or null if this ID does not exist.
     */
    public static PosSalesShopDto getSalesShop(final String id) {
        return shopsByID.get(id);
    }

    /**
     *
     * @return The items sorted by name from cache, or empty when no items are
     *         defined.
     */
    public static Collection<PosSalesItemDto> getSalesItemsByName() {
        return itemsByName.values();
    }

    /**
     *
     * @return The prices sorted by name from cache, or empty when no prices are
     *         defined.
     */
    public static Collection<PosSalesPriceDto> getSalesPricesByName() {
        return pricesByName.values();
    }

    /**
     * Returns item by ID from cache.
     *
     * @param id
     *            The item ID.
     * @return The {@link PosSalesItemDto} or null if this ID does not exist.
     */
    public static PosSalesItemDto getSalesItem(final String id) {
        return itemsByID.get(id);
    }

    /**
     * Returns price by ID from cache.
     *
     * @param id
     *            The price ID.
     * @return The {@link PosSalesPriceDto} or null if this ID does not exist.
     */
    public static PosSalesPriceDto getSalesPrice(final String id) {
        return pricesByID.get(id);
    }

    /**
     * Sets the locations in the cache.
     *
     * @param locations
     *            The location list.
     */
    private static void
            setSalesLocations(final List<PosSalesLocationDto> locations) {

        final TreeMap<String, PosSalesLocationDto> mapByID = new TreeMap<>();
        final TreeMap<String, PosSalesLocationDto> mapByWord = new TreeMap<>();

        for (final PosSalesLocationDto dto : locations) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }

        locationsByID = mapByID;
        locationsByName = mapByWord;
    }

    /**
     * Sets the shops in the cache.
     *
     * @param shops
     *            The shop list.
     */
    private static void setSalesShops(final List<PosSalesShopDto> shops) {

        final TreeMap<String, PosSalesShopDto> mapByID = new TreeMap<>();
        final TreeMap<String, PosSalesShopDto> mapByWord = new TreeMap<>();

        for (final PosSalesShopDto dto : shops) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }

        shopsByID = mapByID;
        shopsByName = mapByWord;
    }

    /**
     * Sets the items in the maps.
     *
     * @param mapByID
     *            Items by ID.
     * @param mapByWord
     *            Items by Word.
     * @param items
     *            The item list.
     */
    private static void setSalesItems(
            final TreeMap<String, PosSalesItemDto> mapByID,
            final TreeMap<String, PosSalesItemDto> mapByWord,
            final List<PosSalesItemDto> items) {

        for (final PosSalesItemDto dto : items) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }
    }

    /**
     * Sets the prices in the maps.
     *
     * @param mapByID
     *            Prices by ID.
     * @param mapByWord
     *            Items by Word.
     * @param prices
     *            The price list.
     */
    private static void setSalesPrices(
            final TreeMap<String, PosSalesPriceDto> mapByID,
            final TreeMap<String, PosSalesPriceDto> mapByWord,
            final List<PosSalesPriceDto> prices) {

        for (final PosSalesPriceDto dto : prices) {
            mapByID.put(dto.getId(), dto);
            mapByWord.put(dto.getName(), dto);
        }
    }

    /**
     * Parses formatted ticket locations and sets the cache.
     *
     * @param formattedLocations
     *            The formatted locations string.
     * @throws IllegalArgumentException
     *             When invalid domain format.
     */
    public static void initSalesLocations(final String formattedLocations) {
        setSalesLocations(parseSalesLocations(formattedLocations));
    }

    /**
     * Parses formatted shops and sets the cache.
     *
     * @param formattedShops
     *            The formatted shops string.
     * @throws IllegalArgumentException
     *             When invalid use format.
     */
    public static void initSalesShops(final String formattedShops) {
        setSalesShops(parseSalesShops(formattedShops));
    }

    /**
     * Parses formatted items and sets the cache.
     *
     * @param formattedItems
     *            Formatted items.
     * @throws IllegalArgumentException
     *             When invalid item format.
     */
    public static void initSalesItems(final String formattedItems) {

        final TreeMap<String, PosSalesItemDto> mapByID = new TreeMap<>();
        final TreeMap<String, PosSalesItemDto> mapByWord = new TreeMap<>();

        setSalesItems(mapByID, mapByWord, parseSalesItems(formattedItems));

        itemsByID = mapByID;
        itemsByName = mapByWord;
    }

    /**
     * Parses formatted prices and sets the cache.
     *
     * @param formattedItems
     *            Formatted prices.
     * @throws IllegalArgumentException
     *             When invalid price format.
     */
    public static void initSalesPrices(final String formattedItems) {

        final TreeMap<String, PosSalesPriceDto> mapByID = new TreeMap<>();
        final TreeMap<String, PosSalesPriceDto> mapByWord = new TreeMap<>();

        setSalesPrices(mapByID, mapByWord, parseSalesPrices(formattedItems));

        pricesByID = mapByID;
        pricesByName = mapByWord;
    }

    /**
     * Parses formatted locations (cache is <b>not</b> updated).
     *
     * @param formattedLocations
     *            The formatted locations string.
     * @return The locations list.
     * @throws IllegalArgumentException
     *             When invalid location format.
     */
    public static List<PosSalesLocationDto>
            parseSalesLocations(final String formattedLocations) {
        final List<PosSalesLocationDto> list = new ArrayList<>();
        parseSalesLocations(list, formattedLocations);
        return list;
    }

    /**
     * Parses formatted locations (cache is <b>not</b> updated).
     *
     * @param list
     *            List to add locations to.
     * @param formattedLocations
     *            The formatted locations string.
     * @throws IllegalArgumentException
     *             When invalid location format.
     */
    private static void parseSalesLocations(
            final List<PosSalesLocationDto> list,
            final String formattedLocations) {

        for (final String item : splitFormattedItems(formattedLocations)) {
            final String[] res = splitItem(item);
            final PosSalesLocationDto dto = new PosSalesLocationDto();
            dto.setId(res[LABEL_SPLIT_IDX_ID]);
            dto.setName(res[LABEL_SPLIT_IDX_NAME]);
            list.add(dto);
        }
    }

    /**
     * Parses formatted shops (cache is <b>not</b> updated).
     *
     * @param formattedShops
     *            The formatted shops string.
     * @return The use list.
     * @throws IllegalArgumentException
     *             When invalid shop format.
     */
    public static List<PosSalesShopDto>
            parseSalesShops(final String formattedShops) {

        final List<PosSalesShopDto> list = new ArrayList<>();

        for (final String item : splitFormattedItems(formattedShops)) {
            final PosSalesShopDto dto = new PosSalesShopDto();
            processFormattedItem(item, dto);
            list.add(dto);
        }
        return list;
    }

    /**
     * Parses formatted items (cache is <b>not</b> updated).
     *
     * @param formattedItems
     *            The formatted items string.
     * @return The item list.
     * @throws IllegalArgumentException
     *             When invalid item format.
     */
    public static List<PosSalesItemDto>
            parseSalesItems(final String formattedItems) {

        final List<PosSalesItemDto> list = new ArrayList<>();

        for (final String item : splitFormattedItems(formattedItems)) {
            final PosSalesItemDto dto = new PosSalesItemDto();
            processFormattedItem(item, dto);
            list.add(dto);
        }
        return list;
    }

    /**
     * Parses formatted prices (cache is <b>not</b> updated).
     *
     * @param formattedPrices
     *            The formatted items string.
     * @return The item list.
     * @throws IllegalArgumentException
     *             When invalid item format.
     */
    public static List<PosSalesPriceDto>
            parseSalesPrices(final String formattedPrices) {

        final List<PosSalesPriceDto> list = new ArrayList<>();

        for (final String item : splitFormattedItems(formattedPrices)) {
            final PosSalesPriceDto dto = new PosSalesPriceDto();
            processFormattedItem(item, dto);
            list.add(dto);
        }
        return list;
    }

}
