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
package org.savapage.core.dao.impl;

import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.savapage.core.SpException;
import org.savapage.core.dao.IAttrDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dao.helpers.ProxyPrinterName;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.dto.MediaCostDto;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.Printer.ChargeType;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.json.JsonAbstractBase;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterDaoImpl extends GenericDaoImpl<Printer>
        implements PrinterDao {

    /** */
    private static final String QPARM_CONTAINING_TEXT = "containingText";
    /** */
    private static final String QPARM_PRINTER_GROUP = "printerGroup";
    /** */
    private static final String QPARM_SEL_DISABLED = "selDisabled";
    /** */
    private static final String QPARM_SEL_DELETED = "selDeleted";

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM Printer T";
    }

    @Override
    public CostMediaAttr getCostMediaAttr() {
        return new CostMediaAttr();
    }

    @Override
    public CostMediaAttr getCostMediaAttr(final String ippMediaName) {
        return new CostMediaAttr(ippMediaName);
    }

    @Override
    public MediaSourceAttr getMediaSourceAttr(final String ippMediaSourceName) {
        return new MediaSourceAttr(ippMediaSourceName);
    }

    @Override
    public ChargeType getChargeType(final String chargeType) {
        return ChargeType.valueOf(chargeType);
    }

    @Override
    public IppMediaCostDto getMediaCost(final Printer printer,
            final String ippMediaName) {

        IppMediaCostDto dto = null;

        MediaCostDto dtoPageCost = null;

        final String defaultkey = getCostMediaAttr().getKey();
        final String mediaKey = getCostMediaAttr(ippMediaName).getKey();

        for (PrinterAttr attr : printer.getAttributes()) {

            final boolean isDefault =
                    attr.getName().equalsIgnoreCase(defaultkey);

            if (attr.getName().equalsIgnoreCase(mediaKey)
                    || (dtoPageCost == null && isDefault)) {

                try {
                    dtoPageCost = JsonAbstractBase.create(MediaCostDto.class,
                            attr.getValue());
                } catch (SpException e) {
                    // Be forgiving :)
                }

                if (!isDefault) {
                    break;
                }

            }
        }

        if (dtoPageCost != null) {
            dto = new IppMediaCostDto();
            dto.setMedia(ippMediaName);
            dto.setActive(Boolean.TRUE);
            dto.setPageCost(dtoPageCost);
        }

        return dto;
    }

    @Override
    public void resetTotals(final Date resetDate, final String resetBy) {

        final String jpql = "UPDATE Printer P SET "
                + "P.totalBytes = 0, P.totalEsu = 0, P.totalJobs = 0, "
                + "P.totalPages = 0, P.totalSheets = 0, "
                + "P.resetDate = :resetDate, P.resetBy = :resetBy";

        Query query = getEntityManager().createQuery(jpql);

        query.setParameter("resetDate", resetDate);
        query.setParameter("resetBy", resetBy);

        query.executeUpdate();
    }

    @Override
    public int prunePrinters(final DaoBatchCommitter batchCommitter) {
        /*
         * NOTE: We do NOT use bulk delete with JPQL since we want the option to
         * roll back the deletions as part of a transaction, and we want to use
         * cascade deletion. Therefore we use the remove() method in
         * EntityManager to delete individual records instead (so cascaded
         * deletes are triggered).
         */
        int nCount = 0;

        final String jpql = "SELECT P.id FROM Printer P WHERE P.deleted = true "
                + "AND P.printsOut IS EMPTY";

        final Query query = getEntityManager().createQuery(jpql);

        @SuppressWarnings("unchecked")
        final List<Long> list = query.getResultList();

        for (final Long id : list) {
            this.delete(this.findById(id));
            nCount++;
            batchCommitter.increment();
        }

        return nCount;
    }

    @Override
    public long countPrintOuts(final Long id) {

        final String jpql = "SELECT COUNT(O.id) FROM Printer P "
                + "JOIN P.printsOut O WHERE P.id = :id";
        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("id", id);

        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @Override
    public boolean isJobTicketRedirectPrinter(final Long id) {

        final String jpql = String.format(
                "SELECT COUNT(PM.id) FROM PrinterGroupMember PM "
                        + "WHERE PM.printer.id = :id AND PM.group.id IN "
                        + "(SELECT PG.id FROM PrinterGroup PG "
                        + " WHERE PG.displayName IN"
                        + " (SELECT A1.value FROM PrinterAttr A1"
                        + "  WHERE A1.name = '%s' " + "  AND A1.printer.id IN " //
                        + "  (SELECT A2.printer.id FROM PrinterAttr A2"
                        + "   WHERE A2.name = '%s' AND A2.value = '%s'"
                        + "  ) GROUP BY A1.value" //
                        + " )" //
                        + ")", //
                PrinterAttrEnum.JOBTICKET_PRINTER_GROUP.getDbName(),
                PrinterAttrEnum.JOBTICKET_ENABLE.getDbName(), IAttrDao.V_YES);

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("id", id);
        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue() > 0;
    }

    @Override
    public Printer findByName(final String printerName) {

        final String key = ProxyPrinterName.getDaoName(printerName);

        final String jpql =
                "SELECT P FROM Printer P WHERE P.printerName = :printerName";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("printerName", key);

        Printer obj;

        try {
            obj = (Printer) query.getSingleResult();
        } catch (NoResultException e) {
            obj = null;
        }

        return obj;
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(P.id) FROM Printer P");

        this.applyListFilter(jpql, filter);

        final Query query = this.createListQuery(jpql.toString(), filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Printer> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        /**
         * #190: Do not use JOIN FETCH construct.
         */
        jpql.append("SELECT P FROM Printer P");

        this.applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.DISPLAY_NAME) {
            jpql.append("P.displayName");
        } else {
            jpql.append("P.displayName");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = this.createListQuery(jpql.toString(), filter);

        //
        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    /**
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The filter.
     * @return The query.
     */
    private Query createListQuery(final String jpql, final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql);

        if (filter.getContainingText() != null) {
            query.setParameter(QPARM_CONTAINING_TEXT, String.format("%%%s%%",
                    filter.getContainingText().toLowerCase()));
        }

        if (filter.getPrinterGroup() != null) {
            query.setParameter(QPARM_PRINTER_GROUP, String.format("%%%s%%",
                    filter.getPrinterGroup().toLowerCase()));
        }

        if (filter.getDisabled() != null) {
            query.setParameter(QPARM_SEL_DISABLED, filter.getDisabled());
        }

        if (filter.getDeleted() != null) {
            query.setParameter(QPARM_SEL_DELETED, filter.getDeleted());
        }

        return query;
    }

    /**
     * Applies PrinterAttr presence constraint to the JPQL string.
     *
     * @param where
     *            The {@link StringBuilder} to append to.
     * @param attrName
     *            The attribute name.
     */
    private void applyPrinterAttrConstraint(final StringBuilder where,
            final PrinterAttrEnum attrName) {
        where.append("(A.name = \'").append(attrName.getDbName()).append("\')");
    }

    /**
     * Applies PrinterAttr boolean constraint to the JPQL string.
     *
     * @param where
     *            The {@link StringBuilder} to append to.
     * @param attrName
     *            The attribute name.
     * @param attrValue
     *            The attribute value.
     */
    private void applyPrinterAttrConstraint(final StringBuilder where,
            final PrinterAttrEnum attrName, final boolean attrValue) {

        where.append("(A.name = \'").append(attrName.getDbName())
                .append("\' AND A.value = \'");
        if (attrValue) {
            where.append(IAttrDao.V_YES);
        } else {
            where.append(IAttrDao.V_NO);
        }
        where.append("\')");
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The {@link StringBuilder} to append to.
     * @param filter
     *            The filter.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getContainingText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" (").append("lower(P.displayName) like :")
                    .append(QPARM_CONTAINING_TEXT)
                    .append(" OR lower(P.printerName) like :")
                    .append(QPARM_CONTAINING_TEXT).append(")");
        }

        if (filter.getDisabled() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" P.disabled = :").append(QPARM_SEL_DISABLED);
        }

        if (filter.getDeleted() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" P.deleted = :").append(QPARM_SEL_DELETED);
        }

        if (filter.getInternal() != null) {

            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;

            final boolean selectInternal = filter.getInternal().booleanValue();

            if (selectInternal) {
                where.append(
                        " P IN (SELECT A.printer FROM PrinterAttr A WHERE ");
                this.applyPrinterAttrConstraint(where,
                        PrinterAttrEnum.ACCESS_INTERNAL, selectInternal);
            } else {
                where.append(" P NOT IN (SELECT A.printer "
                        + "FROM PrinterAttr A WHERE ");
                this.applyPrinterAttrConstraint(where,
                        PrinterAttrEnum.ACCESS_INTERNAL, !selectInternal);
            }
            where.append(")");
        }

        if (filter.getJobTicket() != null) {

            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;

            final boolean selectJobTicket =
                    filter.getJobTicket().booleanValue();

            if (selectJobTicket) {
                where.append(
                        " P IN (SELECT A.printer FROM PrinterAttr A WHERE ");
                this.applyPrinterAttrConstraint(where,
                        PrinterAttrEnum.JOBTICKET_ENABLE, selectJobTicket);
            } else {
                where.append(" P NOT IN (SELECT A.printer "
                        + "FROM PrinterAttr A WHERE ");
                this.applyPrinterAttrConstraint(where,
                        PrinterAttrEnum.JOBTICKET_ENABLE, !selectJobTicket);
            }
            where.append(")");
        }

        if (filter.getSnmp() != null) {

            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;

            where.append(" P IN (SELECT A.printer FROM PrinterAttr A WHERE ");
            this.applyPrinterAttrConstraint(where, PrinterAttrEnum.SNMP_DATE);
            where.append(")");
        }

        if (filter.getPrinterGroup() != null) {

            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;

            where.append(" P IN (SELECT M.printer FROM PrinterGroupMember M"
                    + " WHERE M.group.groupName like :")
                    .append(QPARM_PRINTER_GROUP).append(")");
        }

        //
        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }
    }

    @Override
    public Printer findByNameInsert(final String printerName,
            final MutableBoolean lazyCreated) {

        Printer printer = findByName(printerName);

        lazyCreated.setValue(printer == null);

        if (lazyCreated.isTrue()) {

            printer = new Printer();

            printer.setPrinterName(ProxyPrinterName.getDaoName(printerName));
            printer.setDisplayName(printerName);
            printer.setCreatedDate(new Date());
            printer.setCreatedBy(Entity.ACTOR_SYSTEM);

            this.create(printer);
        }

        return printer;
    }

}
