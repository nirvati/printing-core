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

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.savapage.core.pdf.ITextPdfUrlAnnotator.AnnotationMatch;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PdfLinkTest {

    /**
     * .
     */
    @Test
    public void testMatched() {

        final Set<String> linkSet = new HashSet<>();

        final String[] links = new String[] { //
                //
                        "https://www.example.com/index.php?x=1&2=5",
                        //
                        "http://example.com/index.php?x=1&2=5",
                        //
                        "http://www.example.com/",
                        //
                        "http://WWW.example.com/",
                        //
                        "ftp://www.example.com",
                        //
                        "info@example.com",
                        //
                        "mailto:info@example.com",
                        //
                        "http://localhost:81/intranet/xyz",
                        //
                        "https://secure.example.com/path",
                //
                };

        /*
         * Build text, embed links in noise words.
         */
        final StringBuilder builder = new StringBuilder(512);

        for (final String link : links) {
            linkSet.add(link);
            builder.append(" some text ").append(link);
        }

        /*
         * Every match must match entry in the linkSet.
         */
        final Set<String> matchSet = new HashSet<>();

        for (final AnnotationMatch match : ITextPdfUrlAnnotator
                .findLinks(builder.toString())) {
            final String link = match.getText();
            matchSet.add(link);
            assertTrue(String.format("[%s] must be found", link),
                    linkSet.contains(link));
        }

        /*
         * Every entry in the link Set must be matched.
         */
        assertTrue("All links are matched", matchSet.size() == linkSet.size());

        for (final String link : linkSet.toArray(new String[0])) {
            assertTrue(String.format("[%s] is matched", link),
                    matchSet.contains(link));
        }

    }

    /**
     * .
     */
    @Test
    public void testNonMatched() {

        final String[] links = new String[] { //
                //
                        "xxxx://example.com/index.php?x=1&2=5",
                        //
                        "info$example.com",
                        //
                        "mailto:info#example.com",
                        //
                        "httpx://localhost:81/intranet/xyz",
                        //
                        "httpxs://secure.example.com/path"
                //
                // String below are matched on "www." and xxx@xxx.xx
                //
                // "xxxx://www.example.com/index.php?x=1&2=5",
                //
                // "xxxx://www.example.com/",
                //
                // "xxxx://WWW.example.com/",
                //
                // "ftpx://www.example.com",
                //
                // "@info@example.com",
                //
                };

        /*
         * Build text, embed links in noise words.
         */
        final StringBuilder builder = new StringBuilder(512);

        for (final String link : links) {
            builder.append(" some text ").append(link);
        }

        /*
         * Every match must match entry in the linkSet.
         */
        final List<AnnotationMatch> matchList =
                ITextPdfUrlAnnotator.findLinks(builder.toString());

        for (final AnnotationMatch match : matchList) {
            System.out.println(match.getText());
        }

        /*
         * Every entry in the link Set must be matched.
         */
        assertTrue("All links are non-matched", matchList.isEmpty());

    }

    /**
     * .
     */
    @Test
    public void testOneLine() {

        final String urlA = "www.x.nl";
        final String urlB = "info@x.nl";
        final int nSpacing = 10;

        final String text =
                String.format("%s%s%s", urlA,
                        StringUtils.repeat(' ', nSpacing), urlB);

        final List<AnnotationMatch> matchList =
                ITextPdfUrlAnnotator.findLinks(text);

        assertTrue("All links are non-matched", matchList.size() == 2);

        AnnotationMatch match = matchList.get(0);

        assertTrue("match [0] starts at position", match.getStart() == 0);
        assertTrue("match [0] ends on position",
                match.getEnd() == urlA.length());

        //
        match = matchList.get(1);

        assertTrue("match [1] starts at position",
                match.getStart() == urlA.length() + nSpacing);
        assertTrue("match [1] ends on position",
                match.getEnd() == text.length());

    }

}
