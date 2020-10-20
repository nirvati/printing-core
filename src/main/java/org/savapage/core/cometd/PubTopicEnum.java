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
     * Document Store.
     */
    DOC_STORE("doc-store"),

    /**
     * Atom Feed.
     */
    FEED("feed"),

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
     * Plug-in.
     */
    PLUGIN("plugin"),

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
     * SMTP.
     */
    SMTP("smtp"),

    /**
     * SNMP.
     */
    SNMP("snmp"),

    /**
     * SOffice.
     */
    SOFFICE("soffice"),

    /**
     * Runtime system.
     */
    SYSTEM("system"),

    /**
     * User activity.
     */
    USER("user"),

    /**
     * User synchronization.
     */
    USER_SYNC("user-sync"),

    /**
     * Web Service API.
     */
    WEB_SERVICE("webservice");

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
