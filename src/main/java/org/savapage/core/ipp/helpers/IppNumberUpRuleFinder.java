/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.ipp.helpers;

import org.savapage.core.ipp.rules.IppRuleNumberUp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface IppNumberUpRuleFinder {

    /**
     * Finds a matching {@link IppRuleNumberUp} for a template rule.
     *
     * @param landscapeMinus90
     *            If {@code true}, PDF landscape sheets are +90 rotated to
     *            portrait print area, so user can -90 rotate the printed sheet
     *            to view in landscape. If {@code false}, vice versa.
     * @param template
     *            The template rule with <i>independent</i> variables.
     * @return The template rule object supplemented with <i>dependent</i>
     *         variables, or {@code null} when no rule found.
     */
    IppRuleNumberUp findNumberUpRule(boolean landscapeMinus90,
            IppRuleNumberUp template);
}
