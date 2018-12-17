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
package org.savapage.core.services.helpers.account;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.services.impl.UserAccountContextSavaPage;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.services.impl.UserAccountContextPaperCut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserAccountContextFactory {

    /**
     * NO public instantiation allowed.
     */
    private UserAccountContextFactory() {
    }

    /**
     * Gets the leading context.
     *
     * @return {@link UserAccountContextEnum}.
     */
    public static UserAccountContextEnum getContextEnum() {
        if (hasContextPaperCut()) {
            return UserAccountContextEnum.PAPERCUT;
        }
        return UserAccountContextEnum.SAVAPAGE;
    }

    /**
     * Gets the leading context.
     *
     * @return {@link UserAccountContext}.
     */
    public static UserAccountContext getContext() {

        final UserAccountContext ctx = getContextPaperCut();

        if (ctx == null) {
            return getContextSavaPage();
        }
        return ctx;
    }

    /**
     * Gets context.
     *
     * @param ctx
     *            Context type.
     * @return {@link UserAccountContext}.
     */
    public static UserAccountContext
            getContext(final UserAccountContextEnum ctx) {
        switch (ctx) {
        case PAPERCUT:
            return new UserAccountContextPaperCut(
                    PaperCutServerProxy.create(ConfigManager.instance(), true));
        case SAVAPAGE:
            return UserAccountContextSavaPage.instance();
        default:
            throw new UnknownError(ctx.toString());
        }
    }

    /**
     * @return {@code true} if PaperCut context is available.
     */
    public static boolean hasContextPaperCut() {

        final ConfigManager cm = ConfigManager.instance();

        return cm.isConfigValue(Key.PAPERCUT_ENABLE)
                && cm.isConfigValue(Key.FINANCIAL_USER_ACCOUNT_PAPERCUT_ENABLE);
    }

    /**
     * Gets the PaperCut context.
     *
     * @return {@link UserAccountContext}, or {@code null} when not available.
     */
    public static UserAccountContext getContextPaperCut() {

        if (hasContextPaperCut()) {
            return getContext(UserAccountContextEnum.PAPERCUT);
        }
        return null;
    }

    /**
     * Gets the SavaPage context.
     *
     * @return {@link UserAccountContext}.
     */
    public static UserAccountContext getContextSavaPage() {
        return getContext(UserAccountContextEnum.SAVAPAGE);
    }

}
