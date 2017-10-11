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
package org.savapage.core.jpa.schema;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
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
@Table(name = CostChangeV01.TABLE_NAME,
        indexes = {
                //
                @Index(name = "ix_cost_change_1", columnList = "req_date"),
                //
                @Index(name = "ix_cost_change_2", columnList = "chg_type"),
                //
                @Index(name = "ix_cost_change_3", columnList = "chg_status"),
                //
                @Index(name = "ix_cost_change_4", columnList = "chg_date"),
                //
                @Index(name = "ix_cost_change_5", columnList = "chg_by"),
                //
                @Index(name = "ix_cost_change_6", columnList = "doc_id"),
                //
                @Index(name = "ix_cost_change_7", columnList = "req_user_id")
        //
        })
public class CostChangeV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_cost_change";

    @Id
    @Column(name = "cost_change_id")
    @TableGenerator(name = "costChangePropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE", pkColumnValue = TABLE_NAME,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "costChangePropGen")
    private Long id;

    /**
     * The related document. Note that this is NOT @OneToOne, since the cost of
     * one (1) DocLog can changed more than once.
     */
    @ManyToOne
    @JoinColumn(name = "doc_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_COST_CHANGE_TO_DOCLOG"))
    private DocLogV01 docLog;

    /**
     * Currency Code: BTC, EUR, USD, etc.
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    /**
     * The requesting user. Can be {@code null}, when requester is "admin", or
     * irrelevant.
     */
    @ManyToOne
    @JoinColumn(name = "req_user_id", nullable = true,
            foreignKey = @ForeignKey(name = "FK_COST_CHANGE_TO_USER"))
    private UserV01 reqUser;

    /**
     * The LAZY AccountTrx list.
     */
    @OneToMany(targetEntity = AccountTrxV01.class, mappedBy = "costChange",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountTrxV01> transactions;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DocLogV01 getDocLog() {
        return docLog;
    }

    public void setDocLog(DocLogV01 docLog) {
        this.docLog = docLog;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public UserV01 getReqUser() {
        return reqUser;
    }

    public void setReqUser(UserV01 reqUser) {
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

    public List<AccountTrxV01> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AccountTrxV01> transactions) {
        this.transactions = transactions;
    }

}
