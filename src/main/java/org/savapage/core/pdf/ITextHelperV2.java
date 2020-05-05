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
package org.savapage.core.pdf;

import org.savapage.core.json.PdfProperties;

import com.lowagie.text.pdf.PdfWriter;

/**
 * PDF helper of {@link com.lowagie}.
 *
 * @author Rijk Ravestein
 *
 */
public final class ITextHelperV2 implements ITextHelper {

    /**
     * Utility class.
     */
    private ITextHelperV2() {
    }

    /**
     *
     * @param allow
     *            PDF allowed properties.
     * @return Permissions.
     */
    public static int getPermissions(final PdfProperties.PdfAllow allow) {

        int iPermissions = 0;

        if (allow.getAssembly()) {
            iPermissions |= PdfWriter.ALLOW_ASSEMBLY;
        }
        if (allow.getCopy()) {
            iPermissions |= PdfWriter.ALLOW_COPY;
        }
        if (allow.getCopyForAccess()) {
            iPermissions |= PdfWriter.ALLOW_SCREENREADERS;
        }
        if (allow.getFillin()) {
            iPermissions |= PdfWriter.ALLOW_FILL_IN;
        }
        if (allow.getPrinting()) {
            iPermissions |= PdfWriter.ALLOW_PRINTING;
        }
        if (allow.getDegradedPrinting()) {
            iPermissions |= PdfWriter.ALLOW_DEGRADED_PRINTING;
        }

        if (allow.getModifyContents()) {
            iPermissions |= PdfWriter.ALLOW_MODIFY_CONTENTS;
        }
        if (allow.getModifyAnnotations()) {
            iPermissions |= PdfWriter.ALLOW_MODIFY_ANNOTATIONS;
        }

        return iPermissions;
    }
}
