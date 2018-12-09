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
package org.savapage.core.dao.helpers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.savapage.core.dto.AbstractDto;
import org.savapage.lib.pgp.PGPPublicKeyInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class PGPPubRingKeyDto extends AbstractDto {

    /** */
    private Map<String, String> uids;

    /**
     * @return The UserId map with email address (key) and personal name
     *         (value).
     */
    public Map<String, String> getUids() {
        return uids;
    }

    /**
     * @param uids
     *            The UserId map with email address (key) and personal name
     *            (value).
     */
    public void setUids(final Map<String, String> uids) {
        this.uids = uids;
    }

    /**
     * Creates dto.
     *
     * @param info
     *            Public key info.
     * @return The dto.
     */
    @JsonIgnore
    public static PGPPubRingKeyDto create(final PGPPublicKeyInfo info) {
        final PGPPubRingKeyDto dto = new PGPPubRingKeyDto();

        final Map<String, String> uids = new HashMap<>();

        for (final InternetAddress adr : info.getUids()) {
            uids.put(adr.getAddress(), adr.getPersonal());
        }
        dto.setUids(uids);
        return dto;
    }

    /**
     * Creates JSON representation for database storage.
     *
     * @param info
     *            Public key info.
     * @return The flat JSON string.
     * @throws IOException
     *             When JSON error.
     */
    @JsonIgnore
    public static String toDbJson(final PGPPublicKeyInfo info)
            throws IOException {
        return PGPPubRingKeyDto.create(info).stringify();
    }

}
