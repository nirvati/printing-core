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
package org.savapage.core.jpa.xml;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.PrintOutV01;

/**
 * LPR Print Output Document.
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = PrintOutV01.TABLE_NAME)
public class XPrintOutV01 extends XEntityVersion {

    @Id
    @Column(name = "print_out_id")
    private Long id;

    @Column(name = "printer_id", nullable = false)
    private Long printer;

    @Column(name = "print_mode", length = 8, nullable = false,
            insertable = true, updatable = true)
    private String printMode;

    @Column(name = "paper_size", length = 20, nullable = true,
            insertable = true, updatable = true)
    private String paperSize;

    @Column(name = "paper_height_mm", nullable = true, insertable = true,
            updatable = true)
    private Integer paperHeight;

    @Column(name = "paper_width_mm", nullable = true, insertable = true,
            updatable = true)
    private Integer paperWidth;

    @Column(name = "copies", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfCopies;

    @Column(name = "duplex", nullable = false, insertable = true,
            updatable = true)
    private Boolean duplex;

    @Column(name = "grayscale", nullable = false, insertable = true,
            updatable = true)
    private Boolean grayscale;

    @Column(name = "total_sheets", nullable = false, insertable = true,
            updatable = true)
    private Integer numberOfSheets;

    @Column(name = "total_esu", nullable = false, insertable = true,
            updatable = true)
    private Long numberOfEsu;

    @Column(name = "collate_copies", nullable = true, insertable = true,
            updatable = true)
    private Boolean collateCopies;

    @Column(name = "reverse_pages", nullable = false, insertable = true,
            updatable = true)
    private Boolean reversePages;

    @Column(name = "ipp_options", length = 2000, nullable = true,
            insertable = true, updatable = true)
    private String ippOptions;

    @Column(name = "cups_job_id", nullable = false, insertable = true,
            updatable = true)
    private Integer cupsJobId;

    @Column(name = "cups_number_up", length = 1, nullable = false,
            insertable = true, updatable = true)
    private String cupsNumberUp;

    @Column(name = "cups_page_set", length = 8, nullable = false,
            insertable = true, updatable = true)
    private String cupsPageSet;

    @Column(name = "cups_job_sheets", length = 128, nullable = false,
            insertable = true, updatable = true)
    private String cupsJobSheets;

    @Column(name = "cups_job_state", nullable = false, insertable = true,
            updatable = true)
    private Integer cupsJobState;

    @Column(name = "cups_creation_time", nullable = false, insertable = true,
            updatable = true)
    private Integer cupsCreationTime;

    @Column(name = "cups_completed_time", nullable = true, insertable = true,
            updatable = true)
    private Integer cupsCompletedTime;

    /**
     * for future use
     */
    @Column(name = "color_pages_estimated", nullable = false)
    private Boolean colorPagesEstimated = true;

    /**
     * for future use
     */
    @Column(name = "color_pages_total", nullable = false)
    private Integer colorPagesTotal = 0;

    @Override
    public final String xmlName() {
        return "PrintOut";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrintMode() {
        return printMode;
    }

    public void setPrintMode(String printMode) {
        this.printMode = printMode;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public Integer getPaperHeight() {
        return paperHeight;
    }

    public void setPaperHeight(Integer paperHeight) {
        this.paperHeight = paperHeight;
    }

    public Integer getPaperWidth() {
        return paperWidth;
    }

    public void setPaperWidth(Integer paperWidth) {
        this.paperWidth = paperWidth;
    }

    public Integer getNumberOfCopies() {
        return numberOfCopies;
    }

    public void setNumberOfCopies(Integer numberOfCopies) {
        this.numberOfCopies = numberOfCopies;
    }

    public Boolean getDuplex() {
        return duplex;
    }

    public void setDuplex(Boolean duplex) {
        this.duplex = duplex;
    }

    public Boolean getGrayscale() {
        return grayscale;
    }

    public void setGrayscale(Boolean grayscale) {
        this.grayscale = grayscale;
    }

    public Integer getNumberOfSheets() {
        return numberOfSheets;
    }

    public void setNumberOfSheets(Integer numberOfSheets) {
        this.numberOfSheets = numberOfSheets;
    }

    public Long getNumberOfEsu() {
        return numberOfEsu;
    }

    public void setNumberOfEsu(Long numberOfEsu) {
        this.numberOfEsu = numberOfEsu;
    }

    public Boolean getCollateCopies() {
        return collateCopies;
    }

    public void setCollateCopies(Boolean collateCopies) {
        this.collateCopies = collateCopies;
    }

    public Boolean getReversePages() {
        return reversePages;
    }

    public void setReversePages(Boolean reversePages) {
        this.reversePages = reversePages;
    }

    public String getIppOptions() {
        return ippOptions;
    }

    public void setIppOptions(String ippOptions) {
        this.ippOptions = ippOptions;
    }

    public Integer getCupsJobId() {
        return cupsJobId;
    }

    public void setCupsJobId(Integer cupsJobId) {
        this.cupsJobId = cupsJobId;
    }

    public String getCupsNumberUp() {
        return cupsNumberUp;
    }

    public void setCupsNumberUp(String cupsNumberUp) {
        this.cupsNumberUp = cupsNumberUp;
    }

    public String getCupsPageSet() {
        return cupsPageSet;
    }

    public void setCupsPageSet(String cupsPageSet) {
        this.cupsPageSet = cupsPageSet;
    }

    public String getCupsJobSheets() {
        return cupsJobSheets;
    }

    public void setCupsJobSheets(String cupsJobSheets) {
        this.cupsJobSheets = cupsJobSheets;
    }

    public Integer getCupsJobState() {
        return cupsJobState;
    }

    public void setCupsJobState(Integer cupsJobState) {
        this.cupsJobState = cupsJobState;
    }

    public Integer getCupsCreationTime() {
        return cupsCreationTime;
    }

    public void setCupsCreationTime(Integer cupsCreationTime) {
        this.cupsCreationTime = cupsCreationTime;
    }

    public Integer getCupsCompletedTime() {
        return cupsCompletedTime;
    }

    public void setCupsCompletedTime(Integer cupsCompletedTime) {
        this.cupsCompletedTime = cupsCompletedTime;
    }

    public Long getPrinter() {
        return printer;
    }

    public void setPrinter(Long printer) {
        this.printer = printer;
    }

    public Boolean getColorPagesEstimated() {
        return colorPagesEstimated;
    }

    public void setColorPagesEstimated(Boolean colorPagesEstimated) {
        this.colorPagesEstimated = colorPagesEstimated;
    }

    public Integer getColorPagesTotal() {
        return colorPagesTotal;
    }

    public void setColorPagesTotal(Integer colorPagesTotal) {
        this.colorPagesTotal = colorPagesTotal;
    }

}
