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

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.connection.NoConnectException;
import com.sun.star.connection.XConnection;
import com.sun.star.connection.XConnector;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

/**
 * UNO connection to an SOffice host process.
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeConnection implements SOfficeContext {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SOfficeConnection.class);

    /**
     *
     */
    private static AtomicInteger bridgeIndex = new AtomicInteger();

    /**
     *
     */
    private final SOfficeUnoUrl unoUrl;

    /**
     *
     */
    private XComponent unoBridgeComponent;

    /**
     *
     */
    private XMultiComponentFactory unoServiceManager;

    /**
     *
     */
    private XComponentContext unoComponentContext;

    /**
     *
     */
    private final List<SOfficeConnectionListener> connectionListeners =
            new ArrayList<>();

    /**
     *
     */
    private volatile boolean connected = false;

    /**
     *
     */
    private final XEventListener unoBridgeListener = new XEventListener() {

        @Override
        public void disposing(final EventObject event) {

            if (!connected) {
                return;
            }

            connected = false;

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Disconnect of: '%s'", unoUrl));
            }

            final SOfficeConnectEvent connectionEvent =
                    new SOfficeConnectEvent(SOfficeConnection.this);

            for (final SOfficeConnectionListener lstner : connectionListeners) {
                lstner.onDisconnected(connectionEvent);
            }

        }
    };

    /**
     * Constructor.
     *
     * @param url
     *            The UNO url.
     */
    public SOfficeConnection(final SOfficeUnoUrl url) {
        this.unoUrl = url;
    }

    /**
     * Add a connection listener.
     *
     * @param listener
     *            The listener.
     */
    public void addConnectionEventListener(
            final SOfficeConnectionListener listener) {
        connectionListeners.add(listener);
    }

    /**
     * Connects to the SOffice host process with UNO URL.
     *
     * @throws ConnectException
     *             if connection fails.
     */
    public void connect() throws ConnectException {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Connecting with '%s' ...", unoUrl));
        }

        try {

            final XComponentContext localContext =
                    Bootstrap.createInitialComponentContext(null);

            final XMultiComponentFactory localServiceManager =
                    localContext.getServiceManager();

            final XConnector connector =
                    SOfficeHelper.unoCast(XConnector.class,
                            localServiceManager.createInstanceWithContext(
                                    "com.sun.star.connection.Connector",
                                    localContext));
            final XConnection connection =
                    connector.connect(unoUrl.getConnectString());

            final XBridgeFactory bridgeFactory =
                    SOfficeHelper.unoCast(XBridgeFactory.class,
                            localServiceManager.createInstanceWithContext(
                                    "com.sun.star.bridge.BridgeFactory",
                                    localContext));

            final String bridgeName =
                    "savage_soffice_converter_" + bridgeIndex.getAndIncrement();

            final XBridge bridge = bridgeFactory.createBridge(bridgeName, "urp",
                    connection, null);

            unoBridgeComponent =
                    SOfficeHelper.unoCast(XComponent.class, bridge);
            unoBridgeComponent.addEventListener(unoBridgeListener);

            unoServiceManager =
                    SOfficeHelper.unoCast(XMultiComponentFactory.class,
                            bridge.getInstance("StarOffice.ServiceManager"));

            final XPropertySet properties = SOfficeHelper
                    .unoCast(XPropertySet.class, unoServiceManager);

            unoComponentContext = SOfficeHelper.unoCast(XComponentContext.class,
                    properties.getPropertyValue("DefaultContext"));

            connected = true;

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Connected with '%s'", unoUrl));
            }

            final SOfficeConnectEvent connectionEvent =
                    new SOfficeConnectEvent(this);

            for (final SOfficeConnectionListener lner : connectionListeners) {
                lner.onConnected(connectionEvent);
            }

        } catch (NoConnectException e) {
            throw new ConnectException(String.format(
                    "Connection '%s' failed: %s", unoUrl, e.getMessage()));

        } catch (Exception e) {
            throw new SOfficeException(
                    String.format("Connection '%s' failed.", unoUrl), e);
        }
    }

    /**
     *
     * @return {@code true} when connected.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     *
     */
    public synchronized void disconnect() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Disconnecting '%s' ...", unoUrl));
        }
        unoBridgeComponent.dispose();
    }

    @Override
    public Object getService(final String serviceName) {
        try {
            return unoServiceManager.createInstanceWithContext(serviceName,
                    unoComponentContext);
        } catch (Exception exception) {
            throw new SOfficeException(String
                    .format("Failed to obtain UNO service '%s'", serviceName),
                    exception);
        }
    }

}
