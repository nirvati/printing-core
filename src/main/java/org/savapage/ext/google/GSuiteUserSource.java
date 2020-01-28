/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.ext.google;

import org.savapage.core.config.IConfigProp.LdapType;
import org.savapage.core.users.LdapUserSource;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class GSuiteUserSource extends LdapUserSource {

    /** */
    public GSuiteUserSource() {
        super(LdapType.G_SUITE);
    }

    @Override
    protected Class<?> getCustomSSLSocketFactoryClass() {
        return GSuiteLdapSSLSocketFactory.class;
    }

    @Override
    protected boolean isExtraUserAttributes() {
        return false;
    }

}