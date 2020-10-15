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
package org.savapage.lib.feed;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * A very basic Atom Feed Writer.
 *
 * <a href="https://validator.w3.org/feed/docs/rfc4287.html">RFC4287</a>.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AtomFeedWriter {

    /** */
    private final OutputStreamWriter writer;

    /** */
    private int indent;

    /**
     *
     * @param ostr
     *            OutputStream to write to.
     * @throws FeedException
     *             When error.
     */
    public AtomFeedWriter(final OutputStream ostr) throws FeedException {
        this.writer = new OutputStreamWriter(ostr, Charset.forName("UTF-8"));
    }

    /**
     * @throws FeedException
     *             When error.
     */
    public final void process() throws FeedException {
        try {
            this.onStart();
            this.write();
            this.onEnd();
        } catch (IOException e) {
            throw new FeedException(e);
        } finally {
            try {
                this.writer.close();
            } catch (IOException e) {
                throw new FeedException(e);
            }
        }
    }

    /**
     *
     * @throws IOException
     *             When errors.
     */
    private void write() throws IOException {

        /* Document: start */
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        this.indent = 0;

        // Root element: start
        writer.write("<feed xmlns=\"http://www.w3.org/2005/Atom\""
                + " xml:lang=\"en-US\">");

        ////
        writeElement("id", formatID(getFeedUuid()));

        ////
        writeElementOpt("generator", this.getFeedGenerator());

        ////
        writeElement("updated", formatDate(getFeedUpdated()));
        ////
        writeElement("title", this.getFeedTitle());

        //////
        push();
        indent();
        writer.write("<link rel=\"self\" href=\"");
        writer.write(this.getFeedLinkSelf().toString());
        writer.write("\"/>");
        pop();

        ////
        writeStartElement("author");
        //////
        writeElement("name", this.getFeedAuthorName());
        //////
        writeElementOpt("uri", this.getFeedAuthorUri());
        //////
        writeElementOpt("email", this.getFeedAuthorEmail());
        ////
        writeEndElement("author");

        ///
        final StringBuilder xhtml = new StringBuilder();

        FeedEntryDto dto = getFeedEntry(xhtml);

        while (dto != null) {
            ////
            this.writeStartElement("entry");

            writeElement("id", formatID(dto.getUuid()));
            writeElement("title", dto.getTitle());

            writeStartElement("author");
            writeElement("name", dto.getAuthor());
            writeEndElement("author");

            //////
            push();
            indent();
            writer.write("<category term=\"");
            writer.write(dto.getCategory());
            writer.write("\"/>");
            pop();

            writeElement("summary", dto.getSummary());
            writeElement("updated", formatDate(dto.getUpdated()));

            //////
            if (xhtml.length() > 0) {
                push();
                indent();
                writer.write("<content type=\"xhtml\">\n");
                writer.write(xhtml.toString());
                writeEndElement("content");
            }

            ////
            writeEndElement("entry");

            xhtml.setLength(0);
            dto = getFeedEntry(xhtml);
        }

        // Root element: end
        writeEndElement("feed");

        /* Document: end */
        writer.flush();
    }

    /**
     * RFC3339 formatted Date.
     *
     * @param date
     * @return
     */
    private static String formatDate(final Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(date);
    }

    private static String formatID(final UUID uuid) {
        return String.format("urn:uuid:%s", uuid.toString());
    }

    /**
     *
     * @param localName
     * @param text
     * @throws IOException
     */
    private void writeElement(final String localName, final boolean start)
            throws IOException {

        if (start) {
            writer.write('<');
        } else {
            writer.write("</");
        }
        writer.write(localName);
        writer.write('>');
    }

    private void indent() throws IOException {
        writer.write('\n');
        writer.write(StringUtils.repeat('\t', this.indent));
    }

    private void push() {
        this.indent++;
    }

    private void pop() {
        this.indent--;
    }

    /**
     *
     * @param localName
     * @param text
     * @throws IOException
     */
    private void writeStartElement(final String localName) throws IOException {
        this.push();
        this.indent();
        this.writeElement(localName, true);
    }

    /**
     *
     * @param localName
     * @param text
     * @throws IOException
     */
    private void writeEndElement(final String localName) throws IOException {
        this.indent();
        this.writeElement(localName, false);
        this.pop();
    }

    /**
     *
     * @param localName
     * @param text
     * @throws IOException
     */
    private void writeElement(final String localName, final Object text)
            throws IOException {
        this.writeStartElement(localName);
        writer.write(StringEscapeUtils.escapeXml10(text.toString()));
        this.writeElement(localName, false);
        this.pop();
    }

    /**
     *
     * @param localName
     * @param text
     *            When {@code null} element is not written.
     * @throws IOException
     */
    private void writeElementOpt(final String localName, final Object text)
            throws IOException {
        if (text != null) {
            writeElement(localName, text.toString());
        }
    }

    /**
     * @return Permanent, universally unique identifier for feed.
     */
    protected abstract UUID getFeedUuid();

    /**
     * @return Human-readable title for feed.
     */
    protected abstract String getFeedTitle();

    /**
     * Publishers MAY change the value of this element over time.
     *
     * @return Most recent instant in time when feed was modified in a way the
     *         publisher considers significant. Therefore, not all modifications
     *         necessarily result in a changed atom:updated value.
     */
    protected abstract Date getFeedUpdated();

    /**
     * Feed author email.
     *
     * @return The name.
     */
    protected abstract String getFeedAuthorName();

    /**
     * Feed author email.
     *
     * @return Can be {@code null}.
     */
    protected abstract String getFeedAuthorEmail();

    /**
     * Feed author URI.
     *
     * @return Can be {@code null}.
     */
    protected abstract URI getFeedAuthorUri();

    /**
     * Feed author URI.
     *
     * @return The external link to this feed.
     */
    protected abstract URI getFeedLinkSelf();

    /**
     * @return Can be {@code null}.
     */
    protected abstract String getFeedGenerator();

    /**
     *
     * @param xhtml
     *            The XHTML to append on.
     * @return The feed entry. When {@code null}, no more entries are available.
     */
    protected abstract FeedEntryDto getFeedEntry(StringBuilder xhtml);

    /**
     * .
     */
    protected abstract void onStart();

    /**
     * .
     */
    protected abstract void onEnd();

}
