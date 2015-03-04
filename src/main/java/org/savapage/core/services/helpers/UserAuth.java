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
package org.savapage.core.services.helpers;

import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.helpers.DeviceAttrEnum;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
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
 * @author Datraverse B.V.
 *
 */
public class UserAuth {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(UserAuth.class);

    /**
     * Login method 'Username'.
     * <p>
     * <b>NOTE</b>: Value is used as URL parameter at WebApp Login.
     * </p>
     */
    public static final String MODE_NAME = "name";

    /**
     * Login method 'ID Number'.
     * <p>
     * <b>NOTE</b>: Value is used as URL parameter at WebApp Login.
     * </p>
     */
    public static final String MODE_ID = "id";

    /**
     * Login method 'Local NFC Card'.
     * <p>
     * <b>NOTE</b>: Value is used as URL parameter at WebApp Login.
     * </p>
     */
    public static final String MODE_CARD_LOCAL = "nfc-local";

    /**
     * Login method 'Network NFC Card'.
     * <p>
     * <b>NOTE</b>: Value is used as URL parameter at WebApp Login.
     * </p>
     */
    public static final String MODE_CARD_IP = "nfc-network";

    /**
     *
     * @author Datraverse B.V.
     *
     */
    public static enum Mode {
        NAME, ID, CARD_LOCAL, CARD_IP
    }

    private boolean visibleAuthName;
    private boolean visibleAuthId;
    private boolean visibleAuthCardLocal;
    private boolean visibleAuthCardIp;

    private Mode authModeDefault;

    private boolean authIdPinReq;
    private boolean authIdMasked;
    private boolean authCardPinReq;

    private boolean authCardSelfAssoc;
    private Integer maxIdleSeconds;

    private boolean allowAuthName;
    private boolean allowAuthId;
    private boolean allowAuthCardLocal;
    private boolean allowAuthCardIp;

    /**
     *
     */
    protected UserAuth() {
    }

    /**
     * Gets the String representation from the {@link Mode}.
     *
     * @param mode
     * @return {@code null} when not found.
     */
    public static String mode(final Mode mode) {
        switch (mode) {
        case CARD_IP:
            return MODE_CARD_IP;
        case CARD_LOCAL:
            return MODE_CARD_LOCAL;
        case ID:
            return MODE_ID;
        case NAME:
            return MODE_NAME;
        default:
            return null;
        }
    }

    /**
     * Gets the {@link Mode} representation from the String.
     *
     * @param mode
     * @return {@code null} when not found.
     */
    public static Mode mode(final String mode) {
        if (mode.equals(MODE_NAME)) {
            return Mode.NAME;
        } else if (mode.equals(MODE_ID)) {
            return Mode.ID;
        } else if (mode.equals(MODE_CARD_IP)) {
            return Mode.CARD_IP;
        } else if (mode.equals(MODE_CARD_LOCAL)) {
            return Mode.CARD_LOCAL;
        }
        return null;
    }

