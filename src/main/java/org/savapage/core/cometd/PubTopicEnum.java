/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.cometd;

import java.net.URLConnection;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum PubTopicEnum {

    /**
     * System configuration.
     */
    CONFIG("config"),

    /**
     *
     */
    CUPS("cups"),

    /**
     * Database.
     */
    DB("db"),

    /**
     * Google Cloud Print, waiting for print jobs.
     */
    GCP_PRINT("gcp-print"),

    /**
     * Google Cloud Print SavaPage printer registration.
     */
    GCP_REGISTER("gcp-register"),

    /**
     * {@link URLConnection} to the internet.
     */
    INTERNET_URL_CONNECTION("url-connection"),

    /**
     * IPP.
     */
    IPP("ipp"),

    /**
     * SavaPage Community Membership.
     */
    MEMBERSHIP("membership"),

    /**
     *
     */
    MAILPRINT("mailprint"),

    /**
    *
    */
    WEBPRINT("webprint"),

    /**
     * NFC.
     */
    NFC("nfc"),

    /**
     * PaperCut.
     */
    PAPERCUT("papercut"),

    /**
     * PaperCut.
     */
    PAYMENT_GATEWAY("payment-gateway"),

    /**
     * Proxy Print.
     */
    PROXY_PRINT("proxyprint"),

    /**
     * Quartz.
     */
    SCHEDULER("scheduler"),

    /**
     *
     */
    SERVER_COMMAND("server-command"),

    /**
     * SmartSchool.
     */
    SMARTSCHOOL("smartschool"),

    /**
     * SMTP.
     */
    SMTP("smtp"),

    /**
     * SOffice.
     */
    SOFFICE("soffice"),

    /**
     * User activity.
     */
    USER("user"),

    /**
     * User synchronization.
     */
    USER_SYNC("user-sync");

    /**
     *
     */
    private String channelTopic;

    /**
     * Sets the Channel topic string representation of the enum.
     *
     * @param channelTopic
     *            The string representation of the enum.
     */
    private PubTopicEnum(final String channelTopic) {
        this.channelTopic = channelTopic;
    }

    /**
     * Gets the Channel topic string representation of the enum.
     *
     * @return The representation of the enum.
     */
    public String getChannelTopic() {
        return channelTopic;
    }

}
