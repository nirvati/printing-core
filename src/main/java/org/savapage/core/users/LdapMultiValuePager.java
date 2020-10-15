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

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A range pager to retrieve chunked values of a multi-valued LDAP attribute.
 * <p>
 * The client is responsible for setting the range step.
 * </p>
 * <p>
 * Use {@link #NO_RANGE} as step if range retrieval is not supported (according
 * to the supported controls of the LDAP server). In that case the
 * {@link #nextRange()} method will return just a single chunk with all values.
 * </p>
 * <p>
 * See: <a href="https://community.oracle.com/thread/1157644?tstart=0">https://
 * community.oracle.com/thread/1157644?tstart=0</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public class LdapMultiValuePager extends LdapPagerMixin {
    /**
    *
    */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LdapMultiValuePager.class);

    /**
     * The range step for which NO range retrieval is to be applied.
     */
    public static final int NO_RANGE = 0;

    /**
     * The max size of the returned range chunk.
     */
    private final int rangeStepSize;

    /**
     * The zero-based index walker of the range start.
     */
    private int rangeStartIndex;

    /**
     * The zero-based index walker of the range end.
     */
    private int rangeEndIndex;

    /**
     * {@code true} when the last range chunk has been retrieved.
     */
    private boolean finished = false;

    /**
     *
     */
    private final String multiValuedFieldName;

    /**
     *
     * @param ldapContext
     * @param searchControls
     * @param ldapFilterExpression
     * @param multiValuedFieldName
     * @param rangeStepSize
     */
    public LdapMultiValuePager(final InitialLdapContext ldapContext,
            final SearchControls searchControls,
            final String ldapFilterExpression,
            final String multiValuedFieldName, final int rangeStepSize) {

        super(ldapContext, searchControls, ldapFilterExpression);

        this.multiValuedFieldName = multiValuedFieldName;

        this.rangeStepSize = rangeStepSize;

        /*
         * Init of zero-based range index walkers.
         */
        this.rangeStartIndex = 0;
        this.rangeEndIndex = this.rangeStepSize - 1;
    }

    /**
     * Checks if next range is present.
     *
     * @return {@code true} when next range is present.
     */
    public final boolean hasNextRange() {
        return !this.finished;
    }

    /**
     * Checks if a range needs to be applied.
     *
     * @return {@code true} if range needs to be applied.
     */
    private boolean isRangeToBeApplied() {
        return this.rangeStepSize != NO_RANGE;
    }

    /**
     * Gets the result for the next range.
     *
     * @return The result.
     * @throws NamingException
     *             If a naming exception is encountered.
     */
    public final List<String> nextRange() throws NamingException {

        final List<String> attributeValues = new ArrayList<>();

        this.finished = true;

        /*
         * Set the attribute to return.
         */
        final String rangeProperty;

        if (isRangeToBeApplied()) {
            rangeProperty =
                    ";Range=" + this.rangeStartIndex + "-" + this.rangeEndIndex;
        } else {
            rangeProperty = "";
        }

        getSearchControls().setReturningAttributes(
                new String[] { this.multiValuedFieldName + rangeProperty });

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("ReturningAttribute: " + this.multiValuedFieldName
                    + rangeProperty);
        }

        /*
         * Get the results.
         */
        NamingEnumeration<SearchResult> results = null;

        try {

            results = getLdapContext().search("", getLdapFilterExpression(),
                    getSearchControls());

            /*
             * We expect just one SearchResult.
             */
            if (results.hasMoreElements()) {

                final SearchResult searchResult = results.next();

                final Attributes attributes = searchResult.getAttributes();

                if (attributes != null) {

                    final NamingEnumeration<?> attributesEnum =
                            attributes.getAll();

                    while (attributesEnum.hasMore()) {

                        final Attribute attribute =
                                (Attribute) attributesEnum.next();

                        /*
                         * Check if we are finished.
                         */
                        if (isRangeToBeApplied()) {

                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("attribute.getID() = "
                                        + attribute.getID());
                            }

                            this.finished = attribute.getID().endsWith("*");
                        }

                        /*
                         * Collect the values.
                         */
                        final NamingEnumeration<?> values = attribute.getAll();

                        while (values.hasMore()) {
                            attributeValues.add(values.next().toString());
                        }
                    }
                }
            }

        } finally {
            closeResources(results, null);
        }

        if (!this.finished) {
            this.rangeStartIndex += this.rangeStepSize;
            this.rangeEndIndex += this.rangeStepSize;
        }

        return attributeValues;
    }

}
