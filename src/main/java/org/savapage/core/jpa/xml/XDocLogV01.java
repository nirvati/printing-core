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
package org.savapage.core.jpa.xml;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.DocLogV01;
import org.savapage.core.services.helpers.ExternalSupplierEnum;

/**
 * Document Log.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = DocLogV01.TABLE_NAME)
public class XDocLogV01 extends XEntityVersion {

    @Id
    @Column(name = "doc_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long user;

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
    @Column(name = "title", length = 255, nullable = true, insertable = true,
            updatable = true)
    private String title;

    @Column(name = "protocol", length = 16, nullable = false,
            insertable = true, updatable = true)
    private String deliveryProtocol;

    @Column(name = "total_pages", nullable = true, insertable = true,
            updatable = true)
    private Integer numberOfPages;

    @Column(name = "size_bytes", nullable = false, insertable = true,
            updatable = false)
    private Long numberOfBytes;

    @Column(name = "mimetype", length = 255, nullable = true,
            insertable = true, updatable = true)
    private String mimetype;

    @Column(name = "drm_restricted", nullable = false, insertable = true,
            updatable = true)
    private Boolean drmRestricted = false;

    @Column(name = "doc_in_id", nullable = true)
    private Long docIn;

    @Column(name = "doc_out_id", nullable = true)
    private Long docOut;

    @Column(name = "cost", nullable = false, precision = 10, scale = 6)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "cost_original", nullable = false, precision = 10, scale = 6)
    private BigDecimal costOriginal = BigDecimal.ZERO;;

    @Column(name = "log_comment", length = 255)
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

    @Override
    public final String xmlName() {
        return "DocLog";
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

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

    public Long getUser() {
        return user;
    }

    public void setUser(Long user) {
        this.user = user;
    }

    public Long getDocIn() {
        return docIn;
    }

    public void setDocIn(Long docIn) {
        this.docIn = docIn;
    }

    public Long getDocOut() {
        return docOut;
    }

    public void setDocOut(Long docOut) {
        this.docOut = docOut;
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

    public void setLogComment(String logComment) {
        this.logComment = logComment;
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
