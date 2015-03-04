/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.services.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.savapage.core.dto.RfIdReaderStatusDto;
import org.savapage.core.services.RfIdReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class RfIdReaderServiceImpl extends AbstractService implements
        RfIdReaderService {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(RfIdReaderServiceImpl.class);

    private static final int CONNECT_TIMEOUT_MSEC = 3000;
    private static final int READ_TIMEOUT_MSEC = 2000;

    @Override
    public RfIdReaderStatusDto getReaderStatus(String host, int port) {

        RfIdReaderStatusDto status = new RfIdReaderStatusDto();

        Socket clientSocket = null;

        try {
            SocketAddress sockaddr = new InetSocketAddress(host, port);
            clientSocket = new Socket();
            clientSocket.connect(sockaddr, CONNECT_TIMEOUT_MSEC);

            final BufferedReader inFromServer =
                    new BufferedReader(new InputStreamReader(
                            clientSocket.getInputStream()));
            /*
             * We want a timeout here, for a connect to the wrong server won't
             * give us a reply.
             */
            clientSocket.setSoTimeout(READ_TIMEOUT_MSEC);

            final String line = inFromServer.readLine();

            if (line != null) {
                status.setConnected(line.equals(VENDOR_ID));
            } else {
                status.setConnected(false);
            }

        } catch (SecurityException | IllegalArgumentException | IOException e) {

            status.setError(e.getMessage());
            status.setConnected(false);

        } finally {

            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

        status.timestampNow();
        return status;
    }

}
