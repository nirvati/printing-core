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
package org.savapage.core.services.helpers;

import java.util.List;

import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.DeviceAttrEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renderer for User Authentication parameters of a Terminal {@link Device}.
 * <ul>
 * <li>The visible* members indicate if a Authentication Mode is visible.</li>
 * <li>The allow* members indicate if a Authentication Mode is allowed.</li>
 * </ul>
 * <p>
 * NOTE: An Authentication Mode may be <i>allowed</i> but <u>not</u> be
 * <i>visible</i>.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAuth {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserAuth.class);

    /** */
    private boolean visibleAuthName;
    /** */
    private boolean visibleAuthEmail;
    /** */
    private boolean visibleAuthId;
    /** */
    private boolean visibleAuthCardLocal;
    /** */
    private boolean visibleAuthCardIp;
    /** */
    private boolean visibleAuthYubikey;

    /** */
    private UserAuthModeEnum authModeDefault;

    /** */
    private boolean authIdPinReq;
    /** */
    private boolean authIdMasked;
    /** */
    private boolean authCardPinReq;

    /** */
    private boolean authCardSelfAssoc;
    /** */
    private Integer maxIdleSeconds;

    /** */
    private boolean allowAuthName;
    /** */
    private boolean allowAuthEmail;
    /** */
    private boolean allowAuthId;
    /** */
    private boolean allowAuthCardLocal;
    /** */
    private boolean allowAuthCardIp;
    /** */
    private boolean allowAuthYubikey;

    /**
     *
     */
    protected UserAuth() {
    }

    /**
     * Gets the UI text of an {@link Mode}.
     *
     * @param authMode
     *            The {@link UserAuthModeEnum} (can be {@code null}).
     * @return The UI text.
     */
    public static String getUiText(final UserAuthModeEnum authMode) {
        if (authMode == null) {
            // #21B7: CLOCKWISE TOP SEMICIRCLE ARROW
            return "↷";
        } else {
            switch (authMode) {
            case CARD_IP:
            case CARD_LOCAL:
                return "NFC";
            case OAUTH:
                return "OAuth";
            case YUBIKEY:
                return "YubiKey";
            case ID:
                return "ID";
            case EMAIL:
                return "Email";
            case NAME:
                return "~";
            default:
                throw new SpException(String.format("AuthMode %s not handled.",
                        authMode.toString()));
            }
        }
    }

    /**
     * Renders the User Authentication Mode parameters for a terminal.
     *
     * @param terminal
     *            The terminal where authentication is taken place.
     * @param authModeRequest
     *            The requested authentication mode (used to determine the
     *            default {@link Mode}). Can be {@code null}.
     * @param webAppType
     *            The type of Web App.
     * @param internetAccess
     *            If {@code true}, the Web App is requested from the Internet.
     */
    public UserAuth(final Device terminal, final String authModeRequest,
            final WebAppTypeEnum webAppType, final boolean internetAccess) {

        final boolean isAdminWebAppContext =
                webAppType.equals(WebAppTypeEnum.ADMIN);

        final DeviceDao deviceDao =
                ServiceContext.getDaoContext().getDeviceDao();

        if (terminal != null && !deviceDao.isTerminal(terminal)) {
            throw new SpException("Device [" + terminal.getDisplayName()
                    + "] is NOT of type " + DeviceTypeEnum.TERMINAL);
        }

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Defaults for member variables.
         */
        this.visibleAuthName = false;
        this.visibleAuthEmail = false;
        this.visibleAuthId = false;
        this.visibleAuthCardLocal = false;
        this.visibleAuthCardIp = false;
        this.authModeDefault = null;
        this.maxIdleSeconds = null;

        this.authIdPinReq = cm.isConfigValue(Key.AUTH_MODE_ID_PIN_REQUIRED);
        this.authIdMasked = cm.isConfigValue(Key.AUTH_MODE_ID_IS_MASKED);

        this.authCardPinReq = cm.isConfigValue(Key.AUTH_MODE_CARD_PIN_REQUIRED);
        this.authCardSelfAssoc =
                cm.isConfigValue(Key.AUTH_MODE_CARD_SELF_ASSOCIATION);

        this.allowAuthName = isAdminWebAppContext;
        this.allowAuthEmail = false;
        this.allowAuthId = false;
        this.allowAuthCardLocal = false;
        this.allowAuthCardIp = false;

        /*
         * Intermediate parameters.
         */
        boolean showAuthName = false;
        boolean showAuthEmail = false;
        boolean showAuthId = false;
        boolean showAuthCardLocal = false;
        boolean showAuthCardIp = false;
        boolean showAuthYubiKey = false;

        UserAuthModeEnum authModeReq = null;

        if (authModeRequest != null) {
            authModeReq = UserAuthModeEnum.fromDbValue(authModeRequest);
        }

        /*
         * Check Device for CUSTOM authentication.
         */
        boolean isCustomAuth = false;
        DeviceService.DeviceAttrLookup customAuth = null;

        if (terminal != null && !terminal.getDisabled()) {
            /*
             * Device values.
             */
            customAuth = new DeviceService.DeviceAttrLookup(terminal);

            isCustomAuth = customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_IS_CUSTOM,
                    false);
        }

        if (customAuth != null && isCustomAuth) {
            /*
             * Custom Device values.
             */
            this.allowAuthName =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_NAME, false);
            this.allowAuthEmail =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_EMAIL, false);
            this.allowAuthId =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_ID, false);
            this.allowAuthCardIp =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_IP, false);
            this.allowAuthCardLocal = customAuth
                    .isTrue(DeviceAttrEnum.AUTH_MODE_CARD_LOCAL, false);
            this.allowAuthYubikey =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_YUBIKEY, false);

            this.authModeDefault = UserAuthModeEnum.fromDbValue(
                    customAuth.get(DeviceAttrEnum.AUTH_MODE_DEFAULT,
                            UserAuthModeEnum.NAME.toDbValue()));

            showAuthName = this.allowAuthName;
            showAuthEmail = this.allowAuthEmail;
            showAuthId = this.allowAuthId;
            showAuthCardIp = this.allowAuthCardIp;
            showAuthCardLocal = this.allowAuthCardLocal;
            showAuthYubiKey = this.allowAuthYubikey;

            if (!isAdminWebAppContext) {
                this.maxIdleSeconds = Integer.valueOf(customAuth.get(
                        DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS,
                        cm.getConfigValue(Key.WEBAPP_USER_MAX_IDLE_SECS)));
            }

            this.authIdPinReq = customAuth.isTrue(
                    DeviceAttrEnum.AUTH_MODE_ID_PIN_REQ, this.authIdPinReq);

            this.authIdMasked = customAuth.isTrue(
                    DeviceAttrEnum.AUTH_MODE_ID_IS_MASKED, this.authIdMasked);

            this.authCardPinReq = customAuth.isTrue(
                    DeviceAttrEnum.AUTH_MODE_CARD_PIN_REQ, this.authCardPinReq);

            this.authCardSelfAssoc =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_SELF_ASSOC,
                            this.authCardSelfAssoc);

            /*
             * Correct for disabled Network Card Reader.
             */
            if (this.allowAuthCardIp && terminal.getCardReader() != null) {
                this.allowAuthCardIp = !terminal.getCardReader().getDisabled();
            }

        } else {
            /*
             * Use dedicated Web App values when accessed from the Internet.
             */
            Key keyInternetAuthEnable = null;
            Key keyInternetAuthModes = null;

            if (internetAccess) {
                switch (webAppType) {
                case ADMIN:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_ADMIN_AUTH_MODE_ENABLE;
                    keyInternetAuthModes = Key.WEBAPP_INTERNET_ADMIN_AUTH_MODES;
                    break;
                case PRINTSITE:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_PRINTSITE_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_PRINTSITE_AUTH_MODES;
                    break;
                case JOBTICKETS:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_JOBTICKETS_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_JOBTICKETS_AUTH_MODES;
                    break;
                case MAILTICKETS:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_MAILTICKETS_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_MAILTICKETS_AUTH_MODES;
                    break;
                case PAYMENT:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_PAYMENT_AUTH_MODE_ENABLE;
                    keyInternetAuthModes =
                            Key.WEBAPP_INTERNET_PAYMENT_AUTH_MODES;
                    break;
                case POS:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_POS_AUTH_MODE_ENABLE;
                    keyInternetAuthModes = Key.WEBAPP_INTERNET_POS_AUTH_MODES;
                    break;
                case USER:
                    keyInternetAuthEnable =
                            Key.WEBAPP_INTERNET_USER_AUTH_MODE_ENABLE;
                    keyInternetAuthModes = Key.WEBAPP_INTERNET_USER_AUTH_MODES;
                    break;
                default:
                    break;
                }
            }

            final boolean useInternetValues = keyInternetAuthEnable != null
                    && cm.isConfigValue(keyInternetAuthEnable);

            if (useInternetValues) {

                this.allowAuthName = false;

                final List<UserAuthModeEnum> internetAuthModes =
                        UserAuthModeEnum.parseList(
                                cm.getConfigValue(keyInternetAuthModes));

                int iMode = 0;

                for (final UserAuthModeEnum mode : internetAuthModes) {

                    if (mode == UserAuthModeEnum.OAUTH) {
                        if (internetAuthModes.size() == 1) {
                            this.authModeDefault = mode;
                        }
                        continue;
                    }

                    if (iMode == 0) {
                        this.authModeDefault = mode;
                    }
                    iMode++;

                    switch (mode) {
                    case CARD_LOCAL:
                        this.allowAuthCardLocal = true;
                        showAuthCardLocal = true;
                        break;
                    case CARD_IP:
                        this.allowAuthCardIp = true;
                        showAuthCardIp = true;
                        break;
                    case ID:
                        this.allowAuthId = true;
                        showAuthId = true;
                        break;
                    case EMAIL:
                        this.allowAuthEmail = true;
                        showAuthEmail = true;
                        break;
                    case NAME:
                        this.allowAuthName = true;
                        showAuthName = true;
                        break;
                    case YUBIKEY:
                        this.allowAuthYubikey = true;
                        showAuthYubiKey = true;
                        break;
                    default:
                        break;
                    }
                }

                if (!this.allowAuthName) {
                    this.authCardSelfAssoc = false;
                }

            } else {

                /*
                 * Global values.
                 */
                this.allowAuthName = cm.isConfigValue(Key.AUTH_MODE_NAME);
                this.allowAuthEmail = cm.isConfigValue(Key.AUTH_MODE_EMAIL);
                this.allowAuthId = cm.isConfigValue(Key.AUTH_MODE_ID);
                this.allowAuthCardLocal =
                        cm.isConfigValue(Key.AUTH_MODE_CARD_LOCAL);
                this.allowAuthYubikey = cm.isConfigValue(Key.AUTH_MODE_YUBIKEY);
                this.allowAuthCardIp = false;

                showAuthName = cm.isConfigValue(Key.AUTH_MODE_NAME_SHOW);
                showAuthEmail = cm.isConfigValue(Key.AUTH_MODE_EMAIL_SHOW);
                showAuthId = cm.isConfigValue(Key.AUTH_MODE_ID_SHOW);
                showAuthCardLocal =
                        cm.isConfigValue(Key.AUTH_MODE_CARD_LOCAL_SHOW);
                showAuthYubiKey = cm.isConfigValue(Key.AUTH_MODE_YUBIKEY_SHOW);
                showAuthCardIp = false;

                this.authModeDefault = UserAuthModeEnum
                        .fromDbValue(cm.getConfigValue(Key.AUTH_MODE_DEFAULT));

                /*
                 * This can only occur when we refactored the URL API (URL
                 * parameter names). Just in case we fall-back to basic Username
                 * login.
                 */
                if (this.authModeDefault == null) {
                    LOGGER.warn("System AuthModeDefault ["
                            + cm.getConfigValue(Key.AUTH_MODE_DEFAULT)
                            + "] not found: using Username "
                            + "as default login method");
                    this.authModeDefault = UserAuthModeEnum.NAME;
                }

                boolean isValidDefault = false;

                switch (this.authModeDefault) {
                case CARD_LOCAL:
                    isValidDefault =
                            this.allowAuthCardLocal && showAuthCardLocal;
                    break;
                case ID:
                    isValidDefault = this.allowAuthId && showAuthId;
                    break;
                case EMAIL:
                    isValidDefault = this.allowAuthEmail && showAuthEmail;
                    break;
                case NAME:
                    isValidDefault = this.allowAuthName && showAuthName;
                    break;
                case YUBIKEY:
                    isValidDefault = this.allowAuthYubikey && showAuthYubiKey;
                    break;
                default:
                    break;
                }

                if (!isValidDefault) {
                    this.authModeDefault = UserAuthModeEnum.NAME;
                }
            }
            //
            if (!isAdminWebAppContext) {
                this.maxIdleSeconds =
                        cm.getConfigInt(Key.WEBAPP_USER_MAX_IDLE_SECS);
            }
        }

        /*
         * Just in case.
         */
        if (this.authModeDefault == null) {
            this.authModeDefault = UserAuthModeEnum.NAME;
        }

        /*
         * Mode requested?
         */
        if (authModeReq == null) {

            this.visibleAuthName = this.allowAuthName && showAuthName;
            this.visibleAuthEmail = this.allowAuthEmail && showAuthEmail;
            this.visibleAuthId = this.allowAuthId && showAuthId;
            this.visibleAuthCardLocal =
                    this.allowAuthCardLocal && showAuthCardLocal;
            this.visibleAuthCardIp = this.allowAuthCardIp && showAuthCardIp;
            this.visibleAuthYubikey = this.allowAuthYubikey && showAuthYubiKey;

            /*
             * INVARIANT: Admin WebApp SHOULD always be able to login with
             * user/password in intranet context.
             */
            if (isAdminWebAppContext && !internetAccess) {
                this.visibleAuthName = true;
            }

        } else {
            /*
             * Assign requested mode.
             */
            this.visibleAuthName = false;
            this.visibleAuthEmail = false;
            this.visibleAuthId = false;
            this.visibleAuthCardLocal = false;
            this.visibleAuthCardIp = false;
            this.visibleAuthYubikey = false;

            switch (authModeReq) {
            case CARD_IP:
                this.visibleAuthCardIp = this.allowAuthCardIp;
                break;
            case CARD_LOCAL:
                this.visibleAuthCardLocal = this.allowAuthCardLocal;
                break;
            case ID:
                this.visibleAuthId = this.allowAuthId;
                break;
            case EMAIL:
                this.visibleAuthEmail = this.allowAuthEmail;
                break;
            case NAME:
                this.visibleAuthName = this.allowAuthName;
                break;
            case YUBIKEY:
                this.visibleAuthYubikey = this.allowAuthYubikey;
                break;
            default:
                break;
            }
        }

        /*
         * INVARIANT: Admin WebApp does NOT support Network Card Reader
         * Authentication.
         */
        if (isAdminWebAppContext) {
            this.visibleAuthCardIp = false;
            this.allowAuthCardIp = false;
            this.allowAuthName = true;
        }

        /*
         * INVARIANT: The default MUST match a valid authentication method. If
         * not it should be corrected.
         */
        boolean incorrectDefault =
                (this.authModeDefault == UserAuthModeEnum.NAME
                        && !this.visibleAuthName)
                        || (this.authModeDefault == UserAuthModeEnum.EMAIL
                                && !this.visibleAuthEmail)
                        || (this.authModeDefault == UserAuthModeEnum.ID
                                && !this.visibleAuthId)
                        || (this.authModeDefault == UserAuthModeEnum.CARD_LOCAL
                                && !this.visibleAuthCardLocal)
                        || (this.authModeDefault == UserAuthModeEnum.CARD_IP
                                && !this.visibleAuthCardIp)
                        || (this.authModeDefault == UserAuthModeEnum.YUBIKEY
                                && !this.visibleAuthYubikey);

        if (incorrectDefault) {
            /*
             * Assign the more advanced methods first.
             */
            if (this.visibleAuthYubikey) {
                this.authModeDefault = UserAuthModeEnum.YUBIKEY;
            } else if (this.visibleAuthCardIp) {
                this.authModeDefault = UserAuthModeEnum.CARD_IP;
            } else if (this.visibleAuthCardLocal) {
                this.authModeDefault = UserAuthModeEnum.CARD_LOCAL;
            } else if (this.visibleAuthId) {
                this.authModeDefault = UserAuthModeEnum.ID;
            } else if (this.visibleAuthEmail) {
                this.authModeDefault = UserAuthModeEnum.EMAIL;
            } else if (this.visibleAuthName) {
                this.authModeDefault = UserAuthModeEnum.NAME;
            }
        }
    }

    /**
     * Check if an authentication mode is allowed.
     * <p>
     * NOTE: when Card Self Association is enabled, {@link Mode#ID} is ALWAYS
     * allowed.
     * </p>
     *
     * @param mode
     *            The authentication mode.
     * @return {@code true} when allowed.
     */
    public boolean isAuthModeAllowed(final UserAuthModeEnum mode) {

        if (mode == UserAuthModeEnum.NAME && authCardSelfAssoc) {
            return true;
        }

        return (allowAuthName && mode == UserAuthModeEnum.NAME)
                || (allowAuthEmail && mode == UserAuthModeEnum.EMAIL)
                || (allowAuthId && mode == UserAuthModeEnum.ID)
                || (allowAuthCardLocal && mode == UserAuthModeEnum.CARD_LOCAL)
                || (allowAuthCardIp && mode == UserAuthModeEnum.CARD_IP)
                || (allowAuthYubikey && mode == UserAuthModeEnum.YUBIKEY);
    }

    public boolean isVisibleAuthName() {
        return visibleAuthName;
    }

    public boolean isVisibleAuthEmail() {
        return visibleAuthEmail;
    }

    public boolean isVisibleAuthId() {
        return visibleAuthId;
    }

    public boolean isVisibleAuthCardLocal() {
        return visibleAuthCardLocal;
    }

    public boolean isVisibleAuthCardIp() {
        return visibleAuthCardIp;
    }

    public boolean isVisibleAuthYubikey() {
        return visibleAuthYubikey;
    }

    public UserAuthModeEnum getAuthModeDefault() {
        return authModeDefault;
    }

    public boolean isAuthIdPinReq() {
        return authIdPinReq;
    }

    public boolean isAuthCardPinReq() {
        return authCardPinReq;
    }

    public boolean isAuthIdMasked() {
        return authIdMasked;
    }

    public boolean isAuthCardSelfAssoc() {
        return authCardSelfAssoc;
    }

    public Integer getMaxIdleSeconds() {
        return maxIdleSeconds;
    }

    public boolean isAllowAuthName() {
        return allowAuthName;
    }

    public boolean isAllowAuthEmail() {
        return allowAuthEmail;
    }

    public boolean isAllowAuthId() {
        return allowAuthId;
    }

    public boolean isAllowAuthCardLocal() {
        return allowAuthCardLocal;
    }

    public boolean isAllowAuthCardIp() {
        return allowAuthCardIp;
    }

    public boolean isAllowAuthYubikey() {
        return allowAuthYubikey;
    }

}
