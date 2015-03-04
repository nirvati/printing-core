/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.jpa.schema;

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
import javax.persistence.UniqueConstraint;

import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.PosPurchaseItem;

/**
 * A point-of-sale purchase.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = PosPurchaseV01.TABLE_NAME,
        uniqueConstraints = { @UniqueConstraint(
                columnNames = { PosPurchaseV01.COL_RECEIPT_NUM },
                name = "uc_pos_purchase_1") })
public class PosPurchaseV01 extends org.savapage.core.jpa.Entity implements
        SchemaEntityVersion {

    public static final String TABLE_NAME = "tbl_pos_purchase";

    public static final String COL_RECEIPT_NUM = "receipt_num";

    private static final String TABLE_GENERATOR_NAME = "posPurchasePropGen";

    @Id
    @Column(name = "pos_purchase_id")
    @TableGenerator(name = TABLE_GENERATOR_NAME,
            table = SequenceV01.TABLE_NAME,
            //
            pkColumnName = SequenceV01.COL_SEQUENCE_NAME,
            valueColumnName = SequenceV01.COL_SEQUENCE_NEXT_VALUE,
            //
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = TABLE_GENERATOR_NAME)
    private Long id;

    /**
     * The LAZY {@link PosPurchaseItem} list.
     */
    @OneToMany(targetEntity = PosPurchaseItemV01.class, mappedBy = "purchase",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PosPurchaseItemV01> items;

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
    private AccountTrxV01 accountTrx;

    @Column(name = "comment", length = 255)
    private String comment;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PosPurchaseItemV01> getItems() {
        return items;
    }

    public void setItems(List<PosPurchaseItemV01> items) {
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

    public AccountTrxV01 getAccountTrx() {
        return accountTrx;
    }

    public void setAccountTrx(AccountTrxV01 accountTrx) {
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
