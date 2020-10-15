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
package org.savapage.core.print.proxy;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TicketJobSheetDto {

    /**
     *
     */
    public enum Sheet {
        /** No job sheet. */
        NONE,
        /** At the start of the main job. */
        START,
        /** At the end of the main job. */
        END
    }

    /**
     * The RFC2911 IPP {@code media} keyword.
     */
    private String mediaOption;

    /**
     * The RFC2911 IPP {@code media-source} keyword.
     */
    private String mediaSourceOption;

    /**
     *
     */
    private Sheet sheet;

    /**
     * @return The RFC2911 IPP {@code media} keyword.
     */
    public String getMediaOption() {
        return mediaOption;
    }

    /**
     * @param media
     *            The RFC2911 IPP {@code media} keyword.
     */
    public void setMediaOption(final String media) {
        this.mediaOption = media;
    }

    /**
     * @return The RFC2911 IPP {@code media-source} keyword.
     */
    public String getMediaSourceOption() {
        return mediaSourceOption;
    }

    /**
     * @param mediaSourceOption
     *            The RFC2911 IPP {@code media-source} keyword.
     */
    public void setMediaSourceOption(String mediaSourceOption) {
        this.mediaSourceOption = mediaSourceOption;
    }

    /**
     * @return The {@link Sheet}.
     */
    public Sheet getSheet() {
        return sheet;
    }

    /**
     * @param sheet
     *            The {@link Sheet}.
     */
    public void setSheet(final Sheet sheet) {
        this.sheet = sheet;
    }

    /**
     *
     * @return {@code true} when Job Sheet is enabled.
     */
    public boolean isEnabled() {
        return !this.sheet.equals(Sheet.NONE);
    }
}
