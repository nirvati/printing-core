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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * A point-of-sale purchase.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PosPurchase.TABLE_NAME)
public class PosPurchase extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_pos_purchase";

    /**
     *
     */
    public static final String COL_RECEIPT_NUM = "receipt_num";

    private static final String TABLE_GENERATOR_NAME = "posPurchasePropGen";

    @Id
    @Column(name = "pos_purchase_id")
    @TableGenerator(name = TABLE_GENERATOR_NAME, table = Sequence.TABLE_NAME,
            //
            pkColumnName = Sequence.COL_SEQUENCE_NAME,
            valueColumnName = Sequence.COL_SEQUENCE_NEXT_VALUE,
            //
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = TABLE_GENERATOR_NAME)
    private Long id;

    /**
     * The LAZY {@link PosPurchaseItem} list.
     */
    @OneToMany(targetEntity = PosPurchaseItem.class, mappedBy = "purchase",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PosPurchaseItem> items;

    /**
     * The unique receipt number.
     */
    @Column(name = COL_RECEIPT_NUM, length = 255, nullable = false)
    private String receiptNumber;

    /**
     * The total cost.
     */
    @Column(name = "total_cost", nullable = false,
            precision = DECIMAL_PRECISION_8, scale = DECIMAL_SCALE_2)
    private BigDecimal totalCost;

    /**
     * The LAZY {@link AccountTrx} association.
     */
    @OneToOne(mappedBy = "posPurchase", cascade = { CascadeType.ALL },
            fetch = FetchType.LAZY, optional = true)
    private AccountTrx accountTrx;

    @Column(name = "payment_comment", length = 255)
    private String comment;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PosPurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<PosPurchaseItem> items) {
        this.items = items;
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

    public AccountTrx getAccountTrx() {
        return accountTrx;
    }

    public void setAccountTrx(AccountTrx accountTrx) {
        this.accountTrx = accountTrx;
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
