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
package org.savapage.core.services.helpers.email;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.DataSource;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.template.email.EmailRenderResult;
import org.savapage.core.template.email.EmailStationary;
import org.savapage.core.template.email.EmailTemplateMixin;
import org.savapage.lib.pgp.PGPPublicKeyInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class EmailMsgParms {

    /**
     * HTML content.
     */
    private static final String CONTENT_TYPE_HTML = "text/html";

    /**
     * Plain text content.
     */
    private static final String CONTENT_TYPE_PLAIN = "text/plain";

    /**
     * The email address.
     */
    private String toAddress;

    /**
     * The personal name of the recipient (can be {@code null}).
     */
    private String toName;

    /**
     * The subject of the message.
     */
    private String subject;

    /**
     * The body text with optional newline {@code \n} characters.
     */
    private String body;

    /**
     * The file to attach (can be {@code null}).
     */
    private File fileAttach;

    /**
     * .
     */
    private String contentType;

    /**
     *
     */
    private final Map<String, DataSource> cidMap = new HashMap<>();

    /**
     *
     */
    private List<PGPPublicKeyInfo> publicKeyList;

    /**
     *
     * @return
     */
    public Map<String, DataSource> getCidMap() {
        return cidMap;
    }

    /**
     * The name of the attachment (can be {@code null}).
     */
    private String fileName;

    /**
     * Creates a {@link EmailMsgParms} object with Content-Type is
     * {@link EmailMsgParms#CONTENT_TYPE_PLAIN}.
     */
    public EmailMsgParms() {
        this.contentType = CONTENT_TYPE_PLAIN;
    }

    /**
     *
     * @param isHtml
     *            {@code true} when Content-Type is
     *            {@link EmailMsgParms#CONTENT_TYPE_HTML}.
     */
    public EmailMsgParms(final boolean isHtml) {
        if (isHtml) {
            this.contentType = CONTENT_TYPE_HTML;
        } else {
            this.contentType = CONTENT_TYPE_PLAIN;
        }
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public File getFileAttach() {
        return fileAttach;
    }

    public void setFileAttach(File fileAttach) {
        this.fileAttach = fileAttach;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Creates {@link EmailMsgParms}.
     *
     * @param emailAddr
     *            The email address to send the message to.
     * @param template
     *            The template.
     * @param locale
     *            The locale
     * @param asHtml
     *            If {@code true} rendered as HTML, otherwise as plain text
     * @return The {@link EmailMsgParms}.
     */
    public static EmailMsgParms create(final String emailAddr,
            final EmailTemplateMixin template, final Locale locale,
            final boolean asHtml) {

        template.render(locale, asHtml);

        final EmailRenderResult renderResult = template.render(locale, asHtml);

        final EmailMsgParms emailParms = new EmailMsgParms(asHtml);

        emailParms.setToAddress(emailAddr);
        emailParms.setBody(renderResult.getBody());
        emailParms.setSubject(renderResult.getSubject());

        if (renderResult.getCidMap() != null) {
            emailParms.getCidMap().putAll(renderResult.getCidMap().getMap());
        }

        return emailParms;
    }

    /**
     *
     * @param headerText
     * @param content
     * @param locale
     * @param asHtml
     *            If {@code true}, content is rendered as HTML, otherwise as
     *            plain text.
     */
    public void setBodyInStationary(final String headerText,
            final String content, final Locale locale, final boolean asHtml) {

        if (asHtml) {
            this.contentType = CONTENT_TYPE_HTML;
        } else {
            this.contentType = CONTENT_TYPE_PLAIN;
        }

        final EmailStationary stat = new EmailStationary(
                ConfigManager.getServerCustomEmailTemplateHome(), headerText,
                content);

        final EmailRenderResult result = stat.render(locale, asHtml);

        this.body = result.getBody();

        if (result.hasCidMap()) {
            this.cidMap.putAll(result.getCidMap().getMap());
        }
    }

    /**
     * @return The PGP public keys to encrypt with.
     */
    public List<PGPPublicKeyInfo> getPublicKeyList() {
        return publicKeyList;
    }

    /**
     * @param keyList
     *            The PGP public keys to encrypt with.
     */
    public void setPublicKeyList(final List<PGPPublicKeyInfo> keyList) {
        this.publicKeyList = keyList;
    }

}
