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
package org.savapage.core.print.imap;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;

/**
 * Actor that keeps the connection to the IMAP server alive.
 *
 * <p>
 * A IMAP NOOP command is performed on the (inbox) folder to keep the connection
 * alive. This command makes any {@link IMAPFolder#idle(boolean)} method on the
 * (Inbox) folder return.
 * </p>
 * <p>
 * After a <a href=
 * "http://stackoverflow.com/questions/4155412/javamail-keeping-imapfolder-idle-alive"
 * >suggestion</a> by Juan Martín Sotuyo Dodero.
 * </p>
 *
 * @author Juan Martín Sotuyo Dodero <jmsotuyo@monits.com>
 * @author Datraverse B.V.
 *
 */
public class ImapHeartbeat implements Runnable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ImapHeartbeat.class);

    /**
     * 5 minutes.
     */
    private static final long DEFAULT_HEARTBEAT_SECS = 5 * 1000 * 60;

    private long heartbeatSecs = DEFAULT_HEARTBEAT_SECS;

    private IMAPFolder folder;

    /**
     *
     * @param folder
     * @param heartbeatSecs
     *            The keep interval in seconds
     */
    public ImapHeartbeat(IMAPFolder folder, long heartbeatSecs) {

        this.folder = folder;
        this.heartbeatSecs = heartbeatSecs;

    }

    @Override
    public void run() {

        LOGGER.trace("Started.");

        /*
         * Do NOT use the static Thread.interrupted() method in the while loop,
         * since it clears the interruption "flag". In other words, a second
         * call to interrupted() will return false. So it is safer to use the
         * isInterupted() instance method.
         *
         * WARNING: a thread interruption is ignored when the thread is NOT
         * alive at the time of the interrupt. In this case isInterupted() will
         * return false. That is why we set an extra switch when catching the
         * InterruptedException.
         */
        boolean interruptedException = false;

        boolean noopFailureAlarm = false;

        while (!Thread.currentThread().isInterrupted() && !interruptedException) {

            try {

                Thread.sleep(this.heartbeatSecs * 1000);

                LOGGER.trace("Send ...");

                /*
                 * Perform an IMAP NOOP Command to keep the connection alive.
                 *
                 * NOTE: the NOOP command makes the IMAPFolder.idle() method
                 * return immediately.
                 */
                folder.doCommand(new IMAPFolder.ProtocolCommand() {

                    /**
                     * Note: p can be null when an exception occurred in
                     * {@link ImapListener}.
                     */
                    @Override
                    public Object doCommand(IMAPProtocol p)
                            throws ProtocolException {

                        if (p != null) {
                            p.simpleCommand("NOOP", null);
                        }

                        return null;
                    }

                });

                if (noopFailureAlarm) {
                    /*
                     * Clear the alarm.
                     */
                    noopFailureAlarm = false;
                    LOGGER.warn("IMAP connection restored!");
                }

            } catch (InterruptedException e) {

                LOGGER.trace("Interrupted.");
                interruptedException = true;

            } catch (MessagingException e) {

                /*
                 * What to do when the NOOP command failed?
                 */
                if (!noopFailureAlarm) {
                    /*
                     * Set the alarm.
                     */
                    noopFailureAlarm = true;
                    LOGGER.warn("IMAP connection lost ...");
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(e.getMessage());
                }

            }
        }

        /*
         * Clear any reference to folder see Mantis #301.
         */
        this.folder = null;

        LOGGER.trace("Shutdown.");
    }
}
