/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dao;

import java.util.Date;

import org.savapage.core.dao.PrinterDao.IppKeywordAttr;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.services.helpers.PrinterAttrLookup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PrinterAttrDao extends GenericDao<PrinterAttr> {

    /**
     * Prefix for all rolling statistics attributes.
     */
    String STATS_ROLLING_PREFIX = "stats.rolling";

    /**
     * Finds a {@link PrinterAttr} for a {@link Printer}.
     *
     * @param printerId
     *            The primary key of the {@link Printer}.
     * @param name
     *            The {@link PrinterAttrEnum}.
     * @return The {@link PrinterAttr} or {@code null} when not found.
     */
    PrinterAttr findByName(Long printerId, PrinterAttrEnum name);

    /**
     *
     * @param printerId
     *            The primary key of the {@link Printer}.
     * @param ippKeyword
     *            The {@link IppKeywordAttr}.
     * @return The {@link PrinterAttr} or {@code null} when not found.
     */
    PrinterAttr findByName(Long printerId,
            PrinterDao.IppKeywordAttr ippKeyword);

    /**
     * Deletes all rolling statistics of ALL {@link Printer} instances.
     */
    void deleteRollingStats();

    /**
     * Checks if {@link PrinterAttrLookup} indicates an internal printer.
     *
     * @see {@link PrinterAttrEnum#ACCESS_INTERNAL}.
     * @param lookup
     *            The {@link PrinterAttrLookup}.
     * @return {@code true} is printer is internal printer.
     */
    boolean isInternalPrinter(PrinterAttrLookup lookup);

    /**
     * Gets the date of SNMP retrieval.
     *
     * @param printerId
     *            The primary key of the {@link Printer}.
     * @return The date of the SNMP information, or {@code null} when not found.
     */
    Date getSnmpDate(Long printerId);

    /**
     * Gets the date of SNMP retrieval.
     *
     * @param lookup
     *            The {@link PrinterAttrLookup}.
     * @return The date of the SNMP information, or {@code null} when not found.
     */
    Date getSnmpDate(PrinterAttrLookup lookup);

    /**
     * Gets the JSON serialized SNMP data.
     *
     * @param lookup
     *            The {@link PrinterAttrLookup}.
     * @return The JSON string with SNMP information, or {@code null} when not
     *         found.
     */
    String getSnmpJson(PrinterAttrLookup lookup);

    /**
     * Returns attribute value as boolean.
     *
     * @see {@link PrinterAttr#getValue()}.
     * @param attr
     *            The {@link PrinterAttr} ({@code null} is allowed).
     * @return {@code true} When value is {@code true}.
     */
    boolean getBooleanValue(PrinterAttr attr);

    /**
     * Returns the database value of a boolean value.
     *
     * @param value
     *            The value.
     * @return The string representation of a boolean value
     */
    String getDbBooleanValue(boolean value);

}
