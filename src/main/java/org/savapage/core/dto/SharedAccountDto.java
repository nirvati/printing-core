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

import org.savapage.core.jpa.Account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Shared {@link Account} information.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class SharedAccountDto extends AbstractDto {

    public static final String CHILD_SEPARATOR_TEXT = " / ";
    public static final String CHILD_SEPARATOR_HTML = "&bull;";

    private Long id;
    private String name;
    private Long parentId;
    private String parentName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     * @return The composed parent/child name as text.
     */
    @JsonIgnore
    public String nameAsText() {
        return composeName(false);
    }

    /**
     * @return The composed parent/child name as HTML.
     */
    @JsonIgnore
    public String nameAsHtml() {
        return composeName(true);
    }

    @JsonIgnore
    private String composeName(final boolean isHtml) {

        if (this.parentId == null) {
            return this.name;
        }
        if (isHtml) {
            return String.format("%s %s %s", this.parentName,
                    CHILD_SEPARATOR_HTML, this.name);
        }
        return String.format("%s%s%s", this.parentName, CHILD_SEPARATOR_TEXT,
                this.name);
    }

}
