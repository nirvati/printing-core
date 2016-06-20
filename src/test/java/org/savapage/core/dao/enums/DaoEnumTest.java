/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dao.enums;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class DaoEnumTest {

    @Test
    public void testACLPermission() {

        for (final ACLPermissionEnum perm : ACLPermissionEnum.values()) {
            assertTrue(ACLPermissionEnum.OWNER.isGranted(perm));
        }

        for (final ACLPermissionEnum perm : EnumSet
                .complementOf(EnumSet.of(ACLPermissionEnum.OWNER))) {
            assertTrue(ACLPermissionEnum.MASTER.isGranted(perm));
        }

        for (final ACLPermissionEnum perm : EnumSet.complementOf(EnumSet
                .of(ACLPermissionEnum.OWNER, ACLPermissionEnum.MASTER))) {
            assertTrue(ACLPermissionEnum.OPERATOR.isGranted(perm));
        }

        for (final ACLPermissionEnum permIn : EnumSet.complementOf(EnumSet.of(
                ACLPermissionEnum.OWNER, ACLPermissionEnum.MASTER,
                ACLPermissionEnum.OPERATOR, ACLPermissionEnum.EDITOR))) {
            for (final ACLPermissionEnum perm : ACLPermissionEnum.values()) {
                if (permIn == perm) {
                    assertTrue(permIn.isGranted(perm));
                } else {
                    assertFalse(permIn.isGranted(perm));
                }
            }
        }

        assertTrue(
                ACLPermissionEnum.EDITOR.isGranted(ACLPermissionEnum.READER));

        //
        assertFalse(ACLPermissionEnum.EDITOR == ACLPermissionEnum
                .asRole(ACLPermissionEnum.READER.getFlag()));

        assertTrue(ACLPermissionEnum.EDITOR == ACLPermissionEnum
                .asRole(ACLPermissionEnum.READER.getFlag()
                        | ACLPermissionEnum.EDITOR.getFlag()));

        assertTrue(ACLPermissionEnum.OWNER == ACLPermissionEnum
                .asRole(ACLPermissionEnum.OPERATOR.getFlag()
                        | ACLPermissionEnum.OWNER.getFlag()));
    }

}
