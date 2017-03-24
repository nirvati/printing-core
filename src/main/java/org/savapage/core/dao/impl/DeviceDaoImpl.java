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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DeviceDaoImpl extends GenericDaoImpl<Device>
        implements DeviceDao {

    @Override
    public Device findByName(final String deviceName) {

        final String psql =
                "SELECT D FROM Device D WHERE D.deviceName = :deviceName";
        /*
         * Find the queue by unique name
         */
        final Query query = getEntityManager().createQuery(psql);
        query.setParameter("deviceName", deviceName);

        Device device = null;

        try {
            device = (Device) query.getSingleResult();
        } catch (NoResultException e) {
            device = null;
        }

        return device;
    }

    @Override
    public Device findByHostDeviceType(final String hostname,
            final DeviceTypeEnum deviceType) {

        final String psql =
                "SELECT D FROM Device D WHERE D.hostname = :hostname "
                        + "AND D.deviceType = :deviceType";
        /*
         * Find the queue by unique name
         */
        final Query query = getEntityManager().createQuery(psql);
        query.setParameter("hostname", hostname);
        query.setParameter("deviceType", deviceType.toString());

        Device device;

        try {
            device = (Device) query.getSingleResult();
        } catch (NoResultException e) {
            device = null;
        }

        return device;
    }

    @Override
    public long getListCount(final ListFilter filter) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(P.id) FROM Device P");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql.toString(), filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Device> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        /**
         * #190: Do not use JOIN FETCH construct.
         */
        jpql.append("SELECT P FROM Device P");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.NAME) {
            jpql.append("P.deviceName");
        } else {
            jpql.append("P.deviceName");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = createListQuery(jpql.toString(), filter);

        //
        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.getResultList();
    }

    /**
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The filter.
     * @return The query.
     */
    private Query createListQuery(final String jpql, final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql);

        if (filter.getContainingText() != null) {
            query.setParameter("containingText", String.format("%%%s%%",
                    filter.getContainingText().toLowerCase()));
        }

        if (filter.getDisabled() != null) {
            query.setParameter("selDisabled", filter.getDisabled());
        }

        if (filter.getDeviceType() != null) {
            query.setParameter("deviceType", filter.getDeviceType().toString());
        }

        return query;
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The {@link StringBuilder} to append to.
     * @param filter
     *            The filter.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getContainingText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(P.deviceName) like :containingText");
        }

        if (filter.getDisabled() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" P.disabled = :selDisabled");
        }

        if (filter.getDeviceType() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" P.deviceType = :deviceType");
        }

        //
        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }

    }

    /**
     * Reads the DeviceAttr from the database.
     *
     * @param device
     *            The Device.
     * @param name
     *            The attribute name.
     * @return The attribute or {@code null} when not found.
     */
    private DeviceAttr readAttribute(final Device device, final String name) {

        final String jpql = "SELECT A FROM DeviceAttr A JOIN A.device D "
                + "WHERE D.id = :deviceId AND A.name = :name";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("deviceId", device.getId());
        query.setParameter("name", name);

        DeviceAttr attr;

        try {
            attr = (DeviceAttr) query.getSingleResult();
        } catch (NoResultException e) {
            attr = null;
        }
        return attr;
    }

    @Override
    public void writeAttribute(final Device device, final String key,
            final String value) {

        final DeviceAttrDao dao =
                ServiceContext.getDaoContext().getDeviceAttrDao();

        final DeviceAttr attr = readAttribute(device, key);

        if (attr == null) {

            final DeviceAttr attrNew = new DeviceAttr();

            attrNew.setDevice(device);

            attrNew.setName(key);
            attrNew.setValue(value);

            dao.create(attrNew);

        } else {

            attr.setValue(value);

            dao.update(attr);
        }

    }

    @Override
    public boolean isCardReader(final Device device) {
        return device.getDeviceType()
                .equals(DeviceTypeEnum.CARD_READER.toString());
    }

    @Override
    public boolean isTerminal(final Device device) {
        return device.getDeviceType()
                .equals(DeviceTypeEnum.TERMINAL.toString());
    }

    @Override
    public boolean hasPrinterRestriction(final Device device) {
        return device.getPrinter() != null || device.getPrinterGroup() != null;
    }

}
