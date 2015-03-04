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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.ipp.attribute.AbstractIppDict;
import org.savapage.core.ipp.attribute.IppAttr;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrCollectionSet;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.IppDictEventNotificationAttr;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.attribute.IppDictPrinterDescAttr;
import org.savapage.core.ipp.attribute.IppDictSubscriptionAttr;
import org.savapage.core.ipp.attribute.syntax.AbstractIppAttrSyntax;
import org.savapage.core.ipp.attribute.syntax.IppDateTime;
import org.savapage.core.ipp.attribute.syntax.IppOctetString;
import org.savapage.core.ipp.attribute.syntax.IppRangeOfInteger;
import org.savapage.core.ipp.attribute.syntax.IppResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPP encoder implementing:
 * <ul>
 * <li><a href="http://tools.ietf.org/html/rfc2910">RFC2910</a> (IPP/1.1:
 * Encoding and Transport)</li>
 * <li><a href="http://tools.ietf.org/html/rfc3382">RFC3382</a> ('collection'
 * attribute syntax)</li>
 * </ul>
 *
 * @author Datraverse B.V.
 */
public final class IppEncoder {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(IppEncoder.class);

    /**
     *
     */
    private static final String INDENT_UNIT = "  ";

    /**
     *
     *
     */
    private enum AttrCollectionType {
        /**
         *
         */
        MAIN,

        /**
         *
         */
        NESTED,

        /**
         *
         */
        FIRST_OF_SET,

        /**
         *
         */
        NEXT_OF_SET,
    }

    /**
     *
     */
    private IppEncoder() {

    }

    /**
     * Format an int as a hex string.
     *
     * @param i
     *            The int to format.
     * @param width
     *            The width of the string
     * @return The formatted int.
     */
    private static String hexInt(final int i, final String width) {
        return String.format("0x%0" + width + "X", i);
    }

    /**
     * SIGNED-BYTE.
     *
     * @param i
     * @return
     */
    public static String hexInt8(final int i) {
        return hexInt(i, "2");
    }

    /**
     * SIGNED-SHORT.
     *
     * @param i
     * @return
     */
    public static String hexInt16(final int i) {
        return hexInt(i, "4");
    }

    /**
     * SIGNED-INTEGER.
     *
     * @param i
     * @return
     */
    public static String hexInt32(final int i) {
        return hexInt(i, "8");
    }

    /**
     * Reads the value of a value tag from the input stream.
     *
     * @param valueTag
     *            The value tag.
     * @param istr
     *            The input stream.
     * @param nBytes
     *            Number of bytes to read (length of the value).
     * @param charset
     *            The character set of the resulting string.
     * @return The value or {@code null} when value tag cannot be handled (is
     *         unknown).
     * @throws IOException
     *             When read error.
     */
    private static String readValueTagValue(final IppValueTag valueTag,
            final InputStream istr, int nBytes, final Charset charset)
            throws IOException {

        String str = null;
        byte[] bytes = null;

        switch (valueTag) {
        /*
         * out-of-band values
         */
        case UNSUPPORTED:
        case UNKNOWN:
        case NONE:
            str = "";
            break;
        /*
             *
             */
        case BEGCOLLECTION:
        case ENDCOLLECTION:
            str = "";
            break;

        case BOOLEAN:
            str = String.valueOf(istr.read());
            break;

        case INTEGER:
        case ENUM:
            str = String.valueOf(readInt32(istr));
            break;

        case INTRANGE:
            str = IppRangeOfInteger.format(readInt32(istr), // min
                    readInt32(istr) // max
                    );
            break;

        case DATETIME:
            bytes = new byte[nBytes];
            istr.read(bytes);
            str = IppDateTime.read(bytes);
            break;

        case RESOLUTION:
            bytes = new byte[nBytes];
            istr.read(bytes);
            str = IppResolution.read(bytes);
            break;

        case TEXTWLANG:
        case NAMEWLANG:
        case TEXTWOLANG:
        case NAMEWOLANG:
        case KEYWORD:
        case URI:
        case URISCHEME:
        case CHARSET:
        case NATULANG:
        case MIMETYPE:
        case MEMBERATTRNAME:

            bytes = new byte[nBytes];
            istr.read(bytes);
            str = new String(bytes, charset);

            break;

        case OCTETSTRING:

            bytes = new byte[nBytes];
            istr.read(bytes);
            str = IppOctetString.read(bytes);

            break;

        default:
            /*
             * Eat the bytes!
             */
            bytes = new byte[nBytes];
            istr.read(bytes);

            LOGGER.debug("IPP value tag [" + valueTag.toString()
                    + "] is NOT implemented");
        }

        return str;
    }

