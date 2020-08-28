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
package org.savapage.core.reports.impl;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.fonts.FontLocation;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.reports.AbstractJrDesign;
import org.savapage.core.reports.JrExportFileExtEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.MessagesBundleProp;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

/**
 * Report Creator.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ReportCreator {

    /**
     * The requesting user.
     */
    private final String requestingUser;

    /**
     * {@code true} if {@link #requestingUser} is an administrator.
     */
    private final boolean requestingUserAdmin;

    /**
     * The input data for the report.
     */
    private final String inputData;

    /**
     * {@link Locale} of the report.
     */
    private Locale locale;

    /** */
    protected static final String REPORT_PARM_DATA_SELECTION =
            "SP_DATA_SELECTION";

    /**
     * Constructor.
     *
     * @param user
     *            The requesting user.
     * @param isAdmin
     *            {@code true} if requesting user is an administrator.
     * @param inputData
     *            The input data for the report.
     * @param locale
     *            {@link Locale} of the report.
     */
    protected ReportCreator(final String user, final boolean isAdmin,
            final String inputData, final Locale locale) {
        this.requestingUser = user;
        this.requestingUserAdmin = isAdmin;
        this.inputData = inputData;
        this.locale = locale;
    }

    /**
     * @return {@code true} if {@link #requestingUser} is an administrator.
     */
    public final boolean isRequestingUserAdmin() {
        return this.requestingUserAdmin;
    }

    /**
     * Checks if requesting user is authenticated for generating a report of a
     * requested user (or all users).
     *
     * @param requestedUserKey
     *            Database key of the requested user. Is {@code null} if all
     *            users are requested.
     * @throws SpException
     *             When authentication fails.
     */
    protected final void onUserAuthentication(final Long requestedUserKey) {

        /*
         * Administrator can generate all reports.
         */
        if (this.isRequestingUserAdmin()) {
            return;
        }

        /*
         * INVARIANT: A non-admin user can NOT generate a report of all users.
         */
        if (requestedUserKey == null) {
            this.throwAuthenticationFailed();
        }

        /*
         * INVARIANT: A non-admin user can NOT generate a report of another
         * user.
         */
        final User requestedUser = ServiceContext.getDaoContext().getUserDao()
                .findById(requestedUserKey);

        if (requestedUser == null
                || !requestedUser.getUserId().equals(this.requestingUser)) {
            throwAuthenticationFailed();
        }
    }

    /**
     * Throws an exception.
     *
     * @throws SpException
     */
    protected final void throwAuthenticationFailed() {
        throw new SpException("Authentication failed.");
    }

    /**
     * Creates a report.
     *
     * @param targetFile
     *            The PDF {@link File} to create.
     * @param fileExt
     *            The export file type to create.
     * @throws JRException
     *             When report error.
     */
    public final void create(final File targetFile,
            final JrExportFileExtEnum fileExt) throws JRException {

        /*
         * Find the best match resource bundle for the report.
         */
        final ResourceBundle resourceBundle = MessagesBundleProp
                .getResourceBundle(this.getClass().getPackage(),
                        ReportCreator.class.getSimpleName(), this.locale);
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

        final InternalFontFamilyEnum internalFont = ConfigManager
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

        if (fileExt != JrExportFileExtEnum.PDF) {
            reportParameters.put(JRParameter.IS_IGNORE_PAGINATION,
                    Boolean.TRUE);
        }

        final JRDataSource dataSource = this.onCreateDataSource(this.inputData,
                this.locale, reportParameters);

        final JasperPrint jasperPrint = JasperFillManager
                .fillReport(jasperReport, reportParameters, dataSource);

        if (fileExt == JrExportFileExtEnum.CSV) {

            final SimpleCsvExporterConfiguration config =
                    new SimpleCsvExporterConfiguration();

            config.setWriteBOM(Boolean.TRUE);
            config.setRecordDelimiter("\r\n");

            final JRCsvExporter exporterCSV = new JRCsvExporter();
            exporterCSV.setConfiguration(config);
            exporterCSV.setExporterOutput(
                    new SimpleWriterExporterOutput(targetFile));
            exporterCSV.setExporterInput(new SimpleExporterInput(jasperPrint));

            exporterCSV.exportReport();

        } else if (fileExt == JrExportFileExtEnum.PDF) {

            JasperExportManager.exportReportToPdfFile(jasperPrint,
                    targetFile.getAbsolutePath());

        } else {
            throw new IllegalArgumentException(String
                    .format("No handler for \"%s\".", fileExt.toString()));
        }
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
     *            The report parameter map for setting customer parameters.
     * @return The {@link JRDataSource}.
     */
    protected abstract JRDataSource onCreateDataSource(String input,
            Locale reportlocale, Map<String, Object> reportParameters);

}
