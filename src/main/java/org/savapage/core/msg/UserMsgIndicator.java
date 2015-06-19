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
package org.savapage.core.msg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.Messages;
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
 * @author Datraverse B.V.
 *
 */
public final class UserMsgIndicator {

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(UserMsgIndicator.class);

    /**
     * Message to be delivered to User Web App.
     */
    public enum Msg {

        /**
         * User Account changed.
         */
        ACCOUNT_INFO,

        /**
         * A printed in was denied.
         */
        PRINT_IN_DENIED,
        /**
         * A document was successfully proxy printed.
         */
        PRINT_OUT_COMPLETED,

        /**
         * Proxy Print Authentication required.
         */
        PRINT_OUT_AUTH_REQ,

        /**
         * Request to stop the long poll
         */
        STOP_POLL_REQ
    };

    /**
     * The base file name of the property file holding the date of the last user
     * message.
     */
    final static String FILE_BASENAME = "msg.properties";

    /**
     * The key in the property file holding the date of the last user message.
     */
    final static String PROP_DATE = "date";

    /**
     * The key in the property file holding the {@link Msg} of the last user
     * message.
     */
    final static String PROP_MSG = "msg";

    /**
     * The key in the property file holding the senderId of the last user
     * message.
     */
    final static String PROP_SENDER_ID = "sender-id";

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
     * @throws IOException
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
    public static boolean
            isMsgIndicatorFile(final String user, final File file) {
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
        return new File(String.format("%s%s%s",
                ConfigManager.getUserHomeDir(userId), "/", FILE_BASENAME));
    }

    /**
     * Gets the indicator file store for a user in temporary user directory.
     *
     * @param userId
     *            The user.
     * @return The full path of the file store.
     */
    private static File indicatorFileTemp(final String userId) {
        return new File(String.format("%s%s%s.%s", ConfigManager
                .getUserTempDir(userId), "/", FILE_BASENAME, UUID.randomUUID()
                .toString()));
    }

    /**
     * Checks if the user's SafaPages directory is present, so the message file
     * can be written.
     * <p>
     * Since the SafaPages directory is lazy created upon first use, it is NOT
     * present when the user has never logged in.
     * </p>
     *
     * @param userId
     *            The userid.
     * @return {@code true} when SafaPages directory is present.
     */
    public static boolean isSafePagesDirPresent(final String userId) {
        return new File(ConfigManager.getUserHomeDir(userId)).isDirectory();
    }

    /**
     * Reads the message indicator for a user.
     *
     * @param userId
     * @return The {@link UserMsgIndicator}.
     */
    public static UserMsgIndicator read(final String userId) throws IOException {
        return new UserMsgIndicator(userId);
    }

    /**
     * Notifies a {@link Msg#ACCOUNT_INFO} event.
     *
     * @param userId
     *            The unique user id.
     */
    public static void notifyAccountInfoEvent(final String userId) {

        if (UserMsgIndicator.isSafePagesDirPresent(userId)) {
            try {
                UserMsgIndicator.writeMsg(userId, System.currentTimeMillis(),
                        UserMsgIndicator.Msg.ACCOUNT_INFO, null);
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
     *             safapages directory does not exist (yet).
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
     *             safapages directory does not exist (yet).
     */
    private static void writeMsg(final String userId, final long time,
            final Msg msg, final String senderId) throws IOException {

        FileOutputStream fos = null;

        try {

            Properties props = new Properties();

            props.put(PROP_DATE, String.valueOf(time));
            props.put(PROP_MSG, msg.toString());

            if (senderId != null) {
                props.put(PROP_SENDER_ID, senderId);
            }

            final File fileTarget = indicatorFile(userId);

            File fileTemp = null;

            fileTemp = indicatorFileTemp(userId);
            fos = new FileOutputStream(fileTemp);

            props.store(fos, "Generated by SavaPage: do NOT edit");

            fos.close();
            fos = null;

            java.nio.file.Files.move(
                    FileSystems.getDefault().getPath(
                            fileTemp.getCanonicalPath()), FileSystems
                            .getDefault()
                            .getPath(fileTarget.getCanonicalPath()),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } finally {

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Returns {@link PrintIn} or {@link PrintOut} user messages AFTER prevTime
     * and TILL (including) lastTime.
     *
     * @param em
     *            The JPA entity manager
     * @param userId
     *            The user.
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
            final EntityManager em, final String userId, final Locale locale,
            final Date prevDate, final Date lastDate) {

        final boolean singleTime = prevDate.equals(lastDate);

        JsonUserMsgNotification data = new JsonUserMsgNotification();
        data.setMsgTime(lastDate.getTime());

        String jpql =
                "SELECT D FROM DocLog D" + " JOIN D.user U"
                        + " LEFT JOIN D.docIn DI" + " LEFT JOIN D.docOut DO"
                        + " LEFT JOIN DI.printIn PI"
                        + " LEFT JOIN DO.printOut PO"
                        + " WHERE U.userId = :userid";

        if (singleTime) {
            jpql +=
                    " AND ( (D.createdDate = :lastDate AND PI.printed = false)"
                            + " OR (PO.cupsCompletedTime = :lastSeconds))";
        } else {
            jpql +=
                    " AND ( (D.createdDate > :prevDate"
                            + " AND D.createdDate <= :lastDate AND PI.printed = false)"
                            + " OR (PO.cupsCompletedTime > :prevSeconds"
                            + " AND PO.cupsCompletedTime <= :lastSeconds))";
        }

        Integer lastSeconds = (int) (lastDate.getTime() / 1000L);
        Integer prevSeconds = (int) (prevDate.getTime() / 1000L);

        Query query = em.createQuery(jpql);

        query.setParameter("userid", userId);
        query.setParameter("lastDate", lastDate);
        query.setParameter("lastSeconds", lastSeconds);

        if (!singleTime) {
            query.setParameter("prevDate", prevDate);
            query.setParameter("prevSeconds", prevSeconds);
        }

        @SuppressWarnings("unchecked")
        List<DocLog> list = query.getResultList();

        for (DocLog docLog : list) {

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

        msg.setLevel(1); // TODO

        msg.setText(localizeMsg(msg.getClass(), locale, "msg-print-in-denied",
                "/" + printIn.getQueue().getUrlPath(),
                printIn.getDeniedReason()));

        return msg;
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

        int level = 1;
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
            level = 0;
            key = "msg-print-out-completed";
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
                    "#" + String.valueOf(printOut.getCupsJobId()), printOut
                            .getPrinter().getDisplayName()));
        }
        return msg;
    }

}
