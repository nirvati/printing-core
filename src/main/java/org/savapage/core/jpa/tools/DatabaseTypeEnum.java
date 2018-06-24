/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.jpa.tools;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum DatabaseTypeEnum {

    /**
     * The internal Derby database.
     */
    Internal(DatabaseTypeEnum.INTERNAL, "Derby"),

    /**
     * The internal Derby database.
     */
    PostgreSQL(DatabaseTypeEnum.POSTGRES, DatabaseTypeEnum.POSTGRES);

    private static final String POSTGRES = "PostgreSQL";
    private static final String INTERNAL = "Internal";

    /**
     * The unique ID used in *.properties file.
     */
    private final String propertiesId;

    /**
     * The subdirectory name in DB script directory.
     */
    final private String scriptSubdir;

    private DatabaseTypeEnum(final String propertiesId, final String subdir) {
        this.propertiesId = propertiesId;
        this.scriptSubdir = subdir;
    }

    /**
     * @return The unique ID used in *.properties file.
     */
    public String getPropertiesId() {
        return propertiesId;
    }

    /**
     * @return The subdirectory name in DB script directory.
     */
    public String getScriptSubdir() {
        return scriptSubdir;
    }

}
