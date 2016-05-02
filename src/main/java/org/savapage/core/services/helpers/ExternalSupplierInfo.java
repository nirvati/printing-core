/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Information from an {@link ExternalSupplierEnum} with
 * {@link ExternalSupplierData} .
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class ExternalSupplierInfo {

    private ExternalSupplierEnum supplier;

    /**
     * The account used to access data from the external supplier.
     */
    private String account;

    /**
     * The ID of this info.
     */
    private String id;

    /**
     * The status of this info.
     */
    private String status;

    /**
     * Data supplied by the external source.
     */
    private ExternalSupplierData data;

    /**
     *
     * @return
     */

    public ExternalSupplierEnum getSupplier() {
        return supplier;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setSupplier(ExternalSupplierEnum supplier) {
        this.supplier = supplier;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
