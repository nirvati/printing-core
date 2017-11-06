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
package org.savapage.core.jpa;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = User.TABLE_NAME)
public class User extends org.savapage.core.jpa.Entity {

    /** */
    public static final String TABLE_NAME = "tbl_user";

    /** */
    public static final String ERASED_USER_ID = "";
    /** */
    public static final String ERASED_EXTERNAL_USER_NAME = "";

    @Id
    @Column(name = "user_id")
    @TableGenerator(name = "userPropGen", table = Sequence.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE", pkColumnValue = TABLE_NAME,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "userPropGen")
    private Long id;

    @Column(name = "user_name", length = 50, nullable = false)
    private String userId;

    @Column(name = "external_user_name", length = 255, nullable = false)
    private String externalUserName;

    @Column(name = "full_name", length = 255, nullable = true)
    private String fullName;

    @Column(name = "department", length = 200, nullable = true)
    private String department;

    @Column(name = "office", length = 200, nullable = true)
    private String office;

    @Column(name = "admin", nullable = false)
    private Boolean admin = false;

    @Column(name = "person", nullable = false)
    private Boolean person = true;

    @Column(name = "internal", nullable = false)
    private Boolean internal = false;

    @Column(name = "disabled_print_in", nullable = false)
    private Boolean disabledPrintIn = false;

    @Column(name = "disabled_print_in_until", nullable = true)
    private Date disabledPrintInUntil;

    @Column(name = "disabled_print_out", nullable = false)
    private Boolean disabledPrintOut = false;

    @Column(name = "disabled_print_out_until", nullable = true)
    private Date disabledPrintOutUntil;

    @Column(name = "disabled_pdf_out", nullable = false)
    private Boolean disabledPdfOut = false;

    @Column(name = "disabled_pdf_out_until", nullable = true)
    private Date disabledPdfOutUntil;

    //
    @Column(name = "total_print_in_jobs", nullable = false)
    private Integer numberOfPrintInJobs = 0;

    @Column(name = "total_print_in_pages", nullable = false)
    private Integer numberOfPrintInPages = 0;

    @Column(name = "total_print_in_bytes", nullable = false)
    private Long numberOfPrintInBytes = 0L;

    //
    @Column(name = "total_pdf_out_jobs", nullable = false)
    private Integer numberOfPdfOutJobs = 0;

    @Column(name = "total_pdf_out_pages", nullable = false)
    private Integer numberOfPdfOutPages = 0;

    @Column(name = "total_pdf_out_bytes", nullable = false)
    private Long numberOfPdfOutBytes = 0L;

    //
    @Column(name = "total_print_out_jobs", nullable = false)
    private Integer numberOfPrintOutJobs = 0;

    @Column(name = "total_print_out_pages", nullable = false)
    private Integer numberOfPrintOutPages = 0;

    @Column(name = "total_print_out_bytes", nullable = false)
    private Long numberOfPrintOutBytes = 0L;

    @Column(name = "total_print_out_sheets", nullable = false)
    private Integer numberOfPrintOutSheets = 0;

    @Column(name = "total_print_out_esu", nullable = false)
    private Long numberOfPrintOutEsu = 0L;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_date", nullable = true)
    private Date deletedDate;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "modified_date", nullable = true)
    private Date modifiedDate;

    @Column(name = "modified_by", length = 50, nullable = true)
    private String modifiedBy;

    @Column(name = "reset_by", length = 50, nullable = true)
    private String resetBy;

    @Column(name = "reset_date", nullable = true)
    private Date resetDate;

    @Column(name = "last_user_activity", nullable = true)
    private Date lastUserActivity;

    /**
     * The LAZY DocLog list.
     */
    @OneToMany(targetEntity = DocLog.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DocLog> docLog;

    /**
     * The LAZY UserAttr list.
     */
    @OneToMany(targetEntity = UserAttr.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAttr> attributes;

    /**
     * The LAZY UserCard list.
     */
    @OneToMany(targetEntity = UserCard.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserCard> cards;

    /**
     * The LAZY UserNumber list.
     */
    @OneToMany(targetEntity = UserNumber.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserNumber> idNumbers;

    /**
     * The LAZY UserEmail list.
     */
    @OneToMany(targetEntity = UserEmail.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserEmail> emails;

    /**
     * The LAZY UserAccount list.
     */
    @OneToMany(targetEntity = UserAccount.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAccount> accounts;

    /**
     * The LAZY {@link UserGroupMember} list.
     */
    @OneToMany(targetEntity = UserGroupMember.class, mappedBy = "user",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserGroupMember> groupMembership;

    /**
     * The LAZY CostChange list.
     */
    @OneToMany(targetEntity = CostChange.class, mappedBy = "reqUser",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CostChange> costChanges;

    /**
     *
     * @param id
     */
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

    public List<DocLog> getDocLog() {
        return docLog;
    }

    public void setDocLog(List<DocLog> docLog) {
        this.docLog = docLog;
    }

    public List<UserAttr> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<UserAttr> attributes) {
        this.attributes = attributes;
    }

    public List<UserCard> getCards() {
        return cards;
    }

    public void setCards(List<UserCard> cards) {
        this.cards = cards;
    }

    public List<UserNumber> getIdNumbers() {
        return idNumbers;
    }

    public void setIdNumbers(List<UserNumber> idNumbers) {
        this.idNumbers = idNumbers;
    }

    public List<UserEmail> getEmails() {
        return emails;
    }

    public void setEmails(List<UserEmail> emails) {
        this.emails = emails;
    }

    public List<UserAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<UserAccount> accounts) {
        this.accounts = accounts;
    }

    public List<UserGroupMember> getGroupMembership() {
        return groupMembership;
    }

    public void setGroupMembership(List<UserGroupMember> groupMembership) {
        this.groupMembership = groupMembership;
    }

    public List<CostChange> getCostChanges() {
        return costChanges;
    }

    public void setCostChanges(List<CostChange> costChanges) {
        this.costChanges = costChanges;
    }

}
