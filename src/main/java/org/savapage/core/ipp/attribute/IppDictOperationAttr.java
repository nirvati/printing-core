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
package org.savapage.core.ipp.attribute;

import org.savapage.core.ipp.attribute.syntax.IppBoolean;
import org.savapage.core.ipp.attribute.syntax.IppCharset;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppMimeMediaType;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * A dictionary of operation attributes:
 *
 * @author Rijk Ravestein
 *
 */
public final class IppDictOperationAttr extends AbstractIppDict {

    /**
     * [COMMON] Identifies the charset (coded character set and encoding method)
     * used by any 'text' and 'name' attributes that the client is supplying in
     * this request.
     * <p>
     * It also identifies the charset that the Printer object MUST use (if
     * supported) for all 'text' and 'name' attributes and status messages that
     * the Printer object returns in the response to this request.
     * </p>
     * <p>
     * See Sections 4.1.1 and 4.1.2 for the definition of the 'text' and 'name'
     * attribute syntaxes.
     * </p>
     */
    public static final String ATTR_ATTRIBUTES_CHARSET = "attributes-charset";

    /**
     * [COMMON] Identifies the natural language used by any 'text' and 'name'
     * attributes that the client is supplying in this request.
     * <p>
     * This attribute also identifies the natural language that the Printer
     * object SHOULD use for all 'text' and 'name' attributes and status
     * messages that the Printer object returns in the response to this request.
     * </p>
     */
    public static final String ATTR_ATTRIBUTES_NATURAL_LANG =
            "attributes-natural-language";

    /**
     * [Print-Job Request]: "requesting-user-name" attribute SHOULD be supplied
     * by the client as described in section 8.3.
     */
    public static final String ATTR_REQUESTING_USER_NAME =
            "requesting-user-name";

    public static final String ATTR_PRINTER_URI = "printer-uri";

    public static final String ATTR_JOB_ID = "job-id";
    public static final String ATTR_JOB_URI = "job-uri";
    public static final String ATTR_JOB_NAME = "job-name";
    public static final String ATTR_JOB_K_OCTETS = "job-k-octets";

    public static final String ATTR_DOCUMENT_NAME = "document-name";

    public static final String ATTR_REQUESTED_ATTRIBUTES =
            "requested-attributes";

    public static final String ATTR_DOCUMENT_FORMAT = "document-format";
    public static final String ATTR_COMPRESSION = "compression";

    public static final String ATTR_IPP_ATTRIBUTE_FIDELITY =
            "ipp-attribute-fidelity";

    public static final String ATTR_LIMIT = "limit";

    public static final String ATTR_MY_SUBSCRIPTIONS = "my-subscriptions";
    public static final String ATTR_NOTIFY_SUBSCRIPTION_ID =
            "notify-subscription-id";
    public static final String ATTR_NOTIFY_SUBSCRIPTION_IDS =
            "notify-subscription-ids";
    public static final String ATTR_NOTIFY_WAIT = "notify-wait";
    public static final String ATTR_NOTIFY_GET_INTERVAL = "notify-get-interval";

    /**
     * Group 1: Operation Attributes.
     */
    private final IppAttr[] attributes = {

            /*
             * Common attributes.
             */
            new IppAttr(ATTR_ATTRIBUTES_CHARSET, IppCharset.instance()),
            new IppAttr(ATTR_ATTRIBUTES_NATURAL_LANG,
                    IppNaturalLanguage.instance()),

            /*
             * Either (1) the "printer-uri" (uri) plus "job-id" (integer(1:MAX))
             * or (2) the "job-uri" (uri) operation attribute(s) which define
             * the target of an operation.
             */
            new IppAttr(ATTR_PRINTER_URI, IppUri.instance()),
            new IppAttr(ATTR_JOB_ID, new IppInteger(1)),
            new IppAttr(ATTR_JOB_URI, IppUri.instance()),

            new IppAttr(ATTR_JOB_NAME, IppName.instance()),
            new IppAttr(ATTR_JOB_K_OCTETS, new IppInteger(0)),

            new IppAttr(ATTR_DOCUMENT_NAME, IppName.instance()),
            new IppAttr(ATTR_REQUESTING_USER_NAME, IppName.instance()),
            new IppAttr(ATTR_COMPRESSION, IppKeyword.instance()),
            new IppAttr(ATTR_IPP_ATTRIBUTE_FIDELITY, IppBoolean.instance()),
            new IppAttr(ATTR_REQUESTED_ATTRIBUTES, IppKeyword.instance()),
            new IppAttr(ATTR_DOCUMENT_FORMAT, IppMimeMediaType.instance()),
            new IppAttr(ATTR_LIMIT, IppInteger.instance()),
            new IppAttr(ATTR_MY_SUBSCRIPTIONS, IppBoolean.instance()),
            new IppAttr(ATTR_NOTIFY_SUBSCRIPTION_ID, IppInteger.instance()),
            new IppAttr(ATTR_NOTIFY_SUBSCRIPTION_IDS, IppInteger.instance()),
            new IppAttr(ATTR_NOTIFY_WAIT, IppBoolean.instance()),
            new IppAttr(ATTR_NOTIFY_GET_INTERVAL, IppInteger.instance()),
            //
    };

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppDictOperationAttr INSTANCE =
                new IppDictOperationAttr();
    }

    /**
     * @return The singleton instance.
     */
    public static IppDictOperationAttr instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private IppDictOperationAttr() {
        init(attributes);

    }

    @Override
    public IppAttr getAttr(final String keyword, final IppValueTag valueTag) {
        /*
         * Ignore the value tag.
         */
        return getAttr(keyword);
    }

}
