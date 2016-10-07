/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.inbox;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Rijk Ravestein
 */
@JsonInclude(Include.NON_NULL)
public final class InboxInfoDto {

    /**
     *
     */
    public final static class InboxJob {

        private String file;
        private Long createdTime;
        private String title;
        private Integer pages;

        private Boolean drm;
        private String media;

        /**
         * {@code true} if the PDF orientation of the PDF inbox document is
         * landscape.
         */
        private Boolean landscape;

        /**
         * The PDF rotation the PDF inbox document.
         */
        private Integer rotation;

        /**
         * The rotation on the PDF inbox document set by the User.
         */
        private String rotate;

        /**
         *
         * @return the base file name of the PDF document.
         */
        public String getFile() {
            return file;
        }

        public Integer getPages() {
            return pages;
        }

        public void setFile(String s) {
            file = s;
        }

        public void setPages(Integer s) {
            pages = s;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Boolean getDrm() {
            return drm;
        }

        public void setDrm(Boolean drm) {
            this.drm = drm;
        }

        /**
         * Gets the creation time of the job, as in {@link Date#getTime()}.
         *
         * @return The time the job was created, or {@code null} for pre 0.9.6
         *         versions.
         */
        public Long getCreatedTime() {
            return createdTime;
        }

        /**
         * Sets the creation time of the job, as in {@link Date#getTime()}.
         *
         * @param createdTime
         *            The time the job was created.
         */
        public void setCreatedTime(Long createdTime) {
            this.createdTime = createdTime;
        }

        /**
         *
         * @return the IPP RFC2911 "media" name.
         */
        public String getMedia() {
            return media;
        }

        /**
         *
         * @param media
         *            the IPP RFC2911 "media" name.
         */
        public void setMedia(String media) {
            this.media = media;
        }

        /**
         *
         * @return {@code true} if the PDF orientation of the PDF inbox document
         *         is landscape.
         */
        public Boolean getLandscape() {
            return landscape;
        }

        /**
         *
         * @param landscape
         *            {@code true} if the PDF orientation of the PDF inbox
         *            document is landscape.
         */
        public void setLandscape(final Boolean landscape) {
            this.landscape = landscape;
        }

        /**
         *
         * @return The PDF rotation the PDF inbox document.
         */
        public Integer getRotation() {
            return rotation;
        }

        /**
         *
         * @param rotation
         *            The PDF rotation the PDF inbox document.
         */
        public void setRotation(Integer rotation) {
            this.rotation = rotation;
        }

        /**
         *
         * @return The rotation on the PDF inbox document set by the User.
         */
        public String getRotate() {
            return rotate;
        }

        /**
         *
         * @param rotate
         *            The rotation on the PDF inbox document set by the User.
         */
        public void setRotate(final String rotate) {
            this.rotate = rotate;
        }

        /**
         *
         * @return {@code true} when job must be shown in landscape orientation.
         */
        @JsonIgnore
        public boolean showLandscape() {

            final boolean showLandscape;

            if (this.getLandscape() == null) {
                showLandscape = false;
            } else {
                final boolean isRotate = !StringUtils.isBlank(this.getRotate())
                        && !this.getRotate().equals("0");
                if (this.getLandscape().booleanValue()) {
                    showLandscape = !isRotate;
                } else {
                    showLandscape = isRotate;
                }
            }
            return showLandscape;
        }
    }

    /**
     *
     */
    public static class InboxJobRange {

        private Integer myJob;
        private String myRange;

        public Integer getJob() {
            return myJob;
        }

        public String getRange() {
            return myRange;
        }

        public void setJob(Integer s) {
            myJob = s;
        }

        public void setRange(String s) {
            myRange = s;
        }
    }

    public static class InboxLetterhead {

        private String id;
        private Boolean pub;

        public String getId() {
            return id;
        }

        public void setId(String s) {
            id = s;
        }

        public Boolean getPub() {
            return pub;
        }

        public void setPub(Boolean pub) {
            this.pub = pub;
        }

        @JsonIgnore
        public boolean isPublic() {
            return (pub != null && pub);
        }
    }

    private Long lastPreviewTime;

    private ArrayList<InboxJob> myJobs = new ArrayList<InboxJob>();
    private ArrayList<InboxJobRange> myPages = new ArrayList<InboxJobRange>();

    private InboxLetterhead myLetterhead;

    public ArrayList<InboxJob> getJobs() {
        return myJobs;
    }

    public ArrayList<InboxJobRange> getPages() {
        return myPages;
    }

    public InboxLetterhead getLetterhead() {
        return myLetterhead;
    }

    public void setJobs(ArrayList<InboxJob> jobs) {
        myJobs = jobs;
    }

    public void setPages(ArrayList<InboxJobRange> pages) {
        myPages = pages;
    }

    public void setLetterhead(InboxLetterhead n) {
        myLetterhead = n;
    }

    /**
     * Gets the last time the user previewed the inbox, as in
     * {@link Date#getTime()} .
     *
     * @return The last preview time, or {@code null} for pre 0.9.6 versions.
     */
    public Long getLastPreviewTime() {
        return lastPreviewTime;
    }

    /**
     * Sets the last time the user previewed the inbox, as in
     * {@link Date#getTime()} .
     *
     * @param lastPreviewTime
     *            The last preview time.
     */
    public void setLastPreviewTime(Long lastPreviewTime) {
        this.lastPreviewTime = lastPreviewTime;
    }

    /**
     *
     * @return
     */
    @JsonIgnore
    public int jobCount() {
        // NOTE: Do NOT use getJobCount as method name
        return myJobs.size();
    }

    /**
     * Can reuse, share globally.
     */
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates an instance from JSON string.
     *
     * @param json
     * @return
     * @throws Exception
     */
    public static InboxInfoDto create(final String json) throws Exception {
        return mapper.readValue(json, InboxInfoDto.class);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public String prettyPrinted() throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jsonFactory.createJsonGenerator(sw);
        jg.useDefaultPrettyPrinter();
        mapper.writeValue(jg, this);
        return sw.toString();
    }

}
