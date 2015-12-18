/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import org.savapage.core.dao.enums.ExternalSupplierEnum;

/**
 * Information about from an {@link ExternalSupplierEnum} with
 * {@link ExternalSupplierData} .
 *
 * @author Rijk Ravestein
 *
 */
public final class ExternalSupplierInfo {

    private ExternalSupplierEnum supplier;

    private String id;

    private String status;

    /**
     * Data supplied by the external source.
     */
    private ExternalSupplierData data;

    /**
     *
     * @return
     */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ExternalSupplierEnum getSupplier() {
        return supplier;
    }

    public void setSupplier(ExternalSupplierEnum supplier) {
        this.supplier = supplier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ExternalSupplierData getData() {
        return data;
    }

    public void setData(ExternalSupplierData data) {
        this.data = data;
    }

}
