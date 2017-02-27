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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Query;

import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dto.SharedAccountDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroupAccount;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupAccountDaoImpl extends
        GenericDaoImpl<UserGroupAccount> implements UserGroupAccountDao {

    /**
     * .
     */
    private static class SharedAccountComparator
            implements Comparator<SharedAccountDto> {

        @Override
        public final int compare(final SharedAccountDto o1,
                final SharedAccountDto o2) {
            return o1.nameAsText().compareTo(o2.nameAsText());
        }

    }

    @Override
    public List<SharedAccountDto> getSortedSharedAccounts(final User user) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT DISTINCT A FROM UserGroupMember UGM");
        jpql.append("\nJOIN UGM.group UG");
        jpql.append("\nJOIN UGM.user U");
        jpql.append("\nJOIN UG.accounts UGA");
        jpql.append("\nJOIN UGA.account A");
        jpql.append("\nWHERE U.id = :userId");
        jpql.append(" ORDER BY A.name");

        final Query query = getEntityManager().createQuery(jpql.toString());
        query.setParameter("userId", user.getId());

        // final SortedSet<SharedAccountDto> sortedSet =
        // new TreeSet<>(new SharedAccountComparator());

        @SuppressWarnings("unchecked")
        final List<Account> resultList = query.getResultList();

        final List<SharedAccountDto> sharedAccounts = new ArrayList<>();

        for (final Account account : resultList) {
            final SharedAccountDto dto = new SharedAccountDto();
            dto.setId(account.getId());
            dto.setName(account.getName());

            final Account parent = account.getParent();
            if (parent != null) {
                dto.setParentId(parent.getId());
                dto.setParentName(parent.getName());
            }

            sharedAccounts.add(dto);
        }

        Collections.sort(sharedAccounts, new SharedAccountComparator());

        return sharedAccounts;
    }

}
