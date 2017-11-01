package org.savapage.core.json.rpc;

import org.savapage.core.services.AccountingException;

public final class JsonRpcParserException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link AccountingException}.
     *
     * @param cause
     *            The cause.
     */
    public JsonRpcParserException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@link AccountingException}.
     *
     * @param message
     *            The detail message.
     */
    public JsonRpcParserException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@link AccountingException}.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public JsonRpcParserException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
