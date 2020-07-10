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
package org.savapage.core.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.savapage.core.UnavailableException;

/**
 * Convert JPEG raster to PDF.
 *
 * @author Rijk Ravestein
 *
 */
public final class JPEGToPdf implements IDocFileConverter {

    @Override
    public File convert(final DocContentTypeEnum contentType, final File file)
            throws DocContentToPdfException, UnavailableException {

        final IStreamConverter exec = new ImageToPdf();

        final File filePdf = AbstractFileConverter.getFileSibling(file,
                DocContentTypeEnum.PDF);

        try (FileInputStream istr = new FileInputStream(file);
                DocInputStream istrDoc = new DocInputStream(istr);
                FileOutputStream ostr = new FileOutputStream(filePdf);) {

            exec.convert(contentType, istrDoc, ostr);

        } catch (Exception e) {
            throw new DocContentToPdfException(e.getMessage());
        }

        return filePdf;
    }

    @Override
    public boolean notifyStdOutMsg() {
        return false;
    }

    @Override
    public boolean hasStdErrMsg() {
        return false;
    }

}
