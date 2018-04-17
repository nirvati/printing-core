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
package org.savapage.core.job;

import java.util.HashSet;
import java.util.Set;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.users.conf.InternalGroupList;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SyncUserGroupsJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SyncUserGroupsJob.class);

    /**
     *
     */
    private static final UserGroupService USER_GROUP_SERVICE =
            ServiceContext.getServiceFactory().getUserGroupService();

    /**
     *
     */
    public static final String ATTR_IS_TEST = "test";

    /**
     * {@code true} if this is for testing purposes only: changes are NOT
     * committed to the database.
     */
    private boolean isTest = false;

    /**
     * Prefix to be applied to all messages.
     */
    private String msgTestPfx = "";

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    @Override
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_IS_TEST)) {
            this.isTest = map.getBooleanValue(ATTR_IS_TEST);
        }

        if (this.isTest) {
            msgTestPfx = "[test]";
        }

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        /**
         *
         */
        ServiceContext.setActor(Entity.ACTOR_SYSTEM);

        final DaoBatchCommitter batchCommitter = ServiceContext.getDaoContext()
                .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());

        batchCommitter.setTest(this.isTest);
        batchCommitter.open();

        final UserGroupDao userGroupDao =
                ServiceContext.getDaoContext().getUserGroupDao();

        try {

            pubMsg(Messages.getMessage(getClass(), "SyncUserGroupsJob.start",
                    null));

            final Set<String> internalGroups = InternalGroupList.getGroups();
            final Set<String> internalGroupsCollected = new HashSet<>();

            // Groups from external source
            for (final UserGroup group : userGroupDao.getListChunk(
                    new UserGroupDao.ListFilter(), null, null,
                    UserGroupDao.Field.NAME, true)) {

                final String groupName = group.getGroupName();

                if (USER_GROUP_SERVICE.isReservedGroupName(groupName)) {
                    continue;
                }

                // Internal group names take precedence.
                if (internalGroups.contains(groupName)) {
                    internalGroupsCollected.add(groupName);
                    continue;
                }

                final AbstractJsonRpcMethodResponse rsp = USER_GROUP_SERVICE
                        .syncUserGroup(batchCommitter, groupName);

                batchCommitter.commit();

                pubMsg(rsp);
                // pubMsg(Messages.getMessage(this.getClass(),
                // "SyncUserGroup.success", new String[] { groupName }));
            }

            // Collected Internal Groups
            for (final String groupName : internalGroupsCollected) {

                final AbstractJsonRpcMethodResponse rsp = USER_GROUP_SERVICE
                        .syncInternalUserGroup(batchCommitter, groupName);

                batchCommitter.commit();

                pubMsg(rsp);
                // pubMsg(Messages.getMessage(this.getClass(),
                // "SyncUserGroup.success", new String[] { groupName }));
            }

            //
            msg = AppLogHelper.logInfo(getClass(), "SyncUserGroupsJob.success",
                    msgTestPfx);

        } catch (Exception e) {

            batchCommitter.rollback();

            level = PubLevelEnum.ERROR;
            msg = AppLogHelper.logError(getClass(), "SyncUserGroupsJob.error",
                    msgTestPfx, e.getMessage());

            LOGGER.error(e.getMessage(), e);
        }

        batchCommitter.close();

        try {
            AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC, level,
                    msg);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     *
     * @param rsp
     *            The JSON RPC response.
     */
    private void pubMsg(final AbstractJsonRpcMethodResponse rsp) {
        if (rsp.isError()) {
            pubMsg(PubLevelEnum.ERROR, rsp.asError().getError().getMessage());
        } else {
            pubMsg(rsp.asResult().getResult().data(ResultDataBasic.class)
                    .getMessage());
        }
    }

    /**
     * Publish message on CometD admin channel.
     *
     * @param level
     *            The pub level.
     * @param msg
     *            The message.
     */
    private void pubMsg(final PubLevelEnum level, final String msg) {
        if (this.isTest) {
            AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC, level,
                    String.format("%s %s", msgTestPfx, msg));
        } else {
            AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC, level,
                    msg);
        }
    }

    /**
     * Publish message on CometD admin channel.
     *
     * @param msg
     *            The message.
     */
    private void pubMsg(final String msg) {
        pubMsg(PubLevelEnum.INFO, msg);
    }
}
