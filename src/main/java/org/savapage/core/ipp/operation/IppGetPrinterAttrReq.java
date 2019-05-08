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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.ipp.operation;

import java.io.IOException;
import java.io.InputStream;

/**
 * 3.2.5.1 Get-Printer-Attributes Request.
 *
 * http://tools.ietf.org/html/rfc2911#section-3.2.5
 */
public class IppGetPrinterAttrReq extends AbstractIppRequest {

    /**
     * Sample request:
     *
     * <pre>
     * Group: OPERATION_ATTR
     *   Attr: attributes-charset
     *     Value: utf-8
     *   Attr: attributes-natural-language
     *     Value: en-us
     *   Attr: printer-uri
     *     Value: http://pampus:8080/ipp
     *   Attr: requested-attributes
     *     Value: com.apple.print.recoverable-message
     *     Value: copies-supported
     *     Value: document-format-supported
     *     Value: marker-colors
     *     Value: marker-high-levels
     *     Value: marker-levels
     *     Value: marker-low-levels
     *     Value: marker-message
     *     Value: marker-names
     *     Value: marker-types
     *     Value: printer-is-accepting-jobs
     *     Value: printer-state
     *     Value: printer-state-message
     *     Value: printer-state-reasons
     * </pre>
     *
     * From iPad:
     *
     * <pre>
     * Group: OPERATION_ATTR
     *   Attr: attributes-charset
     *     Value: utf-8
     *   Attr: attributes-natural-language
     *     Value: en-us
     *   Attr: printer-uri
     *     Value: ipp://pampus.local.:8080/printers/airprint
     *   Attr: requested-attributes
     *     Value: copies-supported
     *     Value: document-format-supported
     *     Value: jpeg-k-octets-supported
     *     Value: jpeg-x-dimension-supported
     *     Value: jpeg-y-dimension-supported
     *     Value: media-bottom-margin-supported
     *     Value: media-col-database
     *     Value: landscape-orientation-requested-preferred
     *     Value: media-col-supported
     *     Value: media-left-margin-supported
     *     Value: media-col-ready
     *     Value: media-right-margin-supported
     *     Value: media-top-margin-supported
     *     Value: operations-supported
     *     Value: pdf-k-octets-supported
     *     Value: print-color-mode-supported
     *     Value: print-quality-supported
     *     Value: sides-supported
     * </pre>
     */

    @Override
    public final void process(final InputStream istr) throws IOException {
        readAttributes(istr);
    }

}
