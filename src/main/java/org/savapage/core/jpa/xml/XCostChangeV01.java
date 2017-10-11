/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.jpa.xml;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A cost change (request) for a DocLog.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = XCostChangeV01.TABLE_NAME)
public class XCostChangeV01 extends XEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_cost_change";

    /**
     *
     */
    private static final String XML_NAME = "CostChange";

    @Id
    @Column(name = "cost_change_id")
    private Long id;

    /**
     * The related document. Note that this is NOT @OneToOne, since the cost of
     * one (1) DocLog can changed more than once.
     */
    @Column(name = "doc_id", nullable = false)
    private Long docLog;

    /**
     * Currency Code: BTC, EUR, USD, etc.
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    /**
     * The requesting user. Can be {@code null}, when requester is "admin", or
     * irrelevant.
     */
    @Column(name = "req_user_id", nullable = true)
    private Long reqUser;

    /**
     * The requested currency amount of the change.
     */
    @Column(name = "req_amount", nullable = false,
            precision = org.savapage.core.jpa.Entity.DECIMAL_PRECISION_16,
            scale = org.savapage.core.jpa.Entity.DECIMAL_SCALE_8)
    private BigDecimal reqAmount;

    /**
     * The reason for the request.
     */
    @Column(name = "req_reason", length = 1000, nullable = true)
    private String reqReason;

    /**
     * Timestamp of request creation.
     */
    @Column(name = "req_date", nullable = false)
    private Date reqDate;

    /**
     * The assigned currency amount change.
     */
    @Column(name = "chg_amount", nullable = true,
            precision = org.savapage.core.jpa.Entity.DECIMAL_PRECISION_16,
            scale = org.savapage.core.jpa.Entity.DECIMAL_SCALE_8)
    private BigDecimal chgAmount;

    /**
     * The DocLog cost after the assigned currency amount change.
     */
    @Column(name = "chg_cost", nullable = true,
            precision = org.savapage.core.jpa.Entity.DECIMAL_PRECISION_16,
            scale = org.savapage.core.jpa.Entity.DECIMAL_SCALE_8)
    private BigDecimal chgCost;

    /**
     * See {@link CostChangeTypeEnum}.
     */
    @Column(name = "chg_type", length = 20, nullable = false)
    private String chgType;

    /**
     * See {@link CostChangeStatusEnum}.
     */
    @Column(name = "chg_status", length = 20, nullable = false)
    private String chgStatus;

    /**
     * The reason for the assignment.
     */
    @Column(name = "chg_reason", length = 1000, nullable = true)
    private String chgReason;

    /**
     * Timestamp of request change. The initial value equals time of creation.
     */
    @Column(name = "chg_date", nullable = false)
    private Date chgDate;

    /**
     * The user id of the person who handled the request.
     */
    @Column(name = "chg_by", length = 50, nullable = true)
    private String chgBy;

    @Override
    public final String xmlName() {
        return XML_NAME;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocLog() {
        return docLog;
    }

    public void setDocLog(Long docLog) {
        this.docLog = docLog;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Long getReqUser() {
        return reqUser;
    }

    public void setReqUser(Long reqUser) {
        this.reqUser = reqUser;
    }

    public BigDecimal getReqAmount() {
        return reqAmount;
    }

    public void setReqAmount(BigDecimal reqAmount) {
        this.reqAmount = reqAmount;
    }

    public String getReqReason() {
        return reqReason;
    }

    public void setReqReason(String reqReason) {
        this.reqReason = reqReason;
    }

    public Date getReqDate() {
        return reqDate;
    }

    public void setReqDate(Date reqDate) {
        this.reqDate = reqDate;
    }

    public BigDecimal getChgAmount() {
        return chgAmount;
    }

    public void setChgAmount(BigDecimal chgAmount) {
        this.chgAmount = chgAmount;
    }

    public BigDecimal getChgCost() {
        return chgCost;
    }

    public void setChgCost(BigDecimal chgCost) {
        this.chgCost = chgCost;
    }

    public String getChgType() {
        return chgType;
    }

    public void setChgType(String chgType) {
        this.chgType = chgType;
    }

    public String getChgStatus() {
        return chgStatus;
    }

    public void setChgStatus(String chgStatus) {
        this.chgStatus = chgStatus;
    }

    public String getChgReason() {
        return chgReason;
    }

    public void setChgReason(String chgReason) {
        this.chgReason = chgReason;
    }

    public Date getChgDate() {
        return chgDate;
    }

    public void setChgDate(Date chgDate) {
        this.chgDate = chgDate;
    }

    public String getChgBy() {
        return chgBy;
    }

    public void setChgBy(String chgBy) {
        this.chgBy = chgBy;
    }

}
