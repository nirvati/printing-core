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
package org.savapage.core.users;

import java.io.IOException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.savapage.core.SpException;

/**
 * A pager for LDAP results.
 * <p>
 * The client is responsible for setting the page size.
 * </p>
 * <p>
 * Use {@link #NO_PAGING} as page size if paging is not supported (according to
 * the supported controls of the LDAP server). In that case the
 * {@link #nextPage()} method will return just a single page with all results.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class LdapSearchResultPager extends LdapPagerMixin {

    /**
     * The number of entries to return in a page.
     */
    private final int pageSize;

    /**
     * A page size indicating that NO paging is to be applied.
     */
    public static final int NO_PAGING = 0;

    /**
     * Dummy page number indicating no first page is retrieved.
     */
    private static final int ZERO_PAGE = 0;

    /**
     * One-based ordinal of the current page.
     */
    private int pageNumber = ZERO_PAGE;

    /**
     * The LDAP pager cookie.
     */
    private byte[] cookie = null;

    /**
     *
     * @param ldapContext
     *            The {@link InitialLdapContext}.
     * @param searchControls
     *            The {@link SearchControls}.
     * @param ldapFilterExpression
     *            The LDAP filter expression.
     * @param pageSize
     *            The number of entries to return in a page, or
     *            {@link #NO_PAGING} if all results are to be supplied on one
     *            page.
     */
    public LdapSearchResultPager(final InitialLdapContext ldapContext,
            final SearchControls searchControls,
            final String ldapFilterExpression, final int pageSize) {

        super(ldapContext, searchControls, ldapFilterExpression);

        this.pageSize = pageSize;

    }

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter. Performs the search as specified by the search
     * controls.
     *
     * @return an enumeration of <tt>SearchResult</tt>s for the objects that
     *         satisfy the filter.
     * @throws NamingException
     *             If a naming exception is encountered.
     */
    private NamingEnumeration<SearchResult> search() throws NamingException {

        return this.getLdapContext().search("", getLdapFilterExpression(),
                getSearchControls());
    }

    /**
     * Gets the result for the next page using the {@link PagedResultsControl}.
     *
     * @return The results.
     */
    private NamingEnumeration<SearchResult> nextPagedResults() {

        final PagedResultsControl control;

        NamingEnumeration<SearchResult> results = null;

        try {

            if (isFirstPage()) {
                control =
                        new PagedResultsControl(this.pageSize, Control.CRITICAL);
            } else {
                control =
                        new PagedResultsControl(this.pageSize, this.cookie,
                                Control.CRITICAL);
            }

            getLdapContext().setRequestControls(new Control[] { control });

            results = search();

            /*
             * Next page?
             */
            final Control[] rspControls =
                    this.getLdapContext().getResponseControls();

            if (rspControls == null) {

                this.cookie = null;

            } else {

                for (final Control rspControl : rspControls) {

                    if (rspControl instanceof PagedResultsResponseControl) {

                        final PagedResultsResponseControl prrc =
                                (PagedResultsResponseControl) rspControl;

                        this.cookie = prrc.getCookie();

                        break;
                    }
                }
            }

            this.incrementPageNumber();

        } catch (NamingException | IOException e) {

            closeResources(results, null);

            throw new SpException(e);
        }

        return results;
    }

    /**
     * Gets all result on one (1) page.
     *
     * @return The results.
     */
    private NamingEnumeration<SearchResult> onePagedResults() {

        NamingEnumeration<SearchResult> results = null;

        try {
            results = search();
            /*
             * Force end-of-pages.
             */
            this.cookie = null;

            this.incrementPageNumber();

        } catch (NamingException e) {

            closeResources(results, null);

            throw new SpException(e);
        }

        return results;
    }

    /**
     * Gets the result for the next page.
     * <p>
     * NOTE: The caller is responsible for closing the results when done.
     * </p>
     *
     * @return The results.
     */
    public final NamingEnumeration<SearchResult> nextPage() {
        if (this.pageSize == NO_PAGING) {
            return onePagedResults();
        } else {
            return nextPagedResults();
        }
    }

    /**
     *
     * @return {@code true} when next page is present.
     */
    public final boolean hasNextPage() {
        return this.cookie != null;
    }

    /**
     *
     * @return {@code true} when next page is present.
     */
    public final boolean isFirstPage() {
        return this.pageNumber == ZERO_PAGE;
    }

    /**
     *
     * @return The page number.
     */
    public final int getPageNumber() {
        return pageNumber;
    }

    /**
     * Increments the page number.
     */
    private void incrementPageNumber() {
        pageNumber++;
    }

}
