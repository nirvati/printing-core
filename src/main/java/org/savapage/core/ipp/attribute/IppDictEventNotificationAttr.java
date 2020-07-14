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
import org.savapage.core.ipp.attribute.syntax.IppEnum;
import org.savapage.core.ipp.attribute.syntax.IppInteger;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.attribute.syntax.IppName;
import org.savapage.core.ipp.attribute.syntax.IppNaturalLanguage;
import org.savapage.core.ipp.attribute.syntax.IppOctetString;
import org.savapage.core.ipp.attribute.syntax.IppText;
import org.savapage.core.ipp.attribute.syntax.IppUri;
import org.savapage.core.ipp.encoding.IppValueTag;

/**
 * A dictionary of event notification attributes:
 * <a href="http://tools.ietf.org/html/rfc3995">RFC3995</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppDictEventNotificationAttr extends AbstractIppDict {

    public static final String ATTR_NOTIFY_CHARSET = "notify-charset";

    public static final String ATTR_NOTIFY_NATURAL_LANGUAGE =
            "notify-natural-language";

    public static final String ATTR_NOTIFY_SUBSCRIPTION_ID =
            "notify-subscription-id";

    public static final String ATTR_NOTIFY_SEQUENCE_NUMBER =
            "notify-sequence-number";

    public static final String ATTR_NOTIFY_SUBSCRIBED_EVENT =
            "notify-subscribed-event";

    public static final String ATTR_NOTIFY_TEXT = "notify-text";

    public static final String ATTR_NOTIFY_USER_DATA = "notify-user-data";

    public static final String ATTR_NOTIFY_PRINTER_URI = "notify-printer-uri";

    public static final String ATTR_PRINTER_UP_TIME = "printer-up-time";

    public static final String ATTR_PRINTER_NAME = "printer-name";

    public static final String ATTR_PRINTER_STATE = "printer-state";

    public static final String ATTR_PRINTER_STATE_REASONS =
            "printer-state-reasons";

    public static final String ATTR_PRINTER_IS_ACCEPTING_JOBS =
            "printer-is-accepting-jobs";

    public static final String ATTR_NOTIFY_JOB_ID = "notify-job-id";

    public static final String ATTR_JOB_STATE = "job-state";

    public static final String ATTR_JOB_NAME = "job-name";

    public static final String ATTR_JOB_STATE_REASONS = "job-state-reasons";

    public static final String ATTR_JOB_IMPRESSIONS_COMPLETED =
            "job-impressions-completed";

    /**
     * Group 1: Operation Attributes.
     */
    private final IppAttr[] attributes = {
            //
            new IppAttr(ATTR_NOTIFY_CHARSET, IppCharset.instance()),
            new IppAttr(ATTR_NOTIFY_NATURAL_LANGUAGE,
                    IppNaturalLanguage.instance()),
            new IppAttr(ATTR_NOTIFY_SUBSCRIPTION_ID, IppInteger.instance()),
            new IppAttr(ATTR_NOTIFY_SEQUENCE_NUMBER, IppInteger.instance()),
            new IppAttr(ATTR_NOTIFY_SUBSCRIBED_EVENT, IppKeyword.instance()),
            new IppAttr(ATTR_NOTIFY_TEXT, IppText.instance()),
            new IppAttr(ATTR_NOTIFY_USER_DATA, IppOctetString.instance()),
            new IppAttr(ATTR_NOTIFY_PRINTER_URI, IppUri.instance()),
            new IppAttr(ATTR_PRINTER_IS_ACCEPTING_JOBS, IppBoolean.instance()),
            new IppAttr(ATTR_PRINTER_NAME, IppName.instance()),
            new IppAttr(ATTR_PRINTER_STATE, IppEnum.instance()),
            new IppAttr(ATTR_PRINTER_STATE_REASONS, IppKeyword.instance()),
            new IppAttr(ATTR_PRINTER_UP_TIME, IppInteger.instance()),
            new IppAttr(ATTR_NOTIFY_JOB_ID, IppInteger.instance()),
            new IppAttr(ATTR_JOB_STATE, IppEnum.instance()),
            new IppAttr(ATTR_JOB_STATE_REASONS, IppKeyword.instance()),
            new IppAttr(ATTR_JOB_NAME, IppName.instance()),
            new IppAttr(ATTR_JOB_IMPRESSIONS_COMPLETED, IppInteger.instance()),
            //
    };

    /** */
    private static class SingletonHolder {
        /** */
        public static final IppDictEventNotificationAttr INSTANCE =
                new IppDictEventNotificationAttr();
    }

    /**
     * @return the singleton instance.
     */
    public static IppDictEventNotificationAttr instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     *
     */
    private IppDictEventNotificationAttr() {
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
