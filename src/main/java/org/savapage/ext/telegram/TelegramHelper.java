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
package org.savapage.ext.telegram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TelegramHelper {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TelegramHelper.class);

    /** */
    private static final ConfigManager CONFIG_MANAGER =
            ConfigManager.instance();

    /**
     * Telegram BOT API. {0} = BOT token, {1} = User Telegram ID, {2} =
     * Generated TOTP Code.
     */
    private static final String SEND_MESSAGE_URL_PATTERN =
            "https://api.telegram.org/bot{0}/sendMessage"
                    + "?chat_id={1}&disable_web_page_preview=1&text={2}";

    /** */
    public static final String MY_ID_BOT_USERNAME = "my_id_bot";

    /** */
    private static final int TELEGRAM_API_CONN_TIMEOUT_MSEC = 3000;
    /** */
    private static final int TELEGRAM_API_READ_TIMEOUT_MSEC = 2000;

    /**
     * Main part of the Telegram JSON response, just enough to evaluate success.
     */
    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelegramResponse {

        private boolean ok;
        private Integer error_code;
        private String description;

        public boolean isOk() {
            return ok;
        }

        public void setOk(boolean ok) {
            this.ok = ok;
        }

        public Integer getError_code() {
            return error_code;
        }

        public void setError_code(Integer error_code) {
            this.error_code = error_code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

    }

    /** Utility class. */
    private TelegramHelper() {
    }

    /**
     * @return {@code true} if Telegram messaging is enabled.
     */
    public static boolean isMessagingEnabled() {
        return CONFIG_MANAGER.isConfigValue(Key.EXT_TELEGRAM_ENABLE)
                && StringUtils.isNotBlank(CONFIG_MANAGER
                        .getConfigValue(Key.EXT_TELEGRAM_BOT_TOKEN))
                && StringUtils.isNotBlank(CONFIG_MANAGER
                        .getConfigValue(Key.EXT_TELEGRAM_BOT_USERNAME));
    }

    /**
     * @return {@code true} if User TOTP messaging for Telegram is enabled.
     */
    public static boolean isTOTPEnabled() {
        return CONFIG_MANAGER.isConfigValue(Key.USER_EXT_TELEGRAM_TOTP_ENABLE)
                && isMessagingEnabled();
    }

    /**
     * @return formatted Telegram username (prefixed with "@").
     */
    public static String userNameFormatted(final String username) {
        return "@".concat(username);
    }

    /**
     *
     * @param telegramID
     *            TelegramID to send message to.
     * @param msg
     *            Message.
     * @return {@code false} if logical error.
     * @throws IOException
     *             If technical error.
     */
    public static boolean sendMessage(final String telegramID, final String msg)
            throws IOException {

        final URL url;
        try {
            url = new URL(MessageFormat.format(SEND_MESSAGE_URL_PATTERN,
                    CONFIG_MANAGER.getConfigValue(Key.EXT_TELEGRAM_BOT_TOKEN),
                    telegramID,
                    URLEncoder.encode(msg, "UTF-8").replace("+", "%20")));
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                    "Application error: Telegram URL error.");
        }

        final URLConnection connection = url.openConnection();

        connection.setConnectTimeout(TELEGRAM_API_CONN_TIMEOUT_MSEC);
        connection.setReadTimeout(TELEGRAM_API_READ_TIMEOUT_MSEC);

        final StringBuilder json = new StringBuilder();

        try (InputStream istr = connection.getInputStream();
                InputStreamReader istrReader = new InputStreamReader(istr);
                BufferedReader br = new BufferedReader(istrReader);) {

            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
                json.append(System.lineSeparator());
            }

        }
        final TelegramResponse rsp = JsonHelper
                .createOrNull(TelegramResponse.class, json.toString());

        if (rsp == null) {
            LOGGER.error("{} : JSON to Object error.", json.toString());
            return false;
        }

        if (!rsp.isOk()) {
            LOGGER.warn(json.toString());
        }

        return rsp.isOk();
    }

}
