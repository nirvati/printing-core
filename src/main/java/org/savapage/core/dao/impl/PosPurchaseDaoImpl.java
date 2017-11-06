/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import org.savapage.core.dao.PosPurchaseDao;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DbSimpleEntity;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PosPurchaseDaoImpl extends GenericDaoImpl<PosPurchase>
        implements PosPurchaseDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM PosPurchase T";
    }

    @Override
    public int getHighestReceiptNumber(final ReceiptNumberPrefixEnum prefix) {

        final String prefixDb = prefix.toString();

        final Query query = getEntityManager()
                .createQuery("select max(P.receiptNumber) from PosPurchase P "
                        + "where P.receiptNumber like :receiptNumber");

        query.setParameter("receiptNumber", prefixDb + "%");

        int highest = 0;

        try {
            final String result = (String) query.getSingleResult();
            if (result != null) {
                highest = Integer.parseInt(result.substring(prefixDb.length()));
            }
        } catch (NoResultException e) {
            highest = 0;
        }

        return highest;
    }

    @Override
    public String getNextReceiptNumber(final ReceiptNumberPrefixEnum prefix) {
        final int highest = getHighestReceiptNumber(prefix);
        return String.format("%s%0" + RECEIPT_NUMBER_MIN_WIDTH + "d",
                prefix.toString(), highest + 1);
    }

    @Override
    public int eraseUser(final User user) {
        final String jpql = "UPDATE " + DbSimpleEntity.POS_PURCHASE
                + " SET comment = null WHERE id IN" //
                + " (SELECT PP.id FROM " + DbSimpleEntity.USER_ACCOUNT + " UA"
                + " JOIN " + DbSimpleEntity.ACCOUNT_TRX
                + " TRX ON TRX.account = UA.account"
                + " AND TRX.comment != null" //
                + " JOIN " + DbSimpleEntity.POS_PURCHASE
                + " PP ON TRX.posPurchase = PP.id" //
                + " AND PP.comment != null" //
                + " WHERE UA.user = :user)";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("user", user.getId());
        return query.executeUpdate();
    }
}
