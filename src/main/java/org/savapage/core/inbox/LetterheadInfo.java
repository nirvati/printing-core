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
package org.savapage.core.inbox;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class LetterheadInfo {

    public static class LetterheadJob {

        private String file;
        private String name;
        private Integer pages;
        private Boolean foreground;
        private Boolean pub;

        public String getFile() {
            return file;
        }

        public String getName() {
            return name;
        }

        public Integer getPages() {
            return pages;
        }

        public void setFile(String s) {
            file = s;
        }

        public void setName(String s) {
            name = s;
        }

        public void setPages(Integer s) {
            pages = s;
        }

        public Boolean getForeground() {
            return foreground;
        }

        public void setForeground(Boolean foreground) {
            this.foreground = foreground;
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

    private ArrayList<LetterheadJob> jobs = new ArrayList<LetterheadJob>();

    public ArrayList<LetterheadJob> getJobs() {
        return jobs;
    }

    public void setJobs(ArrayList<LetterheadJob> jobs) {
        this.jobs = jobs;
    }

}
