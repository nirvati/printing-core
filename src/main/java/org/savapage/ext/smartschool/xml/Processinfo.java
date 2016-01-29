/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.ext.smartschool.xml;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class Processinfo {

    /**
     * Possible values: "a4", ...
     */
    private String papersize;

    /**
     * Possible values: "grayscale", ...
     */
    private String rendermode;

    /**
     * Boolean: on/off
     */
    private String duplex;

    /**
     * Boolean: on/off
     */
    private String staple;

    /**
     * Boolean: on/off
     */
    private String punch;

    /**
     * Boolean: on/off
     */
    private String frontcover;
    /**
     * Boolean: on/off
     */
    private String backcover;

    /**
     * Boolean: on/off
     */
    private String confidential;

    public String getPapersize() {
        return papersize;
    }

    public void setPapersize(String papersize) {
        this.papersize = papersize;
    }

    public String getRendermode() {
        return rendermode;
    }

    public void setRendermode(String rendermode) {
        this.rendermode = rendermode;
    }

    public String getDuplex() {
        return duplex;
    }

    public void setDuplex(String duplex) {
        this.duplex = duplex;
    }

    public String getStaple() {
        return staple;
    }

    public void setStaple(String staple) {
        this.staple = staple;
    }

    public String getPunch() {
        return punch;
    }

    public void setPunch(String punch) {
        this.punch = punch;
    }

    public String getFrontcover() {
        return frontcover;
    }

    public void setFrontcover(String frontcover) {
        this.frontcover = frontcover;
    }

    public String getBackcover() {
        return backcover;
    }

    public void setBackcover(String backcover) {
        this.backcover = backcover;
    }

    public String getConfidential() {
        return confidential;
    }

    public void setConfidential(String confidential) {
        this.confidential = confidential;
    }

}
