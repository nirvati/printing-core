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
package org.savapage.core.services;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.services.impl.SavaPageUserAccountContext;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.services.impl.PaperCutUserAccountContext;

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
            return new PaperCutUserAccountContext(
                    PaperCutServerProxy.create(ConfigManager.instance(), true));
        }
        return null;
    }

    /**
     * Gets the SavaPage context.
     *
     * @return {@link UserAccountContext}.
     */
    public static UserAccountContext getContextSavaPage() {
        return SavaPageUserAccountContext.instance();
    }

}
