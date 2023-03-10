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
package org.savapage.core.msg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.helpers.IppQueueHelper;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.IOHelper;
import org.savapage.core.util.Messages;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the date-time of the most recent message to be delivered
 * to User Web App.
 * <p>
 * A file is used in the SafePages directory of the user to read/write the
 * message.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class UserMsgIndicator {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserMsgIndicator.class);

    /**
     * The comment written to the header of the message file.
     */
    private static final String MSG_FILE_COMMENT =
            String.format("Generated by %s: do NOT edit",
                    CommunityDictEnum.SAVAPAGE.getWord());

    /**
     * Message to be delivered to User Web App.
     */
    public enum Msg {

        /**
         * User Account changed.
         */
        ACCOUNT_INFO,

        /**
         * .
         */
        JOBTICKET_CHANGED,

        /**
         * .
         */
        JOBTICKET_DENIED,

        /**
         * A Copy Job Ticket is settled.
         */
        JOBTICKET_SETTLED_COPY,

        /**
         * A Print Job Ticket is settled.
         */
        JOBTICKET_SETTLED_PRINT,

        /**
         * A print-in was denied.
         */
        PRINT_IN_DENIED,

        /**
         * A print-in expired.
         */
        PRINT_IN_EXPIRED,

        /**
         * A document was successfully proxy printed.
         */
        PRINT_OUT_COMPLETED,

        /**
         * External print manager successfully printed a document.
         */
        PRINT_OUT_EXT_COMPLETED,

        /**
         * External print manager failed to print a document.
         */
        PRINT_OUT_EXT_FAILED,

        /**
         * Proxy Print Authentication required.
         */
        PRINT_OUT_AUTH_REQ,

        /**
         * Proxy Print job is held.
         */
        PRINT_OUT_HOLD,

        /**
         * Request to stop the long poll.
         */
        STOP_POLL_REQ
    };

    /**
     * The base file name of the property file holding the date of the last user
     * message.
     */
    public static final String FILE_BASENAME = "msg.properties";

    /**
     * The key in the property file holding the date of the last user message.
     */
    private static final String PROP_DATE = "date";

    /**
     * The key in the property file holding the {@link Msg} of the last user
     * message.
     */
    private static final String PROP_MSG = "msg";

    /**
     * The key in the property file holding the senderId of the last user
     * message.
     */
    private static final String PROP_SENDER_ID = "sender-id";

    /**
     *
     */
    private String userId = null;

    /**
     * Date of the most recent user message.
     */
    private Date messageDate = null;

    /**
     * Most recent user message.
     */
    private Msg message = null;

    /**
     *
     */
    private String senderId = null;

    /**
     *
     */
    private UserMsgIndicator() {
    }

    /**
     * Reads the message indicator property file for a user.
     *
     * @param userId
     *            The user id.
     * @throws IOException
     *             When IO errors.
     */
    private UserMsgIndicator(final String userId) throws IOException {

        this.userId = userId;

        FileInputStream fis = null;

        try {

            fis = new FileInputStream(indicatorFile(userId));

            final Properties props = new Properties();
            props.load(fis);

            /*
             * Date.
             */
            final String date = props.getProperty(PROP_DATE);

            if (date != null) {
                this.messageDate = new Date(Long.parseLong(date));
            }

            /*
             * Sender ID.
             */
            this.setSenderId(props.getProperty(PROP_SENDER_ID));

            /*
             * Message.
             */
            final String msg = props.getProperty(PROP_MSG);

            if (msg == null) {
                this.message = null;
            } else {
                this.message = Msg.valueOf(msg);
            }

        } catch (FileNotFoundException e) {
            /*
             * File might not exist.
             */
            this.message = null;
            this.messageDate = null;
            this.setSenderId(null);

        } finally {

            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    public boolean hasMessage() {
        return (this.messageDate != null);
    }

    public String getUserId() {
        return userId;
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public Msg getMessage() {
        return message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * Checks whether a file is the indicator file for a user.
     *
     * @param user
     *            The user.
     * @param file
     *            The file to check.
     * @return {@code true} if the offered file is the indicator file
     */
    public static boolean isMsgIndicatorFile(final String user,
            final File file) {
        return file.equals(indicatorFile(user));
    }

    /**
     * Gets the indicator file store for a user.
     *
     * @param userId
     *            The user.
     * @return The full path of the file store.
     */
    private static File indicatorFile(final String userId) {
        return new File(
                String.format("%s%c%s", ConfigManager.getUserHomeDir(userId),
                        File.separatorChar, FILE_BASENAME));
    }

    /**
     * Gets the indicator file store for a user in temporary user directory.
     *
     * @param userId
     *            The user.
     * @return The full path of the file store.
     */
    private static File indicatorFileTemp(final String userId) {
        return new File(String.format("%s%c%s_%s.%s",
                ConfigManager.getAppTmpDir(), File.separatorChar, userId,
                FILE_BASENAME, UUID.randomUUID().toString()));
    }

    /**
     * Checks if the user's SafePages directory is present, so the message file
     * can be written.
     * <p>
     * Since the SafePages directory is lazy created upon first use, it is NOT
     * present when the user has never logged in.
     * </p>
     *
     * @param userId
     *            The userid.
     * @return {@code true} when SafePages directory is present.
     */
    public static boolean isSafePagesDirPresent(final String userId) {
        return new File(ConfigManager.getUserHomeDir(userId)).isDirectory();
    }

    /**
     * Reads the message indicator for a user.
     *
     * @param userId
     *            The user id.
     * @return The {@link UserMsgIndicator}.
     * @throws IOException
     *             When error reading the file.
     */
    public static UserMsgIndicator read(final String userId)
            throws IOException {
        return new UserMsgIndicator(userId);
    }

    /**
     * Notifies a {@link Msg#ACCOUNT_INFO} event.
     *
     * @param userId
     *            The unique user id.
     */
    public static void notifyAccountInfoEvent(final String userId) {
        notifyEvent(UserMsgIndicator.Msg.ACCOUNT_INFO, userId);
    }

    /**
     * Notifies a message.
     *
     * @param msg
     *            Message.
     * @param userId
     *            The unique user id.
     */
    private static void notifyEvent(final UserMsgIndicator.Msg msg,
            final String userId) {

        if (UserMsgIndicator.isSafePagesDirPresent(userId)) {
            try {
                UserMsgIndicator.writeMsg(userId, System.currentTimeMillis(),
                        msg, null);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Saves (writes) the date-time and type of the last user message.
     *
     * @param userId
     *            The user_name.
     * @param date
     *            The date-time of the user message.
     * @param msg
     *            The {@link UserMsgIndicator.Msg} type.
     * @param senderId
     *            Any ID the sender wants to add (can be {@code null}).
     * @throws IOException
     *             When message could not be written, e.g. because the User
     *             SafePages directory does not exist (yet).
     */
    public static void write(final String userId, final Date date,
            final Msg msg, final String senderId) throws IOException {

        writeMsg(userId, date.getTime(), msg, senderId);
    }

    /**
     * Saves (writes) the date-time and type of the last user message.
     *
     * @param userId
     *            The user_name.
     * @param time
     *            The date-time of the user message.
     * @param msg
     *            The {@link UserMsgIndicator.Msg} type.
     * @param senderId
     *            Any ID the sender wants to add (can be {@code null}).
     * @throws IOException
     *             When message could not be written, e.g. because the User
     *             SafePages directory does not exist (yet).
     */
    private static void writeMsg(final String userId, final long time,
            final Msg msg, final String senderId) throws IOException {

        FileOutputStream fos = null;
        File fileTemp = null;

        try {
            /*
             * Check target location: Mantis #864.
             */
            final File fileTarget = indicatorFile(userId);
            final File targetLocation = fileTarget.getParentFile();

            if (targetLocation == null) {
                LOGGER.error(String.format("[%s] [%s] has no parent.",
                        msg.toString(), fileTarget.getAbsolutePath()));
                return;
            }

            if (!targetLocation.isDirectory()) {
                LOGGER.warn(String.format(
                        "[%s] Parent of [%s] does not exist "
                                + "or is not a directory.",
                        msg.toString(), fileTarget.getAbsolutePath()));
                return;
            }

            /*
             * Write temp file.
             */
            final Properties props = new Properties();

            props.put(PROP_DATE, String.valueOf(time));
            props.put(PROP_MSG, msg.toString());

            if (senderId != null) {
                props.put(PROP_SENDER_ID, senderId);
            }

            fileTemp = indicatorFileTemp(userId);
            fos = new FileOutputStream(fileTemp);

            props.store(fos, MSG_FILE_COMMENT);

            /*
             * A flush forces any buffered output bytes to be written out, but
             * does not guarantee that they are actually written to a physical
             * device such as a disk drive.
             */
            fos.flush();

            /*
             * A sync() forces all system buffers to synchronize with the
             * underlying device. This method returns after all modified data
             * and attributes of this FileDescriptor have been written to the
             * relevant device(s). In particular, if this FileDescriptor refers
             * to a physical storage medium, such as a file in a file system,
             * sync will not return until all in-memory modified copies of
             * buffers associated with this FileDescriptor have been written to
             * the physical medium.
             */
            fos.getFD().sync();

            // close after sync
            fos.close();

            /*
             * Before moving, check for existence. See comment at the catch()
             * block.
             */
            if (fileTemp.exists()) {

                java.nio.file.Files.move(fileTemp.toPath(), fileTarget.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                LOGGER.warn(
                        String.format("[%s] %s is written, but does not exist",
                                msg.toString(), fileTemp.getCanonicalPath()));
            }
            //
            fos = null;

        } catch (NoSuchFileException e) {
            /*
             * For unknown reasons this exception sometimes occurs in certain
             * setups. Do files reside on specially mounted disk?. This needs to
             * be investigated. For now, we log the exception as a warning.
             *
             * Exception sometimes occurs on XFS File System on VMWare. Could it
             * be that this combi causes problems?
             *
             * Could it be that the temp file is not seen at this point in time?
             * As some admins report orphaned temp files, are these temp files
             * only visible after the atomic move().
             */
            LOGGER.warn(String.format("[%s] %s: %s", msg.toString(),
                    e.getClass().getSimpleName(), e.getMessage()));

        } finally {

            IOHelper.closeQuietly(fos);

            // Mantis #864: extra safety, clean up the temp file.
            if (fileTemp != null) {
                fileTemp.delete();
            }
        }
    }

    /**
     * Returns user print messages from external print manager.
     *
     * @param printOut
     *            The originating {@link PrintOut}, or {@code null} if unknown.
     * @param locale
     *            The message locale.
     * @param messageDate
     *            The time of the message.
     * @param isCompleted
     *            {@code true} if print is completed.
     * @return The messages.
     */
    public static JsonUserMsgNotification getPrintOutExtMsgNotification(
            final PrintOut printOut, final Locale locale,
            final Date messageDate, final boolean isCompleted) {

        final JsonUserMsgNotification json = new JsonUserMsgNotification();
        json.setMsgTime(messageDate.getTime());

        final JsonUserMsg msg = new JsonUserMsg();
        final String jobId;
        final String printerName;

        if (printOut == null) {
            jobId = String.format("(%s)", AdjectiveEnum.EXTERNAL.uiText(locale))
                    .toLowerCase();
            printerName = NounEnum.PRINTER.uiText(locale).toLowerCase();
        } else {
            jobId = String.format("#%s", printOut.getCupsJobId().toString());
            printerName = printOut.getPrinter().getDisplayName();
        }

        final String key;
        if (isCompleted) {
            msg.setLevel(JsonUserMsg.LEVEL_INFO);
            key = "msg-print-out-completed";
        } else {
            msg.setLevel(JsonUserMsg.LEVEL_WARN);
            key = "msg-print-out-canceled";
        }
        msg.setText(
                localizeMsg(msg.getClass(), locale, key, jobId, printerName));

        json.addUserMsg(msg);

        return json;
    }

    /**
     * Returns {@link PrintIn} or {@link PrintOut} user messages AFTER prevTime
     * and TILL (including) lastTime.
     *
     * @param em
     *            The JPA entity manager
     * @param userIdDocLog
     *            The user of the {@link DocLog}.
     * @param locale
     *            The user's locale like 'en', 'nl', 'en-EN', 'nl-NL'.
     * @param prevDate
     *            The previous time a notification was requested.
     * @param lastDate
     *            The time of the last message. NOTE that when lastTime EQ
     *            prevTime, the message(s) ON last time is (are) returned.
     * @return The messages.
     */
    public static JsonUserMsgNotification getPrintMsgNotification(
            final EntityManager em, final String userIdDocLog,
            final Locale locale, final Date prevDate, final Date lastDate) {

        final boolean singleTime = prevDate.equals(lastDate);

        JsonUserMsgNotification data = new JsonUserMsgNotification();
        data.setMsgTime(lastDate.getTime());

        final StringBuilder jpql = new StringBuilder();

        jpql.append("SELECT D FROM DocLog D" + " JOIN D.user U"
                + " LEFT JOIN D.docIn DI" + " LEFT JOIN D.docOut DO"
                + " LEFT JOIN DI.printIn PI" + " LEFT JOIN DO.printOut PO"
                + " WHERE U.userId = :userid");

        if (singleTime) {

            jpql.append(" AND ("
                    + " (D.createdDate = :lastDate AND PI.printed = false)"
                    + " OR (PO.cupsCompletedTime = :lastSeconds))");
        } else {
            jpql.append(" AND ( (D.createdDate > :prevDate"
                    + " AND D.createdDate <= :lastDate AND PI.printed = false)"
                    + " OR (PO.cupsCompletedTime > :prevSeconds"
                    + " AND PO.cupsCompletedTime <= :lastSeconds))");
        }

        final Integer lastSeconds =
                (int) (lastDate.getTime() / DateUtil.DURATION_MSEC_SECOND);
        final Integer prevSeconds =
                (int) (prevDate.getTime() / DateUtil.DURATION_MSEC_SECOND);

        Query query = em.createQuery(jpql.toString());

        query.setParameter("userid", userIdDocLog);
        query.setParameter("lastDate", lastDate);
        query.setParameter("lastSeconds", lastSeconds);

        if (!singleTime) {
            query.setParameter("prevDate", prevDate);
            query.setParameter("prevSeconds", prevSeconds);
        }

        @SuppressWarnings("unchecked")
        final List<DocLog> list = query.getResultList();

        for (final DocLog docLog : list) {

            JsonUserMsg msg = null;

            final DocOut docOut = docLog.getDocOut();

            if (docOut == null) {

                final DocIn docIn = docLog.getDocIn();

                if (docIn != null && docIn.getPrintIn() != null) {
                    msg = createPrintInMsg(docIn.getPrintIn(), locale);
                }

            } else if (docOut.getPrintOut() != null) {
                msg = createPrintOutMsg(docOut.getPrintOut(), locale);
            }

            if (msg != null) {
                data.addUserMsg(msg);
            }
        }

        return data;
    }

    /**
     *
     * @param locale
     * @param key
     * @param args
     * @return
     */
    private static String localizeMsg(Class<? extends Object> klasse,
            final Locale locale, String key, final String... args) {
        return Messages.getMessage(klasse, locale, key, args);
    }

    /**
     *
     * @param printIn
     * @return
     */
    private static JsonUserMsg createPrintInMsg(final PrintIn printIn,
            final Locale locale) {

        JsonUserMsg msg = new JsonUserMsg();

        msg.setLevel(JsonUserMsg.LEVEL_WARN);

        msg.setText(localizeMsg(msg.getClass(), locale, "msg-print-in-denied",
                IppQueueHelper.uiPath(printIn.getQueue()),
                printIn.getDeniedReason()));

        return msg;
    }

    /**
     * @param printOut
     * @return
     */
    private static boolean isExtPaperCutPrint(final PrintOut printOut) {
        final PaperCutService svc =
                ServiceContext.getServiceFactory().getPaperCutService();
        return svc.isExtPaperCutPrint(printOut.getPrinter().getPrinterName());
    }

    /**
     *
     * @param printOut
     * @param locale
     * @return
     */
    private static JsonUserMsg createPrintOutMsg(final PrintOut printOut,
            final Locale locale) {

        final PrintOutDao printOutDao =
                ServiceContext.getDaoContext().getPrintOutDao();

        int level = JsonUserMsg.LEVEL_WARN;
        String key = null;

        switch (printOutDao.getIppJobState(printOut)) {
        case IPP_JOB_STOPPED:
            key = "msg-print-out-stopped";
            break;
        case IPP_JOB_CANCELED:
            key = "msg-print-out-canceled";
            break;
        case IPP_JOB_ABORTED:
            key = "msg-print-out-aborted";
            break;
        case IPP_JOB_COMPLETED:
            if (!isExtPaperCutPrint(printOut)) {
                level = JsonUserMsg.LEVEL_INFO;
                key = "msg-print-out-completed";
            }
            break;
        default:
            key = null;
            break;
        }

        JsonUserMsg msg = null;

        if (key != null) {

            msg = new JsonUserMsg();
            msg.setLevel(level);

            msg.setText(localizeMsg(msg.getClass(), locale, key,
                    "#" + String.valueOf(printOut.getCupsJobId()),
                    printOut.getPrinter().getDisplayName()));
        }
        return msg;
    }

    /**
     *
     * @param dto
     * @param locale
     * @return
     */
    public JsonUserMsg createCopyJobTicketMsg(final OutboxJobDto dto,
            final Locale locale) {

        final JsonUserMsg msg = new JsonUserMsg();

        msg.setLevel(JsonUserMsg.LEVEL_INFO);
        msg.setText(String.format("Copy Job Ticket %s is completed",
                dto.getTicketNumber()));

        return msg;
    }

}
