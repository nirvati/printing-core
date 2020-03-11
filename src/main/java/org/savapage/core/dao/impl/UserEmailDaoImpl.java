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
package org.savapage.core.dao.impl;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.UserEmailDao;
import org.savapage.core.jpa.UserEmail;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserEmailDaoImpl extends GenericDaoImpl<UserEmail>
        implements UserEmailDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM UserEmail T";
    }

    @Override
    public boolean isPrimaryEmail(final UserEmail email) {
        return email.getIndexNumber() == INDEX_NUMBER_PRIMARY_EMAIL;

    }

    @Override
    public void assignPrimaryEmail(final UserEmail email) {
        email.setIndexNumber(INDEX_NUMBER_PRIMARY_EMAIL);
    }

    @Override
    public UserEmail findByEmail(final String emailAddress) {

        UserEmail userEmail = null;

        if (StringUtils.isNotBlank(emailAddress)) {

            final String jpql = "SELECT E FROM UserEmail E JOIN E.user U "
                    + "WHERE E.address = :address";

            final Query query = getEntityManager().createQuery(jpql);

            query.setParameter("address", emailAddress.toLowerCase());

            try {
                userEmail = (UserEmail) query.getSingleResult();
            } catch (NoResultException e) {
                userEmail = null;
            }

        }
        return userEmail;
    }

}
