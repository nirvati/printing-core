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
package org.savapage.core.dao.helpers;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.dao.DocLogDao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean for mapping JSON page request.
 *
 * @author Datraverse B.V.
 */
public class DocLogPagerReq extends AbstractPagerReq {

    /**
     *
     */
    private Select select;

    /**
     *
     */
    private Sort sort;

    /**
     * Reads the page request from the POST parameter.
     *
     * @return The page request.
     */
    public static DocLogPagerReq read(final String data) {

        DocLogPagerReq req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, DocLogPagerReq.class);
            } catch (IOException e) {
                throw new SpException(e.getMessage());
            }
        }
        /*
         * Check inputData separately, since JSON might not have delivered the
         * right parameters and the mapper returned null.
         */
        if (req == null) {
            /*
             * Use the defaults
             */
            req = new DocLogPagerReq();
        }
        return req;
    }

    /**
     *
     * @author rijk
     *
     */
    public static class Select {

        @JsonProperty("date_from")
        private Long dateFrom = null;

        @JsonProperty("date_to")
        private Long dateTo = null;

        @JsonProperty("doc_name")
        private String docName = null;

        @JsonProperty("doc_type")
        private DocLogDao.Type docType = null;

        @JsonProperty("user_id")
        private Long userId = null;

        @JsonProperty("account_id")
        private Long accountId = null;

        @JsonProperty("printer_id")
        private Long printerId = null;

        @JsonProperty("queue_id")
        private Long queueId = null;

        @JsonProperty("signature")
        private String signature = null;

        @JsonProperty("destination")
        private String destination = null;

        @JsonProperty("letterhead")
        private Boolean letterhead = null;

        @JsonProperty("job_state")
        private Integer jobState = null;

        @JsonProperty("duplex")
        private Boolean duplex = null;

        @JsonProperty("author")
        private String author = null;

        @JsonProperty("subject")
        private String subject = null;

        @JsonProperty("keywords")
        private String keywords = null;

        @JsonProperty("userpw")
        private String userPw = null;

        @JsonProperty("ownerpw")
        private String ownerPw = null;

        @JsonProperty("encrypted")
        private Boolean encrypted = null;

        @JsonProperty("ticket_number")
        private String ticketNumber = null;

        public Long getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(Long dateFrom) {
            this.dateFrom = dateFrom;
        }

        public Long getDateTo() {
            return dateTo;
        }

        public void setDateTo(Long dateTo) {
            this.dateTo = dateTo;
        }

        /**
         * Gets the truncated day of dateFrom.
         */
        public Date dateFrom() {
            if (dateFrom != null) {
                return DateUtils.truncate(new Date(dateFrom),
                        Calendar.DAY_OF_MONTH);
            }
            return null;
        }

        /**
         * Gets the truncated next day of dateTo.
         *
         * @return
         */
        public Date dateTo() {
            if (dateTo != null) {
                return DateUtils.truncate(new Date(dateTo
                        + DateUtils.MILLIS_PER_DAY), Calendar.DAY_OF_MONTH);
            }
            return null;
        }

        public String getDocName() {
            return docName;
        }

        public void setDocName(String docName) {
            this.docName = docName;
        }

        public DocLogDao.Type getDocType() {
            return docType;
        }

        public void setDocType(DocLogDao.Type docType) {
            this.docType = docType;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public Long getPrinterId() {
            return printerId;
        }

        public void setPrinterId(Long printerId) {
            this.printerId = printerId;
        }

        public Long getQueueId() {
            return queueId;
        }

        public void setQueueId(Long queueId) {
            this.queueId = queueId;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public Boolean getLetterhead() {
            return letterhead;
        }

        public void setLetterhead(Boolean letterhead) {
            this.letterhead = letterhead;
        }

        public Boolean getDuplex() {
            return duplex;
        }

        public void setDuplex(Boolean duplex) {
            this.duplex = duplex;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        public String getUserPw() {
            return userPw;
        }

        public void setUserPw(String userpw) {
            this.userPw = userpw;
        }

        public String getOwnerPw() {
            return ownerPw;
        }

        public void setOwnerPw(String ownerpw) {
            this.ownerPw = ownerpw;
        }

        public Boolean getEncrypted() {
            return encrypted;
        }

        public void setEncrypted(Boolean encrypted) {
            this.encrypted = encrypted;
        }

        public String getTicketNumber() {
            return ticketNumber;
        }

        public void setTicketNumber(String ticketNumber) {
            this.ticketNumber = ticketNumber;
        }

        public Integer getJobState() {
            return jobState;
        }

        public void setJobState(Integer jobState) {
            this.jobState = jobState;
        }

        public DocLogDao.JobState getPrintOutState() {
            if (jobState == null) {
                return DocLogDao.JobState.ALL;
            }
            switch (jobState) {
            case 1:
                return DocLogDao.JobState.ACTIVE;
            case 2:
                return DocLogDao.JobState.UNFINISHED;
            case 3:
                return DocLogDao.JobState.COMPLETED;
            default:
                return DocLogDao.JobState.ALL;
            }
        }

    }

    public static class Sort {

        public static final String FLD_DATE = "date";
        public static final String FLD_NAME = "name";
        public static final String FLD_QUEUE = "queue";
        public static final String FLD_PRINTER = "printer";

        private String field = null;
        private Boolean ascending = true;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Boolean getAscending() {
            return ascending;
        }

        public void setAscending(Boolean ascending) {
            this.ascending = ascending;
        }

        public DocLogDao.FieldEnum getSortField() {
            switch (field) {
            case FLD_NAME:
                return DocLogDao.FieldEnum.DOC_NAME;
            case FLD_PRINTER:
                return DocLogDao.FieldEnum.PRINTER;
            case FLD_QUEUE:
                return DocLogDao.FieldEnum.QUEUE;
            case FLD_DATE:
            default:
                return DocLogDao.FieldEnum.CREATE_DATE;
            }
        }

    }

    public Select getSelect() {
        return select;
    }

    public void setSelect(Select select) {
        this.select = select;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

}
