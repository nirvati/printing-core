/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.tools.DbTools;

/**
 * IMPORTANT: this class should be set in {@link DbTools} (private member
 * entityClassSequence).
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = Sequence.TABLE_NAME)
public class Sequence extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_sequences";

    /**
     *
     */
    public static final String COL_SEQUENCE_NAME = "SEQUENCE_NAME";

    /**
     *
     */
    public static final String COL_SEQUENCE_NEXT_VALUE = "SEQUENCE_NEXT_VALUE";

    @Id
    @Column(name = COL_SEQUENCE_NAME)
    private String name;

    @Column(name = COL_SEQUENCE_NEXT_VALUE)
    private Long value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

}
