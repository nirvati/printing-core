/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @author Rijk Ravestein
 *
 */
public final class UserNumberDaoImpl extends GenericDaoImpl<UserNumber>
        implements UserNumberDao {

    /**
     *
     */
    private static final String NUMBER_PFX_YUBIKEY = "yubikey-";

    @Override
    public boolean isPrimaryNumber(final UserNumber number) {
        return number.getIndexNumber() == INDEX_NUMBER_PRIMARY_NUMBER
                && !isYubiKeyPubID(number);
    }

    @Override
    public boolean isYubiKeyPubID(final UserNumber number) {
        return number.getNumber().startsWith(NUMBER_PFX_YUBIKEY);
    }

    @Override
    public String getYubiKeyPubID(final UserNumber number) {
        if (this.isYubiKeyPubID(number)) {
            return number.getNumber().substring(NUMBER_PFX_YUBIKEY.length());
        }
        return null;
    }

    @Override
    public String composeYubiKeyDbNumber(final String publicId) {
        return String.format("%s%s", NUMBER_PFX_YUBIKEY, publicId);
    }

    @Override
    public void assignPrimaryNumber(final UserNumber number) {
        number.setIndexNumber(INDEX_NUMBER_PRIMARY_NUMBER);
    }

    @Override
    public void assignYubiKeyNumber(final UserNumber number) {
        number.setIndexNumber(INDEX_NUMBER_YUBIKEY_NUMBER);
    }

    @Override
    public UserNumber findByNumber(final String number) {

        UserNumber userNumber = null;

        if (StringUtils.isNotBlank(number)) {

            final String jpql = "SELECT N FROM UserNumber N JOIN N.user U "
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

    @Override
    public UserNumber findByYubiKeyPubID(final String yubiKeyID) {
        return this.findByNumber(
                String.format("%s%s", NUMBER_PFX_YUBIKEY, yubiKeyID));
    }

}
