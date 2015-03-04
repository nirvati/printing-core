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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * An item related to a {@link PosPurchase}.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = PosPurchaseItem.TABLE_NAME)
public class PosPurchaseItem extends org.savapage.core.jpa.Entity {

    public static final String TABLE_NAME = "tbl_pos_purchase_item";

    private static final String TABLE_GENERATOR_NAME = "posPurchaseItemPropGen";

    @Id
    @Column(name = "pos_purchase_item_id")
    @TableGenerator(name = TABLE_GENERATOR_NAME, table = Sequence.TABLE_NAME,
            //
            pkColumnName = Sequence.COL_SEQUENCE_NAME,
            valueColumnName = Sequence.COL_SEQUENCE_NEXT_VALUE,
            //
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = TABLE_GENERATOR_NAME)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pos_purchase_id", nullable = false)
    private PosPurchase purchase;

    @Column(name = "item_index", nullable = false)
    private Integer itemIndex;

    /**
     * The name of the item.
     */
    @Column(name = "item_name", length = 255, nullable = false)
    private String name;

    /**
     * The number of items purchased.
     */
    @Column(name = "item_quantity", nullable = false)
    private Integer quantity;

    /**
     * The cost of a single item.
     */
    @Column(name = "item_unit_cost", nullable = false,
            precision = DECIMAL_PRECISION_6, scale = DECIMAL_SCALE_2)
    private BigDecimal unitCost;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PosPurchase getPurchase() {
        return purchase;
    }

    public void setPurchase(PosPurchase purchase) {
        this.purchase = purchase;
    }

    public Integer getItemIndex() {
        return itemIndex;
    }

    public void setItemIndex(Integer itemIndex) {
        this.itemIndex = itemIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

}
