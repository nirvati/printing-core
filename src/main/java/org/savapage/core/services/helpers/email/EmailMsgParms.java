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
package org.savapage.core.services.helpers.email;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataSource;
import javax.activation.URLDataSource;

import org.apache.commons.io.IOUtils;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.stringtemplate.v4.ST;

/**
 *
 * @author Datraverse B.V.
 */
public final class EmailMsgParms {

    /**
     * UUID for the main logo in the default email template.
     */
    private static final String CID_LOGO_MAIN =
            "c8fd14b0-36cf-11e5-9028-406186940c49";

    /**
     * UUID for the mini logo in the default email template.
     */
    private static final String CID_LOGO_MINI =
            "c2932e8e-36cf-11e5-80d9-406186940c49";

    private static final char ST_DELIMITER_CHAR_START = '$';
    private static final char ST_DELIMITER_CHAR_STOP = ST_DELIMITER_CHAR_START;

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
     * Sets the message body using the default HTML template.
     *
     * @param headerText
     * @param content
     */
    public void setBodyFromTemplate(final String headerText,
            final String content) {

        this.contentType = CONTENT_TYPE_HTML;

        cidMap.put(CID_LOGO_MAIN, new URLDataSource(
                this.getClass().getResource("savapage-logo-text.png")));

        cidMap.put(CID_LOGO_MINI, new URLDataSource(
                this.getClass().getResource("savapage-logo-32x32.png")));

        final ST tpl;

        try {
            tpl = new ST(
                    IOUtils.toString(this.getClass()
                            .getResourceAsStream("email-template.html")),
                    ST_DELIMITER_CHAR_START, ST_DELIMITER_CHAR_STOP);

        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }

        final Properties strings = new Properties();

        strings.setProperty("HEADER", headerText);
        strings.setProperty("MAIN_A_HREF",
                CommunityDictEnum.SAVAPAGE_DOT_ORG_URL.getWord());
        strings.setProperty("MAIN_A_ALT",
                CommunityDictEnum.SAVAPAGE_DOT_ORG.getWord());
        strings.setProperty("MAIN_IMG_CID", CID_LOGO_MAIN);
        strings.setProperty("CONTENT", content);
        strings.setProperty("FOOTER_IMG_CID", CID_LOGO_MINI);
        strings.setProperty("FOOTER_IMG_ALT",
                CommunityDictEnum.SAVAPAGE.getWord());
        strings.setProperty("FOOTER_APP_NAME",
                ConfigManager.getAppNameVersion());

        // tpl.add("i18n", strings);
        tpl.add("sp", strings);

        this.body = tpl.render();
    }
}
