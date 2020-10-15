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
package org.savapage.core.template.feed;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.savapage.core.template.TemplateAttrEnum;
import org.savapage.core.template.TemplateMixin;
import org.savapage.core.template.dto.TemplateAdminFeedDto;
import org.savapage.core.template.dto.TemplateDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AdminFeedTemplate extends TemplateMixin {

    /** */
    protected static final String RESOURCE_KEY_XHTML = "xhtml";

    /** */
    private final TemplateAdminFeedDto feed;

    /**
     *
     * @param dto
     *            The feed info.
     */
    public AdminFeedTemplate(final TemplateAdminFeedDto dto) {
        super();
        this.feed = dto;
    }

    @Override
    protected Map<String, TemplateDto> onRender(final Locale locale) {
        final Map<String, TemplateDto> map = new HashMap<>();
        map.put(TemplateAttrEnum.FEED_ADMIN.asAttr(), this.feed);
        return map;
    }

    /**
     * Renders the template.
     *
     * @param locale
     *            The {@link Locale}.
     * @return The rendered template.
     */
    public String render(final Locale locale) {

        // ResourceBundle.clearCache(); // TEST

        final ResourceBundle rb = this.getResourceBundle(locale);
        return this.render(rb, rb.getString(RESOURCE_KEY_XHTML), locale);
    }

    @Override
    protected Map<String, String> onRender(final ResourceBundle rcBundle) {
        return null;
    }

}
