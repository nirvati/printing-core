/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.ext.papercut.services.impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.services.impl.AbstractService;
import org.savapage.ext.papercut.DelegatedPrintPeriodDto;
import org.savapage.ext.papercut.PaperCutDbProxy;
import org.savapage.ext.papercut.PaperCutException;
import org.savapage.ext.papercut.PaperCutPrinterUsageLog;
import org.savapage.ext.papercut.PaperCutServerProxy;
import org.savapage.ext.papercut.PaperCutUser;
import org.savapage.ext.papercut.services.PaperCutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class PaperCutServiceImpl extends AbstractService implements
        PaperCutService {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(PaperCutServiceImpl.class);

    @Override
    public PaperCutUser findUser(final PaperCutServerProxy papercut,
            final String userId) {
        return papercut.getUser(userId);
    }

    @Override
    public void lazyAdjustSharedAccount(final PaperCutServerProxy papercut,
            final String topAccountName, final String subAccountName,
            final BigDecimal adjustment, final String comment)
            throws PaperCutException {

        try {

            papercut.adjustSharedAccountAccountBalance(topAccountName,
                    subAccountName, adjustment.doubleValue(), comment);

        } catch (PaperCutException e) {

            final String composedSharedAccountName =
                    papercut.composeSharedAccountName(topAccountName,
                            subAccountName);

            if (LOGGER.isInfoEnabled()) {

                LOGGER.info(String.format(
                        "Shared account [%s] does not exist: added new.",
                        composedSharedAccountName));
            }

            papercut.addNewSharedAccount(topAccountName, subAccountName);

            papercut.adjustSharedAccountAccountBalance(topAccountName,
                    subAccountName, adjustment.doubleValue(), comment);

            AdminPublisher.instance().publish(
                    PubTopicEnum.PAPERCUT,
                    PubLevelEnum.CLEAR,
                    String.format("PaperCut account '%s' created.",
                            composedSharedAccountName));
        }

    }

    @Override
    public void adjustUserAccountBalance(final PaperCutServerProxy papercut,
            final String username, final String userAccountName,
            final BigDecimal adjustment, final String comment)
            throws PaperCutException {

        papercut.adjustUserAccountBalance(username, userAccountName,
                adjustment.doubleValue(), comment);
    }

    @Override
    public List<PaperCutPrinterUsageLog> getPrinterUsageLog(
            final PaperCutDbProxy papercut, final Set<String> uniqueDocNames) {
        return papercut.getPrinterUsageLog(uniqueDocNames);
    }

    @Override
    public void createDelegatedPrintStudentCostCsv(
            final PaperCutDbProxy papercut, final File file,
            final DelegatedPrintPeriodDto dto) throws IOException {
        papercut.createDelegatedPrintStudentCostCsv(file, dto);
    }

}
