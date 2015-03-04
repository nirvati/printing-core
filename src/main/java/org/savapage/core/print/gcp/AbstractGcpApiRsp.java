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

import org.savapage.core.SpException;
import org.savapage.core.json.JsonAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractGcpApiRsp extends JsonAbstractBase {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractGcpApiRsp.class);

    private static final String KEY_SUCCESS = "success";

    private JsonNode rootNode;

    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    protected AbstractGcpApiRsp(byte[] content) {
        try {

            this.rootNode = getMapper().readTree(content);

            this.success = isSuccess(rootNode);

            if (this.success) {
                onSuccess(this.rootNode);
            }

        } catch (Exception e) {
            LOGGER.error(new String(content));
            throw new SpException(e);
        }
    }

    /**
     * Checks if the response reports success.
     *
     * @param root
     *            The root node.
     * @return {@code true} if success.
     */
    protected boolean isSuccess(JsonNode root) {
        /*
         * Be optimistic.
         */
        boolean isSuccess = true;

        /*
         * Use the success attribute if present.
         */
        if (root.has(KEY_SUCCESS)) {
            isSuccess = root.get(KEY_SUCCESS).asBoolean();
        }
        return isSuccess;
    }

    /**
     *
     * @return
     */
    protected JsonNode getRootNode() {
        return rootNode;
    }

    protected abstract void onSuccess(JsonNode root);

}
