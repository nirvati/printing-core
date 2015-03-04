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
package org.savapage.core.jpa.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class DbVersionInfo {

    String prodName;
    String prodVersion;
    int majorVersion;
    int minorVersion;

    String driverName;
    String driverVersion;
    int driverMajorVersion;
    int driverMinorVersion;

    /**
     * Retrieves Database product and version information using a live database
     * connection.
     *
     * <p>
     * NOTE: This generates an
     * {@link org.hibernate.exception.GenericJDBCException} when the database is
     * in use by another application.
     * </p>
     */
    public DbVersionInfo(final EntityManager em) {

        Session session = em.unwrap(Session.class);

        session.doWork(new Work() {

            @Override
            public void execute(Connection conn) throws SQLException {

                DatabaseMetaData dbmd = conn.getMetaData();

                prodName = dbmd.getDatabaseProductName();
                prodVersion = dbmd.getDatabaseProductVersion();
                majorVersion = dbmd.getDatabaseMajorVersion();
                minorVersion = dbmd.getDatabaseMinorVersion();

                driverName = dbmd.getDriverName();
                driverVersion = dbmd.getDriverVersion();
                driverMajorVersion = dbmd.getDriverMajorVersion();
                driverMinorVersion = dbmd.getDriverMinorVersion();
            }

        });

    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public String getProdVersion() {
        return prodVersion;
    }

    public void setProdVersion(String prodVersion) {
        this.prodVersion = prodVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    public int getDriverMajorVersion() {
        return driverMajorVersion;
    }

    public void setDriverMajorVersion(int driverMajorVersion) {
        this.driverMajorVersion = driverMajorVersion;
    }

    public int getDriverMinorVersion() {
        return driverMinorVersion;
    }

    public void setDriverMinorVersion(int driverMinorVersion) {
        this.driverMinorVersion = driverMinorVersion;
    }

}
