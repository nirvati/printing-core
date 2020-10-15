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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * A Point-of-Sale item.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PosItem.TABLE_NAME)
public class PosItem extends org.savapage.core.jpa.Entity {

    public static final String TABLE_NAME = "tbl_pos_item";

    public static final String COL_ITEM_NAME = "item_name";

    private static final String TABLE_GENERATOR_NAME = "posItemPropGen";

    @Id
    @Column(name = "pos_item_id")
    @TableGenerator(name = TABLE_GENERATOR_NAME, table = Sequence.TABLE_NAME,
            pkColumnName = Sequence.COL_SEQUENCE_NAME,
            valueColumnName = Sequence.COL_SEQUENCE_NEXT_VALUE,
            pkColumnValue = TABLE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = TABLE_GENERATOR_NAME)
    private Long id;

    /**
     * The unique name of the item.
     */
    @Column(name = COL_ITEM_NAME, length = 255, nullable = false)
    private String name;

    /**
     * The cost of the item.
     */
    @Column(name = "item_cost", nullable = false,
            precision = DECIMAL_PRECISION_6, scale = DECIMAL_SCALE_2)
    private BigDecimal cost;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

}
