/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.dto;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;
import org.savapage.core.dto.MediaCostDto;
import org.savapage.core.dto.MediaPageCostDto;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.util.BigDecimalUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class DtoTest {

    @Test
    public void test() throws ParseException, IOException {

        test(Locale.US);
        test(new Locale.Builder().setLanguage("nl").setRegion("NL").build());
        test(new Locale.Builder().setLanguage("fr").setRegion("FR").build());
    }

    /**
     *
     * @param locale
     * @throws ParseException
     * @throws IOException
     */
    public void test(final Locale locale) throws ParseException, IOException {

        final MediaCostDto obj = new MediaCostDto();

        final String cost11 = "0.1111";
        final String cost12 = "0.2222";
        final String cost21 = "0.3333";
        final String cost22 = "0.4444";

        //
        MediaPageCostDto newCost;

        //
        newCost = new MediaPageCostDto();
        obj.setCostOneSided(newCost);

        newCost.setCostColor(BigDecimalUtil.localize(new BigDecimal(cost11), 4,
                locale, false));

        newCost.setCostGrayscale(BigDecimalUtil.localize(new BigDecimal(cost12),
                4, locale, false));

        //
        newCost = new MediaPageCostDto();
        obj.setCostTwoSided(newCost);

        newCost.setCostColor(BigDecimalUtil.localize(new BigDecimal(cost21), 4,
                locale, false));

        newCost.setCostGrayscale(BigDecimalUtil.localize(new BigDecimal(cost22),
                4, locale, false));

        //
        final MediaCostDto pageCost = JsonAbstractBase
                .create(MediaCostDto.class, obj.stringify(locale));

        assertTrue(pageCost.getCostOneSided().getCostColor().equals("0.1111"));

    }

}
