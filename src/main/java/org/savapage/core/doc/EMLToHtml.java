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
package org.savapage.core.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.services.helpers.email.EMailConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

/**
 * Create an HTML file from {@link DocContentTypeEnum#EML}.
 * <p>
 * A refactored version of parts from Nick Russler's
 * <a href= "https://github.com/nickrussler/email-to-pdf-converter">code </a>
 * (Apache V2 License).
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class EMLToHtml implements IStreamConverter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EMLToHtml.class);

    /** */
    private static final String CHARSET_NAME_UTF_8 = "utf-8";

    /** */
    private static final String MIME_TYPE_TEXT_PLAIN =
            MimeTypeEnum.TEXT_PLAIN.getWord();
    /** */
    private static final String MIME_TYPE_TEXT_HTML =
            MimeTypeEnum.TEXT_HTML.getWord();

    /** */
    private static final String MIME_SUBTYPE_ALL = "*";

    /** */
    private static final String MIME_TYPE_IMAGE = "image";

    /** */
    private static final String MIME_TYPE_MULTIPART =
            EMailConstants.MIME_TYPE_MULTIPART;

    /** */
    private static final String MIME_HEADER_NAME_CONTENT_ID =
            EMailConstants.MIME_HEADER_NAME_CONTENT_ID;

    /** */
    private static final Pattern PATTERN_IMG_CID_HTML =
            Pattern.compile("cid:(.*?)\"", Pattern.DOTALL);

    /** */
    private static final Pattern PATTERN_IMG_CID_PLAIN =
            Pattern.compile("\\[cid:(.*?)\\]", Pattern.DOTALL);

    /** */
    private static final Pattern PATTERN_HTML_META_CHARSET = Pattern.compile(
            "(<meta(?!\\s*(?:name|value)\\s*=)"
                    + "[^>]*?charset\\s*=[\\s\"']*)([^\\s\"'/>]*)",
            Pattern.DOTALL);

    /** */
    private static final String HTML_TEXT_PLAIN_FONT_SIZE = "12pt";

    /**
     * HTML String format for text/plain. "text-indent: 0px;" is needed to get
     * straight left-side alignment in {@link HtmlToPdf}.
     */
    private static final String HTML_TEXT_PLAIN_WRAPPER_FORMAT =
            "<!DOCTYPE html>" //
                    + "<html><head>" //
                    + "<style>body{font-size: %s;text-indent: 0px;}</style>" //
                    + "<meta charset=\"%s\">" //
                    + "<title>email</title></head>" //
                    + "<body>%s</body></html>";

    /**
     * MIME part processor.
     */
    public interface MimePartProcessor {
        /**
         * Processes a MIMI part.
         *
         * @param part
         *            MIMI part.
         * @param depth
         *            Depth in part tree.
         * @throws Exception
         */
        void process(Part part, int depth) throws Exception;
    }

    /**
     * Replace processor.
     */
    public interface ReplaceProcessor {
        /**
         * Replaces a match with a new string value.
         *
         * @param match
         *            The match.
         * @return replacement.
         * @throws Exception
         */
        String replace(Matcher match) throws Exception;
    }

    /**
     * Wrapper class of a MIME object.
     *
     * @param <T>
     *            Generic type of the wrapped object.
     */
    private static class MimeObjectWrapper<T> {
        /** */
        private T object;
        /** */
        private ContentType contentType;

        MimeObjectWrapper(final T obj, final ContentType content) {
            this.object = obj;
            this.contentType = content;
        }

        public T getObject() {
            return object;
        }

        public void setObject(final T obj) {
            this.object = obj;
        }

        public ContentType getContentType() {
            return contentType;
        }

        public void setContentType(final ContentType content) {
            this.contentType = content;
        }
    }

    @Override
    public long convert(final DocContentTypeEnum contentType,
            final DocInputStream istrDoc, final OutputStream ostrHtml)
            throws Exception {

        final MimeMessage message = new MimeMessage(null, istrDoc);

        final MimeObjectWrapper<String> bodyWrapper = collectBodyPart(message);

        final String charsetName =
                bodyWrapper.getContentType().getParameter("charset");

        //
        final Map<String, MimeObjectWrapper<String>> inlineImageMap =
                collectInlineImages(message);

        String htmlBody = bodyWrapper.getObject();

        if (bodyWrapper.getContentType().match(MIME_TYPE_TEXT_HTML)) {

            if (inlineImageMap.size() > 0) {
                htmlBody = applyInlineImageMapHtml(htmlBody, inlineImageMap);
            }

            htmlBody = applyCharset(htmlBody, charsetName);

        } else {
            /*
             * Embed text/plain into HTML.
             */
            htmlBody = htmlBody//
                    // .replace("'", "&#39;").replace("\"", "&quot;")
                    .replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;");

            htmlBody = "<div style=\"" //
                    + "text-indent: 0px;"
                    // + "white-space: pre-wrap" //
                    + "\">" //
                    + htmlBody.replace("\n", "<br>").replace("\r", "") //
                    + "</div>";

            htmlBody = String.format(HTML_TEXT_PLAIN_WRAPPER_FORMAT,
                    HTML_TEXT_PLAIN_FONT_SIZE, charsetName, htmlBody);

            if (inlineImageMap.size() > 0) {
                htmlBody = applyInlineImageMapPlain(htmlBody, inlineImageMap);
            }
        }

        ostrHtml.write(htmlBody.getBytes());
        return istrDoc.getBytesRead();
    }

    /***
     * Processes the MIME part and recursively processes sub parts.
     *
     * @param part
     *            MIME part.
     * @param depth
     *            Recursion depth.
     * @param processor
     *            MIME part processor.
     * @throws Exception
     */
    private static void processMimePart(final Part part, final int depth,
            final MimePartProcessor processor) throws Exception {

        processor.process(part, depth);

        if (part.isMimeType(MIME_TYPE_MULTIPART + "/" + MIME_SUBTYPE_ALL)) {

            final Multipart multiPart = (Multipart) part.getContent();

            for (int i = 0; i < multiPart.getCount(); i++) {
                // recurse.
                processMimePart(multiPart.getBodyPart(i), depth + 1, processor);
            }
        }
    }

    /**
     * @param part
     *            MIME part
     * @return {@code true} if part is an inline image.
     * @throws MessagingException
     */
    public static boolean isInlineImage(final Part part)
            throws MessagingException {
        return part.isMimeType(MIME_TYPE_IMAGE + "/" + MIME_SUBTYPE_ALL)
                && part.getHeader(MIME_HEADER_NAME_CONTENT_ID) != null;
    }

    /**
     * Collects all inline images by Content-Id.
     *
     * @param message
     *            MIME message.
     * @return {@code HashMap<Content-Id, <Base64Image, ContentType>>}
     * @throws Exception
     */
    private static Map<String, MimeObjectWrapper<String>>

            collectInlineImages(final MimeMessage message) throws Exception {

        final HashMap<String, MimeObjectWrapper<String>> result =
                new HashMap<String, MimeObjectWrapper<String>>();

        processMimePart(message, 0, new MimePartProcessor() {
            @Override
            public void process(final Part part, final int depth)
                    throws Exception {

                if (isInlineImage(part)) {

                    final String id =
                            part.getHeader(MIME_HEADER_NAME_CONTENT_ID)[0];

                    final BASE64DecoderStream b64ds =
                            (BASE64DecoderStream) part.getContent();

                    try (ByteArrayOutputStream bostr =
                            new ByteArrayOutputStream()) {

                        b64ds.transferTo(bostr);

                        final String imageBase64 = new String(Base64
                                .getEncoder().encode(bostr.toByteArray()));

                        result.put(id, new MimeObjectWrapper<String>(
                                imageBase64,
                                new ContentType(part.getContentType())));
                    }

                }
            }
        });

        return result;
    }

    /**
     * Collects the main message body, preferring HTML over plain text.
     *
     * @param message
     *            MIME message.
     * @return the main message body and the corresponding contentType or an
     *         empty text/plain
     * @throws Exception
     */
    private static MimeObjectWrapper<String>
            collectBodyPart(final MimeMessage message) throws Exception {

        final MimeObjectWrapper<String> result = new MimeObjectWrapper<String>(
                "", new ContentType(MIME_TYPE_TEXT_PLAIN + "; charset=\""
                        + CHARSET_NAME_UTF_8 + "\""));

        processMimePart(message, 0, new MimePartProcessor() {

            @Override
            public void process(final Part part, final int level)
                    throws Exception {

                if (!part.isMimeType(MIME_TYPE_TEXT_PLAIN)
                        && !part.isMimeType(MIME_TYPE_TEXT_HTML)) {
                    return;
                }

                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    return;
                }

                final String stringContent = getStringContent(part);

                if (StringUtils.isBlank(stringContent)) {
                    return;
                }

                // use text/plain entries only when we found nothing before
                if (result.getObject().isEmpty()
                        || part.isMimeType(MIME_TYPE_TEXT_HTML)) {

                    result.setObject(stringContent);
                    result.setContentType(
                            new ContentType(part.getContentType()));
                }
            }
        });

        return result;
    }

    /**
     * Get the String Content of a MimePart.
     *
     * @param part
     *            MimePart
     * @return Content as String
     * @throws IOException
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    private static String getStringContent(final Part part)
            throws MessagingException, UnsupportedEncodingException,
            IOException {

        Object objectContent;

        try {
            objectContent = part.getContent();
        } catch (Exception e) {
            LOGGER.debug("getContent() exception [{}]): "
                    + "using getInputStream() instead.", e.toString());
            objectContent = part.getInputStream();
        }

        final String stringContent;

        if (objectContent instanceof String) {
            stringContent = (String) objectContent;
        } else if (objectContent instanceof InputStream) {
            stringContent =
                    new String(IOUtils.toByteArray((InputStream) objectContent),
                            CHARSET_NAME_UTF_8);
        } else {
            stringContent = null;
        }

        return stringContent;
    }

    /**
     * Replaces occurrences of the pattern by using the processor.
     *
     * @param input
     *            String to modify.
     * @param pattern
     *            {@link Pattern}.
     * @param processor
     *            Processor to replace occurrences of the pattern.
     * @return modified input.
     * @throws Exception
     */
    private static String replace(final String input, final Pattern pattern,
            final ReplaceProcessor processor) throws Exception {

        final StringBuffer resultString = new StringBuffer();
        final Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            matcher.appendReplacement(resultString,
                    Matcher.quoteReplacement(processor.replace(matcher)));
        }

        matcher.appendTail(resultString);

        return resultString.toString();
    }

    /**
     * Applies the inline CID images to the HTML body, by replacing CID
     * construct with {@code <img src=\"data:image ...>} syntax.
     *
     * @param htmlBody
     *            HTML before replacement.
     * @param inlineImageMap
     *            CID map.
     * @return HTML after replacement.
     * @throws Exception
     */
    private static String applyInlineImageMapHtml(final String htmlBody,
            final Map<String, MimeObjectWrapper<String>> inlineImageMap)
            throws Exception {

        return replace(htmlBody, PATTERN_IMG_CID_HTML, new ReplaceProcessor() {

            @Override
            public String replace(final Matcher matcher) throws Exception {

                final MimeObjectWrapper<String> base64Wrapper =
                        inlineImageMap.get("<" + matcher.group(1) + ">");

                if (base64Wrapper == null) {
                    // CID not found, return the matcher's string.
                    return matcher.group();
                }

                return "data:" + base64Wrapper.getContentType().getBaseType()
                        + ";base64," + base64Wrapper.getObject() + "\"";
            }
        });
    }

    /**
     * Applies the inline CID images to the HTML wrapper of the plain text body,
     * by replacing CID construct with {@code <img src=\"data:image ...>}
     * syntax.
     *
     * @param htmlBody
     *            HTML before replacement.
     * @param inlineImageMap
     *            CID map.
     * @return HTML after replacement.
     * @throws Exception
     */
    private static String applyInlineImageMapPlain(final String htmlBody,
            final Map<String, MimeObjectWrapper<String>> inlineImageMap)
            throws Exception {

        return replace(htmlBody, PATTERN_IMG_CID_PLAIN, new ReplaceProcessor() {

            @Override
            public String replace(final Matcher matcher) throws Exception {

                final MimeObjectWrapper<String> base64Wrapper =
                        inlineImageMap.get("<" + matcher.group(1) + ">");

                if (base64Wrapper == null) {
                    // CID not found, return the matcher's string.
                    return matcher.group();
                }

                return "<img src=\"data:"
                        + base64Wrapper.getContentType().getBaseType()
                        + ";base64," + base64Wrapper.getObject() + "\" />";
            }
        });
    }

    /**
     * Applies the email header charset by overwriting the html declared charset
     * with email header charset.
     *
     * @param htmlBody
     *            HTML before replacement.
     * @param charsetName
     *            Charset from email header.
     * @return HTML after replacement.
     * @throws Exception
     */
    private static String applyCharset(final String htmlBody,
            final String charsetName) throws Exception {

        return replace(htmlBody, PATTERN_HTML_META_CHARSET,
                new ReplaceProcessor() {

                    @Override
                    public String replace(final Matcher matcher)
                            throws Exception {

                        final String declaredCharset = matcher.group(2);

                        if (!charsetName.equalsIgnoreCase(declaredCharset)) {
                            LOGGER.debug(
                                    "Declared charset [{}] differs "
                                            + "from email header [{}].",
                                    declaredCharset, charsetName);
                        }
                        return matcher.group(1) + charsetName;
                    }
                });
    }

}
