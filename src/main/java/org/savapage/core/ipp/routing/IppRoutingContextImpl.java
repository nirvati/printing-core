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
package org.savapage.core.ipp.routing;

import java.io.File;
import java.io.IOException;

import org.savapage.core.jpa.IppQueue;
import org.savapage.core.util.FileSystemHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRoutingContextImpl implements IppRoutingContext {

    /**
     * Originator IP address.
     */
    private String originatorIp;

    /**
     * URL path of {@link IppQueue#getUrlPath()}.
     */
    private String urlPath;

    /**
     * The PDF file to print.
     */
    private File pdfToPrint;

    @Override
    public String getOriginatorIp() {
        return originatorIp;
    }

    /**
     * @param originatorIp
     *            Originator IP address.
     */
    public void setOriginatorIp(String originatorIp) {
        this.originatorIp = originatorIp;
    }

    @Override
    public String getUrlPath() {
        return urlPath;
    }

    /**
     * @param path
     *            URL path of {@link IppQueue#getUrlPath()}.
     */
    public void setUrlPath(final String path) {
        this.urlPath = path;
    }

    @Override
    public File getPdfToPrint() {
        return pdfToPrint;
    }

    /**
     * @param file
     *            The PDF file to print.
     */
    public void setPdfToPrint(final File file) {
        this.pdfToPrint = file;
    }

    @Override
    public void replacePdfToPrint(final File newFile) throws IOException {
        FileSystemHelper.replaceWithNewVersion(this.pdfToPrint, newFile);
    }

}
