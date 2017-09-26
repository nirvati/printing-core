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

import java.util.Date;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.PrinterGroupDao;
import org.savapage.core.jpa.PrinterGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterGroupDaoImpl extends GenericDaoImpl<PrinterGroup>
        implements PrinterGroupDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM PrinterGroup T";
    }

    @Override
    public int prunePrinterGroups() {

        String group = null;

        if (ConfigManager.instance().isConfigValue(Key.PROXY_PRINT_NON_SECURE)) {
            group =
                    ConfigManager.instance().getConfigValue(
                            Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP);
        }

        final String jpqlMain = "DELETE FROM PrinterGroup P WHERE";
        final String jpqlWhere =
                " (SELECT COUNT(M) FROM P.members M) = 0"
                        + " AND (SELECT COUNT(D) FROM P.devices D) = 0";

        final Query query;

        if (StringUtils.isNotBlank(group)) {
            String qry = jpqlMain + " P.groupName != :group AND " + jpqlWhere;
            query = this.getEntityManager().createQuery(qry);
            query.setParameter("group", group);
        } else {
            query = this.getEntityManager().createQuery(jpqlMain + jpqlWhere);
        }

        return ((Number) query.executeUpdate()).intValue(); // DELETE
    }

    @Override
    public PrinterGroup readOrAdd(final String groupName,
            final String displayName, final String requestingUser,
            final Date requestDate) {

        PrinterGroup printerGroup = findByName(groupName);

        if (printerGroup == null) {
            /*
             * Lazy insert Printer Group.
             */
            printerGroup = new PrinterGroup();

            printerGroup.setCreatedBy(requestingUser);
            printerGroup.setCreatedDate(requestDate);
            printerGroup.setGroupName(groupName);
            printerGroup.setDisplayName(displayName);

            this.create(printerGroup);

        } else {

            if (!printerGroup.getDisplayName().equals(displayName)) {

                /*
                 * Update with new display name.
                 */
                printerGroup.setModifiedBy(requestingUser);
                printerGroup.setModifiedDate(requestDate);
                printerGroup.setDisplayName(displayName);

                this.update(printerGroup);
            }
        }

        return printerGroup;
    }

    @Override
    public PrinterGroup findByName(final String groupName) {

        final String jpql =
                "SELECT P FROM PrinterGroup P "
                        + "WHERE P.groupName = :groupName";

        final String key = groupName.toLowerCase();

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("groupName", key);

        PrinterGroup group;

        try {
            group = (PrinterGroup) query.getSingleResult();
        } catch (NoResultException e) {
            group = null;
        }

        return group;
    }
}
