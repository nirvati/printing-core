/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dao.impl;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.UserCardDao;
import org.savapage.core.jpa.UserCard;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserCardDaoImpl extends GenericDaoImpl<UserCard> implements
        UserCardDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM UserCard T";
    }

    @Override
    public UserCard findByCardNumber(final String cardNumber) {

        UserCard card = null;

        if (StringUtils.isNotBlank(cardNumber)) {

            final String jpql =
                    "SELECT C FROM UserCard C JOIN C.user U "
                            + "WHERE C.number = :number";

            final Query query = getEntityManager().createQuery(jpql);

            query.setParameter("number", cardNumber.toLowerCase());

            try {
                card = (UserCard) query.getSingleResult();
            } catch (NoResultException e) {
                card = null;
            }

        }
        return card;
    }

    @Override
    public boolean isPrimaryCard(final UserCard card) {
        return card.getIndexNumber() == INDEX_NUMBER_PRIMARY_CARD;
    }

    @Override
    public void assignPrimaryCard(final UserCard card) {
        card.setIndexNumber(INDEX_NUMBER_PRIMARY_CARD);
    }

}
