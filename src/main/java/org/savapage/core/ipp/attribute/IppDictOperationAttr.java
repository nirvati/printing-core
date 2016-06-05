/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
 * <a href="http://tools.ietf.org/html/rfc2911">RFC2911</a>
 * <p>
 * "These attributes are passed in the operation and affect the IPP object's
 * behavior while processing the operation request and may affect other
 * attributes or groups of attributes."
 * </p>
 * <p>
 * "Some operation attributes describe the document data associated with the
 * print job and are associated with new Job objects ..."
 * </p>
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

    /*
     * Group 1: Operation Attributes
     */
    private final IppAttr[] attributes = {

            /*
             * COMMON attributes.
             */
            new IppAttr(ATTR_ATTRIBUTES_CHARSET, IppCharset.instance()),
            new IppAttr(ATTR_ATTRIBUTES_NATURAL_LANG,
                    IppNaturalLanguage.instance()),

            /*
             * Target:
             *
             * The "printer-uri" (uri) operation attribute which is the target
             * for this operation as described in section 3.1.5.
             */
            new IppAttr(ATTR_PRINTER_URI, IppUri.instance()),

            /*
             * Target:
             *
             * Either (1) the "printer-uri" (uri) plus "job-id" (integer(1:MAX))
             * or (2) the "job-uri" (uri) operation attribute(s) which define
             * the target for this operation as described in section 3.1.5.
             */
            new IppAttr(ATTR_JOB_ID, new IppInteger(1)),
            new IppAttr(ATTR_JOB_URI, IppUri.instance()),
            new IppAttr(ATTR_JOB_NAME, IppName.instance()),
            new IppAttr(ATTR_JOB_K_OCTETS, new IppInteger(0)),

            new IppAttr(ATTR_DOCUMENT_NAME, IppName.instance()),

            new IppAttr(ATTR_REQUESTING_USER_NAME, IppName.instance()),

            new IppAttr(ATTR_COMPRESSION, IppKeyword.instance()),

            new IppAttr(ATTR_IPP_ATTRIBUTE_FIDELITY, IppBoolean.instance()),

            /*
             * "requested-attributes" (1setOf keyword):
             */
            new IppAttr(ATTR_REQUESTED_ATTRIBUTES, IppKeyword.instance()),
            /*
             * "document-format" (mimeMediaType):
             *
             * The client OPTIONALLY supplies this attribute. The Printer object
             * MUST support this attribute. This attribute is useful for a
             * Printer object to determine the set of supported attribute values
             * that relate to the requested document format. The Printer object
             * MUST return the attributes and values that it uses to validate a
             * job on a create or Validate-Job operation in which this document
             * format is supplied. The Printer object SHOULD return only (1)
             * those attributes that are supported for the specified format and
             * (2) the attribute values that are supported for the specified
             * document format. By specifying the document format, the client
             * can get the Printer object to eliminate the attributes and values
             * that are not supported for a specific document format. For
             * example, a Printer object might have multiple interpreters to
             * support both 'application/postscript' (for PostScript) and
             * 'text/plain' (for text) documents. However, for only one of those
             * interpreters might the Printer object be able to support
             * "number-up" with values of '1', '2', and '4'. For the other
             * interpreter it might be able to only support "number-up" with a
             * value of '1'. Thus a client can use the Get-Printer-Attributes
             * operation to obtain the attributes and values that will be used
             * to accept/reject a create job operation.
             *
             * If the Printer object does not distinguish between different sets
             * of supported values for each different document format when
             * validating jobs in the create and Validate-Job operations, it
             * MUST NOT distinguish between different document formats in the
             * Get-Printer-Attributes operation. If the Printer object does
             * distinguish between different sets of supported values for each
             * different document format specified by the client, this
             * specialization applies only to the following Printer object
             * attributes:
             *
             *
             * - Printer attributes that are Job Template attributes ("xxx-
             * default" "xxx-supported", and "xxx-ready" in the Table in Section
             * 4.2),
             *
             * - "pdl-override-supported",
             *
             * - "compression-supported",
             *
             * - "job-k-octets-supported",
             *
             * - "job-impressions-supported",
             *
             * - "job-media-sheets-supported",
             *
             * - "printer-driver-installer",
             *
             * - "color-supported", and
             *
             * - "reference-uri-schemes-supported"
             *
             * The values of all other Printer object attributes (including
             * "document-format-supported") remain invariant with respect to the
             * client supplied document format (except for new Printer
             * description attribute as registered according to section 6.2).
             *
             * If the client omits this "document-format" operation attribute,
             * the Printer object MUST respond as if the attribute had been
             * supplied with the value of the Printer object's
             * "document-format-default" attribute. It is RECOMMENDED that the
             * client always supply a value for "document-format", since the
             * Printer object's "document-format-default" may be
             * 'application/octet-stream', in which case the returned attributes
             * and values are for the union of the document formats that the
             * Printer can automatically sense. For more details, see the
             * description of the 'mimeMediaType' attribute syntax in section
             * 4.1.9.
             *
             * If the client supplies a value for the "document-format"
             * Operation attribute that is not supported by the Printer, i.e.,
             * is not among the values of the Printer object's
             * "document-format-supported" attribute, the Printer object MUST
             * reject the operation and return the
             * 'client-error-document-format-not-supported' status code.
             */
            new IppAttr(ATTR_DOCUMENT_FORMAT, IppMimeMediaType.instance()),

            /* */
            new IppAttr(ATTR_LIMIT, IppInteger.instance()),

            /* */
            new IppAttr(ATTR_MY_SUBSCRIPTIONS, IppBoolean.instance()),
            /* */
            new IppAttr(ATTR_NOTIFY_SUBSCRIPTION_ID, IppInteger.instance()),
            /* */
            new IppAttr(ATTR_NOTIFY_SUBSCRIPTION_IDS, IppInteger.instance()),
            /* */
            new IppAttr(ATTR_NOTIFY_WAIT, IppBoolean.instance()),
            /* */
            new IppAttr(ATTR_NOTIFY_GET_INTERVAL, IppInteger.instance()),
            //
    };

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppDictOperationAttr#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final IppDictOperationAttr INSTANCE =
                new IppDictOperationAttr();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
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
