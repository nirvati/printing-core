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
package org.savapage.core.doc.soffice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class SOfficeDocFormatRegistrySimple
        implements SOfficeDocFormatRegistry {

    /**
     *
     */
    private final List<SOfficeDocFormat> documentFormats =
            new ArrayList<SOfficeDocFormat>();

    /**
     *
     * @param documentFormat
     *            The format.
     */
    public final void addFormat(final SOfficeDocFormat documentFormat) {
        documentFormats.add(documentFormat);
    }

    @Override
    public final SOfficeDocFormat getFormatByExtension(final String extension) {

        if (extension == null) {
            return null;
        }

        final String lowerExtension = extension.toLowerCase();

        /*
         * A serial search ...
         */
        for (final SOfficeDocFormat format : documentFormats) {
            if (format.getExtension().equals(lowerExtension)) {
                return format;
            }
        }
        return null;
    }

    @Override
    public final SOfficeDocFormat getFormatByMediaType(final String mediaType) {

        if (mediaType == null) {
            return null;
        }

        /*
         * A serial search ...
         */
        for (final SOfficeDocFormat format : documentFormats) {
            if (format.getMediaType().equals(mediaType)) {
                return format;
            }
        }
        return null;
    }

    @Override
    public final Set<SOfficeDocFormat>
            getOutputFormats(final SOfficeDocFamilyEnum family) {

        final Set<SOfficeDocFormat> formats = new HashSet<SOfficeDocFormat>();

        for (final SOfficeDocFormat format : documentFormats) {
            if (format.getStoreProperties(family) != null) {
                formats.add(format);
            }
        }
        return formats;
    }

}
