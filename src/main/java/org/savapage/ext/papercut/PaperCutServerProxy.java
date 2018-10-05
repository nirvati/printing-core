/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.ext.papercut;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcHttpTransportException;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.util.RetryException;
import org.savapage.core.util.RetryExecutor;
import org.savapage.core.util.RetryTimeoutException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutServerProxy {

    /**
     * .
     */
    private final XmlRpcClient xmlRpcClient;

    /**
     * .
     */
    private final String authToken;

    /**
     * {@code true} when a {@link CircuitBreaker} is used to signal PaperCut
     * connection status.
     */
    private boolean useCircuitBreaker;

    /**
     * .
     */
    private static final CircuitBreaker CIRCUIT_BREAKER = ConfigManager
            .getCircuitBreaker(CircuitBreakerEnum.PAPERCUT_CONNECTION);

    /**
     * .
     */
    private enum UserProperty {

        /** */
        BALANCE("balance"),
        /** */
        DISABLED_PRINT("disabled-print"),
        /** */
        EMAIL("email"),
        /** */
        FULL_NAME("full-name"),
        /** */
        NOTES("notes"),
        /** */
        OFFICE("office"),
        /** */
        PRINT_JOB_COUNT("print-stats.job-count"),
        /** */
        PRINT_PAGE_COUNT("print-stats.page-count"),
        /** */
        RESTRICTED("restricted");

        /** */
        private final String propName;

        /**
         * @param name
         *            The property name.
         */
        UserProperty(final String name) {
            this.propName = name;
        }

        /**
         * @return The property name.
         */
        public String getPropName() {
            return this.propName;
        }
    }

    /**
     * A {@link CircuitBreakerOperation} wrapper for the
     * {@link PaperCutProxy#execute(String, Vector))} method.
     *
     */
    private static class PaperCutCircuitBreakerOperation
            implements CircuitBreakerOperation {

        /** */
        private final PaperCutServerProxy paperCutProxy;
        /** */
        private final String method;
        /** */
        private final Vector<Object> parameters;

        /**
         *
         * @param paperCutProxy
         * @param method
         * @param parameters
         */
        public PaperCutCircuitBreakerOperation(
                final PaperCutServerProxy paperCutProxy, final String method,
                final Vector<Object> parameters) {
            this.paperCutProxy = paperCutProxy;
            this.method = method;
            this.parameters = parameters;
        }

        @Override
        public Object execute(final CircuitBreaker circuitBreaker) {

            try {
                return paperCutProxy.execute(method, parameters);
            } catch (PaperCutException e) {
                throw new CircuitNonTrippingException(e.getMessage(), e);
            } catch (PaperCutConnectException e) {
                throw new CircuitTrippingException(e.getMessage(), e);
            }
        }

    };

    /**
     * The constructor.
     *
     * @param xmlRpcClient
     *            The {@link XmlRpcClient}.
     * @param authToken
     *            The authentication token as a string. All RPC calls must pass
     *            through an authentication token. At the current time this is
     *            simply the built-in "admin" user's password.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     */
    private PaperCutServerProxy(final XmlRpcClient xmlRpcClient,
            final String authToken, final boolean useCircuitBreaker) {
        this.authToken = authToken;
        this.xmlRpcClient = xmlRpcClient;
        this.useCircuitBreaker = useCircuitBreaker;
    }

    /**
     * Creates a {@link PaperCutServerProxy} instance from the application
     * configuration.
     *
     * @param cm
     *            The {@link ConfigManager}.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutServerProxy} instance.
     */
    public static PaperCutServerProxy create(final ConfigManager cm,
            final boolean useCircuitBreaker) {

        return PaperCutServerProxy.create(
                cm.getConfigValue(Key.PAPERCUT_SERVER_HOST),
                cm.getConfigInt(Key.PAPERCUT_SERVER_PORT),
                cm.getConfigValue(Key.PAPERCUT_XMLRPC_URL_PATH),
                cm.getConfigValue(Key.PAPERCUT_SERVER_AUTH_TOKEN),
                useCircuitBreaker);
    }

    /**
     * Creates a {@link PaperCutServerProxy} instance.
     *
     * @param server
     *            The name or IP address of the server hosting the Application
     *            Server. The server should be configured to allow XML-RPC
     *            connections from the host running this proxy class.
     * @param port
     *            The port the Application Server is listening on. This is port
     *            9191 on a default install.
     * @param urlPath
     *            The URL path. E.g. {@code /rpc/api/xmlrpc}
     * @param authToken
     *            The authentication token as a string.
     * @param useCircuitBreaker
     *            If {@code true} a {@link CircuitBreakerOperation} is used.
     * @return The {@link PaperCutServerProxy} instance.
     */
    public static PaperCutServerProxy create(final String server,
            final int port, final String urlPath, final String authToken,
            final boolean useCircuitBreaker) {

        final XmlRpcClient xmlRpcClient;

        try {

            final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

            final URL serverUrl = new URL("http", server, port, urlPath);

            config.setServerURL(serverUrl);

            xmlRpcClient = new XmlRpcClient();
            xmlRpcClient.setConfig(config);

        } catch (MalformedURLException e) {
            throw new SpException("Invalid server name supplied");
        }

        return new PaperCutServerProxy(xmlRpcClient, authToken,
                useCircuitBreaker);
    }

    /**
     *
     * @param method
     * @param parameters
     * @return
     * @throws PaperCutException
     */
    private Object execute(final String method, final Vector<Object> parameters)
            throws PaperCutException {

        Object result = null;

        try {
            result = this.xmlRpcClient.execute(method, parameters);

        } catch (XmlRpcHttpTransportException e) {
            /*
             * When request could not be processed by the server, e.g. because
             * the URL path is invalid.
             */
            throw new PaperCutConnectException(e.getMessage(), e);

        } catch (XmlRpcClientException e) {
            /*
             * The invocation failed on the client side(for example when
             * communication with the server failed, e.g. because host cannot be
             * found).
             */
            throw new PaperCutConnectException(e.getMessage(), e);

        } catch (XmlRpcException e) {

            if (e.linkedException instanceof UnknownHostException) {
                throw new PaperCutConnectException(
                        String.format("Unknown host [%s]",
                                e.linkedException.getMessage()),
                        e);
            }

            if (e.linkedException instanceof ConnectException) {
                throw new PaperCutConnectException(
                        String.format("%s", e.linkedException.getMessage()), e);
            }

            /*
             * The invocation failed on the remote side (for example, an
             * exception was thrown within the server)
             */
            String msg = e.getMessage();
            /*
             * Format message so it's a little cleaner. Remove any class
             * definitions.
             */
            msg = msg.replaceAll("\\w+(?:\\.+\\w+)+:\\s?", "");
            throw new PaperCutException(msg, e);
        }
        return result;
    }

    /**
     * Calls the XML-RPC method on the server.
     *
     * @param method
     *            The method to execute.
     * @param parameters
     *            The parameters to the method.
     * @return The value returned from the method.
     * @throws PaperCutException
     *             When PaperCut Server encountered an error during execution of
     *             the RPC.
     */
    private Object call(final String method, final Vector<Object> parameters)
            throws PaperCutException {

        if (!this.useCircuitBreaker) {
            return execute(method, parameters);
        }

        final CircuitBreakerOperation operation =
                new PaperCutCircuitBreakerOperation(this, method, parameters);

        try {

            return CIRCUIT_BREAKER.execute(operation);

        } catch (InterruptedException | CircuitBreakerException e) {
            throw new PaperCutConnectException(e.getMessage(), e);
        } catch (CircuitNonTrippingException e) {
            throw new PaperCutException(e.getMessage(), e);
        }

    }

    /**
     *
     * @return
     */
    private Vector<Object> createParams() {
        final Vector<Object> params = new Vector<Object>();
        params.add(this.authToken);
        return params;
    }

    /**
     * Tests if a user associated with "username" exists.
     *
     * @param username
     *            The username to test.
     * @return Returns true if the user exists in the system, else returns
     *         false.
     */
    public boolean isUserExists(final String username) {

        final Vector<Object> params = createParams();

        params.add(username);

        Boolean exist;

        try {
            exist = (Boolean) call("api.isUserExists", params);
        } catch (PaperCutException e) {
            exist = Boolean.FALSE;
        }
        return exist.booleanValue();
    }

    /**
     * Test to see if a shared account exists.
     *
     * @param accountName
     *            The name of the shared account.
     * @return Return true if the shared account exists, else false.
     */
    public boolean isSharedAccountExists(final String accountName) {

        final Vector<Object> params = createParams();

        params.add(accountName);

        Boolean exist;

        try {
            exist = (Boolean) call("api.isSharedAccountExists", params);
        } catch (PaperCutException e) {
            exist = Boolean.FALSE;
        }

        return exist.booleanValue();
    }

    /**
     * Connects to PaperCut.
     *
     * @throws PaperCutConnectException
     *             When connect fails.
     */
    public void connect() {
        isUserExists("some-dummy-user");
    }

    /**
     * Waits till connected to PaperCut. This method tries to connect after a
     * delay, every "interval", till connected or a timeout occurs.
     *
     * @param delay
     *            The delay (milliseconds) before the first attempt is made.
     * @param interval
     *            The attempt interval (milliseconds).
     * @param timeout
     *            The timeout (milliseconds).
     * @throws Exception
     *             if error.
     * @throws RetryTimeoutException
     *             if timeout.
     */
    public void connect(final long delay, final long interval,
            final long timeout) throws RetryTimeoutException, Exception {

        final PaperCutServerProxy proxy = this;

        final RetryExecutor exec = new RetryExecutor() {

            private int nAttemps = 0;

            @Override
            protected void attempt() throws RetryException, Exception {
                try {
                    proxy.connect();
                    if (nAttemps > 0) {
                        SpInfo.instance().log("Connected to PaperCut.");
                    }
                } catch (PaperCutConnectException e) {
                    if (nAttemps == 0) {
                        SpInfo.instance().log("Connecting to PaperCut...");
                    }
                    nAttemps++;
                    throw new RetryException(e);
                }
            }
        };

        final boolean savedBreakerUsage = this.isUseCircuitBreaker();

        this.setUseCircuitBreaker(false);

        try {
            exec.execute(delay, interval, timeout);
        } finally {
            this.setUseCircuitBreaker(savedBreakerUsage);
        }
    }

    /**
     * Disconnects from PaperCut.
     */
    public void disconnect() {
        // no code intended
    }

    /**
     * Tests the PaperCut server connection.
     * <p>
     * Note: No {@link CircuitBreakerOperation} is used.
     * </p>
     *
     * @return {@code null} when connection succeeded, otherwise a string with
     *         the error message.
     */
    public String testConnection() {

        String error = null;

        try {
            final Vector<Object> params = createParams();
            params.add("some-dummy-user");

            execute("api.isUserExists", params);

        } catch (Exception e) {
            /*
             * Make sure a non-null error message is produced.
             */
            error = "" + e.getMessage();
        }

        return error;
    }

    /**
     * Gets the {@link PaperCutUser}.
     *
     * @param userName
     *            The unique name of the user.
     * @return The {@link PaperCutUser} or {@code null} when the user does not
     *         exist.
     */
    public PaperCutUser getUser(final String userName) {

        final Map<UserProperty, String> props = this.getUserProps(userName);
        final PaperCutUser user;

        if (props == null) {
            user = null;
        } else {
            user = new PaperCutUser();

            user.setDisabledPrint(Boolean
                    .parseBoolean(props.get(UserProperty.DISABLED_PRINT)));

            user.setRestricted(
                    Boolean.parseBoolean(props.get(UserProperty.RESTRICTED)));

            user.setFullName(props.get(UserProperty.FULL_NAME));

            user.setBalance(
                    Double.parseDouble(props.get(UserProperty.BALANCE)));

        }

        return user;
    }

    /**
     * Gets all user properties.
     *
     * @param userName
     *            The name of the user.
     * @return The property values or {@code null} when the user does not exist.
     */
    private Map<UserProperty, String> getUserProps(final String userName) {
        return getUserProps(userName, UserProperty.values());
    }

    /**
     * Gets multiple user properties at once (to save multiple calls).
     *
     * @param userName
     *            The name of the user.
     * @param propertyNames
     *            The names of the properties to get.
     * @return The property values or {@code null} when the user does not exist.
     */
    private Map<UserProperty, String> getUserProps(final String userName,
            final UserProperty[] userProperties) {

        final Vector<String> propertyNames = new Vector<>();

        for (final UserProperty prop : userProperties) {
            propertyNames.add(prop.getPropName());
        }

        final Vector<Object> params = createParams();

        params.add(userName);
        params.add(propertyNames);

        Object[] propertyValues;

        try {
            propertyValues = (Object[]) call("api.getUserProperties", params);
        } catch (PaperCutException e) {
            return null;
        }

        final EnumMap<UserProperty, String> enumMap =
                new EnumMap<UserProperty, String>(UserProperty.class);

        int i = 0;
        for (final Object value : propertyValues) {
            enumMap.put(userProperties[i++], value.toString());
        }
        return enumMap;
    }

    /**
     * Composes a single account name from a top and sub account name.
     * <p>
     * A '\' to denote a subaccount, e.g.: 'top\sub'.
     * </p>
     *
     * @param topAccountName
     *            The top account name.
     * @param subAccountName
     *            The sub account name.
     * @return The composes name.
     */
    public String composeSharedAccountName(final String topAccountName,
            final String subAccountName) {

        final StringBuilder accountName = new StringBuilder(topAccountName);

        if (subAccountName != null) {
            accountName.append('\\').append(subAccountName);
        }
        return accountName.toString();
    }

    /**
     * Adjust a shared account's account balance by an adjustment amount. An
     * adjustment bay be positive (add to the account) or negative (subtract
     * from the account).
     *
     * @param topAccountName
     *            The full name of the top shared account to adjust.
     * @param subAccountName
     *            The full name of the sub shared account to adjust (can be
     *            {@code null}.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to associated with the transaction.
     *            This may be a null string.
     * @throws PaperCutException
     *             When the accountName does not exist.
     */
    public void adjustSharedAccountAccountBalance(final String topAccountName,
            final String subAccountName, final double adjustment,
            final String comment) throws PaperCutException {

        final Vector<Object> params = createParams();
        params.add(composeSharedAccountName(topAccountName, subAccountName));
        params.add(adjustment);
        params.add(StringUtils.trimToEmpty(comment));
        call("api.adjustSharedAccountAccountBalance", params);
    }

    /**
     * Adjusts a user's built-in/default account balance by an adjustment
     * amount. An adjustment may be positive (add to the user's account) or
     * negative (subtract from the account).
     *
     * @param username
     *            The username associated with the user who's account is to be
     *            adjusted.
     * @param userAccountName
     *            Optional name of the user's personal account. If {@code null},
     *            the built-in default account is used. If multiple personal
     *            accounts is enabled the account name must be provided.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to be associated with the transaction.
     *            This may be a null string.
     * @throws PaperCutException
     *             When the user (account) does not exist.
     */
    public void adjustUserAccountBalance(final String username,
            final String userAccountName, final double adjustment,
            final String comment) throws PaperCutException {
        /*
         * If "Multiple personal accounts" is enabled in PaperCut, the account
         * name to adjust must be provided. The user's built-in/default
         * accountName must be provided.
         */
        this.adjustUserAccountBalance(username, adjustment, comment,
                userAccountName);
    }

    /**
     * Adjust a user's account balance by an adjustment amount. An adjustment
     * may be positive (add to the user's account) or negative (subtract from
     * the account).
     *
     * @param username
     *            The username associated with the user who's account is to be
     *            adjusted.
     * @param adjustment
     *            The adjustment amount. Positive to add credit and negative to
     *            subtract.
     * @param comment
     *            A user defined comment to be associated with the transaction.
     *            This may be a null string.
     * @param accountName
     *            Optional name of the user's personal account. If {@code null}
     *            or empty, the built-in default account is used. If multiple
     *            personal accounts is enabled the account name must be
     *            provided.
     * @throws PaperCutException
     *             When the user (account) does not exist.
     */
    public void adjustUserAccountBalance(final String username,
            final double adjustment, final String comment,
            final String accountName) throws PaperCutException {
        Vector<Object> params = createParams();
        params.add(username);
        params.add(adjustment);
        params.add(StringUtils.trimToEmpty(comment));
        params.add(StringUtils.trimToEmpty(accountName));
        call("api.adjustUserAccountBalance", params);
    }

    /**
     * Gets a user's account balance.
     *
     * @param username
     *            The username associated with the user who's account balance is
     *            to be retrieved.
     * @param scale
     *            The scale of the return value.
     * @return The {@link BigDecimal} balance.
     * @throws PaperCutException
     *             When the user (account) does not exist.
     */
    public BigDecimal getUserAccountBalance(final String username,
            final int scale) throws PaperCutException {

        final Vector<Object> params = createParams();
        params.add(username);

        final Double balance =
                (Double) call("api.getUserAccountBalance", params);

        return PaperCutDb.getAmountBigBecimal(balance.doubleValue(), scale);
    }

    /**
     * Create a new shared account with the given name.
     *
     * @param topAccountName
     *            The full name of the top shared account to adjust.
     * @param subAccountName
     *            The full name of the sub shared account to adjust (can be
     *            {@code null}.
     * @throws PaperCutException
     *             When the account already exists.
     */
    public void addNewSharedAccount(final String topAccountName,
            final String subAccountName) throws PaperCutException {
        final Vector<Object> params = createParams();
        params.add(composeSharedAccountName(topAccountName, subAccountName));
        call("api.addNewSharedAccount", params);
    }

    /**
     * @return {@code true} when a {@link CircuitBreaker} is used to signal
     *         PaperCut connection status.
     */
    public boolean isUseCircuitBreaker() {
        return useCircuitBreaker;
    }

    /**
     * @param useBreaker
     *            {@code true} when a {@link CircuitBreaker} is used to signal
     *            PaperCut connection status.
     */
    public void setUseCircuitBreaker(final boolean useBreaker) {
        this.useCircuitBreaker = useBreaker;
    }

}
