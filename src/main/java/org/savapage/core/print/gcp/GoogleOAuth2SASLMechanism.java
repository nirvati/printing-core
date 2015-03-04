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
package org.savapage.core.print.gcp;

import java.io.IOException;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class GoogleOAuth2SASLMechanism extends SASLMechanism {

    private static final String NAME = "X-OAUTH2";

    private static final String AUTH_STANZA =
            "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\""
                    + " mechanism=\"X-OAUTH2\""
                    + " auth:service=\"chromiumsync\""
                    + " auth:allow-generated-jid=\"true\""
                    + " auth:client-uses-full-bind-result=\"true\""
                    + " xmlns:auth=\"http://www.google.com/talk/protocol/auth\">"
                    + "%s" + "</auth>";

    /**
     *
     * @param saslAuthentication
     */
    public GoogleOAuth2SASLMechanism(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);
    }

    @Override
    protected String getName() {
        return NAME;
    }

    @Override
    protected void authenticate() throws IOException, XMPPException {

        /*
         * Combine the google id and OAuth2 access token (password) into an
         * encoded string.
         */
        final byte credentialBytes[] =
                ("\0" + authenticationId + "\0" + password).getBytes("UTF-8");

        final String authenticationText =
                Base64.encodeBytes(credentialBytes, Base64.DONT_BREAK_LINES);

        /*
         * Insert the credentials data into the main auth stanza.
         */
        final String completeAuthStanza =
                String.format(AUTH_STANZA, authenticationText);

        /*
         * Create and send a packet that will use the completed OAuth2 stanza
         * for authentication.
         */
        final Packet p = new Packet() {
            @Override
            public String toXML() {
                return completeAuthStanza;
            }
        };
        getSASLAuthentication().send(p);
    }
}