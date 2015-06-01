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

import org.savapage.core.jpa.tools.DbTools;

/**
 * Parent class for '@Entity' annotated classes.
 * <p>
 * NOTES FOR DEVELOPERS.
 * </p>
 * <ol>
 * <li>Each ancestor of this class should be added to {@link DbTools}
 * entityClasses4Schema</li>
 *
 * <li>You cannot JPA annotate a '@Column' to have a default value. Therefore
 * declare defaults to the instance variables.</li>
 * <li>Bean properties which are NOT part of the database must be annotated with
 * '@Transient'.</li>
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
public class Entity {

    public static final String ACTOR_ADMIN = "[admin]";
    public static final String ACTOR_SYSTEM = "[system]";
    public static final String ACTOR_INSTALL = "[install]";
    public static final String ACTOR_SYSTEM_API = ACTOR_SYSTEM + " (api)";

    protected static final int DECIMAL_PRECISION_6 = 6;
    protected static final int DECIMAL_PRECISION_8 = 8;
    protected static final int DECIMAL_PRECISION_10 = 10;
    protected static final int DECIMAL_PRECISION_16 = 16;

    protected static final int DECIMAL_SCALE_2 = 2;
    protected static final int DECIMAL_SCALE_6 = 6;
    protected static final int DECIMAL_SCALE_8 = 8;

    protected Entity() {
    }
}
