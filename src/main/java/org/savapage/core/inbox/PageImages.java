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
package org.savapage.core.inbox;

import java.util.ArrayList;

import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.util.MediaUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PageImages {

    private Boolean drm = false;
    private ArrayList<PageImageJob> jobs = new ArrayList<>();
    private ArrayList<PageImage> pages = new ArrayList<>();

    /**
     *
     */
    public static class PageImageJob {

        private String title;
        private String rotate;
        private Boolean landscapeView;
        private Boolean drm;
        private Integer pages;
        private Integer pagesSelected;

        /**
         * The IPP PWG media, e.g. 'iso_a4_210x297mm'.
         */
        private String media;

        private String mediaUi;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getRotate() {
            return rotate;
        }

        public void setRotate(String rotate) {
            this.rotate = rotate;
        }

        public Boolean getLandscapeView() {
            return landscapeView;
        }

        public void setLandscapeView(Boolean landscapeView) {
            this.landscapeView = landscapeView;
        }

        public Integer getPages() {
            return pages;
        }

        public void setPages(Integer pages) {
            this.pages = pages;
        }

        /**
         * Adds pages to the total selected pages.
         *
         * @param nPages
         *            The number of pages to add.
         */
        public void addPagesSelected(int nPages) {
            pagesSelected += nPages;
        }

        public Integer getPagesSelected() {
            return pagesSelected;
        }

        public void setPagesSelected(Integer pagesSelected) {
            this.pagesSelected = pagesSelected;
        }

        public Boolean getDrm() {
            return drm;
        }

        public void setDrm(Boolean drm) {
            this.drm = drm;
        }

        /**
         *
         * @return The IPP PWG media, e.g. 'iso_a4_210x297mm'.
         */
        public String getMedia() {
            return media;
        }

        /**
         *
         * @param media
         *            The IPP PWG media, e.g. 'iso_a4_210x297mm'.
         */
        public void setMedia(String media) {
            this.media = media;
        }

        public String getMediaUi() {
            return mediaUi;
        }

        public void setMediaUi(String mediaUi) {
            this.mediaUi = mediaUi;
        }

    }

    /**
     *
     */
    public static class PageImage {

        private String url;
        private Integer pages;
        private Integer overlayPages;
        private String rotate;
        private Boolean drm;
        private Boolean overlay;
        private String media;
        private Long expiryTime;
        private Long expiryTimeSignal;

        /**
         * The index into the job array.
         */
        private Integer job;

        public String getUrl() {
            return url;
        }

        public Integer getPages() {
            return pages;
        }

        public void setUrl(String s) {
            url = s;
        }

        public void setPages(Integer s) {
            pages = s;
        }

        public Integer getOverlayPages() {
            return overlayPages;
        }

        public void setOverlayPages(Integer overlayPages) {
            this.overlayPages = overlayPages;
        }

        public String getRotate() {
            return rotate;
        }

        public void setRotate(String rotate) {
            this.rotate = rotate;
        }

        public Integer getJob() {
            return job;
        }

        public void setJob(Integer job) {
            this.job = job;
        }

        public Boolean getDrm() {
            return drm;
        }

        public void setDrm(Boolean drm) {
            this.drm = drm;
        }

        public Boolean getOverlay() {
            return overlay;
        }

        public void setOverlay(Boolean overlay) {
            this.overlay = overlay;
        }

        public String getMedia() {
            return media;
        }

        public void setMedia(String media) {
            this.media = media;
        }

        public Long getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(Long expiryTime) {
            this.expiryTime = expiryTime;
        }

        public Long getExpiryTimeSignal() {
            return expiryTimeSignal;
        }

        public void setExpiryTimeSignal(Long expiryTimeSignal) {
            this.expiryTimeSignal = expiryTimeSignal;
        }

    }

    public PageImages() {
        drm = false;
    }

    public ArrayList<PageImage> getPages() {
        return pages;
    }

    public void setPages(ArrayList<PageImage> pages) {
        this.pages = pages;
    }

    public ArrayList<PageImageJob> getJobs() {
        return jobs;
    }

    public void setJobs(ArrayList<PageImageJob> jobs) {
        this.jobs = jobs;
    }

    /**
     *
     * @param title
     * @param pages
     * @param rotate
     * @param landscapeView
     * @param drm
     * @param mediaInbox
     *            The inbox media, e.g. 'iso-a4'.
     */
    public void addJob(final String title, final Integer pages,
            final String rotate, final Boolean landscapeView, final Boolean drm,
            final String mediaInbox) {

        final PageImageJob job = new PageImageJob();

        job.setTitle(title);
        job.setPages(pages);
        job.setRotate(rotate);
        job.setLandscapeView(landscapeView);
        job.setDrm(drm);
        job.setPagesSelected(0);

        // media
        final MediaSizeName mediaSizeInbox;

        if (mediaInbox == null) {
            mediaSizeInbox = null;
        } else {
            mediaSizeInbox = MediaUtils
                    .getMediaSizeFromInboxMedia(mediaInbox.toLowerCase());
        }

        final IppMediaSizeEnum mediaSizeIpp;

        if (mediaSizeInbox == null) {
            mediaSizeIpp = null;
            job.setMediaUi(mediaInbox);
        } else {
            mediaSizeIpp = IppMediaSizeEnum.find(mediaSizeInbox);
            job.setMediaUi(MediaUtils.getUserFriendlyMediaName(mediaSizeInbox));
        }

        if (mediaSizeIpp == null) {
            job.setMedia(mediaInbox);
        } else {
            job.setMedia(mediaSizeIpp.getIppKeyword());
        }

        //
        jobs.add(job);

        if (drm) {
            setDrm(true);
        }
    }

    /**
     * Adds pages to the total selected pages of a job.
     *
     * @param iJob
     *            Zero-base index of the job.
     * @param nPages
     *            The number of pages to add.
     */
    public void addPagesSelected(int iJob, int nPages) {
        jobs.get(iJob).addPagesSelected(nPages);
    }

    public Boolean getDrm() {
        return drm;
    }

    public void setDrm(Boolean drm) {
        this.drm = drm;
    }

}
