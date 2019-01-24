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
package org.savapage.core.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.print.proxy.JsonProxyPrintJob;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.SyncPrintJobsResult;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes CUPS job state with PrintOut jobs.
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsSyncPrintJobs extends AbstractJob {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CupsSyncPrintJobs.class);

    /**
     * The max number of {@link PrintOut} instances in list chunk.
     */
    private static final int MAX_RESULT_PRINT_OUT_LIST = 200;

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.PRINT_OUT_HISTORY.setWriteLock(false);
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final AdminPublisher publisher = AdminPublisher.instance();

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        final DaoBatchCommitter batchCommitter = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());

        publisher.publish(PubTopicEnum.CUPS, PubLevelEnum.INFO,
                localizeSysMsg("CupsSyncPrintJobs.start"));

        try {
            batchCommitter.lazyOpen();

            final SyncPrintJobsResult syncResult =
                    syncPrintJobs(batchCommitter);

            batchCommitter.close();

            if (syncResult.getJobsActive() > 0) {
                msg = AppLogHelper.logInfo(getClass(),
                        "CupsSyncPrintJobs.success",
                        String.valueOf(syncResult.getJobsActive()),
                        String.valueOf(syncResult.getJobsUpdated()),
                        String.valueOf(syncResult.getJobsNotFound()));
            } else {
                msg = localizeSysMsg("CupsSyncPrintJobs.success",
                        String.valueOf(syncResult.getJobsActive()),
                        String.valueOf(syncResult.getJobsUpdated()),
                        String.valueOf(syncResult.getJobsNotFound()));
            }

        } catch (Exception e) {

            batchCommitter.rollback();

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            level = PubLevelEnum.ERROR;
            msg = AppLogHelper.logError(getClass(), "CupsSyncPrintJobs.error",
                    e.getMessage());
        }

        if (msg != null) {
            publisher.publish(PubTopicEnum.CUPS, level, msg);
        }
    }

    /**
     * Synchronizes (updates) the PrintOut jobs with the CUPS job state (if the
     * state changed). A match is made between printer, job-id and
     * creation-time. If there is no match, i.e. when creation times differs, no
     * update is done.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The {@link SyncPrintJobsResult}.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    private static SyncPrintJobsResult syncPrintJobs(
            final DaoBatchCommitter batchCommitter) throws IppConnectException {

        final ProxyPrintService proxyPrintService =
                ServiceContext.getServiceFactory().getProxyPrintService();

        final PrintOutDao printOutDAO =
                ServiceContext.getDaoContext().getPrintOutDao();

        SpInfo.instance().log(String.format("| Syncing CUPS jobs ..."));

        /*
         * Init batch.
         */
        final long startTime = System.currentTimeMillis();

        final long nActiveCupsJobs = printOutDAO.countActiveCupsJobs();

        SpInfo.instance()
                .log(String.format("|   %s : %d Active PrintOut jobs.",
                        DateUtil.formatDuration(
                                System.currentTimeMillis() - startTime),
                        nActiveCupsJobs));

        int nJobsActive = 0;
        int nJobsUpdated = 0;
        int nJobsNotFound = 0;

        boolean hasNext = true;

        int cupsJobIdLast = 0;

        while (hasNext) {

            final List<PrintOut> list = printOutDAO.getActiveCupsJobsChunk(
                    Integer.valueOf(MAX_RESULT_PRINT_OUT_LIST));

            final SyncPrintJobsResult result = syncPrintJobs(proxyPrintService,
                    printOutDAO, cupsJobIdLast, list, batchCommitter);

            nJobsActive += result.getJobsActive();
            nJobsUpdated += result.getJobsUpdated();
            nJobsNotFound += result.getJobsNotFound();

            cupsJobIdLast = result.getJobIdLast();

            hasNext = list.size() == MAX_RESULT_PRINT_OUT_LIST
                    && nJobsActive < nActiveCupsJobs;

            batchCommitter.commit(); // !!

            if (result.getJobsActive() > 0) {
                AdminPublisher.instance().publish(PubTopicEnum.CUPS,
                        PubLevelEnum.INFO,
                        String.format(
                                "CUPS Print Job Sync %d/%d: "
                                        + "%d of %d jobs found.",
                                nJobsActive, nActiveCupsJobs,
                                result.getJobsUpdated(),
                                result.getJobsActive()));
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Synced [{}] active PrintOut jobs with CUPS: "
                            + "updated [{}], not found [{}]",
                    nJobsActive, nJobsUpdated, nJobsNotFound);
        }

        if (nJobsActive > 0) {
            SpInfo.instance().log(String.format("|      : %d PrintOut updated.",
                    nJobsUpdated));
            SpInfo.instance().log(String.format(
                    "|      : %d PrintOut not found in CUPS.", nJobsNotFound));
        }

        return new SyncPrintJobsResult(nJobsActive, nJobsUpdated, nJobsNotFound,
                cupsJobIdLast);
    }

    /**
     * Creates a look-up map on CUPS job id.
     *
     * @param printOutList
     *            {@link PrintOut} list, sorted by
     *            {@link PrintOut#getCupsJobId()} ascending an
     *            {@link PrintOut#getId()} descending.
     *
     * @return The lookup map.
     */
    private static Map<Integer, List<PrintOut>>
            createJobIdLookup(final List<PrintOut> printOutList) {

        final Map<Integer, List<PrintOut>> lookup = new HashMap<>();

        for (final PrintOut printOut : printOutList) {

            final Integer key = printOut.getCupsJobId();
            final List<PrintOut> list;

            if (lookup.containsKey(key)) {
                list = lookup.get(key);
            } else {
                list = new ArrayList<>();
            }

            list.add(printOut);
            lookup.put(key, list);
        }
        return lookup;
    }

    /**
     *
     * @param proxyPrintService
     *            {@link ProxyPrintservice}
     * @param printOutDAO
     *            {@link PrintOutDao}.
     * @param cupsJobIdLast
     *            The last CUPS job id handled in previous chunk.
     * @param printOutList
     *            List of jobs ordered by CUPS printer name and job id.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The {@link SyncPrintJobsResult}.
     * @throws IppConnectException
     *             When a connection error occurs.
     */
    private static SyncPrintJobsResult syncPrintJobs(
            final ProxyPrintService proxyPrintService,
            final PrintOutDao printOutDAO, final int cupsJobIdLast,
            final List<PrintOut> printOutList,
            final DaoBatchCommitter batchCommitter) throws IppConnectException {

        final int nJobsActive = printOutList.size();

        int nJobsUpdated = 0;
        int nJobsNotFound = 0;

        final Map<Integer, List<PrintOut>> lookupPrintOut =
                createJobIdLookup(printOutList);

        for (final Entry<Integer, List<PrintOut>> entry : lookupPrintOut
                .entrySet()) {

            final Integer cupsJobId = entry.getKey();

            boolean firstEntry = true;

            for (final PrintOut printOut : entry.getValue()) {

                if (firstEntry) {

                    firstEntry = false;

                    final JsonProxyPrintJob cupsJob;

                    if (cupsJobIdLast == cupsJobId.intValue()) {
                        cupsJob = null;
                    } else {
                        final String printerName =
                                printOut.getPrinter().getPrinterName();

                        cupsJob = proxyPrintService
                                .retrievePrintJob(printerName, cupsJobId);
                    }

                    if (cupsJob != null) {
                        // State change?
                        if (!printOut.getCupsJobState()
                                .equals(cupsJob.getJobState())) {

                            printOut.setCupsJobState(
                                    cupsJob.getIppJobState().asInteger());
                            printOut.setCupsCompletedTime(
                                    cupsJob.getCompletedTime());

                            printOutDAO.update(printOut);
                            nJobsUpdated++;
                            batchCommitter.increment();
                        }
                        continue;
                    }
                }

                /*
                 * Set job status to UNKNOWN, and set Time Completed to current
                 * time to mark as end-state, so this job won't be selected at
                 * the next sync.
                 */
                printOut.setCupsCompletedTime(
                        proxyPrintService.getCupsSystemTime());
                printOut.setCupsJobState(
                        IppJobStateEnum.IPP_JOB_UNKNOWN.asInteger());

                printOutDAO.update(printOut);
                nJobsNotFound++;
                batchCommitter.increment();
            }
        }

        final Integer cupsJobIdLastNew;

        if (printOutList.isEmpty()) {
            cupsJobIdLastNew = 0;
        } else {
            cupsJobIdLastNew =
                    printOutList.get(printOutList.size() - 1).getCupsJobId();
        }

        return new SyncPrintJobsResult(nJobsActive, nJobsUpdated, nJobsNotFound,
                cupsJobIdLastNew.intValue());
    }

}
