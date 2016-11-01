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
package org.savapage.core.services.helpers;

import java.io.File;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.doc.soffice.SOfficeConfig;
import org.savapage.core.doc.soffice.SOfficeHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeConfigProps extends SOfficeConfig {

    /**
     * Constructor.
     */
    public SOfficeConfigProps() {

        final ConfigManager cm = ConfigManager.instance();

        this.setEnabled(cm.isConfigValue(Key.SOFFICE_ENABLE)
                && cm.isConfigValue(Key.DOC_CONVERT_LIBRE_OFFICE_ENABLED)
                && SOfficeHelper.lazyIsInstalled());

        this.setWorkDir(new File(ConfigManager.getAppTmpDir()));

        /*
         * Ports
         */
        final Set<String> portSet =
                cm.getConfigSet(Key.SOFFICE_CONNECTION_PORTS);

        final int[] ports;

        if (portSet.isEmpty()) {
            ports = null;
        } else {
            ports = new int[portSet.size()];
            int i = 0;
            for (final String port : portSet
                    .toArray(new String[portSet.size()])) {
                ports[i++] = Integer.parseInt(port);
            }
        }

        this.setPortNumbers(ports);

        // Optional
        String dir = cm.getConfigValue(Key.SOFFICE_HOME);

        if (StringUtils.isNotBlank(dir)) {
            this.setOfficeLocation(new File(dir));
        }

        // Optional
        dir = cm.getConfigValue(Key.SOFFICE_PROFILE_TEMPLATE_DIR);
        if (StringUtils.isNotBlank(dir)) {
            this.setTemplateProfileDir(new File(dir));
        }

        //
        this.setTasksCountForProcessRestart(
                cm.getConfigInt(Key.SOFFICE_CONNECTION_RESTART_TASK_COUNT));

        this.setTaskQueueTimeout(
                cm.getConfigLong(Key.SOFFICE_TASK_QUEUE_TIMEOUT_MSEC));

        this.setTaskExecutionTimeout(
                cm.getConfigLong(Key.SOFFICE_TASK_EXEC_TIMEOUT_MSEC));

        this.setProcessRetryTimeout(
                cm.getConfigLong(Key.SOFFICE_PROCESS_RETRY_TIMEOUT_MSEC));
    }

}
