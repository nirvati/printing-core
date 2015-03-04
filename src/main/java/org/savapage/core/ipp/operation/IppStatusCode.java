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
package org.savapage.core.ipp.operation;

import org.savapage.core.SpException;

/**
 * Operation Status Codes.
 * <p>
 * See <a href="http://tools.ietf.org/html/rfc2911#section-13.1">RFC2911</a>
 * </p>
 */
public enum IppStatusCode {
    /** success. */

    OK(0x0000),

    /** OK, some attrs ignored. */
    OK_ATTRIGN(0x0001),

    /** OK, some attrs conflicted. */
    OK_ATTRCON(0x0002),

    /** invalid client request. */
    CLI_BADREQ(0x0400),

    /**
     * request is forbidden.
     *
     * The IPP object understood the request, but is refusing to fulfill it.
     * Additional authentication information or authorization credentials will
     * not help and the request SHOULD NOT be repeated. This status code is
     * commonly used when the IPP object does not wish to reveal exactly why the
     * request has been refused or when no other response is applicable.
     */
    CLI_FORBID(0x0401),

    /**
     * authentication required.
     *
     * The request requires user authentication. The IPP client may repeat the
     * request with suitable authentication information. If the request already
     * included authentication information, then this status code indicates that
     * authorization has been refused for those credentials. If this response
     * contains the same challenge as the prior response, and the user agent has
     * already attempted authentication at least once, then the response message
     * may contain relevant diagnostic information. This status codes reveals
     * more information than "client-error-forbidden".
     */
    CLI_NOAUTH(0x0402),

    /**
     * client-error-not-authorized
     * <p>
     * The requester is not authorized to perform the request. Additional
     * authentication information or authorization credentials will not help and
     * the request SHOULD NOT be repeated. This status code is used when the IPP
     * object wishes to reveal that the authentication information is
     * understandable, however, the requester is explicitly not authorized to
     * perform the request. This status codes reveals more information than
     * "client-error-forbidden" and "client-error- not-authenticated".
     * </p>
     */
    CLI_NOPERM(0x0403),

    /**
     * client-error-not-possible
     *
     * This status code is used when the request is for something that can not
     * happen. For example, there might be a request to cancel a job that has
     * already been canceled or aborted by the system. The IPP client SHOULD NOT
     * repeat the request.
     */
    CLI_NOTPOS(0x0404),

    /** client too slow. */
    CLI_TIMOUT(0x0405),
    /** no object found for URI. */
    CLI_NOTFND(0x0406),
    /** object no longer available. */
    CLI_OBJGONE(0x0407),
    /** requested entity too big. */
    CLI_TOOBIG(0x0408),
    /** attribute value too large. */
    CLI_TOOLNG(0x0409),
    /** unsupported doc format. */
    CLI_BADFMT(0x040a),
    /** attributes not supported. */
    CLI_NOTSUP(0x040b),
    /** URI scheme not supported. */
    CLI_NOSCHM(0x040c),
    /** charset not supported. */
    CLI_NOCHAR(0x040d),
    /** attributes conflicted. */
    CLI_ATTRCON(0x040e),
    /** compression not supported. */
    CLI_NOCOMP(0x040f),
    /** data can't be decompressed. */
    CLI_COMPERR(0x0410),
    /** document format error. */
    CLI_FMTERR(0x0411),
    /** error accessing data. */
    CLI_ACCERR(0x0412),
    /** unexpected internal error. */
    SRV_INTERN(0x0500),
    /** operation not supported. */
    SRV_NOTSUP(0x0501),
    /** service unavailable. */
    SRV_UNAVAIL(0x0502),
    /** version not supported. */
    SRV_BADVER(0x0503),
    /** device error. */
    SRV_DEVERR(0x0504),
    /** temporary error. */
    SRV_TMPERR(0x0505),
    /** server not accepting jobs. */
    SRV_REJECT(0x0506),
    /** server too busy. */
    SRV_TOOBUSY(0x0507),
    /** job has been canceled. */
    SRV_CANCEL(0x0508),
    /** multi-doc jobs unsupported. */
    SRV_NOMULTI(0x0509);

    /*
     * Status code classes.
     */
    // #define STATCLASS_OK(x) ((x) >= 0x0000 && (x) <= 0x00ff)
    // #define STATCLASS_INFO(x) ((x) >= 0x0100 && (x) <= 0x01ff)
    // #define STATCLASS_REDIR(x) ((x) >= 0x0200 && (x) <= 0x02ff)
    // #define STATCLASS_CLIERR(x) ((x) >= 0x0400 && (x) <= 0x04ff)
    // #define STATCLASS_SRVERR(x) ((x) >= 0x0500 && (x) <= 0x05ff)

