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

import org.savapage.core.jpa.Entity;

/**
 * JPA Entity Version class for XML export/import.
 * <p>
 * NOTES FOR DEVELOPERS.
 * </p>
 * <p>
 * Extensions of this class must be a copy of sibling in the
 * {@link org.savapage.core.jpa.schema} package.
 * </p>
 * <p>
 * <i>The following changes should be applied:</i>
 * </p>
 * <ol>
 * <li>Primary Key
 * <ul>
 * <li><b>Remove</b> <code>@TableGenerator</code> annotation</li>
 * <li><b>Remove</b> <code>@GeneratedValue</code> annotation</li>
 * </ul>
 * </li>
 *
 * <li><code>@ManyToOne</code> with <code>@JoinColumn</code>:
 * <ul>
 * <li><b>Remove</b> <code>@ManyToOne</code></li>
 * <li><b>Replace</b> <code>@JoinColumn</code> with <code>@Column</code></li>
 * <li><b>Replace</b> current type with type {@link Long} (change setter and
 * getter accordingly).</li>
 * </ul>
 * </li>
 *
 * <li><b>Remove</b> static String <code>TABLE_NAME</code></li>
 *
 * <li><code>@OneToMany</code> annotated attributes (including setter and
 * getter) must be <b>removed</b>.</li>
 *
 * <li><code>@OneToOne</code> annotated attributes which do NOT have a
 * <code>@JoinColumn</code> must be <b>removed</b> (including setter and
 * getter).</li>
 *
 * <li>NOTE: Since {@link javax.persistence.UniqueConstraint},
 * {@link javax.persistence.Index} and {@link javax.persistence.ForeignKey}
 * annotation are exclusively needed for schema (DDL) generation they are NOT
 * needed here.</li>
 *
 * </ol>
 *
 * @author Datraverse B.V.
 *
 */
public abstract class XEntityVersion extends Entity {

    /**
     *
     * @return
     */
    public abstract String xmlName();

}
