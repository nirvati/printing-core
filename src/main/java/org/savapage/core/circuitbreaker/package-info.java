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

/**
 * Provides classes and interfaces implementing the <i>Circuit Breaker</i>
 * concept as described in the book "Release It!" by Michael T. Nygard.
 * <p>
 * Circuit breakers are applied to services that implement integration points to
 * external systems.
 * </p>
 * <p>
 * If such a service returns too many errors or responds too slowly, the breaker
 * trips to {@link CircuitStateEnum#OPEN}. In this state, any client which
 * attempts to reach the service will fail fast.
 * </p>
 * <p>
 * When the breaker has been open for some specified period of time, it will
 * move to {@link CircuitStateEnum#HALF_OPEN}, allowing the client's next
 * request to reach the service. Depending on the result of this call, the
 * circuit breaker will either move to {@link CircuitStateEnum#CLOSED} or back
 * to {@link CircuitStateEnum#OPEN}.
 * </p>
 *
 * @author Rijk Ravestein
 */
package org.savapage.core.circuitbreaker;

