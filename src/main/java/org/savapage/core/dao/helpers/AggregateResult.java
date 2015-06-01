/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.dao.helpers;

import java.math.BigDecimal;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class AggregateResult<T extends Number> {

    private final long count;
    private final T sum;

    private final T min;
    private final T max;
    private final BigDecimal avg;

    public AggregateResult() {
        this.count = 0;
        this.sum = null;
        this.min = null;
        this.max = null;
        this.avg = null;
    }

    public AggregateResult(final Long count, final T sum, final T min,
            final T max, final Double avg) {
        this.count = count.longValue();
        this.sum = sum;
        this.min = min;
        this.max = max;
        if (avg == null) {
            this.avg = null;
        } else {
            this.avg = BigDecimal.valueOf(avg.doubleValue());
        }
    }

    public long getCount() {
        return this.count;
    }

    public T getSum() {
        return this.sum;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public BigDecimal getAvg() {
        return avg;
    }

}
