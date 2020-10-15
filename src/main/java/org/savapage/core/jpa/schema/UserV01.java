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
package org.savapage.core.jpa.schema;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = UserV01.TABLE_NAME, indexes = { //
        // Note that we can NOT use a unique index since Users are
        // LOGICALLY deleted.
        @Index(name = "ix_user_1", columnList = "user_name"), //
        @Index(name = "ix_user_2", columnList = "full_name")//
})
public class UserV01 implements SchemaEntityVersion {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_user";

    @Id
    @Column(name = "user_id")
    @TableGenerator(name = "userPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "userPropGen")
    private Long id;

    @Column(name = "user_name", length = 50, nullable = false,
            insertable = true, updatable = true)
    private String userId;

    @Column(name = "external_user_name", length = 255, nullable = false,
            insertable = true, updatable = true)
    private String externalUserName;

    @Column(name = "full_name", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String fullName;

    @Column(name = "department", length = 200, nullable = true,
            insertable = true, updatable = true)
    private String department;

    @Column(name = "office", length = 200, nullable = true, insertable = true,
            updatable = true)
    private String office;

    @Column(name = "admin", nullable = false, insertable = true,
            updatable = true)
    private Boolean admin = false;

    @Column(name = "person", nullable = false, insertable = true,
            updatable = true)
    private Boolean person = true;

    @Column(name = "internal", nullable = false, insertable = true,
            updatable = true)
    private Boolean internal = false;

    @Column(name = "disabled_print_in", nullable = false, insertable = true,
            updatable = true)
    private Boolean disabledPrintIn = false;

    @Column(name = "disabled_print_in_until", nullable = true,
            insertable = true, updatable = true)
    private Date disabledPrintInUntil;

    @Column(name = "disabled_print_out", nullable = false, insertable = true,
            updatable = true)
    private Boolean disabledPrintOut = false;

    @Column(name = "disabled_print_out_until", nullable = true,
            insertable = true, updatable = true)
    private Date disabledPrintOutUntil;

    @Column(name = "disabled_pdf_out", nullable = false, insertable = true,
            updatable = true)
    private Boolean disabledPdfOut = false;

    @Column(name = "disabled_pdf_out_until", nullable = true, insertable = true,
            updatable = true)
    private Date disabledPdfOutUntil;

    //
    @Column(name = "total_print_in_jobs", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfPrintInJobs = 0;

    @Column(name = "total_print_in_pages", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfPrintInPages = 0;

    @Column(name = "total_print_in_bytes", nullable = false, insertable = true,
            updatable = true)
    private Long numberOfPrintInBytes = 0L;

    //
    @Column(name = "total_pdf_out_jobs", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfPdfOutJobs = 0;

    @Column(name = "total_pdf_out_pages", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfPdfOutPages = 0;

    @Column(name = "total_pdf_out_bytes", nullable = false, insertable = true,
            updatable = true)
    private Long numberOfPdfOutBytes = 0L;

    //
    @Column(name = "total_print_out_jobs", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfPrintOutJobs = 0;

    @Column(name = "total_print_out_pages", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfPrintOutPages = 0;

    @Column(name = "total_print_out_bytes", nullable = false, insertable = true,
            updatable = true)
    private Long numberOfPrintOutBytes = 0L;

    @Column(name = "total_print_out_sheets", nullable = false,
            insertable = true, updatable = true)
    private Integer numberOfPrintOutSheets = 0;

    @Column(name = "total_print_out_esu", nullable = false, insertable = true,
            updatable = true)
    private Long numberOfPrintOutEsu = 0L;

    @Column(name = "deleted", nullable = false, insertable = true,
            updatable = true)
    private Boolean deleted = false;

    @Column(name = "deleted_date", nullable = true, insertable = true,
            updatable = true)
    private Date deletedDate;

    @Column(name = "created_date", nullable = false, insertable = true,
            updatable = true)
    private Date createdDate;

    @Column(name = "created_by", length = 50, nullable = false,
            insertable = true, updatable = true)
    private String createdBy;

    @Column(name = "modified_date", nullable = true, insertable = true,
            updatable = true)
    private Date modifiedDate;

    @Column(name = "modified_by", length = 50, nullable = true,
            insertable = true, updatable = true)
    private String modifiedBy;

    @Column(name = "reset_by", length = 50, nullable = true, insertable = true,
            updatable = true)
    private String resetBy;

    @Column(name = "reset_date", nullable = true, insertable = true,
            updatable = true)
    private Date resetDate;

    @Column(name = "last_user_activity", nullable = true, insertable = true,
            updatable = true)
    private Date lastUserActivity;

    /**
     * The LAZY DocLog list.
     */
    @OneToMany(targetEntity = DocLogV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocLogV01> docLog;

    /**
     * The LAZY UserAttr list.
     */
    @OneToMany(targetEntity = UserAttrV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAttrV01> attributes;

    /**
     * The LAZY UserCard list.
     */
    @OneToMany(targetEntity = UserCardV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserCardV01> cards;

    /**
     * The LAZY UserNumber list.
     */
    @OneToMany(targetEntity = UserNumberV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserNumberV01> idNumbers;

    /**
     * The LAZY UserEmail list.
     */
    @OneToMany(targetEntity = UserEmailV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserEmailV01> emails;

    /**
     * The LAZY UserAccount list.
     */
    @OneToMany(targetEntity = UserAccountV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAccountV01> accounts;

    /**
     * The LAZY {@link UserGroupMemberV01} list.
     */
    @OneToMany(targetEntity = UserGroupMemberV01.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGroupMemberV01> groupMembership;

    /**
     * The LAZY CostChange list.
     */
    @OneToMany(targetEntity = CostChangeV01.class, mappedBy = "reqUser",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CostChangeV01> costChanges;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getExternalUserName() {
        return externalUserName;
    }

    public void setExternalUserName(String externalUserName) {
        this.externalUserName = externalUserName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setPerson(Boolean person) {
        this.person = person;
    }

    public Boolean getPerson() {
        return person;
    }

    public Boolean getInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    public Integer getNumberOfPrintInJobs() {
        return numberOfPrintInJobs;
    }

    public void setNumberOfPrintInJobs(Integer numberOfPrintInJobs) {
        this.numberOfPrintInJobs = numberOfPrintInJobs;
    }

    public Integer getNumberOfPrintInPages() {
        return numberOfPrintInPages;
    }

    public void setNumberOfPrintInPages(Integer numberOfPrintInPages) {
        this.numberOfPrintInPages = numberOfPrintInPages;
    }

    public Long getNumberOfPrintInBytes() {
        return numberOfPrintInBytes;
    }

    public void setNumberOfPrintInBytes(Long numberOfPrintInBytes) {
        this.numberOfPrintInBytes = numberOfPrintInBytes;
    }

    public Integer getNumberOfPdfOutJobs() {
        return numberOfPdfOutJobs;
    }

    public void setNumberOfPdfOutJobs(Integer numberOfPdfOutJobs) {
        this.numberOfPdfOutJobs = numberOfPdfOutJobs;
    }

    public Integer getNumberOfPdfOutPages() {
        return numberOfPdfOutPages;
    }

    public void setNumberOfPdfOutPages(Integer numberOfPdfOutPages) {
        this.numberOfPdfOutPages = numberOfPdfOutPages;
    }

    public Long getNumberOfPdfOutBytes() {
        return numberOfPdfOutBytes;
    }

    public void setNumberOfPdfOutBytes(Long numberOfPdfOutBytes) {
        this.numberOfPdfOutBytes = numberOfPdfOutBytes;
    }

    public Integer getNumberOfPrintOutJobs() {
        return numberOfPrintOutJobs;
    }

    public void setNumberOfPrintOutJobs(Integer numberOfPrintOutJobs) {
        this.numberOfPrintOutJobs = numberOfPrintOutJobs;
    }

    public Integer getNumberOfPrintOutPages() {
        return numberOfPrintOutPages;
    }

    public void setNumberOfPrintOutPages(Integer numberOfPrintOutPages) {
        this.numberOfPrintOutPages = numberOfPrintOutPages;
    }

    public Long getNumberOfPrintOutBytes() {
        return numberOfPrintOutBytes;
    }

    public void setNumberOfPrintOutBytes(Long numberOfPrintOutBytes) {
        this.numberOfPrintOutBytes = numberOfPrintOutBytes;
    }

    public Integer getNumberOfPrintOutSheets() {
        return numberOfPrintOutSheets;
    }

    public void setNumberOfPrintOutSheets(Integer numberOfPrintOutSheets) {
        this.numberOfPrintOutSheets = numberOfPrintOutSheets;
    }

    public Long getNumberOfPrintOutEsu() {
        return numberOfPrintOutEsu;
    }

    public void setNumberOfPrintOutEsu(Long numberOfPrintOutEsu) {
        this.numberOfPrintOutEsu = numberOfPrintOutEsu;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
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

    public String getResetBy() {
        return resetBy;
    }

    public void setResetBy(String resetBy) {
        this.resetBy = resetBy;
    }

    public Date getResetDate() {
        return resetDate;
    }

    public void setResetDate(Date resetDate) {
        this.resetDate = resetDate;
    }

    public Date getLastUserActivity() {
        return lastUserActivity;
    }

    public void setLastUserActivity(Date lastUserActivity) {
        this.lastUserActivity = lastUserActivity;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public Boolean getDisabledPrintIn() {
        return disabledPrintIn;
    }

    public void setDisabledPrintIn(Boolean disabledPrintIn) {
        this.disabledPrintIn = disabledPrintIn;
    }

    public Date getDisabledPrintInUntil() {
        return disabledPrintInUntil;
    }

    public void setDisabledPrintInUntil(Date disabledPrintInUntil) {
        this.disabledPrintInUntil = disabledPrintInUntil;
    }

    public Boolean getDisabledPrintOut() {
        return disabledPrintOut;
    }

    public void setDisabledPrintOut(Boolean disabledPrintOut) {
        this.disabledPrintOut = disabledPrintOut;
    }

    public Date getDisabledPrintOutUntil() {
        return disabledPrintOutUntil;
    }

    public void setDisabledPrintOutUntil(Date disabledPrintOutUntil) {
        this.disabledPrintOutUntil = disabledPrintOutUntil;
    }

    public Boolean getDisabledPdfOut() {
        return disabledPdfOut;
    }

    public void setDisabledPdfOut(Boolean disabledPdfOut) {
        this.disabledPdfOut = disabledPdfOut;
    }

    public Date getDisabledPdfOutUntil() {
        return disabledPdfOutUntil;
    }

    public void setDisabledPdfOutUntil(Date disabledPdfOutUntil) {
        this.disabledPdfOutUntil = disabledPdfOutUntil;
    }

    public List<DocLogV01> getDocLog() {
        return docLog;
    }

    public void setDocLog(List<DocLogV01> docLog) {
        this.docLog = docLog;
    }

    public List<UserAttrV01> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<UserAttrV01> attributes) {
        this.attributes = attributes;
    }

    public List<UserCardV01> getCards() {
        return cards;
    }

    public void setCards(List<UserCardV01> cards) {
        this.cards = cards;
    }

    public List<UserNumberV01> getIdNumbers() {
        return idNumbers;
    }

    public void setIdNumbers(List<UserNumberV01> idNumbers) {
        this.idNumbers = idNumbers;
    }

    public List<UserEmailV01> getEmails() {
        return emails;
    }

    public void setEmails(List<UserEmailV01> emails) {
        this.emails = emails;
    }

    public List<UserAccountV01> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<UserAccountV01> accounts) {
        this.accounts = accounts;
    }

    public List<UserGroupMemberV01> getGroupMembership() {
        return groupMembership;
    }

    public void setGroupMembership(List<UserGroupMemberV01> groupMembership) {
        this.groupMembership = groupMembership;
    }

    public List<CostChangeV01> getCostChanges() {
        return costChanges;
    }

    public void setCostChanges(List<CostChangeV01> costChanges) {
        this.costChanges = costChanges;
    }

}
