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
package org.savapage.core.job;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreConfig;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean-up document stores.
 *
 * @author Rijk Ravestein
 *
 */
public final class DocStoreClean extends AbstractJob {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocStoreClean.class);

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        // Conservative strategy.
        ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
    }

    @Override
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ConfigManager cm = ConfigManager.instance();

        final DocStoreService docStoreService =
                ServiceContext.getServiceFactory().getDocStoreService();

        /*
         * INVARIANT.
         */
        if (!cm.isConfigValue(Key.DOC_STORE_ENABLE)) {
            LOGGER.debug("Document store disabled.");
            return;
        }

        final List<DocStoreConfig> storeList = new ArrayList<>();

        for (final DocStoreTypeEnum store : DocStoreTypeEnum.values()) {
            for (final DocStoreBranchEnum branch : DocStoreBranchEnum
                    .values()) {
                storeList.add(docStoreService.getConfig(store, branch));
            }
        }

        final AdminPublisher publisher = AdminPublisher.instance();

        for (final DocStoreConfig storeConfig : storeList) {

            final DocStoreTypeEnum store = storeConfig.getStore();
            final DocStoreBranchEnum branch = storeConfig.getBranch();

            final StringBuilder cleanObj = new StringBuilder();
            cleanObj.append(store.toString().toLowerCase())
                    .append(File.separator)
                    .append(branch.getBranch().toString());

            final String entity = String.format("[%s]", cleanObj.toString());

            final String pubMsgKeyBase = "DocStoreClean";

            long nRemoved = 0;

            try {

                onCleanStepBegin(entity, publisher, pubMsgKeyBase);

                final DocStoreService service =
                        ServiceContext.getServiceFactory().getDocStoreService();

                final int keepDays = storeConfig.getDaysToKeep();

                final long timeOpen = System.currentTimeMillis();

                nRemoved = service.clean(store, branch,
                        ServiceContext.getTransactionDate(), keepDays,
                        RunModeSwitch.REAL);

                final Duration duration = Duration
                        .ofMillis(System.currentTimeMillis() - timeOpen);

                onCleanStepEnd(entity, duration, nRemoved, publisher,
                        pubMsgKeyBase);

            } catch (Exception e) {

                LOGGER.error(e.getMessage(), e);

                final String msg = AppLogHelper.logError(getClass(),
                        pubMsgKeyBase + ".error", String.format("[%s] %s",
                                e.getClass().getName(), e.getMessage()));

                publisher.publish(PubTopicEnum.DOC_STORE, PubLevelEnum.ERROR,
                        msg);
            }
        }
    }

    /**
     * @param entity
     *            Description of cleanup entity.
     * @param publisher
     *            The message publisher.
     * @param pubMsgKeyPdf
     *            Prefix of message key.
     */
    private void onCleanStepBegin(final String entity,
            final AdminPublisher publisher, final String pubMsgKeyPdf) {

        SpInfo.instance()
                .log(String.format("| Cleaning Document Store %s ...", entity));

        publisher.publish(PubTopicEnum.DOC_STORE, PubLevelEnum.INFO,
                this.localizeSysMsg(
                        String.format("%s.start", pubMsgKeyPdf, entity)));
    }

    /**
     *
     * @param entity
     *            Description of cleanup entity.
     * @param duration
     *            {@code null} when cleaning was not performed.
     * @param nDeleted
     *            Number of documents deleted.
     * @param publisher
     *            The message publisher.
     * @param pubMsgKeyPfx
     *            Prefix of message key.
     */
    private void onCleanStepEnd(final String entity, final Duration duration,
            final long nDeleted, final AdminPublisher publisher,
            final String pubMsgKeyPfx) {

        final String formattedDuration;
        if (duration == null) {
            formattedDuration = "-";
        } else {
            formattedDuration = DateUtil.formatDuration(duration.toMillis());
        }

        SpInfo.instance().log(String.format("|          %s : %d %s cleaned.",
                formattedDuration, nDeleted, entity));

        if (nDeleted == 0) {

            publisher.publish(PubTopicEnum.DOC_STORE, PubLevelEnum.INFO,
                    this.localizeSysMsg(
                            String.format("%s.success.zero", pubMsgKeyPfx),
                            entity));

        } else if (nDeleted == 1) {

            final String msg = AppLogHelper.logInfo(this.getClass(),
                    String.format("%s.success.single", pubMsgKeyPfx), entity);

            publisher.publish(PubTopicEnum.DOC_STORE, PubLevelEnum.INFO, msg);

        } else {

            final String msg = AppLogHelper.logInfo(this.getClass(),
                    String.format("%s.success.plural", pubMsgKeyPfx),
                    String.valueOf(nDeleted), entity);
            publisher.publish(PubTopicEnum.DOC_STORE, PubLevelEnum.INFO, msg);
        }
    }

}
