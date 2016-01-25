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
package org.savapage.core.dao;

import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.services.helpers.PrinterAttrLookup;

/**
 *
 * @author Datraverse B.V.
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
