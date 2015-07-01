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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.services.helpers.ExternalSupplierEnum;

/**
 * Document Log.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DocLog.TABLE_NAME)
public class DocLog extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_doc_log";

    /**
     * .
     */
    public static final int COL_TITLE_LENGTH = 255;

    /**
     * .
     */
    public static final int COL_LOG_COMMENT_LENGTH = 255;

    @Id
    @Column(name = "doc_id")
    @TableGenerator(name = "doclogPropGen", table = Sequence.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE",
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "doclogPropGen")
    private Long id;

    /**
     * foreignKey = @ForeignKey(name = "FK_DOC_LOG_TO_USER")
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_date", nullable = false, insertable = true,
            updatable = false)
    private Date createdDate;

    @Column(name = "created_day", nullable = false, insertable = true,
            updatable = false)
    private Date createdDay;

    /**
     * See {@link java.util.UUID#randomUUID()};
     */
    @Column(name = "uuid", length = 64, nullable = false, insertable = true,
            updatable = true)
    private String uuid;

    /**
     * The name of the document.
     * <p>
     * Length is maximized according to IPP "job-name" (name(MAX)).
     * </p>
     */
    @Column(name = "title", length = COL_TITLE_LENGTH, nullable = true,
            insertable = true, updatable = true)
    private String title;

    @Column(name = "protocol", length = 16, nullable = false,
            insertable = true, updatable = true)
    private String deliveryProtocol;

    /**
     * The number of pages in the input or output document. In case of a
     * {@link PrintOut} this is the number of pages of the single document, i.e.
     * the number is NOT multiplied by the number of copies.
     */
    @Column(name = "total_pages", nullable = true, insertable = true,
            updatable = true)
    private Integer numberOfPages;

    @Column(name = "size_bytes", nullable = false, insertable = true,
            updatable = false)
    private Long numberOfBytes;

    /**
     * <p>
     * According to RFC 4288
     * "Media Type Specifications and Registration Procedures", type (eg.
     * "application") and subtype (eg "vnd...") both can be max 127 characters.
     * That makes 127+1+127 = 255 if you include the slash.
     * </p>
     */
    @Column(name = "mimetype", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String mimetype;

    @Column(name = "drm_restricted", nullable = false, insertable = true,
            updatable = true)
    private Boolean drmRestricted = false;

    /**
     * The optional EAGER DocIn association.
     * <p>
     * foreignKey = @ForeignKey(name = "FK_DOC_LOG_TO_DOC_IN")
     * </p>
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "doc_in_id", nullable = true)
    private DocIn docIn;

    /**
     * The optional EAGER DocOut association.
     * <p>
     * foreignKey = @ForeignKey(name = "FK_DOC_LOG_TO_DOC_OUT")
     * </p>
     */
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER,
            optional = true)
    @JoinColumn(name = "doc_out_id", nullable = true)
    private DocOut docOut;

    /**
     * The LAZY AccountTrx list.
     */
    @OneToMany(targetEntity = AccountTrx.class, mappedBy = "docLog",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountTrx> transactions;

    /**
     *
     */
    @Column(name = "cost", nullable = false, precision = 10, scale = 6)
    private BigDecimal cost = BigDecimal.ZERO;

    /**
     *
     */
    @Column(name = "cost_original", nullable = false, precision = 10, scale = 6)
    private BigDecimal costOriginal = BigDecimal.ZERO;;

    /**
     *
     */
    @Column(name = "log_comment", length = COL_LOG_COMMENT_LENGTH)
    private String logComment;

    /**
     * for future use
     */
    @Column(name = "invoiced", nullable = false)
    private Boolean invoiced = true;

    /**
     * for future use
     */
    @Column(name = "refunded", nullable = false)
    private Boolean refunded = false;

    /**
     * See {@link ExternalSupplierEnum}.
     */
    @Column(name = "ext_supplier", length = 16)
    private String externalSupplier;

    /**
     * .
     */
    @Column(name = "ext_id", length = 64)
    private String externalId;

    /**
     * .
     */
    @Column(name = "ext_status", length = 16)
    private String externalStatus;

    /**
     * .
     */
    @Column(name = "ext_data", length = 2000)
    private String externalData;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getCreatedDay() {
        return createdDay;
    }

    public void setCreatedDay(Date createdDay) {
        this.createdDay = createdDay;
    }

    public String getDeliveryProtocol() {
        return deliveryProtocol;
    }

    public void setDeliveryProtocol(String deliveryProtocol) {
        this.deliveryProtocol = deliveryProtocol;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Sets the document title.
     * <p>
     * Note: the title is truncated, to protect overflow (when user print input
     * is used).
     * </p>
     *
     * @param rawTitle
     *            The raw title.
     */
    public final void setTitle(final String rawTitle) {
        this.title = StringUtils.abbreviate(rawTitle, COL_TITLE_LENGTH);
    }

    /**
     * @return The number of pages in the input or output document. In case of a
     *         {@link PrintOut} this is the number of pages of a single
     *         document, i.e. the number is NOT multiplied by the number of
     *         copies.
     */
    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    /**
     *
     * @param numberOfPages
     *            The number of pages in the input or output document. In case
     *            of a {@link PrintOut} this is the number of pages of a single
     *            document, i.e. the number is NOT multiplied by the number of
     *            copies.
     */
    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public Long getNumberOfBytes() {
        return numberOfBytes;
    }

    public void setNumberOfBytes(Long numberOfBytes) {
        this.numberOfBytes = numberOfBytes;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public Boolean getDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(Boolean drmRestricted) {
        this.drmRestricted = drmRestricted;
    }

    public DocIn getDocIn() {
        return docIn;
    }

    public void setDocIn(DocIn docIn) {
        this.docIn = docIn;
    }

    public DocOut getDocOut() {
        return docOut;
    }

    public void setDocOut(DocOut docOut) {
        this.docOut = docOut;
    }

    public List<AccountTrx> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AccountTrx> transactions) {
        this.transactions = transactions;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getCostOriginal() {
        return costOriginal;
    }

    public void setCostOriginal(BigDecimal costOriginal) {
        this.costOriginal = costOriginal;
    }

    public String getLogComment() {
        return logComment;
    }

    /**
     * Sets the log comment.
     * <p>
     * Note: the comment is truncated, to protect overflow (when user input is
     * used, or comment is provided by external supplier).
     * </p>
     *
     * @param rawComment
     *            The raw log comment.
     */
    public final void setLogComment(final String rawComment) {
        this.logComment =
                StringUtils.abbreviate(rawComment, COL_LOG_COMMENT_LENGTH);
    }

    public Boolean getInvoiced() {
        return invoiced;
    }

    public void setInvoiced(Boolean invoiced) {
        this.invoiced = invoiced;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    /**
     *
     * @return See {@link ExternalSupplierEnum}.
     */
    public String getExternalSupplier() {
        return externalSupplier;
    }

    /**
     *
     * @param externalSupplier
     *            See {@link ExternalSupplierEnum}.
     */
    public void setExternalSupplier(String externalSupplier) {
        this.externalSupplier = externalSupplier;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getExternalData() {
        return externalData;
    }

    public void setExternalData(String externalData) {
        this.externalData = externalData;
    }

}
