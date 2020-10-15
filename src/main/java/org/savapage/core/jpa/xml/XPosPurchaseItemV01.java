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
package org.savapage.core.jpa.xml;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.PosPurchaseItemV01;
import org.savapage.core.jpa.schema.PosPurchaseV01;

/**
 * An item related to a {@link PosPurchaseV01}.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PosPurchaseItemV01.TABLE_NAME)
public class XPosPurchaseItemV01 extends XEntityVersion {

    @Id
    @Column(name = "pos_purchase_item_id")
    private Long id;

    @Column(name = "pos_purchase_id", nullable = false)
    private Long purchase;

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

    @Override
    public final String xmlName() {
        return "PosPurchaseItem";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPurchase() {
        return purchase;
    }

    public void setPurchase(Long purchase) {
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
