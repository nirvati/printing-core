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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.UserNumberDao;
import org.savapage.core.jpa.UserNumber;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class UserNumberDaoImpl extends GenericDaoImpl<UserNumber>
        implements UserNumberDao {

    @Override
    public boolean isPrimaryNumber(final UserNumber number) {
        return number.getIndexNumber() == INDEX_NUMBER_PRIMARY_NUMBER;
    }

    @Override
    public void assignPrimaryNumber(final UserNumber number) {
        number.setIndexNumber(INDEX_NUMBER_PRIMARY_NUMBER);
    }

    @Override
    public UserNumber findByNumber(final String number) {

        UserNumber userNumber = null;

        if (StringUtils.isNotBlank(number)) {

            final String jpql =
                    "SELECT N FROM UserNumber N JOIN N.user U "
                            + "WHERE N.number = :number";

            final Query query = getEntityManager().createQuery(jpql);

            query.setParameter("number", number);

            try {
                userNumber = (UserNumber) query.getSingleResult();
            } catch (NoResultException e) {
                userNumber = null;
            }

        }
        return userNumber;
    }

}