    /**
     * 16-bit (2 bytes).
     *
     * @param buf
     * @return
     * @throws IOException
     */
    public static int readInt16(final InputStream istr) throws IOException {
        return readInt(istr, 2);
    }

    /**
     * 32-bit (4 bytes).
     *
     * @param buf
     * @return
     * @throws IOException
     */
    public static int readInt32(final InputStream istr) throws IOException {
        return readInt(istr, 4);
    }

    /**
     *
     * @param buf
     * @param nBytes
     * @return
     * @throws IOException
     */
    private static int readInt(final InputStream istr, final int nBytes)
            throws IOException {

        String strHex = "";

        for (int i = 0; i < nBytes; i++) {
            strHex += String.format("%02X", istr.read());
        }

        /*
         * A temporary fix for Mantis #394 "NumberFormatException for IPP 4-byte
         * integer 0xFFFFFFFF".
         */
        if (strHex.equalsIgnoreCase("FFFFFFFF")) {
            return -1;
        } else {
            return Integer.parseInt(strHex, 16);
        }
    }

    /**
     *
     * @param b1
     * @param b2
     * @return
     * @throws IOException
     */
    public static int readInt16(byte b1, byte b2) {
        return Integer.parseInt(String.format("%02X%02X", b1, b2), 16);
    }

    /**
     *
     * @param b1
     * @param b2
     * @return
     * @throws IOException
     */
    public static int readInt32(byte b1, byte b2, byte b3, byte b4) {
        return Integer.parseInt(
                String.format("%02X%02X%02X%02X", b1, b2, b3, b4), 16);
    }

    /**
     *
     * @param ostr
     * @param value
     * @throws IOException
     */
    public static void writeInt8(OutputStream ostr, int value)
            throws IOException {
        write(ostr, value, 1);
    }

    /**
     *
     * @param ostr
     * @param value
     * @throws IOException
     */
    public static void writeInt16(OutputStream ostr, int value)
            throws IOException {
        write(ostr, value, 2);
    }

    /**
     *
     * @param ostr
     * @param value
     * @throws IOException
     */
    public static void writeInt32(OutputStream ostr, int value)
            throws IOException {
        write(ostr, value, 4);
    }

    /**
     *
     * @param ostr
     * @param value
     * @param nBytes
     * @throws IOException
     */
    private static void write(OutputStream ostr, int value, int nBytes)
            throws IOException {
        for (byte b : asBytes(value, nBytes)) {
            ostr.write(b);
        }
    }

    /**
     * Converts an int value to a byte (octet) array representation.
     *
     * @param input
     * @param nBytes
     *            Number of byes in the output array
     * @return
     */
    private static byte[] asBytes(final int input, final int nBytes) {
        int wlk = input;
        byte[] conv = new byte[nBytes];
        int j = 0;
        for (int i = nBytes; i > 0; i--, j++) {
            if (j < 4) {
                conv[i - 1] = (byte) (wlk & 0xff);
                wlk >>= 8;
            } else {
                conv[i - 1] = 0;
            }
        }
        return conv;
    }

