/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.imaging;

import java.util.StringTokenizer;

import org.savapage.core.doc.DocContent;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ImageUrl {

    /**
     * Width in pixels of a SafePage thumbnail.
     */
    public static final Integer THUMBNAIL_WIDTH = 120;

    /**
     * (Initial) width in pixels of a SafePage browser page. This value is a
     * trade-off between "sharpness" and performance. I.e. a complex (large) PDF
     * page with lots of graphics takes longer to convert to an image.
     */
    public static final Integer BROWSER_PAGE_WIDTH = 800;

    /**
     * The global format used to generate (thumbnail and browser) images for
     * SafePages.
     * <p>
     * NOTE: Even if JPEG is approximately 30% of PNG size, we use PNG anyway
     * because {@link Pdf2ImgCairoCmd} is dependent on PNG.
     * </p>
     */
    public static final String FILENAME_EXT_IMAGE = DocContent.FILENAME_EXT_PNG;

    /**
     * .
     */
    private static final String IMG_FILENAME = "a." + FILENAME_EXT_IMAGE;

    /**
     * .
     */
    private static final String IMG_FILENAME_B64 = "a.b64";

    /**
     * .
     */
    public static final String MOUNT_PATH = "/page-image";

    /**
     * Boolean.
     */
    private static final String PARM_BASE64 = "b";

    /**
     * The requesting user.
     */
    private static final String PARM_USER = "u";

    /**
     * Name of the job (filename).
     */
    private static final String PARM_JOB = "j";

    /**
     * String: 0 | 90 | 180 | 270 | -90 | -180.
     */
    private static final String PARM_ROTATE = "r";

    /**
     * Unique value to enforce no caching.
     */
    private static final String PARM_NOCACHE = "c";

    /**
     * Integer: zero-based page ordinal in the job (or ordinal over ALL jobs if
     * 'job' is not specified).
     */
    private static final String PARM_PAGE_ORDINAL = "p";

    /**
     * Boolean.
     */
    private static final String PARM_THUMBNAIL = "t";

    /**
     * Boolean.
     */
    private static final String PARM_LETTERHEAD = "l";

    /**
     * Boolean.
     */
    private static final String PARM_LETTERHEAD_PUBLIC = "lp";

    /**
     * String.
     */
    private static final String PARM_FILE = "f";

    /**
     * {@code true} if output should be BASE64 format.
     */
    private boolean base64 = false;

    /**
     * The userid.
     */
    private String user;

    /**
     * The basename of the job file. If {@code null}, then the {@code page}
     * parameter is the ordinal page number over all jobs.
     */
    private String job;

    /**
     * The zero-based page ordinal number in the job (or over all jobs).
     */
    private String page;

    /**
     * The rotation to be applied for this page. If {@code null}, no rotation is
     * applied.
     */
    private String rotate;

    /**
     * {@code true} if a thumbnail is requested, {@code false} if detailed
     * image.
     */
    private boolean thumbnail = false;

    /**
     * {@code true} if this is a letterhead image.
     */
    private boolean letterhead = false;

    /**
     * {@code true} if this is a public letterhead image.
     */
    private boolean letterheadPublic = false;

    /**
     * A unique value, used to force a web browser not to cache the image.
     */
    private String nocache;

    /**
     *
     */
    public ImageUrl() {

    }

    /**
     * Deep copy.
     *
     * @param url
     *            The object to copy.
     */
    public ImageUrl(final ImageUrl url) {
        this.base64 = url.base64;
        this.job = url.job;
        this.letterhead = url.letterhead;
        this.letterheadPublic = url.letterheadPublic;
        this.nocache = url.nocache;
        this.page = url.page;
        this.rotate = url.rotate;
        this.thumbnail = url.thumbnail;
        this.user = url.user;
    }

    /**
     * Sets an URL parameter value.
     *
     * @param parm
     *            The URL parameter.
     * @param value
     *            The URL parameter value.
     */
    public void setParm(final String parm, final String value) {
        switch (parm) {
        case PARM_BASE64:
            setBase64(true);
            break;
        case PARM_JOB:
            setJob(value);
            break;
        case PARM_LETTERHEAD:
            setLetterhead(true);
            break;
        case PARM_LETTERHEAD_PUBLIC:
            setLetterheadPublic(true);
            break;
        case PARM_NOCACHE:
            setNocache(value);
            break;
        case PARM_PAGE_ORDINAL:
            setPage(value);
            break;
        case PARM_ROTATE:
            setRotate(value);
            break;
        case PARM_THUMBNAIL:
            setThumbnail(true);
            break;
        case PARM_USER:
            setUser(value);
            break;
        default:
            break;
        }
    }

    /**
     * URL template for a detailed page over ALL the jobs.
     * <ul>
     * <li>Replace {0} with zero-based page ordinal.</li>
     * <li>Replace {1} with nocache value.</li>
     * </ul>
     *
     * @param user
     * @param base64
     * @return
     */
    public static String composeDetailPageTemplate(final String user,
            boolean base64) {
        ImageUrl urlTpl = new ImageUrl();
        urlTpl.setUser(user);
        urlTpl.setPage("{0}");
        urlTpl.setNocache("{1}");
        urlTpl.setBase64(base64);
        return urlTpl.composeImageUrl();
    }

    /**
     * Extracts zero-based page ordinal from URL path.
     * <p>
     * For example: "/page-image/u/rijk/p/3/c/1591207056614/f/a.png" contains
     * page ordinal "3".
     * </p>
     *
     * @param urlPath
     *            URL path.
     *
     * @return Zero-based page ordinal.
     */
    public static int getPageOrdinalFromPath(final String urlPath) {

        final StringTokenizer tokenizer = new StringTokenizer(urlPath, "/");

        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            if (token.equals(PARM_PAGE_ORDINAL)) {
                if (tokenizer.hasMoreTokens()) {
                    return Integer.valueOf(tokenizer.nextToken()).intValue();
                }
                break;
            }
        }
        throw new IllegalArgumentException("No page ordinal found");
    }

    /**
     *
     * As described on <a href=
     * "https://cwiki.apache.org/confluence/display/WICKET/Request+mapping">this
     * page</a>.
     *
     * @return The composed image URL.
     */
    public String composeImageUrl() {

        StringBuilder buf = new StringBuilder();

        buf.append(MOUNT_PATH);

        appendParmToImageUrl(buf, PARM_USER, this.getUser());
        appendParmToImageUrl(buf, PARM_JOB, this.getJob());
        appendParmToImageUrl(buf, PARM_PAGE_ORDINAL, this.getPage());
        appendParmToImageUrl(buf, PARM_ROTATE, this.getRotate());
        appendParmToImageUrl(buf, PARM_THUMBNAIL, this.isThumbnail());
        appendParmToImageUrl(buf, PARM_LETTERHEAD, this.isLetterhead());
        appendParmToImageUrl(buf, PARM_LETTERHEAD_PUBLIC,
                this.isLetterheadPublic());
        appendParmToImageUrl(buf, PARM_NOCACHE, this.nocache);
        appendParmToImageUrl(buf, PARM_BASE64, this.isBase64());

        if (isBase64()) {
            appendParmToImageUrl(buf, PARM_FILE, IMG_FILENAME_B64);
        } else {
            appendParmToImageUrl(buf, PARM_FILE, IMG_FILENAME);
        }
        return buf.toString();
    }

    /**
     *
     * @param buf
     * @param parm
     * @param value
     * @return
     */
    private StringBuilder appendParmToImageUrl(StringBuilder buf, String parm,
            String value) {

        if (value != null) {
            buf.append("/").append(parm).append("/").append(value);
        }
        return buf;
    }

    /**
     *
     * @param buf
     * @param parm
     * @param value
     * @return
     */
    private StringBuilder appendParmToImageUrl(StringBuilder buf, String parm,
            boolean value) {

        if (value) {
            buf.append("/").append(parm).append("/").append("1");
        }
        return buf;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getRotate() {
        return rotate;
    }

    public void setRotate(String rotate) {
        this.rotate = rotate;
    }

    public boolean isThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
    }

    public boolean isLetterhead() {
        return letterhead;
    }

    public void setLetterhead(boolean letterhead) {
        this.letterhead = letterhead;
    }

    public boolean isLetterheadPublic() {
        return letterheadPublic;
    }

    public void setLetterheadPublic(boolean letterheadPublic) {
        this.letterheadPublic = letterheadPublic;
    }

    public String getNocache() {
        return nocache;
    }

    public void setNocache(String nocache) {
        this.nocache = nocache;
    }

    public boolean isBase64() {
        return base64;
    }

    public void setBase64(boolean base64) {
        this.base64 = base64;
    }

}