    /**
     * Renders the User Authentication Mode parameters for a terminal.
     *
     * @param terminal
     *            The terminal where authentication is taken place.
     * @param authModeRequest
     *            The requested authentication mode (used to determine the
     *            default {@link Mode}). Can be {@code null}.
     * @param isAdminWebAppContext
     *            {@code true} if this is an Admin WebApp context.
     */
    public UserAuth(final Device terminal, final String authModeRequest,
            boolean isAdminWebAppContext) {

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
        this.allowAuthId = false;
        this.allowAuthCardLocal = false;
        this.allowAuthCardIp = false;

        /*
         * Intermediate parameters.
         */
        boolean showAuthName = false;
        boolean showAuthId = false;
        boolean showAuthCardLocal = false;
        boolean showAuthCardIp = false;

        Mode authModeReq = null;

        if (authModeRequest != null) {
            authModeReq = UserAuth.mode(authModeRequest);
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

            isCustomAuth =
                    customAuth
                            .isTrue(DeviceAttrEnum.AUTH_MODE_IS_CUSTOM, false);
        }

        if (customAuth != null && isCustomAuth) {
            /*
             * Custom Device values.
             */
            this.allowAuthName =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_NAME, false);
            this.allowAuthId =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_ID, false);
            this.allowAuthCardIp =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_IP, false);
            this.allowAuthCardLocal =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_LOCAL,
                            false);

            this.authModeDefault =
                    UserAuth.mode(customAuth.get(
                            DeviceAttrEnum.AUTH_MODE_DEFAULT,
                            UserAuth.mode(UserAuth.Mode.NAME)));

            showAuthName = this.allowAuthName;
            showAuthId = this.allowAuthId;
            showAuthCardIp = this.allowAuthCardIp;
            showAuthCardLocal = this.allowAuthCardLocal;

            if (!isAdminWebAppContext) {
                this.maxIdleSeconds =
                        Integer.valueOf(customAuth.get(
                                DeviceAttrEnum.WEBAPP_USER_MAX_IDLE_SECS,
                                cm.getConfigValue(Key.WEBAPP_USER_MAX_IDLE_SECS)));
            }

            this.authIdPinReq =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_ID_PIN_REQ,
                            this.authIdPinReq);

            this.authIdMasked =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_ID_IS_MASKED,
                            this.authIdMasked);

            this.authCardPinReq =
                    customAuth.isTrue(DeviceAttrEnum.AUTH_MODE_CARD_PIN_REQ,
                            this.authCardPinReq);

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
             * Global values.
             */
            this.allowAuthName = cm.isConfigValue(Key.AUTH_MODE_NAME);
            this.allowAuthId = cm.isConfigValue(Key.AUTH_MODE_ID);
            this.allowAuthCardLocal =
                    cm.isConfigValue(Key.AUTH_MODE_CARD_LOCAL);
            this.allowAuthCardIp = false;

            showAuthName = cm.isConfigValue(Key.AUTH_MODE_NAME_SHOW);
            showAuthId = cm.isConfigValue(Key.AUTH_MODE_ID_SHOW);
            showAuthCardLocal = cm.isConfigValue(Key.AUTH_MODE_CARD_LOCAL_SHOW);
            showAuthCardIp = false;

            this.authModeDefault =
                    UserAuth.mode(cm.getConfigValue(Key.AUTH_MODE_DEFAULT));

            /*
             * This can only occur when we refactored the URL API (URL parameter
             * names). Just in case we fall-back to basic Username login.
             */
            if (this.authModeDefault == null) {
                LOGGER.warn("System AuthModeDefault ["
                        + cm.getConfigValue(Key.AUTH_MODE_DEFAULT)
                        + "] not found: using Username "
                        + "as default login method");
                this.authModeDefault = Mode.NAME;
            }

            boolean isValidDefault = false;

            switch (this.authModeDefault) {
            case CARD_LOCAL:
                isValidDefault = this.allowAuthCardLocal && showAuthCardLocal;
                break;
            case ID:
                isValidDefault = this.allowAuthId && showAuthId;
                break;
            case NAME:
                isValidDefault = this.allowAuthName && showAuthName;
                break;
            default:
                break;
            }

            if (!isValidDefault) {
                this.authModeDefault = Mode.NAME;
            }

            if (!isAdminWebAppContext) {
                this.maxIdleSeconds =
                        cm.getConfigInt(Key.WEBAPP_USER_MAX_IDLE_SECS);
            }

        }

        /*
         * Just in case.
         */
        if (this.authModeDefault == null) {
            this.authModeDefault = UserAuth.Mode.NAME;
        }

        /*
         * Mode requested?
         */
        if (authModeReq == null) {

            this.visibleAuthName = this.allowAuthName && showAuthName;
            this.visibleAuthId = this.allowAuthId && showAuthId;
            this.visibleAuthCardLocal =
                    this.allowAuthCardLocal && showAuthCardLocal;
            this.visibleAuthCardIp = this.allowAuthCardIp && showAuthCardIp;

            /*
             * INVARIANT: Admin WebApp SHOULD always be able to login with
             * user/password.
             */
            if (isAdminWebAppContext) {
                this.visibleAuthName = true;
            }

        } else {
            /*
             * Assign requested mode.
             */
            this.visibleAuthName = false;
            this.visibleAuthId = false;
            this.visibleAuthCardLocal = false;
            this.visibleAuthCardIp = false;

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
            case NAME:
                this.visibleAuthName = this.allowAuthName;
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
                (this.authModeDefault == Mode.NAME && !this.visibleAuthName)
                        || (this.authModeDefault == Mode.ID && !this.visibleAuthId)
                        || (this.authModeDefault == Mode.CARD_LOCAL && !this.visibleAuthCardLocal)
                        || (this.authModeDefault == Mode.CARD_IP && !this.visibleAuthCardIp);

        if (incorrectDefault) {
            /*
             * Assign the more advanced methods first.
             */
            if (this.visibleAuthCardIp) {
                this.authModeDefault = Mode.CARD_IP;
            } else if (this.visibleAuthCardLocal) {
                this.authModeDefault = Mode.CARD_LOCAL;
            } else if (this.visibleAuthId) {
                this.authModeDefault = Mode.ID;
            } else if (this.visibleAuthName) {
                this.authModeDefault = Mode.NAME;
            }
        }

    }

    /**
     * Check if an authentication mode is allowed.
     * <p>
     * NOTE: when Card Self Association is enabled, {@link Mode#NAME} is ALWAYS
     * allowed.
     * </p>
     *
     * @param mode
     * @return
     */
    public boolean isAuthModeAllowed(UserAuth.Mode mode) {

        if (mode == UserAuth.Mode.NAME && authCardSelfAssoc) {
            return true;
        }

        return (allowAuthName && mode == UserAuth.Mode.NAME)
                || (allowAuthId && mode == UserAuth.Mode.ID)
                || (allowAuthCardLocal && mode == UserAuth.Mode.CARD_LOCAL)
                || (allowAuthCardIp && mode == UserAuth.Mode.CARD_IP);
    }

    public boolean isVisibleAuthName() {
        return visibleAuthName;
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

    public Mode getAuthModeDefault() {
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

    public boolean isAllowAuthId() {
        return allowAuthId;
    }

    public boolean isAllowAuthCardLocal() {
        return allowAuthCardLocal;
    }

    public boolean isAllowAuthCardIp() {
        return allowAuthCardIp;
    }

}
