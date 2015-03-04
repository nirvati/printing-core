package org.savapage.core.ipp.operation;

import java.io.InputStream;

import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.attribute.syntax.IppBoolean;

/**
 * The client submits the Get-Jobs request to a Printer object.
 * 
 */
public class IppGetJobsReq extends AbstractIppRequest {

    /**
     * 'completed': This includes any Job object whose state is 'completed',
     * 'canceled', or 'aborted'.
     */
    public static final String JOB_COMPLETED = "completed";

    /**
     * 'not-completed': This includes any Job object whose state is 'pending',
     * 'processing', 'processing-stopped', or 'pending- held'.
     */
    public static final String JOB_NOT_COMPLETED = "not-completed";

    /*
     * Group 1: Operation Attributes
     * 
     * 
     * Natural Language and Character Set:
     * 
     * The "attributes-charset" and "attributes-natural-language" attributes as
     * described in section 3.1.4.1.
     * 
     * 
     * Target:
     * 
     * The "printer-uri" (uri) operation attribute which is the target for this
     * operation as described in section 3.1.5.
     * 
     * 
     * Requesting User Name:
     * 
     * The "requesting-user-name" (name(MAX)) attribute SHOULD be supplied by
     * the client as described in section 8.3.
     * 
     * 
     * "limit" (integer(1:MAX)): ...
     * 
     * "requested-attributes" (1setOf type2 keyword):
     * 
     * The client OPTIONALLY supplies this attribute. The Printer object MUST
     * support this attribute. It is a set of Job attribute names and/or
     * attribute groups names in whose values the requester is interested. This
     * set of attributes is returned for each Job object that is returned. The
     * allowed attribute group names are the same as those defined in the
     * Get-Job-Attributes operation in section 3.3.4. If the client does not
     * supply this attribute, the Printer MUST respond as if the client had
     * supplied this attribute with two values: 'job- uri' and 'job-id'.
     * 
     * 
     * "which-jobs" (type2 keyword): ...
     * 
     * "my-jobs" (boolean): ...
     */

    @Override
    void process(final InputStream istr) throws Exception {
        readAttributes(istr);
    }

    /**
     * The maximum number of jobs that a client will receive from the Printer
     * even if "which-jobs" or "my-jobs" constrain which jobs are returned.
     * <p>
     * If {@code null} the Printer object responds with all applicable jobs.
     * </p>
     * 
     * @return {@code null} if NO limit.
     */
    public final String getLimit() {
        IppAttrValue value = getAttrValue("limit");
        if (value == null) {
            return null;
        }
        return value.getValues().get(0);
    }

    /**
     * If the client does not supply this attribute, the Printer object MUST
     * respond as if the client had supplied the attribute with a value of
     * 'not-completed'.
     * 
     * @return {@link IppGetJobsReq#JOB_COMPLETED} or
     *         {@link IppGetJobsReq#JOB_NOT_COMPLETED}
     */
    public final String getWhichJobs() {
        IppAttrValue value = getAttrValue("which-jobs");
        if (value == null) {
            return JOB_NOT_COMPLETED;
        }
        return value.getValues().get(0);
    }

    /**
     * Indicates whether jobs from all users or just the jobs submitted by the
     * requesting user of this request are considered as candidate jobs to be
     * returned by the Printer object.
     * 
     * @return {@code true} if jobs from THIS requesting user. {@code false} if
     *         jobs from ALL users.
     */
    public final boolean isMyJobs() {
        IppAttrValue value = getAttrValue("my-jobs");
        if (value == null) {
            return false;
        }
        return value.getValues().get(0).equals(IppBoolean.TRUE);
    }

}
