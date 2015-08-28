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
package org.savapage.core.ipp.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.savapage.core.ipp.IppResponseHeader;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streaming parser for IPP content.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class IppContentParser {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IppContentParser.class);

    private static final String TRACE_SEP = "+---------------------------"
            + "-------------------------------------------+";

    /**
     * <pre>
     * -----------------------------------------------
     * | version-number (2 bytes - required)
     * -----------------------------------------------
     *
     * -----------------------------------------------
     * | operation-id (request) or status-code (response)
     * | (2 bytes - required)
     * -----------------------------------------------
     *
     * -----------------------------------------------
     * | request-id (4 bytes - required)
     * -----------------------------------------------
     * </pre>
     */
    private final static int N_BYTES_HEADER = 8;

    /**
     * <pre>
     *    -----------------------------------------------
     *    |                   value-tag                 |   1 byte
     *    -----------------------------------------------
     *    |               name-length  (value is u)     |   2 bytes
     *    -----------------------------------------------
     *    |                     name                    |   u bytes
     *    -----------------------------------------------
     *    |              value-length  (value is v)     |   2 bytes
     *    -----------------------------------------------
     *    |                     value                   |   v bytes
     *    -----------------------------------------------
     * </pre>
     */
    private final static int N_BYTES_FIELD_LENGTH = 2;

    /**
     * Number of bytes counted of 'name-length' or 'value-length' field.
     */
    private int iBytesFieldLength;

    /**
     * Collected bytes of 'name-length' or 'value-length' field.
     */
    private final byte[] bytesFieldLength = new byte[N_BYTES_FIELD_LENGTH];

    /**
     * Length of 'name' or 'value' field.
     */
    private int nBytesField;

    /**
     * Number of bytes counted of 'name' or 'value' field.
     */
    private int iBytesField;

    /**
     * Collected bytes of 'name' or 'value' field.
     */
    private byte[] bytesField;

    /**
     *
     */
    enum StateEnum {
        /**
         *
         */
        HEADER,
        /**
         *
         */
        ATTR_GROUP,
        /**
         *
         */
        ATTR_VALUE_TAG,
        /**
         *
         */
        ATTR_NAME_LENGTH,
        /**
         *
         */
        ATTR_NAME,
        /**
         *
         */
        ATTR_VALUE_LENGTH,
        /**
         *
         */
        ATTR_VALUE,
        /**
         *
         */
        END_OF_ATTR
    }

    private StateEnum state = StateEnum.HEADER;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     *
     * @throws Exception
     */
    private void onHeader() throws Exception {

        final InputStream istr = new ByteArrayInputStream(buffer.toByteArray());
        final IppResponseHeader header = new IppResponseHeader();

        header.read(istr);

        if (LOGGER.isTraceEnabled()) {

            Writer traceLog = new StringWriter();
            traceLog.write("\n");
            traceLog.write(TRACE_SEP);
            traceLog.write("\n| Status [" + header.getStatusCode() + "] IPP "
                    + header.getVersionMajor() + "." + header.getVersionMinor()
                    + " request-id [" + header.getRequestId() + "]\n");
            traceLog.write(TRACE_SEP);
            LOGGER.trace(traceLog.toString());
        }

        onHeader(header);
    }

    /**
     *
     * @param group
     * @throws Exception
     */
    private void onGroup(byte[] group) throws Exception {

        Writer traceLog = null;

        if (LOGGER.isTraceEnabled()) {
            traceLog = new StringWriter();
        }

        final InputStream istr = new ByteArrayInputStream(group);
        final List<IppAttrGroup> groups =
                IppEncoder.readAttributes(istr, traceLog);

        if (traceLog != null && LOGGER.isTraceEnabled()) {
            LOGGER.trace(traceLog.toString());
        }

        onGroup(groups.get(0));
    }

    abstract protected void onContentEnd();

    abstract protected void onHeader(IppResponseHeader responseHeader)
            throws Exception;

    abstract protected void onGroup(IppAttrGroup group) throws Exception;

    abstract protected void onException(Exception e);

    /**
     * @throws Exception
     *
     * @param bytes
     * @throws
     */
    public void parse(byte[] bytes) {
        try {
            read(bytes);
        } catch (Exception e) {
            onException(e);
        }
    }

    /**
     * @throws Exception
     *
     * @param bytes
     * @throws
     */
    public void read(byte[] bytes) throws Exception {

        StateEnum stateNext;

        /*
         * Byte by byte...
         */
        for (byte theByte : bytes) {

            /*
             * When expecting an ATTR_VALUE_TAG, we may encounter an ATTR_GROUP
             * or an END_OF_ATTR: so we correct the expected state according to
             * the actual byte.
             */
            if (state == StateEnum.ATTR_VALUE_TAG
                    && IppDelimiterTag.isValidEnum(theByte)) {
                /*
                 * Flush the previous group (if present).
                 */
                if (buffer.size() > 1) {
                    onGroup(buffer.toByteArray());
                    buffer.reset();
                }
                state = StateEnum.ATTR_GROUP;
            }

            buffer.write(theByte);

            /*
             * Determine next state.
             */
            stateNext = state;

            switch (state) {

            case HEADER:

                if (buffer.size() == N_BYTES_HEADER) {
                    onHeader();
                    buffer.reset();
                    stateNext = StateEnum.ATTR_GROUP;
                }
                break;

            case ATTR_GROUP:

                if (theByte == IppDelimiterTag.END_OF_ATTR.asInt()) {
                    stateNext = StateEnum.END_OF_ATTR;
                    onContentEnd();
                } else {
                    stateNext = StateEnum.ATTR_VALUE_TAG;
                }
                break;

            case ATTR_VALUE_TAG:

                stateNext = StateEnum.ATTR_NAME_LENGTH;
                iBytesFieldLength = 0;
                break;

            case ATTR_NAME_LENGTH:
            case ATTR_VALUE_LENGTH:

                bytesFieldLength[iBytesFieldLength] = theByte;
                iBytesFieldLength++;

                if (iBytesFieldLength == N_BYTES_FIELD_LENGTH) {

                    nBytesField =
                            IppEncoder.readInt16(bytesFieldLength[0],
                                    bytesFieldLength[1]);

                    if (state == StateEnum.ATTR_NAME_LENGTH) {
                        /*
                         * additional-value?
                         */
                        if (nBytesField == 0) {
                            stateNext = StateEnum.ATTR_VALUE_LENGTH;
                            iBytesFieldLength = 0;
                        } else {
                            stateNext = StateEnum.ATTR_NAME;
                        }

                    } else {
                        /*
                         * no value?
                         */
                        if (nBytesField == 0) {
                            stateNext = StateEnum.ATTR_VALUE_TAG;
                        } else {
                            stateNext = StateEnum.ATTR_VALUE;
                        }
                    }

                    /*
                     * Beware of an additional-value, which has name-length
                     * zero.
                     */
                    if (nBytesField > 0) {
                        iBytesField = 0;
                        bytesField = new byte[nBytesField];
                    }

                }
                break;

            case ATTR_NAME:
            case ATTR_VALUE:

                bytesField[iBytesField] = theByte;
                iBytesField++;

                if (iBytesField == nBytesField) {

                    if (state == StateEnum.ATTR_NAME) {
                        stateNext = StateEnum.ATTR_VALUE_LENGTH;
                    } else {
                        stateNext = StateEnum.ATTR_VALUE_TAG;
                    }

                    iBytesFieldLength = 0;
                }

            } // end-of-switch

            state = stateNext;
        }

    }
}
