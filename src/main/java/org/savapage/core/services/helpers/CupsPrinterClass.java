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
package org.savapage.core.services.helpers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * A CUPS Printer Class.
 *
 * @author Rijk Ravestein
 *
 */
public final class CupsPrinterClass {

    private String name;
    private URI printerUri;
    private final List<String> memberNames = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getPrinterUri() {
        return printerUri;
    }

    public void setPrinterUri(URI printerUri) {
        this.printerUri = printerUri;
    }

    public List<String> getMemberNames() {
        return memberNames;
    }

    public void addMemberName(final String member) {
        memberNames.add(member);
    }

}
