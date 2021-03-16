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
package org.savapage.core.dao.impl;

import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.helpers.ProxyPrinterName;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.User;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.util.JsonHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintOutDaoImpl extends GenericDaoImpl<PrintOut>
        implements PrintOutDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM PrintOut T";
    }

    @Override
    public PrintOut findActiveCupsJob(final String jobPrinterName,
            final Integer jobId) {

        final String jpql = "SELECT O FROM PrintOut O WHERE O.id = "
                + "(SELECT MAX(M.id) FROM PrintOut M JOIN M.printer P "
                + "WHERE M.cupsJobId = :jobId "
                + "AND P.printerName = :printerName "
                + "AND (M.cupsCompletedTime IS NULL"
                + " OR M.cupsCompletedTime = 0)"
                + "AND M.cupsJobState < :stateFinished)";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("jobId", jobId);
        query.setParameter("printerName",
                ProxyPrinterName.getDaoName(jobPrinterName));
        query.setParameter("stateFinished",
                IppJobStateEnum.getFirstAbsentOnQueueOrdinal().asInteger());

        PrintOut printOut;

        try {
            printOut = (PrintOut) query.getSingleResult();
        } catch (NoResultException e) {
            printOut = null;
        }

        return printOut;
    }

    @Override
    public PrintOut findEndOfStateCupsJob(final String jobPrinterName,
            final Integer jobId) {

        final String jpql = "SELECT O FROM PrintOut O WHERE O.id = "
                + "(SELECT MAX(M.id) FROM PrintOut M JOIN M.printer P "
                + "WHERE M.cupsJobId = :jobId "
                + "AND P.printerName = :printerName "
                + "AND M.cupsCompletedTime > 0)";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("jobId", jobId);
        query.setParameter("printerName",
                ProxyPrinterName.getDaoName(jobPrinterName));

        PrintOut printOut;

        try {
            printOut = (PrintOut) query.getSingleResult();
        } catch (NoResultException e) {
            printOut = null;
        }

        return printOut;
    }

    @Override
    public List<PrintOut> getActiveCupsJobsChunk(final Integer maxResults,
            final boolean regardCompletedTime) {

        final StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT O FROM PrintOut O " //
                + "WHERE O.cupsJobId > 0 "
                + "AND O.cupsJobState < :cupsJobState ");

        if (regardCompletedTime) {
            jpql.append("AND (O.cupsCompletedTime IS NULL"
                    + " OR O.cupsCompletedTime = 0) ");
        }
        jpql.append("ORDER BY O.cupsJobId, O.id DESC");

        final Query query = getEntityManager().createQuery(jpql.toString());

        query.setParameter("cupsJobState",
                IppJobStateEnum.getFirstAbsentOnQueueOrdinal().asInteger());

        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        @SuppressWarnings("unchecked")
        final List<PrintOut> jobs = query.getResultList();

        return jobs;
    }

    /**
     * @param countUsers
     *            If {@code true}, distinct users are counted.
     * @param regardCompletedTime
     *            If {@code true}, look at CUPS job state and take completed
     *            time into account. If {@code false}, look at CUPS job state
     *            only
     * @return Number of distinct users or CUPS jobs.
     */
    private long countActiveCupsJobs(final boolean countUsers,
            final boolean regardCompletedTime) {

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        final Root<PrintOut> root = cq.from(PrintOut.class);

        final Predicate prd1 =
                cb.greaterThan(root.get("cupsJobId"), Integer.valueOf(0));

        final Predicate prd2 = cb.lessThan(root.get("cupsJobState"),
                IppJobStateEnum.getFirstAbsentOnQueueOrdinal().asInteger());

        if (regardCompletedTime) {
            final Predicate prd3a = cb.isNull(root.get("cupsCompletedTime"));
            final Predicate prd3b =
                    cb.equal(root.get("cupsCompletedTime"), Integer.valueOf(0));

            cq.where(cb.and(prd1, prd2), cb.and(cb.or(prd3a, prd3b)));
        } else {
            cq.where(cb.and(prd1, prd2));
        }

        if (countUsers) {
            final Join<PrintOut, DocOut> joinDocOut = root.join("docOut");
            final Join<DocOut, DocLog> joinDocLog = joinDocOut.join("docLog");
            final Join<DocLog, User> joinUser = joinDocLog.join("user");
            cq.select(cb.countDistinct(joinUser.get("id")));
        } else {
            cq.select(cb.count(root.get("id")));
        }

        return getEntityManager().createQuery(cq).getSingleResult().longValue();
    }

    @Override
    public long countActiveCupsJobs(final boolean regardCompletedTime) {
        return this.countActiveCupsJobs(false, regardCompletedTime);
    }

    @Override
    public long countActiveCupsJobUsers(final boolean regardCompletedTime) {
        return this.countActiveCupsJobs(true, regardCompletedTime);
    }

    /**
     * @param suppl
     *            External supplier.
     * @param stat
     *            External status.
     * @param countUsers
     *            If {@code true}, distinct users are counted.
     * @return Number of distinct users or jobs.
     */
    private long countExtSupplierJobs(final ExternalSupplierEnum suppl,
            final ExternalSupplierStatusEnum stat, final boolean countUsers) {

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        final Root<PrintOut> root = cq.from(PrintOut.class);
        final Join<PrintOut, DocOut> joinDocOut = root.join("docOut");
        final Join<DocOut, DocLog> joinDocLog = joinDocOut.join("docLog");

        final Predicate prd1 =
                cb.equal(joinDocLog.get("externalSupplier"), suppl.toString());

        final Predicate prd2 =
                cb.equal(joinDocLog.get("externalStatus"), stat.toString());

        cq.where(cb.and(prd1, prd2));

        if (countUsers) {
            final Join<DocLog, User> joinUser = joinDocLog.join("user");
            cq.select(cb.countDistinct(joinUser.get("id")));
        } else {
            cq.select(cb.count(root.get("id")));
        }

        return getEntityManager().createQuery(cq).getSingleResult().longValue();
    }

    @Override
    public long countExtSupplierJobs(final ExternalSupplierEnum suppl,
            final ExternalSupplierStatusEnum stat) {
        return countExtSupplierJobs(suppl, stat, false);
    }

    @Override
    public long countExtSupplierJobUsers(final ExternalSupplierEnum suppl,
            final ExternalSupplierStatusEnum stat) {
        return countExtSupplierJobs(suppl, stat, true);
    }

    @Override
    public boolean updateCupsJob(final Long printOutId,
            final IppJobStateEnum ippState, final Integer cupsCompletedTime) {

        final String jpql = "UPDATE PrintOut SET cupsJobState = :cupsJobState"
                + ", cupsCompletedTime = :cupsCompletedTime  WHERE id = :id";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("cupsJobState", ippState.asInteger())
                .setParameter("cupsCompletedTime", cupsCompletedTime)
                .setParameter("id", printOutId);

        return executeSingleRowUpdate(query);
    }

    @Override
    public boolean updateCupsJobPrinter(final Long printOutId,
            final Printer printer, final JsonProxyPrintJob printJob,
            final Map<String, String> optionValues) {

        final String jpql = "UPDATE PrintOut" + " SET cupsJobId = :cupsJobId"
                + ", ippOptions = :ippOptions"
                + ", cupsJobState = :cupsJobState"
                + ", cupsCreationTime = :cupsCreationTime"
                + ", cupsCompletedTime = :cupsCompletedTime"
                + ", printer = :printer" + " WHERE id = :id ";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("cupsJobId", printJob.getJobId())
                .setParameter("cupsJobState", printJob.getJobState())
                .setParameter("ippOptions",
                        JsonHelper.stringifyStringMap(optionValues))
                .setParameter("cupsCreationTime", printJob.getCreationTime())
                .setParameter("cupsCompletedTime", printJob.getCompletedTime())
                .setParameter("printer", printer)
                .setParameter("id", printOutId);

        return executeSingleRowUpdate(query);
    }

    @Override
    public IppJobStateEnum getIppJobState(final PrintOut printOut) {

        final Integer jobState = printOut.getCupsJobState();

        if (jobState == null) {
            return null;
        }
        return IppJobStateEnum.asEnum(jobState);
    }

}
