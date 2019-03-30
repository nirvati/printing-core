/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.print.proxy;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.ipp.IppJobStateEnum;
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;

/**
 * Print Job details returned from CUPS.
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonProxyPrintJob {

    /** */
    private Integer jobId;
    /** */
    private Integer jobState;
    /** */
    private String jobStateMessage;
    /**
     * List op IPP keywords (can be {@code null}). See
     * {@link IppDictJobDescAttr#ATTR_JOB_STATE_REASONS}.
     */
    private List<String> jobStateReasons;
    /** */
    private String dest;
    /** */
    private String user;
    /** */
    private String title;
    /** */
    private Integer creationTime;
    /** */
    private Integer completedTime;

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

    public String getJobStateMessage() {
        return jobStateMessage;
    }

    public void setJobStateMessage(String jobStateMessage) {
        this.jobStateMessage = jobStateMessage;
    }

    /**
     * @return List op IPP keywords (can be {@code null}). See
     *         {@link IppDictJobDescAttr#ATTR_JOB_STATE_REASONS}.
     */
    public List<String> getJobStateReasons() {
        return jobStateReasons;
    }

    /**
     *
     * @param jobStateReasons
     *            List op IPP keywords (can be {@code null}). See
     *            {@link IppDictJobDescAttr#ATTR_JOB_STATE_REASONS}.
     */
    public void setJobStateReasons(List<String> jobStateReasons) {
        this.jobStateReasons = jobStateReasons;
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

    /**
     * @return State massage and state reasons for logging.
     */
    public String createStateMsgForLogging() {

        final StringBuilder msg = new StringBuilder();

        if (StringUtils.isNotBlank(this.getJobStateMessage())) {
            msg.append("[").append(this.getJobStateMessage()).append("]");
        }
        for (final String reason : this.getJobStateReasons()) {
            msg.append("[").append(reason).append("]");
        }

        return msg.toString();
    }

}