    /**
     *
     * @param attrGroups
     * @param ostr
     * @param charset
     * @return The tracelog.
     * @throws IOException
     */
    public static void writeAttributes(List<IppAttrGroup> attrGroups,
            final OutputStream ostr, final Charset charset, Writer traceLog)
            throws IOException {

        int nTraceLogIndent = 0;

        /*
         * Groups
         */
        for (final IppAttrGroup group : attrGroups) {

            nTraceLogIndent = 0;

            if (traceLog != null) {
                traceLog.write("\nGroup : "
                        + group.getDelimiterTag().toString());
            }
            ostr.write(group.getDelimiterTag().asInt());

            /*
             * Attributes
             */
            for (final IppAttrValue attr : group.getAttributes()) {

                IppValueTag valueTag =
                        attr.getAttribute().getSyntax().getValueTag();

                if (traceLog != null) {
                    traceLog.append("\n")
                            .append(StringUtils.repeat(INDENT_UNIT,
                                    nTraceLogIndent + 1)).append("Attr: ")
                            .append(attr.getAttribute().getKeyword())
                            .append(" - ").append(valueTag.toString());
                }

                /*
                 * Value(s)
                 */
                int i = 0;

                for (final String value : attr.getValues()) {

                    // Attribute type
                    ostr.write(valueTag.asInt());

                    if (traceLog != null) {
                        traceLog.append("\n")
                                .append(StringUtils.repeat(INDENT_UNIT,
                                        nTraceLogIndent + 2)).append("Value: ")
                                .append(value);
                    }

                    if (i == 0) {
                        // name length + name
                        final String keyword = attr.getAttribute().getKeyword();
                        writeInt16(ostr, keyword.length());
                        ostr.write(keyword.getBytes());
                    } else {
                        // length zero
                        writeInt16(ostr, 0);
                    }

                    attr.write(ostr, value, charset);

                    i++;
                }
            }

            /*
             * Collections
             */
            for (final IppAttrCollection collection : group.getCollections()) {

                writeCollection(collection, AttrCollectionType.MAIN, ostr,
                        charset, traceLog, nTraceLogIndent + 1);

            }

        }
    }

