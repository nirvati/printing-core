/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.community.MemberCard.Stat;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserCardDao;
import org.savapage.core.dao.UserEmailDao;
import org.savapage.core.dao.UserNumberDao;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserCard;
import org.savapage.core.jpa.UserEmail;
import org.savapage.core.jpa.UserNumber;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.users.CommonUser;
import org.savapage.core.users.IUserSource;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.EmailValidator;
import org.savapage.core.util.Messages;
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job synchronizing a user source with the database.
 * <p>
 * This job executes according to a fixed schedule or as a one-shot job. It
 * returns immediately when it is scheduled and one of the following conditions
 * applies:
 * </p>
 * <ul>
 * <li>Auto-sync scheduling is DISABLED. See
 * {@link IConfigProp.Key#SCHEDULE_AUTO_SYNC_USER}.</li>
 * <li>NO user source. See {@link IConfigProp#AUTH_METHOD_V_NONE}.</li>
 * </ul>
 *
 * @author Datraverse B.V.
 */
public final class SyncUsersJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SyncUsersJob.class);

    /**
     *
     */
    public static final String ATTR_IS_TEST = "test";

    /**
     *
     */
    public static final String ATTR_IS_DELETE_USERS = "delete-users";

    /**
     *
     */
    public static final String ATTR_IS_ONE_SHOT = "one-shot";

    /**
     * {@code true} if this is for testing purposes only: changes are NOT
     * committed to the database.
     */
    private boolean isTest = false;

    /**
     * {@code true} if users which are not in the external source should be
     * deleted in the database.
     */
    private boolean isDeleteUsers = false;

    /**
     * The number of bytes of user files deleted.
     */
    private long nBytesUserFilesDeleted = 0;

    /**
     * The number of bytes of user files present.
     */
    private long nBytesUserFilesPresent = 0;

    /**
     * Prefix to be applied to all messages.
     */
    private String msgTestPfx = "";

    /**
     *
     */
    private boolean syncSuccess;

    /**
     *
     */
    private RfidNumberFormat rfidNumberFormat = null;

    /**
     *
     */
    private final EmailValidator emailValidator = new EmailValidator();

    /**
     *
     */
    private Integer minLengthIdNumber = null;

    /**
     * The user source instance.
     */
    private IUserSource userSource = null;

    /**
     *
     */
    private DaoBatchCommitter batchCommitter;

    /**
     *
     */
    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {

        this.syncSuccess = false;
        ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {

        ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);

        if (!isInterrupted() && this.syncSuccess) {
            SpJobScheduler.instance()
                    .scheduleOneShotUserGroupsSync(this.isTest);
        }

    }

    @Override
    public void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Return if this is a scheduled (not a one-shot) job and auto-sync
         * scheduling is DISABLED or NO authentication method.
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED)) {

            if (!cm.isConfigValue(Key.SCHEDULE_AUTO_SYNC_USER)) {
                return;
            }

            if (cm.getConfigValue(Key.AUTH_METHOD).equals(
                    IConfigProp.AUTH_METHOD_V_NONE)) {
                return;
            }
        }

        /*
         *
         */
        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_IS_TEST)) {
            this.isTest = map.getBooleanValue(ATTR_IS_TEST);
        }
        if (map.containsKey(ATTR_IS_DELETE_USERS)) {
            isDeleteUsers = map.getBooleanValue(ATTR_IS_DELETE_USERS);
        }

        if (this.isTest) {
            msgTestPfx = "[test]";
        }

        /*
         *
         */
        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        ServiceContext.setActor(Entity.ACTOR_SYSTEM);

        try {
            syncUsers();
            msg =
                    AppLogHelper.logInfo(getClass(), "SyncUsersJob.success",
                            msgTestPfx);

            if (!this.isTest) {
                MemberCard.instance().init();
            }

            this.syncSuccess = true;

        } catch (Exception e) {

            ServiceContext.getDaoContext().rollback();

            level = PubLevelEnum.ERROR;

            msg =
                    AppLogHelper.logError(getClass(), "SyncUsersJob.error",
                            msgTestPfx, e.getMessage());

            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);

        } finally {

            try {
                /*
                 * Do NOT use pubMsg since we use the ready-to-use result
                 * message from the application log.
                 */
                AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC,
                        level, msg);

                if (!this.isTest) {

                    final MemberCard memberCard = MemberCard.instance();

                    if (memberCard.getStatus() == Stat.EXCEEDED) {
                        level = PubLevelEnum.WARN;
                        msg =
                                AppLogHelper.logWarning(getClass(),
                                        "Membership.exceeded",
                                        CommunityDictEnum.MEMBERSHIP.getWord());
                    } else {
                        // Just for now ...
                        level = PubLevelEnum.INFO;
                        msg = memberCard.getCommunityNotice();
                    }

                    AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC,
                            level, msg);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }

    /**
     * Publish message on CometD admin channel.
     *
     * @param msg
     *            The message.
     */
    private void pubMsg(final String msg) {
        if (this.isTest) {
            AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC,
                    PubLevelEnum.INFO, msgTestPfx + " " + msg);
        } else {
            AdminPublisher.instance().publish(PubTopicEnum.USER_SYNC,
                    PubLevelEnum.INFO, msg);
        }
    }

    /**
     * Gets the users from the source.
     *
     * @return Sorted set with users.
     */
    private SortedSet<CommonUser> getSourceUsers() {

        final ConfigManager cm = ConfigManager.instance();

        this.userSource = cm.getUserSource();

        this.rfidNumberFormat = this.userSource.createRfidNumberFormat();

        this.minLengthIdNumber =
                ConfigManager.instance().getConfigInt(
                        Key.USER_ID_NUMBER_LENGTH_MIN);

        final String group = cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

        final SortedSet<CommonUser> users;

        if (group.isEmpty()) {
            users = this.userSource.getUsers();
        } else {
            users = this.userSource.getUsersInGroup(group, true);
        }
        return users;
    }

    /**
     * Gets the next User from the User Source.
     *
     * @param iter
     *            The {@link Iterator}/
     * @return {@code null} when EOF.
     */
    private CommonUser nextSrc(final Iterator<CommonUser> iter) {
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    /**
     * Gets the next database User and load it into the session context.
     *
     * @param iter
     *            The {@link Iterator}.
     * @return {@code null} when EOF.
     */
    private User nextDb(final Iterator<User> iter) {
        if (iter.hasNext()) {
            return ServiceContext.getDaoContext().getUserDao()
                    .findById(iter.next().getId());
        }
        return null;
    }

    /**
     * Adds/Removes card number(s) to/from the User, based on the card number
     * offered from the user source.
     * <p>
     * If the {@link IUserSource#isCardNumberProvided()} is {@code false}, the
     * current Card Number(s) of the user are left as they are.
     * </p>
     * <p>
     * IMPORTANT: This method MUST guard the <i>unique constraint</i> on card
     * number, since the enforcing index only gets updated <i>after</i> a
     * commit, a commit is forced when {@link UserCard} is attached or detached
     * from any User.
     * </p>
     * <p>
     * This algorithm assures that no duplicates exist at commit time:
     * </p>
     * <ul>
     * <li>If a duplicate card number is identified for a User to be processed
     * in a <i>next</i> call to this method, we <i>must</i> detach it from the
     * next User and attach it to the current User.</li>
     * <li>If a duplicate is identified for a User processed in a
     * <i>previous</i> method call, we <i>must</i> detach the card for the
     * current User.</li>
     * </ul>
     *
     * @param user
     *            The User to attach or detach the Card to/from.
     * @param isExistingUser
     *            {@code true} if the user already exists, {@code false} if this
     *            is a new user.
     * @param cardNumberSrc
     *            The card number from the User source (can be {@code null} when
     *            not present.
     * @param trxDate
     *            The transaction date.
     * @param trxBy
     *            The transaction actor.
     * @return {@code true} if UserCard of the User was updated (card attach or
     *         detach).
     */
    private boolean handlePrimaryCardNumber(final User user,
            final boolean isExistingUser, final String cardNumberSrc,
            final Date trxDate, final String trxBy) {

        if (!this.userSource.isCardNumberProvided()) {
            return false;
        }

        if (this.isTest) {
            return !isExistingUser
                    || !StringUtils.defaultString(
                            USER_SERVICE.getPrimaryCardNumber(user))
                            .equalsIgnoreCase(
                                    StringUtils.defaultString(cardNumberSrc));
        }

        final UserCardDao userCardDao =
                ServiceContext.getDaoContext().getUserCardDao();

        String primaryCardNumber = cardNumberSrc;

        if (primaryCardNumber != null) {

            try {
                primaryCardNumber =
                        this.rfidNumberFormat
                                .getNormalizedNumber(cardNumberSrc);

            } catch (NumberFormatException e) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx
                            + " Card ["
                            + cardNumberSrc
                            + "] Format Error handled as NO card present for User ["
                            + user.getUserId() + "]");
                }

                primaryCardNumber = null;
            }
        }

        /*
         * Assume NO Primary Card is attached.
         */
        boolean attachPrimaryCard = false;

        /*
         * Assume NO update (attach/detach) performed.
         */
        boolean isUpdated = false;

        /*
         * Assume NO commit is needed to update the unique index on card number
         * address in the CardNumber table.
         */
        boolean commitAtNextIncrement = false;

        /*
         * Card offered?
         */
        if (StringUtils.isBlank(primaryCardNumber)) {

            /*
             * No Primary Card offered: REMOVE the Primary Card that was
             * attached before.
             */
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx
                        + " No Primary Card to Attach for User ["
                        + user.getUserId() + "]");
            }

            if (user.getCards() != null) {

                final Iterator<UserCard> iter = user.getCards().iterator();

                while (iter.hasNext()) {

                    final UserCard card = iter.next();

                    if (userCardDao.isPrimaryCard(card)) {

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx
                                    + " Detached Primary Card ["
                                    + card.getNumber() + "] from User ["
                                    + user.getUserId() + "]");
                        }

                        userCardDao.delete(card);
                        iter.remove();
                        isUpdated = true;
                        commitAtNextIncrement = true;
                        break;
                    }
                }
            }

            attachPrimaryCard = false;

        } else {

            /*
             * Check occurrence of Card Number offered.
             */
            final UserCard userCard =
                    userCardDao.findByCardNumber(primaryCardNumber);

            if (userCard == null) {
                /*
                 * Card is NOT present, and can be used.
                 */
                attachPrimaryCard = true;

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " New Card [" + primaryCardNumber
                            + "]");
                }

                // This commit prevents that a single card number linked to
                // different users in the source leads to index constraint
                // violations.
                commitAtNextIncrement = true;

            } else if (userCard.getUser().getInternal()) {

                /*
                 * Card already present at INTERNAL user.
                 */
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " Card [" + primaryCardNumber
                            + "] Attached to Previous User ["
                            + userCard.getUser().getUserId()
                            + "]. Ignored Attach to Current User ["
                            + user.getUserId() + "]");
                }

                attachPrimaryCard = false;

            } else if (userCard.getUser().getId().equals(user.getId())) {

                /*
                 * Email already present at SAME user, but as primary?
                 */

                if (userCardDao.isPrimaryCard(userCard)) {
                    /*
                     * Card already present as PRIMARY at SAME user: no action
                     * needed.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Primary Card ["
                                + primaryCardNumber
                                + "] already Attached to User ["
                                + user.getUserId() + "]");
                    }

                    attachPrimaryCard = false;

                } else {
                    /*
                     * Card already present at SAME user, but NOT as PRIMARY: we
                     * need to re-attach it.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Card [" + primaryCardNumber
                                + "] already Attached to User ["
                                + user.getUserId() + "]. Re-Attach as Primary");
                    }

                    /*
                     * Re-attach will be handled as well...
                     */
                    attachPrimaryCard = true;
                }

            } else {
                /*
                 * Card already present at OTHER user
                 */
                final String currentUserId = user.getUserId();
                final String otherUserId = userCard.getUser().getUserId();

                /*
                 * Check if OTHER User is a NEXT User. We can use the
                 * compareTo() method since we are part of a balanced line
                 * batch, where users are sorted ascending.
                 */
                if (currentUserId.compareTo(otherUserId) < 0) {
                    /*
                     * OTHER User IS a NEXT User: detach from next user and
                     * attach to current.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Card [" + primaryCardNumber
                                + "] Detach from Next User [" + otherUserId
                                + "]. Attach to Current User [" + currentUserId
                                + "]");
                    }

                    /*
                     * Detaching CardNumber from next user.
                     */
                    final User userDb =
                            ServiceContext.getDaoContext().getUserDao()
                                    .findById(userCard.getUser().getId());

                    if (detachCardFromUser(userCardDao, userDb,
                            primaryCardNumber)) {

                        userDb.setModifiedBy(trxBy);
                        userDb.setModifiedDate(trxDate);

                        ServiceContext.getDaoContext().getUserDao()
                                .update(userDb);

                        commitAtNextIncrement = true;

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx + " Detached Card ["
                                    + primaryCardNumber + "] from Next User ["
                                    + otherUserId + "]");
                        }
                    }

                    attachPrimaryCard = true;

                } else {
                    /*
                     * OTHER User is a PREVIOUS User: detach or ignore.
                     */
                    isUpdated =
                            detachCardFromUser(userCardDao, user,
                                    primaryCardNumber);

                    if (isUpdated) {

                        commitAtNextIncrement = true;

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx + " Card ["
                                    + primaryCardNumber
                                    + "] Attached to Previous User ["
                                    + otherUserId
                                    + "]. Detached from Current User ["
                                    + currentUserId + "]");
                        }

                    } else if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Card [" + primaryCardNumber
                                + "] Attached to Previous User [" + otherUserId
                                + "]. Ignored Attach to Current User ["
                                + currentUserId + "]");
                    }

                    attachPrimaryCard = false;
                }
            }

        }

        /*
         * (Re) Attach the primary card.
         */
        if (attachPrimaryCard) {

            if (isExistingUser) {

                USER_SERVICE.assocPrimaryCardNumber(user, primaryCardNumber);

            } else {

                user.setCards(new ArrayList<UserCard>());

                final UserCard userCard = new UserCard();

                userCard.setUser(user);
                userCard.setNumber(primaryCardNumber);
                userCardDao.assignPrimaryCard(userCard);

                user.getCards().add(userCard);
            }

            isUpdated = true;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " Attached Card ["
                        + primaryCardNumber + "] to User [" + user.getUserId()
                        + "] ");
            }
        }

        if (commitAtNextIncrement) {
            this.batchCommitter.commitAtNextIncrement();
        }
        return isUpdated;
    }

    /**
     * Adds/Removes ID number(s) to/from the User, based on the ID number
     * offered from the user source.
     * <p>
     * If the {@link IUserSource#isIdNumberProvided()} is {@code false}, the
     * current ID Number(s) of the user are left as they are.
     * </p>
     * <p>
     * IMPORTANT: This method MUST guard the <i>unique constraint</i> on ID
     * number, since the enforcing index only gets updated <i>after</i> a
     * commit, a commit is forced when {@link UserNumber} is attached or
     * detached from any User..
     * </p>
     * <p>
     * This algorithm assures that no duplicates exist at commit time:
     * </p>
     * <ul>
     * <li>If a duplicate ID number is identified for a User to be processed in
     * a <i>next</i> call to this method, we <i>must</i> detach it from the next
     * User and attach it to the current User.</li>
     * <li>If a duplicate is identified for a User processed in a
     * <i>previous</i> method call, we <i>must</i> detach the ID for the current
     * User.</li>
     * </ul>
     *
     * @param user
     *            The User to attach or detach the ID to/from.
     * @param isExistingUser
     *            {@code true} if the user already exists, {@code false} if this
     *            is a new user.
     * @param idNumberSrc
     *            The ID number from the User source (can be {@code null} when
     *            not present.
     * @param trxDate
     *            The transaction date.
     * @param trxBy
     *            The transaction actor.
     * @return {@code true} if UserNumber of the User was updated (ID attach or
     *         detach).
     */
    private boolean handlePrimaryIdNumber(final User user,
            final boolean isExistingUser, final String idNumberSrc,
            final Date trxDate, final String trxBy) {

        if (!this.userSource.isIdNumberProvided()) {
            return false;
        }

        if (this.isTest) {
            return !isExistingUser
                    || !StringUtils.defaultString(
                            USER_SERVICE.getPrimaryIdNumber(user))
                            .equalsIgnoreCase(
                                    StringUtils.defaultString(idNumberSrc));
        }

        final UserNumberDao userNumberDao =
                ServiceContext.getDaoContext().getUserNumberDao();

        String primaryIdNumber = idNumberSrc;

        /*
         * INVARIANT: Primary ID number MUST be valid.
         */
        if (primaryIdNumber != null && this.minLengthIdNumber != null) {

            if (primaryIdNumber.length() < this.minLengthIdNumber) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " ID [" + idNumberSrc
                            + "] Length Error handled as "
                            + "NO ID present for User [" + user.getUserId()
                            + "]");
                }

                primaryIdNumber = null;
            }
        }

        /*
         * Assume NO Primary ID is attached.
         */
        boolean attachPrimaryId = false;

        /*
         * Assume NO update (attach/detach) performed.
         */
        boolean isUpdated = false;

        /*
         * Assume NO commit is needed to update the unique index on ID number
         * address in the UserNumber table.
         */
        boolean commitAtNextIncrement = false;

        /*
         * ID offered?
         */
        if (StringUtils.isBlank(primaryIdNumber)) {

            /*
             * No ID Number offered: REMOVE IDs that were attached before.
             */
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " No Primary ID to Attach for User ["
                        + user.getUserId() + "]");
            }

            if (user.getIdNumbers() != null) {

                final Iterator<UserNumber> iter =
                        user.getIdNumbers().iterator();

                while (iter.hasNext()) {

                    final UserNumber number = iter.next();

                    if (userNumberDao.isPrimaryNumber(number)) {

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx + " Detached Primary ID ["
                                    + number.getNumber() + "] from User ["
                                    + user.getUserId() + "]");
                        }
                        userNumberDao.delete(number);
                        iter.remove();
                        isUpdated = true;
                        commitAtNextIncrement = true;
                        break;
                    }
                }
            }

            attachPrimaryId = false;

        } else {

            /*
             * Check occurrence of ID Number offered.
             */
            final UserNumber userNumber =
                    userNumberDao.findByNumber(primaryIdNumber);

            if (userNumber == null) {
                /*
                 * ID is NOT present.
                 */
                attachPrimaryId = true;

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " New ID [" + primaryIdNumber
                            + "]");
                }

                // This commit prevents that a single User ID linked to
                // different users in the source leads to index constraint
                // violations.
                commitAtNextIncrement = true;

            } else if (userNumber.getUser().getInternal()) {

                /*
                 * ID already present at INTERNAL user.
                 */
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " ID [" + primaryIdNumber
                            + "] Attached to Previous User ["
                            + userNumber.getUser().getUserId()
                            + "]. Ignored Attach to Current User ["
                            + user.getUserId() + "]");
                }

                attachPrimaryId = false;

            } else if (userNumber.getUser().getId().equals(user.getId())) {

                /*
                 * ID Number already present at SAME user, but as primary?
                 */

                if (userNumberDao.isPrimaryNumber(userNumber)) {
                    /*
                     * ID already present as PRIMARY at SAME user: no action
                     * needed.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Primary ID ["
                                + primaryIdNumber
                                + "] already Attached to User ["
                                + user.getUserId() + "]");
                    }

                    attachPrimaryId = false;

                } else {
                    /*
                     * IDalready present at SAME user, but NOT as PRIMARY: we
                     * need to re-attach it.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " ID [" + primaryIdNumber
                                + "] already Attached to User ["
                                + user.getUserId() + "]. Re-Attach as Primary");
                    }

                    /*
                     * Re-attach will be handled as well...
                     */
                    attachPrimaryId = true;
                }

            } else {
                /*
                 * ID already present at OTHER user
                 */
                final String currentUserId = user.getUserId();
                final String otherUserId = userNumber.getUser().getUserId();

                /*
                 * Check if OTHER User is a NEXT User. We can use the
                 * compareTo() method since we are part of a balanced line
                 * batch, where users are sorted ascending.
                 */
                if (currentUserId.compareTo(otherUserId) < 0) {
                    /*
                     * OTHER User IS a NEXT User: detach from next user and
                     * attach to current.
                     */

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " ID [" + primaryIdNumber
                                + "] Detach from Next User [" + otherUserId
                                + "]. Attach to Current User [" + currentUserId
                                + "]");
                    }

                    /*
                     * Detaching ID number from next user.
                     */
                    final User userDb =
                            ServiceContext.getDaoContext().getUserDao()
                                    .findById(userNumber.getUser().getId());

                    if (detachIdNumberFromUser(userNumberDao, userDb,
                            primaryIdNumber)) {

                        userDb.setModifiedBy(trxBy);
                        userDb.setModifiedDate(trxDate);

                        ServiceContext.getDaoContext().getUserDao()
                                .update(userDb);

                        commitAtNextIncrement = true;

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx + " Detached Primary ID ["
                                    + primaryIdNumber + "] from Next User ["
                                    + otherUserId + "]");
                        }
                    }

                    attachPrimaryId = true;

                } else {
                    /*
                     * OTHER User is a PREVIOUS User: detach or ignore.
                     */
                    isUpdated =
                            detachIdNumberFromUser(userNumberDao, user,
                                    primaryIdNumber);

                    if (isUpdated) {

                        commitAtNextIncrement = true;

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx + " ID [" + primaryIdNumber
                                    + "] Attached to Previous User ["
                                    + otherUserId
                                    + "]. Detached from Current User ["
                                    + currentUserId + "]");
                        }
                    } else if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " ID [" + primaryIdNumber
                                + "] Attached to Previous User [" + otherUserId
                                + "]. Ignored Attach to Current User ["
                                + currentUserId + "]");
                    }

                    attachPrimaryId = false;

                }
            }
        }

        if (attachPrimaryId) {

            if (isExistingUser) {

                USER_SERVICE.assocPrimaryIdNumber(user, primaryIdNumber);

            } else {

                user.setIdNumbers(new ArrayList<UserNumber>());

                final UserNumber userNumber = new UserNumber();
                userNumber.setUser(user);

                userNumber.setNumber(primaryIdNumber);
                userNumberDao.assignPrimaryNumber(userNumber);

                user.getIdNumbers().add(userNumber);
            }

            isUpdated = true;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " Attached ID [" + primaryIdNumber
                        + "] to User [" + user.getUserId() + "] ");
            }
        }

        if (commitAtNextIncrement) {
            this.batchCommitter.commitAtNextIncrement();
        }

        return isUpdated;
    }

    /**
     * Adds/Removes Email address(es) to/from the User, based on the Email
     * offered from the user source.
     * <p>
     * If the {@link IUserSource#isEmailProvided()} is {@code false}, the
     * current Email address(es) of the user are left as they are.
     * </p>
     * <p>
     * IMPORTANT: This method MUST guard the <i>unique constraint</i> on Email
     * address, since the enforcing index only gets updated <i>after</i> a
     * commit, a commit is forced when {@link UserEmail} is attached or detached
     * from any User.
     * </p>
     * <p>
     * This algorithm assures that no duplicates exist at commit time:
     * </p>
     * <ul>
     * <li>If a duplicate Email address is identified for a User to be processed
     * in a <i>next</i> call to this method, we <i>must</i> detach it from the
     * next User and attach it to the current User.</li>
     * <li>If a duplicate is identified for a User processed in a
     * <i>previous</i> method call, we <i>must</i> detach the Email address for
     * the current User.</li>
     * </ul>
     *
     * @param user
     *            The User to attach or detach the Email to/from.
     * @param isExistingUser
     *            {@code true} if the user already exists, {@code false} if this
     *            is a new user.
     * @param emailAddressSrc
     *            The Email from the User source (can be {@code null} when not
     *            present.
     * @param trxDate
     *            The transaction date.
     * @param trxBy
     *            The transaction actor.
     * @return {@code true} if {@link UserEmail} of the User was updated (Email
     *         attach or detach).
     */
    private boolean handlePrimaryEmail(final User user,
            final boolean isExistingUser, final String emailAddressSrc,
            final Date trxDate, final String trxBy) {

        if (!this.userSource.isEmailProvided()) {
            return false;
        }

        if (this.isTest) {
            return !isExistingUser
                    || !StringUtils.defaultString(
                            USER_SERVICE.getPrimaryEmailAddress(user))
                            .equalsIgnoreCase(
                                    StringUtils.defaultString(emailAddressSrc));
        }

        final UserEmailDao userEmailDao =
                ServiceContext.getDaoContext().getUserEmailDao();

        String primaryEmailAddress = emailAddressSrc;

        /*
         * INVARIANT: Primary Email address MUST be valid.
         */
        if (primaryEmailAddress != null) {

            primaryEmailAddress = primaryEmailAddress.toLowerCase();

            if (!this.emailValidator.validate(primaryEmailAddress)) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " Email [" + emailAddressSrc
                            + "] Format Error handled as NO Primary "
                            + "Email present for User [" + user.getUserId()
                            + "]");
                }

                primaryEmailAddress = null;
            }
        }

        /*
         * Assume NO Primary Email is attached.
         */
        boolean attachPrimaryEmail = false;

        /*
         * Assume NO update (attach/detach) performed.
         */
        boolean isUpdated = false;

        /*
         * Assume NO commit is needed to update the unique index on email
         * address in the UserEmail table.
         */
        boolean commitAtNextIncrement = false;

        /*
         * Primary Email offered?
         */
        if (StringUtils.isBlank(primaryEmailAddress)) {

            /*
             * No Primary Email offered: REMOVE the Primary Email that was
             * attached before.
             */
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx
                        + " No Primary Email to Attach for User ["
                        + user.getUserId() + "]");
            }

            if (user.getEmails() != null) {

                final Iterator<UserEmail> iter = user.getEmails().iterator();

                while (iter.hasNext()) {

                    final UserEmail email = iter.next();

                    if (userEmailDao.isPrimaryEmail(email)) {

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx
                                    + " Detached Primary Email ["
                                    + email.getAddress() + "] from User ["
                                    + user.getUserId() + "]");
                        }

                        userEmailDao.delete(email);
                        iter.remove();
                        isUpdated = true;
                        commitAtNextIncrement = true;
                        break;
                    }
                }
            }

            attachPrimaryEmail = false;

        } else {
            /*
             * Check occurrence of Email offered.
             */
            final UserEmail userEmail =
                    userEmailDao.findByEmail(primaryEmailAddress);

            if (userEmail == null) {
                /*
                 * Email is NOT already present, and can be used.
                 */
                attachPrimaryEmail = true;

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " New Email ["
                            + primaryEmailAddress + "]");
                }

                // This commit prevents that a single email address linked to
                // different users in the source leads to index constraint
                // violations.
                commitAtNextIncrement = true;

            } else if (userEmail.getUser().getInternal()) {

                /*
                 * Email already present at INTERNAL user.
                 */
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msgTestPfx + " Email [" + primaryEmailAddress
                            + "] Attached to Previous User ["
                            + userEmail.getUser().getUserId()
                            + "]. Ignored Attach to Current User ["
                            + user.getUserId() + "]");
                }

                attachPrimaryEmail = false;

            } else if (userEmail.getUser().getId().equals(user.getId())) {

                /*
                 * Email already present at SAME user, but as primary?
                 */

                if (userEmailDao.isPrimaryEmail(userEmail)) {
                    /*
                     * Email already present as PRIMARY at SAME user: no action
                     * needed.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Primary Email ["
                                + primaryEmailAddress
                                + "] already Attached to User ["
                                + user.getUserId() + "]");
                    }

                    attachPrimaryEmail = false;

                } else {
                    /*
                     * Email already present at SAME user, but NOT as PRIMARY:
                     * we need to re-attach it.
                     */
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Email ["
                                + primaryEmailAddress
                                + "] already Attached to User ["
                                + user.getUserId() + "]. Re-Attach as Primary");
                    }
                    /*
                     * Re-attach will be handled as well...
                     */
                    attachPrimaryEmail = true;
                }

            } else {
                /*
                 * Email already present at OTHER user
                 */
                final String currentUserId = user.getUserId();
                final String otherUserId = userEmail.getUser().getUserId();

                /*
                 * Check if OTHER User is a NEXT User. We can use the
                 * compareTo() method since we are part of a balanced line
                 * batch, where users are sorted ascending.
                 */
                if (currentUserId.compareTo(otherUserId) < 0) {
                    /*
                     * OTHER User IS a NEXT User: detach from next user and
                     * attach to current.
                     */

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Email ["
                                + primaryEmailAddress
                                + "] Detach from Next User [" + otherUserId
                                + "]. Attach to Current User [" + currentUserId
                                + "]");
                    }

                    /*
                     * Detaching email from next user.
                     */
                    final User userDb =
                            ServiceContext.getDaoContext().getUserDao()
                                    .findById(userEmail.getUser().getId());

                    if (detachEmailFromUser(userEmailDao, userDb,
                            primaryEmailAddress)) {

                        userDb.setModifiedBy(trxBy);
                        userDb.setModifiedDate(trxDate);

                        ServiceContext.getDaoContext().getUserDao()
                                .update(userDb);

                        commitAtNextIncrement = true;

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx
                                    + " Detached Primary Email ["
                                    + primaryEmailAddress
                                    + "] from Next User [" + otherUserId + "]");
                        }
                    }

                    attachPrimaryEmail = true;

                } else {

                    /*
                     * OTHER User is a PREVIOUS User: detach or ignore.
                     */
                    isUpdated =
                            detachEmailFromUser(userEmailDao, user,
                                    primaryEmailAddress);

                    if (isUpdated) {

                        commitAtNextIncrement = true;

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(msgTestPfx + " Email ["
                                    + primaryEmailAddress
                                    + "] Attached to Previous User ["
                                    + otherUserId
                                    + "]. Detached from Current User ["
                                    + currentUserId + "]");
                        }
                    } else if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(msgTestPfx + " Email ["
                                + primaryEmailAddress
                                + "] Attached to Previous User [" + otherUserId
                                + "]. Ignored Attach to Current User ["
                                + currentUserId + "]");

                    }

                    attachPrimaryEmail = false;
                }
            }
        }

        /*
         * (Re) Attach the primary email address.
         */
        if (attachPrimaryEmail) {

            if (isExistingUser) {

                USER_SERVICE.assocPrimaryEmail(user, primaryEmailAddress);

            } else {

                user.setEmails(new ArrayList<UserEmail>());

                final UserEmail userEmail = new UserEmail();

                userEmail.setUser(user);
                userEmail.setAddress(primaryEmailAddress);
                userEmailDao.assignPrimaryEmail(userEmail);

                user.getEmails().add(userEmail);
            }

            isUpdated = true;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " Attached Primary Email ["
                        + primaryEmailAddress + "] to User ["
                        + user.getUserId() + "] ");
            }
        }

        if (commitAtNextIncrement) {
            this.batchCommitter.commitAtNextIncrement();
        }

        return isUpdated;
    }

    /**
     * Detaches an {@link UserEmail} from a {@link User}.
     *
     * @param userEmailDao
     *            The {@link UserEmailDao}.
     * @param user
     *            The {@link User}.
     * @param addressToDetach
     *            The email address to detach.
     * @return {@code true} when successfully detached.
     */
    private static boolean detachEmailFromUser(final UserEmailDao userEmailDao,
            final User user, final String addressToDetach) {

        boolean isDetached = false;

        final List<UserEmail> emails = user.getEmails();

        if (emails != null) {

            final Iterator<UserEmail> iter = emails.iterator();

            while (iter.hasNext()) {

                final UserEmail email = iter.next();

                if (email.getAddress().equals(addressToDetach)) {
                    userEmailDao.delete(email);
                    iter.remove();
                    isDetached = true;
                    break;
                }
            }
        }
        return isDetached;
    }

    /**
     * Detaches an {@link User} from a {@link User}.
     *
     * @param userCardDao
     *            The {@link UserCardDao}.
     * @param user
     *            The {@link User}.
     * @param cardToDetach
     *            The card number to detach.
     * @return {@code true} when successfully detached.
     */
    private static boolean detachCardFromUser(final UserCardDao userCardDao,
            final User user, final String cardNumberToDetach) {

        boolean isDetached = false;

        final List<UserCard> cards = user.getCards();

        if (cards != null) {

            final Iterator<UserCard> iter = cards.iterator();

            while (iter.hasNext()) {

                final UserCard card = iter.next();

                if (card.getNumber().equals(cardNumberToDetach)) {
                    userCardDao.delete(card);
                    iter.remove();
                    isDetached = true;
                    break;
                }
            }
        }
        return isDetached;
    }

    /**
     * Detaches an {@link UserEmail} from a {@link User}.
     *
     * @param userNumberDao
     *            The {@link UserNumberDao}.
     * @param user
     *            The {@link User}.
     * @param idNumberToDetach
     *            The ID number to detach.
     * @return {@code true} when successfully detached.
     */
    private static boolean detachIdNumberFromUser(
            final UserNumberDao userNumberDao, final User user,
            final String idNumberToDetach) {

        boolean isDetached = false;

        final List<UserNumber> numbers = user.getIdNumbers();

        if (numbers != null) {

            final Iterator<UserNumber> iter = numbers.iterator();

            while (iter.hasNext()) {

                final UserNumber number = iter.next();

                if (number.getNumber().equals(idNumberToDetach)) {
                    userNumberDao.delete(number);
                    iter.remove();
                    isDetached = true;
                    break;
                }
            }
        }
        return isDetached;
    }

    /**
     *
     * @param user
     *            The User.
     * @param createdDate
     * @param createdBy
     */
    private void addUser(final CommonUser user, final Date createdDate,
            final String createdBy) {

        final String username = user.getUserName();

        if (ConfigManager.isInternalAdmin(username)) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " User [" + user.getUserName()
                        + "] NOT added to DB: reserved user");
            }

        } else {

            final User userDb = new User();

            userDb.setUserId(username);
            userDb.setAdmin(false);
            userDb.setInternal(false);
            userDb.setFullName(user.getFullName());
            userDb.setExternalUserName(user.getUserName());
            userDb.setCreatedBy(createdBy);
            userDb.setCreatedDate(createdDate);

            handlePrimaryEmail(userDb, false, user.getEmail(), createdDate,
                    createdBy);
            handlePrimaryCardNumber(userDb, false, user.getCardNumber(),
                    createdDate, createdBy);
            handlePrimaryIdNumber(userDb, false, user.getIdNumber(),
                    createdDate, createdBy);

            ServiceContext.getDaoContext().getUserDao().create(userDb);

            this.batchCommitter.increment();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " added [" + user.getUserName()
                        + "] to DB");
            }
        }
    }

    /**
     * Updates a user from the external source
     * <p>
     * An external user will overwrite an internal user with the same user name.
     * As a result the user will become external.
     * </p>
     *
     * @param user
     * @param userDb
     * @return {@code true} if updated (because there was a difference).
     */
    private boolean updateUser(final CommonUser user, final User userDb,
            final Date modifiedDate, final String modifiedBy) {

        boolean updated = false;

        if (handlePrimaryEmail(userDb, true, user.getEmail(), modifiedDate,
                modifiedBy)) {
            updated = true;
        }

        if (handlePrimaryCardNumber(userDb, true, user.getCardNumber(),
                modifiedDate, modifiedBy)) {
            updated = true;
        }

        if (handlePrimaryIdNumber(userDb, true, user.getIdNumber(),
                modifiedDate, modifiedBy)) {
            updated = true;
        }

        if (userDb.getFullName() == null
                || !userDb.getFullName().equals(user.getFullName())) {
            userDb.setFullName(user.getFullName());
            updated = true;
        }

        if (userDb.getInternal()) {
            userDb.setInternal(false);
            updated = true;
        }

        if (updated) {

            userDb.setModifiedBy(modifiedBy);
            userDb.setModifiedDate(modifiedDate);

            ServiceContext.getDaoContext().getUserDao().update(userDb);

            this.batchCommitter.increment();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msgTestPfx + " updated [" + user.getUserName()
                        + "] in DB");
            }
        }
        return updated;
    }

    /**
     * Logically deletes an external user.
     *
     * @param userDb
     *            The {@link User} from the database.
     */
    private void deleteUser(final User userDb) {

        final List<UserEmail> emails = userDb.getEmails();
        final List<UserCard> cards = userDb.getCards();
        final List<UserNumber> numbers = userDb.getIdNumbers();

        if ((emails != null && !emails.isEmpty())
                || (cards != null && !cards.isEmpty())
                || (numbers != null && !numbers.isEmpty())) {
            this.batchCommitter.commitAtNextIncrement();
        }

        USER_SERVICE.performLogicalDelete(userDb);

        ServiceContext.getDaoContext().getUserDao().update(userDb);

        this.batchCommitter.increment();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(msgTestPfx + " deleted [" + userDb.getUserId() + "]");
        }
    }

    /**
     * Deletes user files of external user (user data in the database is NOT
     * removed).
     * <p>
     * When in <i>test</i> mode the remove is NOT performed but simulated.
     * </p>
     *
     * @param userDb
     *            The user to remove the files for.
     * @throws IOException
     *             When file system errors.
     */
    private void deleteUserFiles(final User userDb) throws IOException {
        nBytesUserFilesDeleted +=
                ConfigManager.getUserHomeDirSize(userDb.getUserId());
        if (!this.isTest) {
            ConfigManager.removeUserHomeDir(userDb.getUserId());
        }
    }

    /**
     * Synchronizes users between user source and SavaPage database.
     *
     * @throws Exception
     *             When an error occurs.
     */
    private void syncUsers() throws Exception {

        final boolean isUpdateUsers =
                ConfigManager.instance().isConfigValue(
                        Key.USER_SOURCE_UPDATE_USER_DETAILS);

        final Date syncDate = ServiceContext.getTransactionDate();
        final String syncActor = ServiceContext.getActor();

        pubMsg(Messages.getMessage(getClass(), "SyncUsersJob.start", null));

        nBytesUserFilesPresent = 0;
        nBytesUserFilesDeleted = 0;

        int nAdded = 0;
        int nDeleted = 0;
        int nUpdated = 0;
        int nIdentical = 0;
        int nSameUser = 0;
        int nNonExist = 0;

        int nInternalUsers = 0;
        int nInternalUsersUpd = 0;

        final List<User> usersDb =
                getListChunk(DaoContextImpl.peekEntityManager(), null, null);

        final SortedSet<CommonUser> users = getSourceUsers();

        pubMsg("Synchronizing [" + users.size() + "] users with ["
                + usersDb.size() + "] in database");

        /*
         * Balanced line between users in the source and users in the database.
         */
        final Iterator<CommonUser> iterSrc = users.iterator();
        final Iterator<User> iterDb = usersDb.iterator();

        /*
         * Initial reads + batch committer.
         */
        CommonUser userSrc = nextSrc(iterSrc);
        User userDb = nextDb(iterDb);

        this.batchCommitter =
                ServiceContext.getDaoContext().createBatchCommitter(
                        ConfigManager.getDaoBatchChunkSize());

        this.batchCommitter.setTest(this.isTest);

        /*
         * Process the balanced line.
         */
        while (userSrc != null || userDb != null) {

            if (userSrc == null) {

                /*
                 * No more users in source.
                 */
                if (userDb.getInternal()) {
                    /*
                     * Do NOT delete internal users.
                     */
                    nInternalUsers++;
                } else {
                    if (isDeleteUsers) {
                        deleteUser(userDb);
                        nDeleted++;
                    } else {
                        nNonExist++;
                    }
                    deleteUserFiles(userDb);
                }
                userDb = nextDb(iterDb);
                continue;
            }

            if (userDb == null) {
                /*
                 * No more users in Db: add user + read next
                 */
                addUser(userSrc, syncDate, syncActor);
                nAdded++;
                userSrc = nextSrc(iterSrc);
                continue;
            }

            final int compare =
                    userDb.getUserId().compareTo(userSrc.getUserName());

            if (compare == 0) {

                nBytesUserFilesPresent +=
                        ConfigManager.getUserHomeDirSize(userDb.getUserId());

                nSameUser++;

                if (isUpdateUsers) {

                    if (userDb.getInternal()) {
                        nInternalUsersUpd++;
                    }

                    if (updateUser(userSrc, userDb, syncDate, syncActor)) {
                        nUpdated++;
                    } else {
                        nIdentical++;
                    }
                } else {
                    if (userDb.getInternal()) {
                        nInternalUsers++;
                    }
                }

                userSrc = nextSrc(iterSrc);
                userDb = nextDb(iterDb);

            } else if (compare < 0) {

                if (userDb.getInternal()) {
                    /*
                     * Do NOT delete internal users.
                     */
                    nInternalUsers++;
                } else {
                    if (isDeleteUsers) {
                        deleteUser(userDb);
                        nDeleted++;
                    } else {
                        nNonExist++;
                    }
                    deleteUserFiles(userDb);
                }
                userDb = nextDb(iterDb);

            } else {

                addUser(userSrc, syncDate, syncActor);
                nAdded++;
                userSrc = nextSrc(iterSrc);

            }

        } // end-while

        /*
         * Commit any remaining increments.
         */
        this.batchCommitter.commit();

        /*
         *
         */
        String msg = null;

        if (isDeleteUsers) {

            if (isUpdateUsers) {
                msg =
                        "Users: identical [" + nIdentical + "] added ["
                                + nAdded + "] updated [" + nUpdated
                                + "] deleted [" + nDeleted + "] internal ["
                                + nInternalUsers + "]";
            } else {
                msg =
                        "Users: same [" + nSameUser + "] added [" + nAdded
                                + "] deleted [" + nDeleted + "] internal ["
                                + nInternalUsers + "]";
            }

        } else {

            if (isUpdateUsers) {
                msg =
                        "Users: identical [" + nIdentical + "] added ["
                                + nAdded + "] updated [" + nUpdated
                                + "] non-exist [" + nNonExist + "] internal ["
                                + nInternalUsers + "]";

            } else {
                msg =
                        "Users: same [" + nSameUser + "] added [" + nAdded
                                + "] non-exist [" + nNonExist + "] internal ["
                                + nInternalUsers + "]";
            }
        }

        pubMsg(msg);

        msg =
                "Files: present ["
                        + NumberUtil.humanReadableByteCount(
                                nBytesUserFilesPresent, true) + "]";

        if (nBytesUserFilesDeleted > 0) {
            msg +=
                    " deleted ["
                            + NumberUtil.humanReadableByteCount(
                                    nBytesUserFilesDeleted, true) + "]";
        }
        pubMsg(msg);
    }

    /**
     * Gets a chunk of (external AND internal) non-deleted users ordered
     * ascending by userId.
     *
     * @param em
     *            The JPA entity manager. The caller is responsible for the
     *            close() of the entity manager.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of users. If {@code null}, then the chunk starts
     *            with the first user.
     * @param maxResults
     *            The maximum number of users in the chunk. If {@code null},
     *            then ALL (remaining users) are returned.
     * @return
     */
    private static List<User> getListChunk(final EntityManager em,
            final Integer startPosition, final Integer maxResults) {

        final String jpql =
                "SELECT U FROM User U" + " WHERE U.deleted = false"
                        + " ORDER BY U.userId ";

        final Query query = em.createQuery(jpql);

        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

}
