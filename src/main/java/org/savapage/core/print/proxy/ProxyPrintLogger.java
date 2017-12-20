/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.print.proxy;

import java.util.List;

import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.operation.IppOperationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPP request/response logger.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintLogger {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProxyPrintLogger.class);

    /** */
    private ProxyPrintLogger() {
    }

    /**
     * Logs an INFO message.
     *
     * @param ippOperation
     *            The IPP operation.
     * @param ippReq
     *            The IPP request.
     * @param ippRsp
     *            The IPP response.
     */
    public static void log(final IppOperationId ippOperation,
            final List<IppAttrGroup> ippReq, final List<IppAttrGroup> ippRsp) {

        if (!LOGGER.isInfoEnabled()) {
            return;
        }

        final StringBuilder msg = new StringBuilder();
        msg.append(ippOperation);

        log("Request", ippReq, msg);
        log("Response", ippRsp, msg);

        LOGGER.info(msg.toString());
    }

    /**
     * @param prefix
     *            The prefix text.
     * @param ippMsg
     *            The IPP message.
     * @param msg
     *            The log message to append on.
     */
    private static void log(final String prefix,
            final List<IppAttrGroup> ippMsg, final StringBuilder msg) {

        msg.append("\n").append(prefix);

        for (final IppAttrGroup group : ippMsg) {

            msg.append("\n\t").append(group.getDelimiterTag()).append(":");

            for (final IppAttrCollection collection : group.getCollections()) {
                logCollection(collection, msg, "\t\t");
            }

            for (final IppAttrValue attrValue : group.getAttributes()) {
                msg.append("\n\t\t")
                        .append(attrValue.getAttribute().getKeyword());
                for (final String val : attrValue.getValues()) {
                    msg.append(" [").append(val).append("]");
                }
            }
        }
    }

    /**
     *
     * @param collection
     *            The IPP attribute collection
     * @param msg
     *            The log message to append on.
     * @param tabs
     *            The tabs string.
     */
    private static void logCollection(final IppAttrCollection collection,
            final StringBuilder msg, final String tabs) {

        msg.append("\n").append(tabs).append(collection.getKeyword())
                .append(" (collection)");

        for (final IppAttrValue attrValue : collection.getAttributes()) {

            msg.append("\n").append(tabs).append("\t")
                    .append(attrValue.getAttribute().getKeyword());

            for (final String val : attrValue.getValues()) {
                msg.append(" [").append(val).append("]");
            }
        }

        for (final IppAttrCollection nestedCollection : collection
                .getCollections()) {
            // recurse
            logCollection(nestedCollection, msg, String.format("%s\t", tabs));
        }
    }

}
