/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dto;

import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RedirectPrinterDto extends AbstractDto {

    /**
     * The database key of the printer.
     */
    private Long id;

    /**
     * The display name.
     */
    private String name;

    /**
     * {@code true} when this is the preferred printer.
     */
    private boolean preferred;

    /**
     *
     */
    private String deviceUri;

    /**
     * The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} option op the
     * printer.
     */
    private JsonProxyPrinterOpt mediaSourceOpt;

    /**
     * The default media-source choice. When {@code null}, no default is
     * available.
     */
    private JsonProxyPrinterOptChoice mediaSourceOptChoice;

    /**
     *
     * @return
     */
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(final boolean preferred) {
        this.preferred = preferred;
    }

    public String getDeviceUri() {
        return deviceUri;
    }

    public void setDeviceUri(final String deviceUri) {
        this.deviceUri = deviceUri;
    }

    /**
     * @return The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} option.
     */
    public JsonProxyPrinterOpt getMediaSourceOpt() {
        return mediaSourceOpt;
    }

    /**
     * @param mediaSource
     *            The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} option.
     */
    public void setMediaSourceOpt(final JsonProxyPrinterOpt mediaSource) {
        this.mediaSourceOpt = mediaSource;
    }

    /**
     * @return The default media-source choice. When {@code null}, no default is
     *         available.
     */
    public JsonProxyPrinterOptChoice getMediaSourceOptChoice() {
        return mediaSourceOptChoice;
    }

    /**
     * @param mediaSourceOptChoice
     *            The default media-source choice. When {@code null}, no default
     *            is available.
     */
    public void setMediaSourceOptChoice(
            JsonProxyPrinterOptChoice mediaSourceOptChoice) {
        this.mediaSourceOptChoice = mediaSourceOptChoice;
    }

}
