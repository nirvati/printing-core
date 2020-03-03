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
package org.savapage.core.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.savapage.core.dao.PrinterDao.CostMediaAttr;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.ipp.IppMediaSizeEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PrinterDaoTest {

    @Test
    public void testCostMediaAttr1() {

        final String dbKeyPfx = PrinterAttrEnum.PFX_COST_MEDIA.getDbName();

        //
        assertEquals(CostMediaAttr.isValidKey(""), false);
        assertEquals(CostMediaAttr.isValidKey(dbKeyPfx),
                false);

        //
        String key =
                dbKeyPfx
                        + CostMediaAttr.COST_3_MEDIA_DEFAULT;
        assertEquals(CostMediaAttr.isValidKey(key), true);

        //
        key =
                dbKeyPfx
                        + IppMediaSizeEnum.ISO_A3.getIppKeyword();
        assertEquals(CostMediaAttr.isValidKey(key), true);

        //
        key =
                dbKeyPfx
                        + IppMediaSizeEnum.ISO_A4.getIppKeyword();
        assertEquals(CostMediaAttr.isValidKey(key), true);

        //
        key =
                dbKeyPfx
                        + IppMediaSizeEnum.NA_LETTER.getIppKeyword();
        assertEquals(CostMediaAttr.isValidKey(key), true);

        //
        key =
                dbKeyPfx
                        + IppMediaSizeEnum.NA_LEGAL.getIppKeyword();
        assertEquals(CostMediaAttr.isValidKey(key), true);

        //
        key = dbKeyPfx + "-";
        assertEquals(CostMediaAttr.isValidKey(key), false);

    }

    @Test
    public void testCostMediaAttr2() {

        String ippMedia = IppMediaSizeEnum.ISO_A4.getIppKeyword();
        String key = new CostMediaAttr(ippMedia).getKey();

        assertEquals(
                CostMediaAttr.createFromDbKey(key).getIppMediaName()
                        .equals(ippMedia), true);
    }

    @Test
    public void testCostMediaAttr3() {

        String ippMedia = IppMediaSizeEnum.NA_LETTER.getIppKeyword();
        String key = new CostMediaAttr(ippMedia).getKey();

        assertEquals(
                CostMediaAttr.createFromDbKey(key).getIppMediaName()
                        .equals(ippMedia), true);
    }

    @Test
    public void testCostMediaAttr4() {

        String ippMedia = IppMediaSizeEnum.NA_LEGAL.getIppKeyword();
        String key = new CostMediaAttr(ippMedia).getKey();

        assertEquals(
                CostMediaAttr.createFromDbKey(key).getIppMediaName()
                        .equals(ippMedia), true);
    }

}
