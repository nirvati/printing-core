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
package org.savapage.core.doc.store;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Branch in a Document Store.
 *
 * @author Rijk Ravestein
 *
 */
public enum DocStoreBranchEnum {

    /** */
    IN_PRINT(Paths.get("in/print")),
    /** */
    OUT_PRINT(Paths.get("out/print")),
    /** */
    OUT_PDF(Paths.get("out/pdf"));

    /** */
    private final Path branch;

    /**
     *
     * @param path
     *            Branch path in a store.
     */
    DocStoreBranchEnum(final Path path) {
        this.branch = path;
    }

    /**
     *
     * @return Branch path in document store.
     */
    public Path getBranch() {
        return this.branch;
    }
}
