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
package org.savapage.core.pdf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfBorderDictionary;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.ContentByteUtils;
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.PdfContentStreamProcessor;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

/**
 * Creates {@link PdfAnnotation} on URL text.
 *
 * @author Rijk Ravestein
 *
 */
public final class ITextPdfUrlAnnotator implements TextExtractionStrategy {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ITextPdfUrlAnnotator.class);

    /**
     * Regular expression for email address.
     */
    private static final String REGEX_EMAIL_ADDRESS =
            "[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})";

    /**
     * Pattern for email address.
     */
    private static final String PATTERN_EMAIL = "\\b" + REGEX_EMAIL_ADDRESS;

    /**
     * Full mailto: pattern.
     */
    private static final String PATTERN_MAILTO =
            "\\b(mailto:)" + REGEX_EMAIL_ADDRESS;

    /**
     * Note: suffix punctuation {@code :,.;} is ignored.
     */
    private static final String REGEX_URL_WITHOUT_SCHEME =
            "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    /**
     * URL pattern for www.*.
     */
    private static final String PATTERN_WWW =
            "\\b(www|WWW)\\." + REGEX_URL_WITHOUT_SCHEME;

    /**
     * Full URL pattern.
     */
    private static final String PATTERN_URL =
            "\\b(https?|ftp|file)://" + REGEX_URL_WITHOUT_SCHEME;

    /** */
    private static final float ANNOTATION_BORDER_WIDTH = 0.5f;

    /**
     * Percentage of text rectangle height used to calculate annotation
     * rectangle padding. Note: this is a simulation/approximation of font
     * ascend/descend.
     */
    private static final float ANNOTATION_TEXT_PADDING_PERC = 0.25f;

    /**
     * Helper class.
     */
    public static class AnnotationMatch {

        private final String text;
        private final int start;
        private final int end;
        private final URL url;

        public String getText() {
            return text;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public URL getUrl() {
            return url;
        }

        /**
         *
         * @param text
         * @param start
         * @param end
         * @param url
         */
        public AnnotationMatch(final String text, final int start,
                final int end, final URL url) {

            this.text = text;
            this.start = start;
            this.end = end;
            this.url = url;
        }
    }

    /**
     * The {@link PdfStamper} to annotate the PDF links on.
     */
    private final PdfStamper stamper;

    /**
     * The 1-based page ordinal of the stamper to add the annotation on.
     */
    private final int nStamperPage;

    /** */
    private final boolean isPageSeenAsLandscape;

    /** */
    private final int pageRotation;

    /** */
    private final Rectangle pageRectangle;

    /**
     * Add annotation border style (or not).
     */
    private final boolean addBorderStyle;

    /** */
    private TextRenderInfo textRenderInfoStartWlk;

    /** */
    private Rectangle rectangleFirstWlk;

    /** */
    private Rectangle rectangleLastWlk;

    /** */
    private StringBuilder collectedTextWlk = new StringBuilder();

    /**
     * Constructor.
     *
     * @param target
     *            The {@link PdfStamper} to annotate the PDF links on.
     * @param nPage
     *            The 1-based page ordinal of the stamper to add the annotation
     *            on.
     */
    public ITextPdfUrlAnnotator(final PdfStamper target, final int nPage) {

        this.stamper = target;
        this.nStamperPage = nPage;

        // No border.
        this.addBorderStyle = false;

        try {
            final PdfReader reader = this.stamper.getReader();

            this.isPageSeenAsLandscape = PdfPageRotateHelper
                    .isSeenAsLandscape(reader, this.nStamperPage);
            this.pageRotation = reader.getPageRotation(nStamperPage);
            this.pageRectangle = reader.getPageSize(this.nStamperPage);

        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * Adds an {@link URL} annotation to the {@link PdfStamper}.
     *
     * @param llx
     *            Lower left x.
     * @param lly
     *            Lower left y.
     * @param urx
     *            Upper Right x.
     * @param ury
     *            Upper Right y.
     * @param url
     *            The {@link URL}.
     */
    private void addAnnotation(final float llx, final float lly,
            final float urx, final float ury, final URL url) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    String.format("PDF url [%s] at x|y lower %f|%f upper %f|%f",
                            url.toExternalForm(), llx, lly, urx, ury));
        }

        final PdfAction action = new PdfAction(url);

        final PdfAnnotation annLink = new PdfAnnotation(stamper.getWriter(),
                llx, lly, urx, ury, action);

        if (this.addBorderStyle) {
            annLink.setBorderStyle(new PdfBorderDictionary(
                    ANNOTATION_BORDER_WIDTH, PdfBorderDictionary.STYLE_SOLID));
        }

        stamper.addAnnotation(annLink, this.nStamperPage);
    }

    /**
     * @param info
     *            The {@link TextRenderInfo}.
     * @return The bounding rectangle of the rendered text.
     */
    private Rectangle getRectangle(final TextRenderInfo info) {

        final Vector lowerLeftWlk = info.getBaseline().getStartPoint();
        final Vector upperRightWlk = info.getAscentLine().getEndPoint();

        return new Rectangle(lowerLeftWlk.get(0), lowerLeftWlk.get(1),
                upperRightWlk.get(0), upperRightWlk.get(1));
    }

    /**
     * Finds {@link AnnotationMatch} instances in a text string.
     *
     * @param pattern
     *            The {@link Pattern} to find the instances.
     * @param text
     *            The input string to search.
     * @param urlFormat
     *            The format string as in
     *            {@link String#format(String, Object...)} to create the
     *            {@link URL} in the {@link AnnotationMatch}.
     * @return A list of {@link AnnotationMatch} instances.
     */
    private static List<AnnotationMatch> findLinks(final Pattern pattern,
            final String text, final String urlFormat) {

        final List<AnnotationMatch> matchList = new ArrayList<>();
        final Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                final String urlText =
                        String.format(urlFormat, matcher.group());
                final URL url = new URL(urlText);
                matchList.add(new AnnotationMatch(matcher.group(),
                        matcher.start(), matcher.end(), url));
            } catch (MalformedURLException e) {
                // Log and ignore
                LOGGER.warn(e.getMessage());
            }
        }

        return matchList;
    }

    /**
     * Finds {@link AnnotationMatch} instances in a text string.
     *
     * @param text
     *            The input string to search.
     * @return A list of {@link AnnotationMatch} instances.
     */
    public static List<AnnotationMatch> findLinks(final String text) {

        final List<AnnotationMatch> matchListTot = new ArrayList<>();

        List<AnnotationMatch> matchList;

        //
        String searchText = text;

        matchList = findLinks(Pattern.compile(PATTERN_URL), searchText, "%s");

        char[] textAsChars = searchText.toCharArray();

        for (final AnnotationMatch match : matchList) {
            /*
             * Wipe to prevent duplicate matches.
             */
            for (int i = match.getStart(); i < match.getEnd(); i++) {
                textAsChars[i] = ' ';
            }
        }
        matchListTot.addAll(matchList);

        //
        searchText = new String(textAsChars);

        matchList = findLinks(Pattern.compile(PATTERN_WWW), searchText,
                "https://%s");
        matchListTot.addAll(matchList);

        //
        matchList =
                findLinks(Pattern.compile(PATTERN_MAILTO), searchText, "%s");

        textAsChars = searchText.toCharArray();

        for (final AnnotationMatch match : matchList) {
            /*
             * Wipe to prevent duplicate matches.
             */
            for (int i = match.getStart(); i < match.getEnd(); i++) {
                textAsChars[i] = ' ';
            }
        }

        matchListTot.addAll(matchList);

        //
        searchText = new String(textAsChars);

        matchList = findLinks(Pattern.compile(PATTERN_EMAIL), searchText,
                "mailto:%s");
        matchListTot.addAll(matchList);

        return matchListTot;
    }

    /**
     * Checks the collected text for links and adds annotations.
     */
    private void checkCollectedText() {

        if (this.textRenderInfoStartWlk == null) {
            return;
        }

        /*
         * Annotation coordinates must be converted from technical PDF text
         * coordinates to the "logical" coordinates as perceived by user.
         */
        final float llx;
        final float lly;
        final float urx;
        final float ury;

        // Padding
        final float llxPadding;
        final float llyPadding;
        final float urxPadding;
        final float uryPadding;

        final float techPadding;

        // Determine technical orientation of text.
        final boolean textTechHorizontal = this.rectangleFirstWlk
                .getLeft() < this.rectangleFirstWlk.getRight();

        final boolean textSeenVertical;

        if (textTechHorizontal) {

            textSeenVertical =
                    this.isPageSeenAsLandscape && this.pageRotation != 0;

            if (textSeenVertical) {

                if (this.pageRotation != PdfPageRotateHelper.ROTATION_90) {
                    // TODO
                    LOGGER.warn(
                            "Page [{}] Rotation [{}] to Landscape: "
                                    + "not implemented yet.",
                            this.nStamperPage, this.pageRotation);
                    return;
                }

                final float perceivedPageHeight = this.pageRectangle.getWidth();

                llx = this.rectangleFirstWlk.getBottom();
                lly = perceivedPageHeight - this.rectangleLastWlk.getRight();
                urx = this.rectangleLastWlk.getTop();
                ury = perceivedPageHeight - this.rectangleFirstWlk.getLeft();

                techPadding = (urx - llx) * ANNOTATION_TEXT_PADDING_PERC;

                llxPadding = -techPadding;
                llyPadding = -techPadding;
                urxPadding = techPadding;
                uryPadding = techPadding;

            } else {

                llx = this.rectangleFirstWlk.getLeft();
                lly = this.rectangleFirstWlk.getBottom();
                urx = this.rectangleLastWlk.getRight();
                ury = this.rectangleLastWlk.getTop();

                techPadding = (ury - lly) * ANNOTATION_TEXT_PADDING_PERC;

                llxPadding = -techPadding;
                llyPadding = -techPadding;
                urxPadding = techPadding;
                uryPadding = techPadding;
            }

        } else {

            textSeenVertical = false;

            // PDF Text is technically vertical.
            final float heightCorrection;

            if (this.isPageSeenAsLandscape && this.pageRotation != 0) {
                heightCorrection = this.pageRectangle.getWidth();
            } else {
                heightCorrection = this.pageRectangle.getHeight();
            }

            llx = this.rectangleFirstWlk.getBottom();
            lly = heightCorrection - this.rectangleFirstWlk.getLeft();
            urx = this.rectangleLastWlk.getTop();
            ury = heightCorrection - this.rectangleLastWlk.getRight();

            techPadding = (ury - lly) * ANNOTATION_TEXT_PADDING_PERC;

            llxPadding = -techPadding;
            llyPadding = -techPadding;
            urxPadding = techPadding;
            uryPadding = techPadding;
        }

        final Rectangle infoRectTotal = new Rectangle(llx, lly, urx, ury);

        //
        final TextRenderInfo info = this.textRenderInfoStartWlk;
        final String text = this.collectedTextWlk.toString();
        final float fontWidthTotal = info.getFont().getWidth(text);

        for (final AnnotationMatch match : findLinks(text)) {

            final String prefix = text.substring(0, match.getStart());

            /*
             * Get the font width of text parts.
             */
            final float fontWidthPrefix = info.getFont().getWidth(prefix);

            final float fontWidthAnnotation =
                    info.getFont().getWidth(match.getText());

            final float llxWlk;
            final float llyWlk;
            final float urxWlk;
            final float uryWlk;

            if (textSeenVertical) {

                final float infoWidthPrefix = infoRectTotal.getHeight()
                        * fontWidthPrefix / fontWidthTotal;

                final float infoWidthAnnotation = infoRectTotal.getHeight()
                        * fontWidthAnnotation / fontWidthTotal;

                final float infoTopWlk =
                        infoRectTotal.getTop() - infoWidthPrefix;
                final float infoBottomWlk = infoTopWlk - infoWidthAnnotation;

                llxWlk = infoRectTotal.getLeft();
                llyWlk = infoBottomWlk;
                urxWlk = infoRectTotal.getRight();
                uryWlk = infoTopWlk;

            } else {

                final float infoWidthPrefix = infoRectTotal.getWidth()
                        * fontWidthPrefix / fontWidthTotal;

                final float infoWidthAnnotation = infoRectTotal.getWidth()
                        * fontWidthAnnotation / fontWidthTotal;

                final float infoLeftWlk =
                        infoRectTotal.getLeft() + infoWidthPrefix;
                final float infoRightWlk = infoLeftWlk + infoWidthAnnotation;

                llxWlk = infoLeftWlk;
                llyWlk = infoRectTotal.getBottom();
                urxWlk = infoRightWlk;
                uryWlk = infoRectTotal.getBottom() + infoRectTotal.getHeight();
            }

            this.addAnnotation(llxWlk + llxPadding, llyWlk + llyPadding,
                    urxWlk + urxPadding, uryWlk + uryPadding, match.getUrl());
        }

        this.textRenderInfoStartWlk = null;
    }

    @Override
    public void renderText(final TextRenderInfo info) {

        final String text = info.getText();
        final Rectangle rectangle = getRectangle(info);

        final boolean checkCollectedText;

        if (this.textRenderInfoStartWlk != null //
                // same line
                && rectangle.getBottom() == this.rectangleFirstWlk.getBottom()
                // same font
                && info.getFont().getPostscriptFontName()
                        .equals(this.textRenderInfoStartWlk.getFont()
                                .getPostscriptFontName())) {
            /*
             * How to check same word consistently for all kind of PDFs?
             *
             * For now, if x-left of this rendered text is less then half a
             * space of x-right of the previous rendered text, we consider same
             * word.
             */
            final boolean sameWord =
                    (rectangle.getLeft() - rectangleLastWlk.getRight()) < info
                            .getSingleSpaceWidth() / 2;

            checkCollectedText = !sameWord;

        } else if (this.textRenderInfoStartWlk != null //
                // rotated
                && rectangle.getLeft() > rectangle.getRight() //
                // same rotated line
                && rectangle.getLeft() == this.rectangleFirstWlk.getLeft()
                // same font
                && info.getFont().getPostscriptFontName()
                        .equals(this.textRenderInfoStartWlk.getFont()
                                .getPostscriptFontName())) {
            /*
             * How to check same word consistently for all kind of PDFs?
             *
             * For now, if y-bottom of this rendered text is less then half a
             * space of y-top of the previous rendered text, we consider same
             * word.
             */
            final boolean sameWord =
                    (rectangle.getBottom() - rectangleLastWlk.getTop()) < info
                            .getSingleSpaceWidth() / 2;

            checkCollectedText = !sameWord;

        } else {
            checkCollectedText = true;
        }

        if (checkCollectedText) {

            this.checkCollectedText();

            this.textRenderInfoStartWlk = info;
            this.collectedTextWlk = new StringBuilder();
            this.rectangleFirstWlk = rectangle;
        }

        this.collectedTextWlk.append(text);

        this.rectangleLastWlk = rectangle;
    }

    @Override
    public void renderImage(final ImageRenderInfo arg0) {
        // noop
    }

    @Override
    public void endTextBlock() {
        // noop
    }

    @Override
    public void beginTextBlock() {
        // noop
    }

    @Override
    public String getResultantText() {
        return this.collectedTextWlk.toString();
    }

    /**
     * Annotates URL links in PDF file.
     *
     * @param reader
     *            PDF in.
     * @param stamper
     *            PDF out.
     * @throws IOException
     *             If IO error.
     */
    public static void annotate(final PdfReader reader,
            final PdfStamper stamper) throws IOException {

        final int pageCount = reader.getNumberOfPages();

        for (int i = 1; i <= pageCount; i++) {

            final ITextPdfUrlAnnotator delegate =
                    new ITextPdfUrlAnnotator(stamper, i);

            final FilteredTextRenderListener listener =
                    new FilteredTextRenderListener(delegate);

            final PdfContentStreamProcessor processor =
                    new PdfContentStreamProcessor(listener);

            final PdfDictionary pageDic = reader.getPageN(i);

            final PdfDictionary resourcesDic =
                    pageDic.getAsDict(PdfName.RESOURCES);

            try {
                final byte[] content =
                        ContentByteUtils.getContentBytesForPage(reader, i);

                processor.processContent(content, resourcesDic);

            } catch (ExceptionConverter e) {
                // TODO
                LOGGER.warn(String.format("%s [%s]",
                        e.getClass().getSimpleName(), e.getMessage()));
            }

            // Flush remaining text
            delegate.checkCollectedText();
        }

    }
}
