/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core;

import static org.junit.Assert.assertTrue;

import java.util.Map.Entry;

import org.junit.Test;
import org.savapage.core.outbox.OutboxInfoDto;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class OutboxInfoTest {

    @Test
    public void testOrdering() {

        final OutboxInfoDto info = new OutboxInfoDto();

        final String[] values = {"9", "8", "a", "7", "Z", "6"};

        for (int i = 0; i < values.length; i++) {
            final OutboxJobDto job = new OutboxJobDto();
            job.setFile(values[i]);
            info.addJob(values[i], job);
        }

        int i = 0;

        for (final Entry<String, OutboxJobDto> entry : info.getJobs().entrySet()) {
            final String value = entry.getKey();
            assertTrue(value == values[i++]);
        }

    }

}
