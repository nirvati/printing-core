/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.dao.impl;

import java.util.Date;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.PrinterAttrDao;
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
public final class PrinterAttrDaoImpl extends GenericDaoImpl<PrinterAttr>
        implements PrinterAttrDao {

    /**
     * This SQL LIKE value is used to select all rolling statistics.
     * <p>
     * INVARIANT: all rolling statistics MUST hold the prefix
     * {@link #STATS_ROLLING_PREFIX} in their name.
     * </p>
     *
     */
    private static final String SQL_LIKE_STATS_ROLLING =
            STATS_ROLLING_PREFIX + "%";

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM PrinterAttr T";
    }

    @Override
    public PrinterAttr findByName(final Long printerId,
            final IppKeywordAttr ippKeyword) {
        return findByName(printerId, ippKeyword.getKey());
    }

    @Override
    public PrinterAttr findByName(final Long printerId,
            final PrinterAttrEnum name) {
        return findByName(printerId, name.getDbName());
    }

    /**
     * Finds a {@link PrinterAttr} for a {@link Printer}.
     *
     * @param printerId
     *            The primary key of the {@link Printer}.
     * @param name
     *            The attribute name .
     * @return The {@link PrinterAttr} or {@code null} when not found.
     */
    private PrinterAttr findByName(final Long printerId, final String dbName) {

        final String jpql = "SELECT A FROM PrinterAttr A JOIN A.printer P "
                + "WHERE P.id = :printerId AND A.name = :name";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("printerId", printerId);
        query.setParameter("name", dbName);

        PrinterAttr attr;

        try {
            attr = (PrinterAttr) query.getSingleResult();
        } catch (NoResultException e) {
            attr = null;
        }

        return attr;
    }

    @Override
    public void deleteRollingStats() {
        final String jpql = "DELETE PrinterAttr A WHERE A.name LIKE :name";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("name", SQL_LIKE_STATS_ROLLING);
        query.executeUpdate();
    }

    @Override
    public boolean isInternalPrinter(final PrinterAttrLookup lookup) {
        return lookup.get(PrinterAttrEnum.ACCESS_INTERNAL, V_NO)
                .equalsIgnoreCase(V_YES);
    }

    @Override
    public Date getSnmpDate(final PrinterAttrLookup lookup) {
        return dateOrNull(lookup.get(PrinterAttrEnum.SNMP_DATE));
    }

    @Override
    public Date getSnmpDate(final Long printerId) {
        final PrinterAttr attr =
                this.findByName(printerId, PrinterAttrEnum.SNMP_DATE);
        if (attr == null) {
            return null;
        }
        return dateOrNull(attr.getValue());
    }

    /**
     *
     * @param date
     * @return
     */
    private static Date dateOrNull(final String date) {
        if (date == null) {
            return null;
        }
        return new Date(Long.valueOf(date));
    }

    @Override
    public String getSnmpJson(final PrinterAttrLookup lookup) {
        return lookup.get(PrinterAttrEnum.SNMP_INFO);
    }

    @Override
    public boolean getBooleanValue(final PrinterAttr attr) {
        return attr != null && StringUtils.defaultString(attr.getValue(), V_NO)
                .equalsIgnoreCase(V_YES);
    }

    @Override
    public String getDbBooleanValue(final boolean value) {
        if (value) {
            return V_YES;
        }
        return V_NO;
    }

}