    /**
     *
     */
    private int bitPattern = 0;

    /**
     * Creates an enum value from an integer.
     *
     * @param value
     *            The integer.
     */
    IppStatusCode(final int value) {
        this.bitPattern = value;
    }

    /**
     * Gets the integer representing this enum value.
     *
     * @return The integer.
     */
    public int asInt() {
        return this.bitPattern;
    }

    /**
     *
     * @param value
     * @return
     */
    public static IppStatusCode asEnum(final int value) {
        if (value == IppStatusCode.OK.asInt()) {
            return OK;
        } else if (value == IppStatusCode.OK_ATTRIGN.asInt()) {
            return OK_ATTRIGN;
        } else if (value == IppStatusCode.OK_ATTRCON.asInt()) {
            return OK_ATTRCON;
        } else if (value == IppStatusCode.CLI_BADREQ.asInt()) {
            return CLI_BADREQ;
        } else if (value == IppStatusCode.CLI_FORBID.asInt()) {
            return CLI_FORBID;
        } else if (value == IppStatusCode.CLI_NOAUTH.asInt()) {
            return CLI_NOAUTH;
        } else if (value == IppStatusCode.CLI_NOPERM.asInt()) {
            return CLI_NOPERM;
        } else if (value == IppStatusCode.CLI_NOTPOS.asInt()) {
            return CLI_NOTPOS;
        } else if (value == IppStatusCode.CLI_TIMOUT.asInt()) {
            return CLI_TIMOUT;
        } else if (value == IppStatusCode.CLI_NOTFND.asInt()) {
            return CLI_NOTFND;
        } else if (value == IppStatusCode.CLI_OBJGONE.asInt()) {
            return CLI_OBJGONE;
        } else if (value == IppStatusCode.CLI_TOOBIG.asInt()) {
            return CLI_TOOBIG;
        } else if (value == IppStatusCode.CLI_TOOLNG.asInt()) {
            return CLI_TOOLNG;
        } else if (value == IppStatusCode.CLI_BADFMT.asInt()) {
            return CLI_BADFMT;
        } else if (value == IppStatusCode.CLI_NOTSUP.asInt()) {
            return CLI_NOTSUP;
        } else if (value == IppStatusCode.CLI_NOSCHM.asInt()) {
            return CLI_NOSCHM;
        } else if (value == IppStatusCode.CLI_NOCHAR.asInt()) {
            return CLI_NOCHAR;
        } else if (value == IppStatusCode.CLI_ATTRCON.asInt()) {
            return CLI_ATTRCON;
        } else if (value == IppStatusCode.CLI_NOCOMP.asInt()) {
            return CLI_NOCOMP;
        } else if (value == IppStatusCode.CLI_COMPERR.asInt()) {
            return CLI_COMPERR;
        } else if (value == IppStatusCode.CLI_FMTERR.asInt()) {
            return CLI_FMTERR;
        } else if (value == IppStatusCode.CLI_ACCERR.asInt()) {
            return CLI_ACCERR;
        } else if (value == IppStatusCode.SRV_INTERN.asInt()) {
            return SRV_INTERN;
        } else if (value == IppStatusCode.SRV_NOTSUP.asInt()) {
            return SRV_NOTSUP;
        } else if (value == IppStatusCode.SRV_UNAVAIL.asInt()) {
            return SRV_UNAVAIL;
        } else if (value == IppStatusCode.SRV_BADVER.asInt()) {
            return SRV_BADVER;
        } else if (value == IppStatusCode.SRV_DEVERR.asInt()) {
            return SRV_DEVERR;
        } else if (value == IppStatusCode.SRV_TMPERR.asInt()) {
            return SRV_TMPERR;
        } else if (value == IppStatusCode.SRV_REJECT.asInt()) {
            return SRV_REJECT;
        } else if (value == IppStatusCode.SRV_TOOBUSY.asInt()) {
            return SRV_TOOBUSY;
        } else if (value == IppStatusCode.SRV_CANCEL.asInt()) {
            return SRV_CANCEL;
        } else if (value == IppStatusCode.SRV_NOMULTI.asInt()) {
            return SRV_NOMULTI;
        }
        throw new SpException("value [" + value
                + "] can not be converted to enum");
    }

}
