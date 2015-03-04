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
package org.savapage.core.print.smartschool;

import java.io.IOException;

import org.savapage.core.SpException;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.services.helpers.ExternalSupplierData;

/**
 * Data of SmartSchool Print Request.
 *
 * @author Datraverse B.V.
 *
 */
public final class SmartSchoolPrintInData extends JsonAbstractBase implements
        ExternalSupplierData {

    /**
     * The unique SmartSchool account name.
     */
    private String account;

    private Integer copies;

    private IppMediaSizeEnum mediaSize;

    private Boolean duplex;

    private Boolean color;

    /**
     *
     * @return The unique SmartSchool account name.
     */
    public String getAccount() {
        return account;
    }

    /**
     *
     * @param account
     *            The unique SmartSchool account name.
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     *
     * @return
     */
    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public IppMediaSizeEnum getMediaSize() {
        return mediaSize;
    }

    public void setMediaSize(IppMediaSizeEnum mediaSize) {
        this.mediaSize = mediaSize;
    }

    public Boolean getDuplex() {
        return duplex;
    }

    public void setDuplex(Boolean duplex) {
        this.duplex = duplex;
    }

    public Boolean getColor() {
        return color;
    }

    public void setColor(Boolean color) {
        this.color = color;
    }

    @Override
    public String dataAsString() {
        try {
            return this.stringify();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    };

}
