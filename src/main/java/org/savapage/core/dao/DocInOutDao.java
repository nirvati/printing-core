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

import java.util.List;

import javax.persistence.NonUniqueResultException;

import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DocInOutDao extends GenericDao<DocInOut> {

    /**
     * Finds the single {@link DocLog} source of a {@link DocOut}.
     * <p>
     * A {@link NonUniqueResultException} is thrown when more than one (1)
     * {@link DocLog} instance is found.
     * </p>
     *
     * @param docOutId
     *            The primary key of the {@link DocOut}.
     * @return The {@link DocLog} or {@code null} when not found.
     */
    DocLog findDocOutSource(Long docOutId);

    /**
     * Finds the {@link PrintOut} instances related to a {@link DocIn}.
     *
     * @param docInId
     *            The primary key of the {@link DocIn}.
     * @return list.
     */
    List<PrintOut> getPrintOutOfDocIn(Long docInId);

    /**
     * Finds the {@link DocIn} instances related to a {@link DocOut}.
     *
     * @param docOutId
     *            The primary key of the {@link DocOut}.
     * @return list.
     */
    List<DocIn> getDocInOfDocOut(Long docOutId);
}
