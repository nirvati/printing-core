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
package org.savapage.core.users;

import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract LDAP pager.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class LdapPagerMixin {

    /**
    *
    */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LdapPagerMixin.class);

    /**
    *
    */
    private final InitialLdapContext ldapContext;

    /**
    *
    */
    private final SearchControls searchControls;

    /**
    *
    */
    private final String ldapFilterExpression;

    /**
     *
     * @param ldapContext
     *            The {@link InitialLdapContext}.
     * @param searchControls
     *            The {@link SearchControls}.
     * @param ldapFilterExpression
     *            The LDAP filter expression.
     */
    protected LdapPagerMixin(final InitialLdapContext ldapContext,
            final SearchControls searchControls,
            final String ldapFilterExpression) {

        this.ldapContext = ldapContext;
        this.searchControls = searchControls;
        this.ldapFilterExpression = ldapFilterExpression;

    }

    /**
     *
     * @return The {@link InitialLdapContext}.
     */
    protected final InitialLdapContext getLdapContext() {
        return ldapContext;
    }

    protected final SearchControls getSearchControls() {
        return searchControls;
    }

    protected final String getLdapFilterExpression() {
        return ldapFilterExpression;
    }

    /**
     * Closes resources.
     *
     * @param results
     *            The results.
     * @param ctx
     *            The {@link DirContext}.
     */
    protected final void closeResources(
            final NamingEnumeration<SearchResult> results,
            final DirContext ctx) {

        if (results != null) {
            try {
                results.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        if (ctx != null) {
            try {
                ctx.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

}
