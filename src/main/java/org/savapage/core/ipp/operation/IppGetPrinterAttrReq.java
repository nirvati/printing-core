package org.savapage.core.ipp.operation;

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
    public final void process(final InputStream istr) throws Exception {
        readAttributes(istr);
    }

}
