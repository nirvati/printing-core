/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.reports.impl;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.fonts.FontLocation;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.reports.AbstractJrDesign;
import org.savapage.core.util.MessagesBundleProp;

/**
 * Creator of a User List Report.
 *
 * @author Datraverse B.V.
 * @since 0.9.9
 */
public abstract class ReportCreator {

    /**
     * The requesting user.
     */
    private final String requestingUser;

    /**
     * The input data for the report.
     */
    private final String inputData;

    /**
     * {@link Locale} of the report.
     */
    private Locale locale;

    /**
     * Constructor.
     *
     * @param requestingUser
     *            The requesting user.
     * @param inputData
     *            The input data for the report.
     * @param locale
     *            {@link Locale} of the report.
     */
    protected ReportCreator(final String requestingUser,
            final String inputData, final Locale locale) {
        this.requestingUser = requestingUser;
        this.inputData = inputData;
        this.locale = locale;
    }

    /**
     * Creates a PDF report.
     *
     * @param pdfFile
     *            The PDF {@link File} to create.
     * @throws JRException
     *             When report error.
     */
    public final void create(final File pdfFile) throws JRException {

        /*
         * Find the best match resource bundle for the report.
         */
        final ResourceBundle resourceBundle =
                MessagesBundleProp.getResourceBundle(this.getClass()
                        .getPackage(), ReportCreator.class.getSimpleName(),
                        this.locale);
        /*
         * INVARIANT: The locale of the report must be consistent.
         *
         * Since, the localized text is composed from different resources, we
         * must make sure we use a locale that is consistently supported.
         *
         * Since no exact ResourceBundle match might be found, we use the locale
         * of the ResourceBundle for the whole report.
         */
        this.locale = resourceBundle.getLocale();

        //
        final InputStream istr =
                AbstractJrDesign.getJrxmlAsStream(this.getReportId());

        final JasperReport jasperReport =
                JasperCompileManager.compileReport(istr);

        final InternalFontFamilyEnum internalFont =
                ConfigManager
                        .getConfigFontFamily(Key.REPORTS_PDF_INTERNAL_FONT_FAMILY);

        if (FontLocation.isFontPresent(internalFont)) {
            jasperReport.getDefaultStyle()
                    .setFontName(internalFont.getJrName());
        }

        final Map<String, Object> reportParameters = new HashMap<>();

        reportParameters.put("REPORT_LOCALE", this.locale);
        reportParameters.put("REPORT_RESOURCE_BUNDLE", resourceBundle);

        reportParameters.put("SP_APP_VERSION",
                ConfigManager.getAppNameVersion());
        reportParameters.put("SP_REPORT_ACTOR", requestingUser);
        reportParameters.put("SP_REPORT_IMAGE",
                AbstractJrDesign.getHeaderImage());

        final JRDataSource dataSource =
                this.onCreateDataSource(this.inputData, this.locale,
                        reportParameters);

        final JasperPrint jasperPrint =
                JasperFillManager.fillReport(jasperReport, reportParameters,
                        dataSource);

        JasperExportManager.exportReportToPdfFile(jasperPrint,
                pdfFile.getAbsolutePath());
    }

    /**
     * Gets the identification of the Jasper Report XML *.jrxml file.
     *
     * @return The report ID.
     */
    protected abstract String getReportId();

    /**
     * The callback that creates a {@link JRDataSource} and (optionally) sets
     * custom report parameters.
     *
     * @param input
     *            The input data as used in the constructor of this instance.
     * @param reportlocale
     *            {@link Locale} of the report.
     * @param reportParameters
     *            The report parameter map.
     * @return The {@link JRDataSource}.
     */
    protected abstract JRDataSource onCreateDataSource(final String input,
            Locale reportlocale, Map<String, Object> reportParameters);

}
