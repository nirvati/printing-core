/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.ipp.IppJobStateEnum;

/**
 * Print Output Document.
 * <p>
 * CUPS Print jobs are identified by a locally-unique job ID number from 1 to
 * (2^31 - 1).
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
@Entity
@Table(name = PrintOut.TABLE_NAME)
public class PrintOut extends org.savapage.core.jpa.Entity {

    /**
     *
     */
    public static final String TABLE_NAME = "tbl_print_out";

    @Id
    @Column(name = "print_out_id")
    @TableGenerator(name = "printOutPropGen", table = Sequence.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE", pkColumnValue = TABLE_NAME,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "printOutPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "printer_id", nullable = false)
    private Printer printer;

    /**
     * See {@link PrintModeEnum}.
     */
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

    //
    @Column(name = "collate_copies", nullable = true, insertable = true,
            updatable = true)
    private Boolean collateCopies;

    @Column(name = "reverse_pages", nullable = false, insertable = true,
            updatable = true)
    private Boolean reversePages;

    @Column(name = "ipp_options", length = 2000, nullable = true,
            insertable = true, updatable = true)
    private String ippOptions;

    /**
     * CUPS Print jobs are identified by a locally-unique job ID number from 1
     * to (2^31 - 1).
     */
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

    /**
     * {@link IppJobStateEnum} as integer.
     */
    @Column(name = "cups_job_state", nullable = false, insertable = true,
            updatable = true)
    private Integer cupsJobState;

    /**
     * Unix epoch time (seconds).
     */
    @Column(name = "cups_creation_time", nullable = false, insertable = true,
            updatable = true)
    private Integer cupsCreationTime;

    /**
     * Unix epoch time (seconds).
     */
    @Column(name = "cups_completed_time", nullable = true, insertable = true,
            updatable = true)
    private Integer cupsCompletedTime;

    @OneToOne(mappedBy = "printOut", cascade = { CascadeType.ALL },
            fetch = FetchType.EAGER, optional = false)
    private DocOut docOut;

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

    /**
     *
     * @return
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
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

    public DocOut getDocOut() {
        return docOut;
    }

    public void setDocOut(DocOut docOut) {
        this.docOut = docOut;
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

    /**
     * @return {@link IppJobStateEnum} as integer.
     */
    public Integer getCupsJobState() {
        return cupsJobState;
    }

    /**
     * @param cupsJobState
     *            {@link IppJobStateEnum} as integer.
     */
    public void setCupsJobState(Integer cupsJobState) {
        this.cupsJobState = cupsJobState;
    }

    /**
     *
     * @return Unix epoch time (seconds).
     */
    public Integer getCupsCreationTime() {
        return cupsCreationTime;
    }

    /**
     *
     * @param cupsCreationTime
     *            Unix epoch time (seconds).
     */
    public void setCupsCreationTime(Integer cupsCreationTime) {
        this.cupsCreationTime = cupsCreationTime;
    }

    /**
     *
     * @return Unix epoch time (seconds).
     */
    public Integer getCupsCompletedTime() {
        return cupsCompletedTime;
    }

    /**
     *
     * @param cupsCompletedTime
     *            Unix epoch time (seconds).
     */
    public void setCupsCompletedTime(Integer cupsCompletedTime) {
        this.cupsCompletedTime = cupsCompletedTime;
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
