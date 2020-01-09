/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.jpa.xml;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.PosPurchaseV01;

/**
 * A point-of-sale purchase.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PosPurchaseV01.TABLE_NAME)
public class XPosPurchaseV01 extends XEntityVersion {

    @Id
    @Column(name = "pos_purchase_id")
    private Long id;

    /**
     * The unique receipt number.
     */
    @Column(name = PosPurchaseV01.COL_RECEIPT_NUM, length = 255,
            nullable = false)
    private String receiptNumber;

    /**
     * The total cost.
     */
    @Column(name = "total_cost", nullable = false,
            precision = DECIMAL_PRECISION_8, scale = DECIMAL_SCALE_2)
    private BigDecimal totalCost;

    @Column(name = "payment_comment", length = 255)
    private String comment;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Override
    public final String xmlName() {
        return "PosPurchase";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

}
