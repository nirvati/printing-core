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
package org.savapage.core.ipp.rules;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A rule to add extra PPD option values.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRuleExtra extends IppRuleGeneric
        implements IppRuleChecker {

    /**
     * Extra PPD option (key) values.
     */
    private List<Pair<String, String>> extraPPD;

    /**
     * @param rule
     *            The name of the rule.
     */
    public IppRuleExtra(final String rule) {
        super(rule);
    }

    /**
     * @return Extra PPD option (key) values.
     */
    public List<Pair<String, String>> getExtraPPD() {
        return extraPPD;
    }

    /**
     * @param list
     *            Extra PPD option (key) values.
     */
    public void setExtraPPD(final List<Pair<String, String>> list) {
        this.extraPPD = list;
    }

}
