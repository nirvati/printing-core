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
package org.savapage.core.dto;

import java.util.Map;

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
     * The localized media-type choice. {@code null} when no media-type chosen.
     */
    private JsonProxyPrinterOptChoice mediaTypeOptChoice;

    /**
     * Map with key (media-source choice) and value (media).
     */
    private Map<String, String> mediaSourceMediaMap;

    /**
     * The {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE} option of the
     * printer.
     */
    private JsonProxyPrinterOpt mediaSourceOpt;

    /**
     * The default media-source choice. When {@code null}, no default is
     * available.
     */
    private JsonProxyPrinterOptChoice mediaSourceOptChoice;

    /**
     * The default media-source choice for Job Sheet. When {@code null}, no
     * default is available.
     */
    private JsonProxyPrinterOptChoice mediaSourceJobSheetOptChoice;

    /**
     * The {@link IppDictJobTemplateAttr#ATTR_OUTPUT_BIN} option of the printer.
     * {@code null} when not present.
     */
    private JsonProxyPrinterOpt outputBinOpt;

    /**
     * The default output-bin choice. When {@code null}, no default is
     * available.
     */
    private JsonProxyPrinterOptChoice outputBinOptChoice;

    /**
     * The
     * {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET}
     * option of the printer. {@code null} when not present.
     */
    private JsonProxyPrinterOpt jogOffsetOpt;

    /**
     * The default jog-offset choice. When {@code null}, no default is
     * available.
     */
    private JsonProxyPrinterOptChoice jogOffsetOptChoice;

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
     * @return The localized media-type choice. {@code null} when no media-type
     *         chosen.
     */
    public JsonProxyPrinterOptChoice getMediaTypeOptChoice() {
        return mediaTypeOptChoice;
    }

    /**
     * @param optChoice
     *            The localized media-type choice. {@code null} when no
     *            media-type chosen.
     */
    public void
            setMediaTypeOptChoice(final JsonProxyPrinterOptChoice optChoice) {
        this.mediaTypeOptChoice = optChoice;
    }

    /**
     * @return Map with key (media-source choice) and value (media).
     */
    public Map<String, String> getMediaSourceMediaMap() {
        return mediaSourceMediaMap;
    }

    /**
     * @param map
     *            Map with key (media-source choice) and value (media).
     */
    public void setMediaSourceMediaMap(final Map<String, String> map) {
        this.mediaSourceMediaMap = map;
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

    /**
     * @return The default media-source choice for Job Sheet. When {@code null},
     *         no default is available.
     */
    public JsonProxyPrinterOptChoice getMediaSourceJobSheetOptChoice() {
        return mediaSourceJobSheetOptChoice;
    }

    /**
     * @param mediaSourceJobSheetOptChoice
     *            The default media-source choice for Job Sheet. When
     *            {@code null}, no default is available.
     */
    public void setMediaSourceJobSheetOptChoice(
            JsonProxyPrinterOptChoice mediaSourceJobSheetOptChoice) {
        this.mediaSourceJobSheetOptChoice = mediaSourceJobSheetOptChoice;
    }

    /**
     * @return The {@link IppDictJobTemplateAttr#ATTR_OUTPUT_BIN} option of the
     *         printer. {@code null} when not present.
     */
    public JsonProxyPrinterOpt getOutputBinOpt() {
        return outputBinOpt;
    }

    /**
     * @param outputBinOpt
     *            The {@link IppDictJobTemplateAttr#ATTR_OUTPUT_BIN} option of
     *            the printer. {@code null} when not present.
     */
    public void setOutputBinOpt(JsonProxyPrinterOpt outputBinOpt) {
        this.outputBinOpt = outputBinOpt;
    }

    /**
     * @return The default output-bin choice. When {@code null}, no default is
     *         available.
     */
    public JsonProxyPrinterOptChoice getOutputBinOptChoice() {
        return outputBinOptChoice;
    }

    /**
     * @param outputBinOptChoice
     *            The default output-bin choice. When {@code null}, no default
     *            is available.
     */
    public void setOutputBinOptChoice(
            JsonProxyPrinterOptChoice outputBinOptChoice) {
        this.outputBinOptChoice = outputBinOptChoice;
    }

    /**
     * @return The
     *         {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET}
     *         option of the printer. {@code null} when not present.
     *
     */
    public JsonProxyPrinterOpt getJogOffsetOpt() {
        return jogOffsetOpt;
    }

    /**
     * @param jogOffsetOpt
     *            The
     *            {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_FINISHINGS_JOG_OFFSET}
     *            option of the printer. {@code null} when not present.
     */
    public void setJogOffsetOpt(JsonProxyPrinterOpt jogOffsetOpt) {
        this.jogOffsetOpt = jogOffsetOpt;
    }

    /**
     * @return The default jog-offset choice. When {@code null}, no default is
     *         available.
     *
     */
    public JsonProxyPrinterOptChoice getJogOffsetOptChoice() {
        return jogOffsetOptChoice;
    }

    /**
     * @param choice
     *            The default jog-offset choice. When {@code null}, no default
     *            is available.
     */
    public void setJogOffsetOptChoice(JsonProxyPrinterOptChoice choice) {
        this.jogOffsetOptChoice = choice;
    }

}
