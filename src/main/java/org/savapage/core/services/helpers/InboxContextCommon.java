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

import org.savapage.core.jpa.DocLog;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class InboxContextCommon implements InboxContext {

    /** */
    private final String userIdInbox;
    /** */
    private final String userIdDocLog;

    /**
     * @param inboxUserId
     *            The user id for SafePages.
     * @param docLogUserId
     *            User id for {@link DocLog}.
     */
    public InboxContextCommon(final String inboxUserId,
            final String docLogUserId) {
        this.userIdInbox = inboxUserId;
        this.userIdDocLog = docLogUserId;
    }

    /**
     * @return The user id for SafePages.
     */
    @Override
    public final String getUserIdInbox() {
        return this.userIdInbox;
    }

    /**
     * @return The user id for {@link DocLog}.
     */
    @Override
    public final String getUserIdDocLog() {
        if (userIdDocLog == null) {
            return this.userIdInbox;
        }
        return this.userIdDocLog;
    }

}
