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
package org.savapage.core.jpa;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.savapage.core.dao.enums.CostChangeStatusEnum;
import org.savapage.core.dao.enums.CostChangeTypeEnum;

/**
 * A cost change (request) for a DocLog.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = CostChange.TABLE_NAME)
public class CostChange extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_cost_change";

    @Id
    @Column(name = "cost_change_id")
    @TableGenerator(name = "costChangePropGen", table = Sequence.TABLE_NAME,
            pkColumnName = Sequence.COL_SEQUENCE_NAME,
            valueColumnName = Sequence.COL_SEQUENCE_NEXT_VALUE,
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "costChangePropGen")
    private Long id;

    /**
     * The related document. Note that this is NOT @OneToOne, since the cost of
     * one (1) DocLog can changed more than once.
     * <p>
     * foreignKey = @ForeignKey(name = "FK_COST_CHANGE_TO_DOCLOG")
     * </p>
     */
    @ManyToOne
    @JoinColumn(name = "doc_id", nullable = false)
    private DocLog docLog;

    /**
     * Currency Code: BTC, EUR, USD, etc.
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    /**
     * The requesting user. Can be {@code null}, when requester is "admin", or
     * irrelevant.
     * <p>
     * foreignKey = @ForeignKey(name = "FK_COST_CHANGE_TO_USER")
     * </p>
     */
    @ManyToOne
    @JoinColumn(name = "req_user_id", nullable = true)
    private User reqUser;

    /**
     * The LAZY AccountTrx list.
     */
    @OneToMany(targetEntity = AccountTrx.class, mappedBy = "costChange",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountTrx> transactions;

    /**
     * The requested currency amount of the change.
     */
    @Column(name = "req_amount", nullable = false,
            precision = DECIMAL_PRECISION_16, scale = DECIMAL_SCALE_8)
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
            precision = DECIMAL_PRECISION_16, scale = DECIMAL_SCALE_8)
    private BigDecimal chgAmount;

    /**
     * The DocLog cost after the assigned currency amount change.
     */
    @Column(name = "chg_cost", nullable = true,
            precision = DECIMAL_PRECISION_16, scale = DECIMAL_SCALE_8)
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DocLog getDocLog() {
        return docLog;
    }

    public void setDocLog(DocLog docLog) {
        this.docLog = docLog;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public User getReqUser() {
        return reqUser;
    }

    public void setReqUser(User reqUser) {
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

    public List<AccountTrx> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AccountTrx> transactions) {
        this.transactions = transactions;
    }

}
