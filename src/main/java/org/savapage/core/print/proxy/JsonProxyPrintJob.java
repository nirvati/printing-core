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
package org.savapage.core.print.proxy;

import org.savapage.core.ipp.IppJobStateEnum;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Print Job details returned from CUPS.
 *
 * @author Datraverse B.V.
 */
public class JsonProxyPrintJob {

    @JsonProperty("job-id")
    Integer jobId;

    @JsonProperty("job-state")
    Integer jobState;

    @JsonProperty("dest")
    String dest;

    @JsonProperty("user")
    String user;

    @JsonProperty("title")
    String title;

    @JsonProperty("creation-time")
    Integer creationTime;

    @JsonProperty("completed-time")
    Integer completedTime;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobState() {
        return jobState;
    }

    public void setJobState(Integer jobState) {
        this.jobState = jobState;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return The CUPS creation time.
     */
    public Integer getCreationTime() {
        return creationTime;
    }

    /**
     *
     * @param creationTime
     *            The CUPS creation time.
     */
    public void setCreationTime(Integer creationTime) {
        this.creationTime = creationTime;
    }

    /**
     *
     * @return The CUPS completed time.
     */
    public Integer getCompletedTime() {
        return completedTime;
    }

    /**
     *
     * @param completedTime
     *            The CUPS completed time.
     */
    public void setCompletedTime(Integer completedTime) {
        this.completedTime = completedTime;
    }

    public IppJobStateEnum getIppJobState() {
        return IppJobStateEnum.asEnum(jobState);
    }

}
