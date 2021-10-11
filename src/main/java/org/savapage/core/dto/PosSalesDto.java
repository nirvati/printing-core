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
package org.savapage.core.dto;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.services.helpers.account.UserAccountContextEnum;

/**
 * A POS sales.
 *
 * @author Rijk Ravestein
 *
 */
public final class PosSalesDto extends PosTransactionDto {

    /**
     * User DB key of the purchaser.
     */
    private Long userKey;

    private UserAccountContextEnum accountContext;

    private DeliveryEnum invoiceDelivery;

    private String posLocation;
    private String posShop;
    private String posItem;

    public Long getUserKey() {
        return userKey;
    }

    public void setUserKey(Long userKey) {
        this.userKey = userKey;
    }

    public UserAccountContextEnum getAccountContext() {
        return accountContext;
    }

    public void setAccountContext(UserAccountContextEnum accountContext) {
        this.accountContext = accountContext;
    }

    public DeliveryEnum getInvoiceDelivery() {
        return invoiceDelivery;
    }

    public void setInvoiceDelivery(DeliveryEnum invoiceDelivery) {
        this.invoiceDelivery = invoiceDelivery;
    }

    public String getPosLocation() {
        return posLocation;
    }

    public void setPosLocation(String posLocation) {
        this.posLocation = posLocation;
    }

    public String getPosShop() {
        return posShop;
    }

    public void setPosShop(String posShop) {
        this.posShop = posShop;
    }

    public String getPosItem() {
        return posItem;
    }

    public void setPosItem(String posItem) {
        this.posItem = posItem;
    }

    public String createComment() {

        final StringBuilder cmt = new StringBuilder();

        for (final String part : new String[] { this.posLocation, this.posShop,
                this.posItem }) {
            if (StringUtils.isNotBlank(part)) {
                if (cmt.length() > 0) {
                    cmt.append("/");
                }
                cmt.append(part);
            }
        }
        if (StringUtils.isNotBlank(this.getComment())) {
            if (cmt.length() > 0) {
                cmt.append(":");
            }
            cmt.append(this.getComment());
        }
        return cmt.toString();
    }

}
