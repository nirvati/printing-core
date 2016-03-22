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
package org.savapage.core.config.validator;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ValidationResult {

    private String value = null;
    private String message = "";

    /**
     *
     */
    private ValidationStatusEnum status = ValidationStatusEnum.OK;

    public ValidationResult(final String value) {
        this.setValue(value);
        status = ValidationStatusEnum.OK;
    }

    public ValidationResult(final String value,
            final ValidationStatusEnum status, final String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isValid() {
        return status == ValidationStatusEnum.OK;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ValidationStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ValidationStatusEnum status) {
        this.status = status;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
