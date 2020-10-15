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
package org.savapage.core.template.dto;

import java.util.Locale;

import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateAppDto implements TemplateDto {

    private String name;
    private String nameVersion;
    private String nameVersionBuild;

    private String slogan;

    private String savapageDotOrg;
    private String wwwSavaPageDotOrgUrl;
    private String wwwSavaPageDotOrg;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameVersion() {
        return nameVersion;
    }

    public void setNameVersion(String nameVersion) {
        this.nameVersion = nameVersion;
    }

    public String getNameVersionBuild() {
        return nameVersionBuild;
    }

    public void setNameVersionBuild(String nameVersionBuild) {
        this.nameVersionBuild = nameVersionBuild;
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan;
    }

    public String getSavapageDotOrg() {
        return savapageDotOrg;
    }

    public void setSavapageDotOrg(String savapageDotOrg) {
        this.savapageDotOrg = savapageDotOrg;
    }

    public String getWwwSavaPageDotOrgUrl() {
        return wwwSavaPageDotOrgUrl;
    }

    public void setWwwSavaPageDotOrgUrl(String wwwSavaPageDotOrgUrl) {
        this.wwwSavaPageDotOrgUrl = wwwSavaPageDotOrgUrl;
    }

    public String getWwwSavaPageDotOrg() {
        return wwwSavaPageDotOrg;
    }

    public void setWwwSavaPageDotOrg(String wwwSavaPageDotOrg) {
        this.wwwSavaPageDotOrg = wwwSavaPageDotOrg;
    }

    /**
     *
     * @param locale
     * @return
     */
    public static TemplateAppDto create(final Locale locale) {

        final TemplateAppDto dto = new TemplateAppDto();

        dto.name = CommunityDictEnum.SAVAPAGE.getWord(locale);
        dto.nameVersion = ConfigManager.getAppNameVersion();
        dto.nameVersionBuild = ConfigManager.getAppNameVersionBuild();

        dto.wwwSavaPageDotOrgUrl =
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord(locale);
        dto.savapageDotOrg = CommunityDictEnum.SAVAPAGE_DOT_ORG.getWord(locale);
        dto.wwwSavaPageDotOrg =
                CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG.getWord(locale);

        dto.slogan = CommunityDictEnum.SAVAPAGE_SLOGAN.getWord(locale);

        return dto;
    }

}
