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
package org.savapage.core.print.smartschool.xml;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 * @author Datraverse B.V
 *
 */
public class Document {

    private String id;

    private String name;

    private String comment;

    private Requester requester;

    private Billinginfo billinginfo;

    private Processinfo processinfo;

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Requester getRequester() {
        return requester;
    }

    public void setRequester(Requester requester) {
        this.requester = requester;
    }

    public Billinginfo getBillinginfo() {
        return billinginfo;
    }

    public void setBillinginfo(Billinginfo billinginfo) {
        this.billinginfo = billinginfo;
    }

    public Processinfo getProcessinfo() {
        return processinfo;
    }

    public void setProcessinfo(Processinfo processinfo) {
        this.processinfo = processinfo;
    }

}
