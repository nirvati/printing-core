/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.savapage.core.dto.IppCostRule;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppCostRuleTest {

    @Test
    public void test1() {

        final BigDecimal amount = BigDecimal.ONE;

        final IppCostRule rule = new IppCostRule("test1", amount);

        final String ippAttr = IppDictJobTemplateAttr.ATTR_MEDIA_COLOR;
        final String ippChoice = "white";
        final boolean mustBeChosen = true;

        rule.addRuleChoice(ippAttr, ippChoice, mustBeChosen);
        final Map<String, String> ippChoices = new HashMap<>();

        ippChoices.put(ippAttr, "red");
        assertTrue(rule.calcCost(ippChoices) == null);

        ippChoices.put(ippAttr, ippChoice);
        ippChoices.put(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_EXT,
                IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_EXTERNAL_LAMINATE);

        assertTrue(rule.calcCost(ippChoices).equals(amount));
    }

}