    /**
     *
     * @param collection
     * @param isNested
     * @param ostr
     * @param charset
     * @param traceLog
     * @param nTraceLogIndent
     * @throws IOException
     */
    private static void writeCollection(final IppAttrCollection collection,
            final AttrCollectionType attrCollectionType,
            final OutputStream ostr, final Charset charset,
            final Writer traceLog, final int nTraceLogIndent)
            throws IOException {

        final String collectionName = collection.getKeyword();

        if (collection.isSetOfCollections()) {

            boolean isFirst = true;

            for (final IppAttrCollection memberCollection : collection
                    .getCollections()) {

                final AttrCollectionType collectionSetType;

                if (isFirst) {
                    collectionSetType = AttrCollectionType.FIRST_OF_SET;
                    isFirst = false;
                } else {
                    collectionSetType = AttrCollectionType.NEXT_OF_SET;
                }

                // Recurse.
                writeCollection(memberCollection, collectionSetType, ostr,
                        charset, traceLog, nTraceLogIndent + 1);
            }

        } else {

            if (attrCollectionType == AttrCollectionType.NESTED) {

                /*
                 * The nested collection member.
                 */
                ostr.write(IppValueTag.MEMBERATTRNAME.asInt());

                // Name (void).
                writeInt16(ostr, 0);

                // Value.
                writeInt16(ostr, collectionName.length());
                ostr.write(collectionName.getBytes());

                if (traceLog != null) {
                    traceLog.append("\n")
                            .append(StringUtils
                                    .repeat(INDENT_UNIT, nTraceLogIndent))
                            .append("[")
                            .append(IppValueTag.MEMBERATTRNAME.toString())
                            .append("] ").append(collectionName);
                }
            }

            /*
             * Begin collection.
             */
            ostr.write(IppValueTag.BEGCOLLECTION.asInt());

            final String collectionNameWlk;

            switch (attrCollectionType) {

            case MAIN:
                // no break intended.
            case FIRST_OF_SET:
                collectionNameWlk = collectionName;
                break;

            case NESTED:
                // no break intended.
            case NEXT_OF_SET:
                collectionNameWlk = "";
                break;

            default:
                throw new SpException("Unhandled value [" + attrCollectionType
                        + "] for enum ["
                        + AttrCollectionType.class.getSimpleName() + "]");
            }

            // Name.
            writeInt16(ostr, collectionNameWlk.length());
            ostr.write(collectionNameWlk.getBytes());

            // Value (void).
            writeInt16(ostr, 0);

            //
            if (traceLog != null) {
                traceLog.append("\n")
                        .append(StringUtils
                                .repeat(INDENT_UNIT, nTraceLogIndent))
                        .append("[")
                        .append(IppValueTag.BEGCOLLECTION.toString())
                        .append("] ").append(collectionNameWlk);
            }

            /*
             * Members
             */
            for (final IppAttrValue member : collection.getAttributes()) {

                final String memberName = member.getAttribute().getKeyword();

                /*
                 * The member.
                 */
                ostr.write(IppValueTag.MEMBERATTRNAME.asInt());

                // Name (void).
                writeInt16(ostr, 0);

                // Value.
                writeInt16(ostr, memberName.length());
                ostr.write(memberName.getBytes());

                /*
                 * The values
                 */
                final AbstractIppAttrSyntax memberSyntax =
                        member.getAttribute().getSyntax();

                if (traceLog != null) {
                    traceLog.append("\n")
                            .append(StringUtils.repeat(INDENT_UNIT,
                                    nTraceLogIndent + 1)).append("[")
                            .append(IppValueTag.MEMBERATTRNAME.toString())
                            .append("] ").append(memberName);
                }

                for (final String value : member.getValues()) {

                    // Value tag
                    ostr.write(memberSyntax.getValueTag().asInt());

                    // Name (void).
                    writeInt16(ostr, 0);

                    // Value
                    memberSyntax.write(ostr, value, charset);

                    if (traceLog != null) {
                        traceLog.append("\n")
                                .append(StringUtils.repeat(INDENT_UNIT,
                                        nTraceLogIndent + 2)).append("Value: ")
                                .append(value).append(" [")
                                .append(memberSyntax.getValueTag().toString())
                                .append("]");
                    }
                }

            }

            /*
             * Nested collections.
             */
            for (final IppAttrCollection nestedCollection : collection
                    .getCollections()) {
                // Recurse.
                writeCollection(nestedCollection, AttrCollectionType.NESTED,
                        ostr, charset, traceLog, nTraceLogIndent + 1);
            }

            /*
             * End collection.
             */
            ostr.write(IppValueTag.ENDCOLLECTION.asInt());

            // Name (void).
            writeInt16(ostr, 0);

            // Value (void).
            writeInt16(ostr, 0);

            //
            if (traceLog != null) {
                traceLog.append("\n")
                        .append(StringUtils
                                .repeat(INDENT_UNIT, nTraceLogIndent))
                        .append("[")
                        .append(IppValueTag.ENDCOLLECTION.toString())
                        .append("]");
            }

        }
    }

