/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.jpa.schema;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * CUPS Print Output Document.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PrintOutV01.TABLE_NAME,
        indexes = { //
                @Index(name = "ix_print_out_1", columnList = "cups_job_id"),
                @Index(name = "ix_print_out_2", columnList = "cups_job_state"),
                @Index(name = "ix_print_out_3", columnList = "printer_id") })
public class PrintOutV01 implements SchemaEntityVersion {

    public static final String TABLE_NAME = "tbl_print_out";

    @Id
    @Column(name = "print_out_id")
    @TableGenerator(name = "printOutPropGen", table = SequenceV01.TABLE_NAME,
            pkColumnName = "SEQUENCE_NAME",
            valueColumnName = "SEQUENCE_NEXT_VALUE", pkColumnValue = TABLE_NAME,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE,
            generator = "printOutPropGen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "printer_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PRINT_OUT_TO_PRINTER"))
    private PrinterV01 printer;

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

    @OneToOne(mappedBy = "printOut", cascade = { CascadeType.ALL },
            fetch = FetchType.EAGER, optional = false)
    private DocOutV01 docOut;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PrinterV01 getPrinter() {
        return printer;
    }

    public void setPrinter(PrinterV01 printer) {
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

    public DocOutV01 getDocOut() {
        return docOut;
    }

    public void setDocOut(DocOutV01 docOut) {
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
