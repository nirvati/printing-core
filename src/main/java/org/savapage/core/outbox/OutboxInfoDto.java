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
package org.savapage.core.outbox;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.savapage.core.dto.AbstractDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonInclude(Include.NON_NULL)
public final class OutboxInfoDto extends AbstractDto {

    /**
     *
     * @author rijk
     *
     */
    public final static class LocaleInfo {

        private String cost;
        private String submitTime;
        private String expiryTime;
        private String remainTime;

        public String getCost() {
            return cost;
        }

        public void setCost(String cost) {
            this.cost = cost;
        }

        public String getSubmitTime() {
            return submitTime;
        }

        public void setSubmitTime(String submitTime) {
            this.submitTime = submitTime;
        }

        public String getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(String expiryTime) {
            this.expiryTime = expiryTime;
        }

        public String getRemainTime() {
            return remainTime;
        }

        public void setRemainTime(String remainTime) {
            this.remainTime = remainTime;
        }

    }

    /**
     *
     */
    public final static class OutboxJob {

        private String file;
        private String printerName;
        private String jobName;
        private int copies;
        private int pages;
        private int sheets;
        private boolean removeGraphics;
        private BigDecimal cost;
        private long submitTime;
        private long expiryTime;
        private Boolean fitToPage;
        private Map<String, String> optionValues;

        private LocaleInfo localeInfo;

        /**
         * Note: {@link LinkedHashMap} is insertion ordered.
         */
        private LinkedHashMap<String, Integer> uuidPageCount;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getPrinterName() {
            return printerName;
        }

        public void setPrinterName(String printerName) {
            this.printerName = printerName;
        }

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public int getCopies() {
            return copies;
        }

        public void setCopies(int copies) {
            this.copies = copies;
        }

        public int getPages() {
            return pages;
        }

        public void setPages(int pages) {
            this.pages = pages;
        }

        public int getSheets() {
            return sheets;
        }

        public void setSheets(int sheets) {
            this.sheets = sheets;
        }

        public boolean isRemoveGraphics() {
            return removeGraphics;
        }

        public void setRemoveGraphics(boolean removeGraphics) {
            this.removeGraphics = removeGraphics;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }

        public LocaleInfo getLocaleInfo() {
            return localeInfo;
        }

        public void setLocaleInfo(LocaleInfo localeInfo) {
            this.localeInfo = localeInfo;
        }

        /**
         * @return The time the job was submitted as in {@link Date#getTime()}.
         */
        public long getSubmitTime() {
            return submitTime;
        }

        /**
         * Sets the time the job was submitted.
         *
         * @param submitTime
         *            Time as in {@link Date#getTime()}.
         */
        public void setSubmitTime(long submitTime) {
            this.submitTime = submitTime;
        }

        /**
         *
         * @return The time the job expires as in {@link Date#getTime()}.
         */
        public long getExpiryTime() {
            return expiryTime;
        }

        /**
         * Sets the time the job was expires.
         *
         * @param expiryTime
         *            Time as in {@link Date#getTime()}.
         */
        public void setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
        }

        public Boolean getFitToPage() {
            return fitToPage;
        }

        public void setFitToPage(Boolean fitToPage) {
            this.fitToPage = fitToPage;
        }

        public Map<String, String> getOptionValues() {
            return optionValues;
        }

        public void setOptionValues(Map<String, String> optionValues) {
            this.optionValues = optionValues;
        }

        /**
         * Deep copy of option values.
         *
         * @param optionValues
         */
        public void putOptionValues(final Map<String, String> optionValues) {
            if (this.optionValues == null) {
                this.optionValues = new HashMap<>();
            }
            this.optionValues.putAll(optionValues);
        }

        /**
         * Note: {@link LinkedHashMap} is insertion ordered.
         */
        public LinkedHashMap<String, Integer> getUuidPageCount() {
            return uuidPageCount;
        }

        /**
         * Note: {@link LinkedHashMap} is insertion ordered.
         */
        public void setUuidPageCount(
                LinkedHashMap<String, Integer> uuidPageCount) {
            this.uuidPageCount = uuidPageCount;
        }

    }

    /**
     * {@link LocaleInfo#date} hold the earliest submitted date of the jobs.
     */
    private LocaleInfo localeInfo;

    /**
     * Note: {@link LinkedHashMap} is insertion ordered.
     */
    private LinkedHashMap<String, OutboxJob> jobs = new LinkedHashMap<>();

    /**
     * Note: {@link LinkedHashMap} is insertion ordered.
     */
    public LinkedHashMap<String, OutboxJob> getJobs() {
        return jobs;
    }

    /**
     * Note: {@link LinkedHashMap} is insertion ordered.
     */
    public void setJobs(LinkedHashMap<String, OutboxJob> jobs) {
        this.jobs = jobs;
    }

    public LocaleInfo getLocaleInfo() {
        return localeInfo;
    }

    public void setLocaleInfo(LocaleInfo localeInfo) {
        this.localeInfo = localeInfo;
    }

    /**
     * @return The number of jobs.
     */
    @JsonIgnore
    public int getJobCount() {
        return this.jobs.size();
    }

    /**
     * Adds a job.
     *
     * @param job
     *            The job to add.
     */
    @JsonIgnore
    public void addJob(final String fileName, final OutboxJob job) {
        this.jobs.put(fileName, job);
    }

    /**
     * Creates an instance from JSON string.
     *
     * @param json
     * @return
     * @throws Exception
     */
    public static OutboxInfoDto create(final String json) throws Exception {
        return getMapper().readValue(json, OutboxInfoDto.class);
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
        getMapper().writeValue(jg, this);
        return sw.toString();
    }

}
