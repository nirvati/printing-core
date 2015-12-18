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
package org.savapage.core.dao.impl;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.dao.enums.DeviceAttrEnum;
import org.savapage.core.jpa.DeviceAttr;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class DeviceAttrDaoImpl extends GenericDaoImpl<DeviceAttr>
        implements DeviceAttrDao {

    @Override
    public DeviceAttr
            findByName(final Long deviceId, final DeviceAttrEnum name) {
        final String jpql =
                "SELECT A FROM DeviceAttr A JOIN A.device P "
                        + "WHERE P.id = :deviceId AND A.name = :name";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("deviceId", deviceId);
        query.setParameter("name", name.getDbName());

        DeviceAttr attr;

        try {
            attr = (DeviceAttr) query.getSingleResult();
        } catch (NoResultException e) {
            attr = null;
        }

        return attr;
    }

}
