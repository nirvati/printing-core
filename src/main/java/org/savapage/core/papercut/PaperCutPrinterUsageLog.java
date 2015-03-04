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
package org.savapage.core.papercut;

/**
 * @author Datraverse B.V.
 *
 */
public final class PaperCutPrinterUsageLog {

    private String documentName;
    private boolean printed;
    private boolean cancelled;
    private String deniedReason;
    private double usageCost;
    private Long accountIdCharged;
    private Long accountIdAssoc;

    /**
     *
     * @return
     */
    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public boolean isPrinted() {
        return printed;
    }

    public void setPrinted(boolean printed) {
        this.printed = printed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public double getUsageCost() {
        return usageCost;
    }

    public void setUsageCost(double usageCost) {
        this.usageCost = usageCost;
    }

    public Long getAccountIdCharged() {
        return accountIdCharged;
    }

    public void setAccountIdCharged(Long accountIdCharged) {
        this.accountIdCharged = accountIdCharged;
    }

    public Long getAccountIdAssoc() {
        return accountIdAssoc;
    }

    public void setAccountIdAssoc(Long accountIdAssoc) {
        this.accountIdAssoc = accountIdAssoc;
    }

}
