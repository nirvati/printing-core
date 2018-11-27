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
package org.savapage.core.doc.store;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocStoreConfig {

    /** */
    private final boolean enabled;
    /** */
    private final int daysToKeep;

    /** */
    private final DocStoreTypeEnum store;

    /** */
    private final DocStoreBranchEnum branch;

    /**
     *
     * @param enabled
     *            {@code true} if enabled.
     * @param daysToKeep
     *            Number of days to keep documents in store.
     */
    public DocStoreConfig(final DocStoreTypeEnum store,
            final DocStoreBranchEnum branch, final boolean enabled,
            final int daysToKeep) {
        this.store = store;
        this.branch = branch;
        this.enabled = enabled;
        this.daysToKeep = daysToKeep;
    }

    /**
     *
     * @return {@code true} if enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return Number of days to keep documents in store.
     */
    public int getDaysToKeep() {
        return daysToKeep;
    }

    /**
     * @return The store.
     */
    public DocStoreTypeEnum getStore() {
        return store;
    }

    /**
     * @return The branch.
     */
    public DocStoreBranchEnum getBranch() {
        return branch;
    }

}
