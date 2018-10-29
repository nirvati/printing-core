/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import org.savapage.core.jpa.schema.UserGroupV01;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = UserGroupV01.TABLE_NAME)
public class XUserGroupV01 extends XEntityVersion {

    @Id
    @Column(name = "user_group_id")
    private Long id;

    @Column(name = "group_name", length = 255, nullable = false)
    private String groupName;

    @Column(name = "full_name", length = 255, nullable = true)
    private String fullName;

    @Column(name = "initial_settings_enabled", nullable = false)
    private Boolean initialSettingsEnabled = true;

    @Column(name = "initially_restricted", nullable = false)
    private Boolean initiallyRestricted = false;

    @Column(name = "initial_credit", nullable = false, precision = 8, scale = 2)
    private BigDecimal initialCredit = BigDecimal.ZERO;

    @Column(name = "initial_overdraft", nullable = false,
            precision = DECIMAL_PRECISION_16, scale = DECIMAL_SCALE_6)
    private BigDecimal initialOverdraft = BigDecimal.ZERO;

    @Column(name = "initial_use_global_overdraft", nullable = false)
    private Boolean initialUseGlobalOverdraft = Boolean.TRUE;

    /**
     * NONE | DAILY | WEEKLY | MONTHLY | CUSTOM
     * <p>
     * When CUSTOM, see UserGroupAttr attribute "????" with a comma separated
     * list of dates in the form YYYY-MM-DD. (e.g.
     * 2010-03-15,2010-08-20,*-08-01,*-*-01)
     * </p>
     */
    @Column(name = "schedule_period", length = 10, nullable = false)
    private String schedulePeriod;

    @Column(name = "schedule_amount", nullable = false, precision = 8,
            scale = 2)
    private BigDecimal scheduleAmount = BigDecimal.ZERO;

    @Column(name = "allow_accum", nullable = false)
    private Boolean allowAccumulation = false;

    @Column(name = "max_accum_balance", nullable = false, precision = 8,
            scale = 2)
    private BigDecimal maxAccumulationBalance = BigDecimal.ZERO;

    /**
     * Needed for bulk operations (?)
     */
    @Column(name = "reset_statistics", nullable = false)
    private Boolean resetStatistics = false;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "modified_date", nullable = true)
    private Date modifiedDate;

    @Column(name = "modified_by", length = 50, nullable = true)
    private String modifiedBy;

    @Override
    public final String xmlName() {
        return "UserGroup";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getInitialSettingsEnabled() {
        return initialSettingsEnabled;
    }

    public void setInitialSettingsEnabled(Boolean initialSettingsEnabled) {
        this.initialSettingsEnabled = initialSettingsEnabled;
    }

    public Boolean getInitiallyRestricted() {
        return initiallyRestricted;
    }

    public void setInitiallyRestricted(Boolean initiallyRestricted) {
        this.initiallyRestricted = initiallyRestricted;
    }

    public BigDecimal getInitialCredit() {
        return initialCredit;
    }

    public void setInitialCredit(BigDecimal initialCredit) {
        this.initialCredit = initialCredit;
    }

    public BigDecimal getInitialOverdraft() {
        return initialOverdraft;
    }

    public void setInitialOverdraft(BigDecimal initialOverdraft) {
        this.initialOverdraft = initialOverdraft;
    }

    public Boolean getInitialUseGlobalOverdraft() {
        return initialUseGlobalOverdraft;
    }

    public void
            setInitialUseGlobalOverdraft(Boolean initialUseGlobalOverdraft) {
        this.initialUseGlobalOverdraft = initialUseGlobalOverdraft;
    }

    public String getSchedulePeriod() {
        return schedulePeriod;
    }

    public void setSchedulePeriod(String schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
    }

    public BigDecimal getScheduleAmount() {
        return scheduleAmount;
    }

    public void setScheduleAmount(BigDecimal scheduleAmount) {
        this.scheduleAmount = scheduleAmount;
    }

    public Boolean getAllowAccumulation() {
        return allowAccumulation;
    }

    public void setAllowAccumulation(Boolean allowAccumulation) {
        this.allowAccumulation = allowAccumulation;
    }

    public BigDecimal getMaxAccumulationBalance() {
        return maxAccumulationBalance;
    }

    public void setMaxAccumulationBalance(BigDecimal maxAccumulationBalance) {
        this.maxAccumulationBalance = maxAccumulationBalance;
    }

    public Boolean getResetStatistics() {
        return resetStatistics;
    }

    public void setResetStatistics(Boolean resetStatistics) {
        this.resetStatistics = resetStatistics;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

}