    /**
     * Generic read of attributes from an input stream, returning a dictionary
     * of attribute names with their value, and a list of attribute groups.
     *
     * @param istr
     *            The input stream.
     * @param attrGroups
     *            The list of attribute groups.
     * @param traceLog
     *            The tracelog.
     * @return The attributes per group.
     *
     * @throws IOException
     *             When reading from the stream goes wrong.
     */
    public static List<IppAttrGroup> readAttributes(final InputStream istr,
            Writer traceLog) throws IOException {

        List<IppAttrGroup> attrGroups = new ArrayList<>();

        /**
         * <pre>
         *    -----------------------------------------------
         *    |                 attribute-group             |   n bytes - 0 or more
         *    -----------------------------------------------
         *    |              end-of-attributes-tag          |   1 byte   - required
         *    -----------------------------------------------
         * </pre>
         *
         * The "attribute-group" field occurs 0 or more times.
         *
         * Each "attribute-group" field represents a single group of attributes,
         * such as an Operation Attributes group or a Job Attributes group (see
         * the Model document). The IPP model document specifies the required
         * attribute groups and their order for each operation request and
         * response.
         *
         * The "end-of-attributes-tag" field is always present.
         *
         * Each "attribute-group" field is encoded as follows:
         *
         * <pre>
         *    -----------------------------------------------
         *    |           begin-attribute-group-tag         |  1 byte
         *    ----------------------------------------------------------
         *    |                   attribute                 |  p bytes |- 0 or more
         *    ----------------------------------------------------------
         * </pre>
         *
         * The "begin-attribute-group-tag" field marks the beginning of an
         * "attribute-group" field and its value identifies the type of
         * attribute group, e.g. Operations Attributes group versus a Job
         * Attributes group. The "begin-attribute-group-tag" field also marks
         * the end of the previous attribute group except for the "begin-
         * attribute-group-tag" field in the first "attribute-group" field of a
         * request or response. The "begin-attribute-group-tag" field acts as an
         * "attribute-group" terminator because an "attribute-group" field
         * cannot nest inside another "attribute-group" field.
         *
         * An "attribute-group" field contains zero or more "attribute" fields.
         *
         * Note, the values of the "begin-attribute-group-tag" field and the
         * "end-of-attributes-tag" field are called "delimiter-tags".
         *
         * An "attribute" field is encoded as follows:
         *
         * <pre>
         *    -----------------------------------------------
         *    |          attribute-with-one-value           |  q bytes
         *    ----------------------------------------------------------
         *    |             additional-value                |  r bytes |- 0 or more
         *    ----------------------------------------------------------
         * </pre>
         *
         * When an attribute is single valued (e.g. "copies" with value of 10)
         * or multi-valued with one value (e.g. "sides-supported" with just the
         * value 'one-sided') it is encoded with just an "attribute-with-one-
         * value" field. When an attribute is multi-valued with n values (e.g.
         * "sides-supported" with the values 'one-sided' and 'two-sided-long-
         * edge'), it is encoded with an "attribute-with-one-value" field
         * followed by n-1 "additional-value" fields.
         *
         * Each "attribute-with-one-value" field is encoded as follows:
         *
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
         *
         * An "attribute-with-one-value" field is encoded with five subfields:
         *
         * The "value-tag" field specifies the attribute syntax, e.g. 0x44 for
         * the attribute syntax 'keyword'.
         *
         * The "name-length" field specifies the length of the "name" field in
         * bytes, e.g. u in the above diagram or 15 for the name "sides-
         * supported".
         *
         * The "name" field contains the textual name of the attribute, e.g.
         * "sides-supported".
         *
         * The "value-length" field specifies the length of the "value" field in
         * bytes, e.g. v in the above diagram or 9 for the (keyword) value
         * 'one-sided'.
         *
         * The "value" field contains the value of the attribute, e.g. the
         * textual value 'one-sided'.
         *
         * Each "additional-value" field is encoded as follows:
         *
         * <pre>
         *    -----------------------------------------------
         *    |                   value-tag                 |   1 byte
         *    -----------------------------------------------
         *    |            name-length  (value is 0x0000)   |   2 bytes
         *    -----------------------------------------------
         *    |              value-length (value is w)      |   2 bytes
         *    -----------------------------------------------
         *    |                     value                   |   w bytes
         *    -----------------------------------------------
         * </pre>
         *
         * An "additional-value" is encoded with four subfields:
         *
         * The "value-tag" field specifies the attribute syntax, e.g. 0x44 for
         * the attribute syntax 'keyword'.
         *
         * The "name-length" field has the value of 0 in order to signify that
         * it is an "additional-value". The value of the "name-length" field
         * distinguishes an "additional-value" field ("name-length" is 0) from
         * an "attribute-with-one-value" field ("name-length" is not 0).
         *
         * The "value-length" field specifies the length of the "value" field in
         * bytes, e.g. w in the above diagram or 19 for the (keyword) value
         * 'two-sided-long-edge'.
         *
         * The "value" field contains the value of the attribute, e.g. the
         * textual value 'two-sided-long-edge'.
         */

        Charset myCharset = Charset.forName("US-ASCII");

        IppAttrValue attrWlk = null;

        final Stack<IppAttrCollection> collectionStack = new Stack<>();

        int chWlk = istr.read();

        int nTraceLogIndent = 0;

        while (chWlk > -1 && chWlk != IppDelimiterTag.END_OF_ATTR.asInt()) {

            IppDelimiterTag delimiterTag = IppDelimiterTag.asEnum(chWlk);

            if (traceLog != null) {
                traceLog.write("\nGroup: " + delimiterTag.toString());
                nTraceLogIndent = 0;
            }

            IppAttrGroup groupWlk = new IppAttrGroup(delimiterTag);
            attrGroups.add(groupWlk);

            IppAttrCollection collectionWlk = null;
            IppAttrCollection simpleCollectionWlk = null;
            int setOfCollectionWlkSize = 0;

            IppValueTag valueTagPrev = null;

            String collectionNameWlk = null;
            String collectionMemberNameWlk = null;
            IppAttrValue collectionMemberValueWlk = null;

            /*
             * Zero or more "attribute" fields.
             */
            chWlk = istr.read();

            while (chWlk > -1 && !IppDelimiterTag.isReservedForFutureUse(chWlk)
                    && chWlk != IppDelimiterTag.END_OF_ATTR.asInt()
                    && chWlk != IppDelimiterTag.OPERATION_ATTR.asInt()
                    && chWlk != IppDelimiterTag.SUBSCRIPTION_ATTR.asInt()
                    && chWlk != IppDelimiterTag.EVENT_NOTIFICATION_ATTR.asInt()
                    && chWlk != IppDelimiterTag.PRINTER_ATTR.asInt()
                    && chWlk != IppDelimiterTag.JOB_ATTR.asInt()) {

                // -----------------------------------------------
                // | value-tag | 1 byte
                // -----------------------------------------------
                final IppValueTag valueTag = IppValueTag.asEnum(chWlk);

                // -----------------------------------------------
                // | name-length (value is u) | 2 bytes
                // -----------------------------------------------
                final int lengthName = IppEncoder.readInt16(istr);

                final boolean isAdditionalValue = (lengthName == 0);

                // -----------------------------------------------
                // | name | u bytes
                // -----------------------------------------------
                final String name =
                        readValueTagValue(IppValueTag.KEYWORD, istr,
                                lengthName, myCharset);

                // -----------------------------------------------
                // | value-length (value is v) | 2 bytes
                // -----------------------------------------------
                final int lengthValue = IppEncoder.readInt16(istr);

                // -----------------------------------------------
                // | value | v bytes
                // -----------------------------------------------
                final String value =
                        readValueTagValue(valueTag, istr, lengthValue,
                                myCharset);

                /*
                 * Collection?
                 */
                if (valueTag == IppValueTag.BEGCOLLECTION
                        || valueTag == IppValueTag.ENDCOLLECTION
                        || valueTag == IppValueTag.MEMBERATTRNAME) {

                    switch (valueTag) {

                    case BEGCOLLECTION:

                        // See sample at RFC3382 Appendix A
                        boolean isSimpleCollection =
                                StringUtils.isNotBlank(name);

                        // See sample at RFC3382 Table 4
                        boolean isNestedCollection =
                                valueTagPrev == IppValueTag.MEMBERATTRNAME
                                        && StringUtils.isBlank(name)
                                        && StringUtils.isBlank(value);

                        // See sample at RFC3382 Appendix B
                        boolean is1SetOfCollection =
                                valueTagPrev == IppValueTag.ENDCOLLECTION
                                        && StringUtils.isBlank(name)
                                        && StringUtils.isBlank(value);

                        if (is1SetOfCollection) {

                            /*
                             * At this point we know we are dealing with
                             * "1setOf Collection". Therefore, we must correct
                             * our earlier assumption of a "Simple Collection".
                             */
                            if (setOfCollectionWlkSize == 0) {

                                /*
                                 * Create a new collection to replace the old
                                 * one ...
                                 */
                                final IppAttrCollection collectionNew =
                                        new IppAttrCollectionSet(name);

                                /*
                                 * ... move the attributes and collections ...
                                 */
                                collectionNew.setAttributes(simpleCollectionWlk
                                        .getAttributes());

                                collectionNew
                                        .setCollections(simpleCollectionWlk
                                                .getCollections());

                                simpleCollectionWlk.resetAttributes();
                                simpleCollectionWlk.resetCollections();

                                /*
                                 * ... and add the new collection.
                                 */
                                simpleCollectionWlk
                                        .addCollection(collectionNew);

                                /*
                                 * ... re-push the corrected 1setof container on
                                 * the stack again.
                                 */
                                collectionStack.push(simpleCollectionWlk);
                            }

                            setOfCollectionWlkSize++;

                            /*
                             * Create a new collection to work with.
                             */
                            collectionWlk = new IppAttrCollection(name);

                            /*
                             * ... append to the 1setof collection ...
                             */
                            simpleCollectionWlk.addCollection(collectionWlk);

                            /*
                             * ... push the new container on the stack.
                             */
                            collectionStack.push(collectionWlk);

                        } else {

                            if (isSimpleCollection) {

                                setOfCollectionWlkSize = 0;
                                collectionNameWlk = name;

                            } else {
                                collectionNameWlk = collectionMemberNameWlk;
                            }

                            final IppAttrCollection collectionNew =
                                    new IppAttrCollection(collectionNameWlk);

                            if (collectionWlk == null) {
                                groupWlk.addCollection(collectionNew);
                            } else {
                                collectionWlk.addCollection(collectionNew);
                            }

                            collectionWlk = collectionStack.push(collectionNew);

                            if (isSimpleCollection) {
                                simpleCollectionWlk = collectionWlk;
                            }

                        }

                        break;

                    case ENDCOLLECTION:

                        collectionStack.pop();

                        if (collectionStack.isEmpty()) {
                            collectionWlk = null;
                            collectionNameWlk = null;
                        } else {
                            collectionWlk = collectionStack.peek();
                            collectionNameWlk = collectionWlk.getKeyword();
                        }

                        collectionMemberNameWlk = null;

                        break;

                    case MEMBERATTRNAME:

                        collectionMemberNameWlk = value;
                        collectionMemberValueWlk = null;

                        break;

                    default:
                        break;
                    }

                    if (traceLog != null) {

                        if (valueTag == IppValueTag.BEGCOLLECTION) {
                            nTraceLogIndent++;
                        } else if (valueTag == IppValueTag.ENDCOLLECTION) {
                            nTraceLogIndent--;
                        }

                        traceLog.append("\n")
                                .append(StringUtils.repeat(INDENT_UNIT,
                                        nTraceLogIndent))
                                .append(delimiterTag.toString()).append(" : ");

                        if (valueTag == IppValueTag.BEGCOLLECTION) {

                            traceLog.append(collectionNameWlk);

                        } else if (valueTag == IppValueTag.MEMBERATTRNAME) {

                            traceLog.append(collectionMemberNameWlk);
                        }

                        traceLog.append(" [").append(valueTag.toString())
                                .append("]");

                        if (valueTag == IppValueTag.BEGCOLLECTION) {
                            nTraceLogIndent++;
                        } else if (valueTag == IppValueTag.ENDCOLLECTION) {
                            nTraceLogIndent--;
                            traceLog.append("\n");
                        }
                    }

                } else {

                    if (collectionMemberNameWlk != null) {

                        /*
                         * Lazy initialize/add collection attribute value.
                         */
                        if (collectionMemberValueWlk == null) {

                            final IppAttr attr =
                                    AbstractIppDict.createAttr(
                                            collectionMemberNameWlk, valueTag);

                            collectionMemberValueWlk = new IppAttrValue(attr);

                            collectionWlk
                                    .addAttribute(collectionMemberValueWlk);
                        }

                        /*
                         * .. and add the value.
                         */
                        collectionMemberValueWlk.addValue(value);

                        if (traceLog != null) {
                            traceLog.append("\n")
                                    .append(StringUtils.repeat(INDENT_UNIT,
                                            nTraceLogIndent + 1))
                                    .append("Value: ").append(value);
                        }

                    } else {

                        if (!isAdditionalValue) {

                            IppAttr ippAttr;

                            switch (delimiterTag) {

                            case OPERATION_ATTR:
                                ippAttr =
                                        IppDictOperationAttr.instance()
                                                .getAttr(name, valueTag);
                                if (name.equals(IppDictOperationAttr.ATTR_ATTRIBUTES_CHARSET)) {
                                    myCharset =
                                            Charset.forName(value.toUpperCase());
                                }
                                break;

                            case PRINTER_ATTR:
                                ippAttr =
                                        IppDictPrinterDescAttr.instance()
                                                .getAttr(name, valueTag);

                                if (ippAttr == null) {
                                    ippAttr =
                                            IppDictJobTemplateAttr.instance()
                                                    .getAttr(name);
                                }

                                break;

                            case JOB_ATTR:
                                ippAttr =
                                        IppDictJobDescAttr.instance().getAttr(
                                                name, valueTag);
                                break;

                            case SUBSCRIPTION_ATTR:
                                ippAttr =
                                        IppDictSubscriptionAttr.instance()
                                                .getAttr(name, valueTag);
                                break;

                            case EVENT_NOTIFICATION_ATTR:
                                ippAttr =
                                        IppDictEventNotificationAttr.instance()
                                                .getAttr(name, valueTag);
                                break;

                            default:
                                ippAttr = null;
                                break;
                            }

                            /*
                             * Create ad-hoc.
                             */
                            if (ippAttr == null) {

                                ippAttr =
                                        AbstractIppDict.createAttr(name,
                                                valueTag);

                                if (ippAttr == null) {
                                    LOGGER.error("Keyword [" + name + "] ["
                                            + valueTag.toString()
                                            + "] is NOT supported");
                                }

                            }

                            if (traceLog != null) {
                                traceLog.write("\n" + INDENT_UNIT
                                        + delimiterTag + " : " + name + " ["
                                        + valueTag.toString() + "]");
                            }

                            if (ippAttr == null) {

                                attrWlk = null;

                                if (traceLog != null) {
                                    traceLog.write(" [*IGNORED*]");
                                }

                            } else {

                                attrWlk = new IppAttrValue(ippAttr);
                                groupWlk.addAttribute(attrWlk);
                            }
                        }

                        /*
                         * Add the value to the attribute.
                         */
                        if (traceLog != null) {
                            traceLog.write("\n    Value: " + value);
                        }

                        if (attrWlk != null) {
                            attrWlk.addValue(value);
                        }

                    }

                }

                // save the value tag
                valueTagPrev = valueTag;

                // read next byte
                chWlk = istr.read();
            }
        }

        return attrGroups;
    }
}
