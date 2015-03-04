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
package org.savapage.core.json.rpc.impl;

import org.savapage.core.dao.helpers.AccessControlScopeEnum;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class ParamsPrinterAccessControl extends AbstractJsonRpcMethodParms {

    public enum Action {
        ADD, REMOVE, REMOVE_ALL, LIST
    }


    private String printerName;
    private String groupName;

    private AccessControlScopeEnum scope;
    private Action action;


    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public AccessControlScopeEnum getScope() {
        return scope;
    }

    public void setScope(AccessControlScopeEnum scope) {
        this.scope = scope;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }


}
