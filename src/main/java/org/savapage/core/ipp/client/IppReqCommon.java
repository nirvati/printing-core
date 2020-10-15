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
package org.savapage.core.ipp.client;

import org.savapage.core.ipp.attribute.AbstractIppDict;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.encoding.IppDelimiterTag;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class IppReqCommon implements IppClientRequest {

    /**
     * Creates the first Group with Operation Attributes.
     *
     * @return The {@link IppAttrGroup}.
     */
    public static IppAttrGroup createOperationGroup() {

        /*
         * Group 1: Operation Attributes
         */
        final IppAttrGroup group =
                new IppAttrGroup(IppDelimiterTag.OPERATION_ATTR);

        AbstractIppDict dict = IppDictOperationAttr.instance();

        // ------------------------------------------------------------------
        group.add(dict.getAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_CHARSET),
                "utf-8");
        group.add(
                dict.getAttr(IppDictOperationAttr.ATTR_ATTRIBUTES_NATURAL_LANG),
                "en-us");

        return group;
    }

}
