/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.pdf;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.awt.geom.Rectangle2D;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfBorderDictionary;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

/**
 * Creates {@link PdfAnnotation} on URL text.
 *
 * @author Datraverse B.V.
 *
 */
public final class ITextPdfUrlAnnotator implements TextExtractionStrategy {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ITextPdfUrlAnnotator.class);

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
    private static final String PATTERN_MAILTO = "\\b(mailto:)"
            + REGEX_EMAIL_ADDRESS;

    /**
     * URL pattern for www.*.
     */
    private static final String PATTERN_WWW = "\\b(www\\.\\S+|WWW\\.\\S+)";

    /**
     * Full URL pattern.
     */
    private static final String PATTERN_URL =
            "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    /**
     * No border by default.
     */
    private final boolean addBorderStyle = false;

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

    /**
     * .
     */
    private TextRenderInfo textRenderInfoStartWlk;

    /**
     * .
     */
    private Rectangle rectangleFirstWlk;

    /**
     * .
     */
    private Rectangle rectangleLastWlk;

    /**
     * .
     */
    private StringBuilder collectedTextWlk = new StringBuilder();

    /**
     * Constructor.
     *
     * @param stamper
     *            The {@link PdfStamper} to annotate the PDF links on.
     * @param nPage
     *            The 1-based page ordinal of the stamper to add the annotation
     *            on.
     */
    public ITextPdfUrlAnnotator(final PdfStamper stamper, final int nPage) {
        this.stamper = stamper;
        this.nStamperPage = nPage;
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
            LOGGER.trace(String.format(
                    "PDF url [%s] at x|y lower %f|%f upper %f|%f",
                    url.toExternalForm(), llx, lly, urx, ury));
        }

        final PdfAction action = new PdfAction(url);

        final PdfAnnotation annLink =
                new PdfAnnotation(stamper.getWriter(), llx, lly, urx, ury,
                        action);

        if (this.addBorderStyle) {
            annLink.setBorderStyle(new PdfBorderDictionary(0.5f,
                    PdfBorderDictionary.STYLE_SOLID));
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
                matchList.add(new AnnotationMatch(matcher.group(), matcher
                        .start(), matcher.end(), url));
            } catch (MalformedURLException e) {
                // Log and ignore
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage());
                }
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

        matchList =
                findLinks(Pattern.compile(PATTERN_WWW), searchText, "http://%s");
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

        matchList =
                findLinks(Pattern.compile(PATTERN_EMAIL), searchText,
                        "mailto:%s");
        matchListTot.addAll(matchList);

        return matchListTot;
    }

    /**
     * Checks the collected text for links and adds annotations.
     */
    public void checkCollectedText() {

        if (this.textRenderInfoStartWlk == null) {
            return;
        }

        final TextRenderInfo info = this.textRenderInfoStartWlk;
        final String text = this.collectedTextWlk.toString();

        // System.out.println("[" + text + "]");

        final float fontWidthTotal = info.getFont().getWidth(text);

        final Rectangle infoRectTotal =
                new Rectangle(this.rectangleFirstWlk.getLeft(),
                        this.rectangleFirstWlk.getBottom(),
                        this.rectangleLastWlk.getRight(),
                        this.rectangleLastWlk.getTop());

        //
        for (final AnnotationMatch match : findLinks(text)) {

            final String prefix = text.substring(0, match.getStart());

            /*
             * Get the font width of text parts.
             */
            final float fontWidthPrefix = info.getFont().getWidth(prefix);

            final float fontWidthAnnotation =
                    info.getFont().getWidth(match.getText());

            /*
             * Convert text font width to PDF info width.
             */
            final float infoWidthPrefix =
                    infoRectTotal.getWidth() * fontWidthPrefix / fontWidthTotal;

            final float infoWidthAnnotation =
                    infoRectTotal.getWidth() * fontWidthAnnotation
                            / fontWidthTotal;

            /*
             * Calculate info x-left x-right of the annotation.
             */
            final float infoLeftWlk = infoRectTotal.getLeft() + infoWidthPrefix;

            final float infoRightWlk = infoLeftWlk + infoWidthAnnotation;

            /*
             * The y-baseline.
             */
            final Rectangle2D.Float infoRectBaseline =
                    info.getBaseline().getBoundingRectange();

            /*
             * Add the annotation.
             */
            this.addAnnotation(infoLeftWlk, infoRectBaseline.y, infoRightWlk,
                    infoRectBaseline.y + infoRectTotal.getHeight(),
                    match.getUrl());
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
                && info.getFont()
                        .getPostscriptFontName()
                        .equals(this.textRenderInfoStartWlk.getFont()
                                .getPostscriptFontName())
        //
        ) {

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

}
