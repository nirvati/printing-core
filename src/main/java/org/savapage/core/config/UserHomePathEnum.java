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
package org.savapage.core.config;

import java.io.File;

/**
 * User home paths.
 *
 * @author Rijk Ravestein
 *
 */
public enum UserHomePathEnum {

    /** */
    LETTERHEADS("letterheads"),

    /** */
    OUTBOX("outbox"),

    /** */
    PGP_PUBRING("pgp.pubring");

    /** */
    private final String path;

    /**
     *
     * @param subdir
     *            Relative path in User SafaPages directory.
     */
    UserHomePathEnum(final String subdir) {
        this.path = subdir;
    }

    /**
     * @return Relative path in User SafaPages directory.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @param userid
     *            User id.
     * @return Full path of user subdirectory.
     */
    public String getFullPath(final String userid) {
        return String.format("%s%c%s", ConfigManager.getUserHomeDir(userid),
                File.separatorChar, this.getPath());
    }
}