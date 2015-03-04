package org.savapage.core.ipp.operation;

import java.io.InputStream;
import java.io.OutputStream;

import org.savapage.core.ipp.attribute.IppAttrValue;

/**
 * This REQUIRED operation allows a client to request the values of the
 * attributes of a Printer object. In the request, the client supplies the set
 * of Printer attribute names and/or attribute group names in which the
 * requester is interested. In the response, the Printer object returns a
 * corresponding attribute set with the appropriate attribute values filled in.
 *
 * <p>
 * For Printer objects, the possible names of attribute groups are:
 * <ul>
 * <li>'job-template': the subset of the Job Template attributes that apply to a
 * Printer object (the last two columns of the table in Section 4.2) that the
 * implementation supports for Printer objects.</li>
 *
 * <li>'printer-description': the subset of the attributes specified in Section
 * 4.4 that the implementation supports for Printer objects.</li>
 *
 * <li>'all': the special group 'all' that includes all attributes that the
 * implementation supports for Printer objects.</li>
 *
 * </ul>
 * </p>
 */
public class IppGetPrinterAttrOperation extends AbstractIppOperation {

    /**
     * The special group 'all' that includes all attributes that the
     * implementation supports for Printer objects.
     */
    public static final String ATTR_GRP_ALL = "all";

    /**
     * Subset of the Job Template attributes that apply to a Printer object (the
     * last two columns of the table in Section 4.2) that the implementation
     * supports for Printer objects.
     */
    public static final String ATTR_GRP_JOB_TPL = "job-template";

    /**
     * Subset of the attributes specified in Section 4.4 that the implementation
     * supports for Printer objects.
     */
    public static final String ATTR_GRP_PRINTER_DESC = "printer-description";

    /**
     *
     */
    public static final String ATTR_GRP_MEDIA_COL_DATABASE =
            "media-col-database";

    /**
     *
     */
    private final IppGetPrinterAttrReq request = new IppGetPrinterAttrReq();

    /**
     *
     */
    private final IppGetPrinterAttrRsp response = new IppGetPrinterAttrRsp();

    /**
     *
     * @return
     */
    public IppAttrValue getRequestedAttributes() {
        return request.getRequestedAttributes();
    }

    /*
     * Since a client MAY request specific attributes or named groups, there is
     * a potential that there is some overlap. For example, if a client
     * requests, 'printer-name' and 'all', the client is actually requesting the
     * "printer-name" attribute twice: once by naming it explicitly, and once by
     * inclusion in the 'all' group. In such cases, the Printer object NEED NOT
     * return each attribute only once in the response even if it is requested
     * multiple times. The client SHOULD NOT request the same attribute in
     * multiple ways.
     *
     * It is NOT REQUIRED that a Printer object support all attributes belonging
     * to a group (since some attributes are OPTIONAL). However, it is REQUIRED
     * that each Printer object support all group names.
     */

    @Override
    protected final void process(final InputStream istr, final OutputStream ostr)
            throws Exception {
        request.process(istr);
        response.process(this, request, ostr);
    }

}
