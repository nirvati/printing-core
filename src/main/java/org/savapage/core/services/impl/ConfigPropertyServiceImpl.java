/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.validator.ValidationResult;
import org.savapage.core.config.validator.ValidationStatusEnum;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.ResultString;
import org.savapage.core.json.rpc.impl.ParamsNameValue;
import org.savapage.core.services.ConfigPropertyService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ConfigPropertyServiceImpl extends AbstractService
        implements ConfigPropertyService {

    /** */
    private static final ConfigManager CONFIG_MNGR = ConfigManager.instance();

    @Override
    public AbstractJsonRpcMethodResponse getPropertyValue(final String name) {

        final IConfigProp.Key key = CONFIG_MNGR.getConfigKey(name);

        final String value;

        if (key == null) {
            value = null;
        } else {
            value = CONFIG_MNGR.getConfigValue(key);
        }

        if (value == null) {
            return createError("msg-config-property-not-found", name);
        }

        final ResultString result = new ResultString();
        result.setValue(value);
        return JsonRpcMethodResult.createResult(result);
    }

    @Override
    public AbstractJsonRpcMethodResponse
            setPropertyValue(final ParamsNameValue parm) {

        final IConfigProp.Key key = CONFIG_MNGR.getConfigKey(parm.getName());

        if (key == null) {
            return createError("msg-config-property-not-found", parm.getName());
        }

        final boolean hasAccess;

        if (CONFIG_MNGR.isConfigApiUpdatable(key)) {

            switch (key) {
            case FINANCIAL_GLOBAL_CURRENCY_CODE:
                hasAccess =
                        StringUtils.isBlank(CONFIG_MNGR.getConfigValue(key));
                break;

            default:
                hasAccess = true;
                break;
            }

        } else {
            hasAccess = false;
        }

        if (!hasAccess) {
            return createErrorMsg("msg-config-property-update-failure",
                    parm.getName(),
                    ValidationStatusEnum.ERROR_ACCESS_DENIED.toString());
        }

        final ValidationResult result =
                CONFIG_MNGR.validate(key, parm.getValue());

        if (!result.isValid()) {
            return createErrorMsg("msg-config-property-update-failure",
                    parm.getName(),
                    String.format("%s - %s", result.getStatus().toString(),
                            result.getMessage()));
        }

        CONFIG_MNGR.updateConfigKey(key, parm.getValue(),
                ServiceContext.getActor());

        CONFIG_MNGR.calcRunnable();

        return createOkResult("msg-config-property-update-success",
                parm.getName());
    }

}
