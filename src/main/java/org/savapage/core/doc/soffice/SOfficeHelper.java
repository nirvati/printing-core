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
package org.savapage.core.doc.soffice;

import java.io.File;
import java.util.Map;

import org.savapage.core.SpException;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.savapage.core.system.SystemInfo;

import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.UnoRuntime;

/**
 * A helper for other SOffice classes.
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeHelper {

    /**
    *
    */
    private static volatile Boolean cachedInstallIndication = null;

    /**
     * Java System property with location of OpenOffice or LibreOffice package.
     * This can be passed as VM argument, for instance:
     * <p>
     * {@code -Dsoffice.home=/usr/lib/libreoffice}
     * </p>
     */
    private static final String SYS_PROP_OFFICE_HOME = "soffice.home";

    /**
     * The office binary.
     */
    public static final String SOFFICE_BIN = "soffice.bin";

    /**
     *
     */
    private static final String PROGRAM_SOFFICE_BIN = "program/" + SOFFICE_BIN;

    /**
     * The UNO service.
     */
    public static final String UNO_SERVICE_FRAME_DESKTOP =
            "com.sun.star.frame.Desktop";

    /**
     * Possible locations of the OpenOffice or LibreOffice package.
     */
    private static final String[] OFFICE_LOCATION_CANDIDATES = new String[] {
            "/opt/libreoffice", "/usr/lib/libreoffice", "/usr/lib/openoffice" };

    /**
     *
     */
    private SOfficeHelper() {
    }

    /**
     *
     * @return The name of the office suite.
     */
    public static String name() {
        return "LibreOffice";
    }

    /**
     * Gets the location of the OpenOffice or LibreOffice package, either from
     * the system property {@link #SYS_PROP_OFFICE_HOME}, or by probing a number
     * of candidate locations.
     *
     * @return The location of the Office package.
     */
    public static File getOfficeLocation() {

        if (System.getProperty(SYS_PROP_OFFICE_HOME) != null) {
            return new File(System.getProperty(SYS_PROP_OFFICE_HOME));
        }

        for (final String candidate : OFFICE_LOCATION_CANDIDATES) {
            if (getOfficeExecutable(new File(candidate)).isFile()) {
                return new File(candidate);
            }
        }
        return null;
    }

    /**
     *
     * @param officeHome
     *            The office location.
     * @return The office executable.
     */
    public static File getOfficeExecutable(final File officeHome) {
        return new File(officeHome, PROGRAM_SOFFICE_BIN);
    }

    /**
     * Casts a raw object to a UNO object.
     *
     * @param <T>
     *            The UNO class.
     * @param type
     *            The UNO class object.
     * @param object
     *            The raw object.
     * @return The typed UNO object.
     */

    public static <T> T unoCast(final Class<T> type, final Object object) {
        return UnoRuntime.queryInterface(type, object);
    }

    /**
     * Converts a file for URL format.
     *
     * @param file
     *            The file.
     * @return The URL string.
     */
    public static String toUrl(final File file) {

        final String path = file.toURI().getRawPath();

        final String url;

        if (path.startsWith("//")) {
            url = "file:" + path;
        } else {
            url = String.format("file://%s", path);
        }

        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Creates a UNO property.
     *
     * @param name
     *            The name.
     * @param value
     *            The value
     * @return the property.
     */
    public static PropertyValue unoProperty(final String name,
            final Object value) {

        final PropertyValue propertyValue = new PropertyValue();
        propertyValue.Name = name;
        propertyValue.Value = value;

        return propertyValue;
    }

    /**
     * Converts property map to UNO format.
     *
     * @param properties
     *            The property map.
     * @return The UNO properties.
     */
    public static PropertyValue[]
            toUnoProperties(final Map<String, Object> properties) {

        final PropertyValue[] propertyValues =
                new PropertyValue[properties.size()];

        int i = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> subProperties =
                        (Map<String, Object>) value;
                value = toUnoProperties(subProperties);
            }
            propertyValues[i++] = unoProperty(entry.getKey(), value);
        }
        return propertyValues;
    }

    /**
     * Gets the document family of an UNO document.
     *
     * @param document
     *            The UNO document.
     * @return The document family.
     * @throws SOfficeException
     *             if not found.
     */
    public static SOfficeDocFamilyEnum getDocumentFamily(
            final XComponent document) throws SOfficeException {

        final XServiceInfo serviceInfo = unoCast(XServiceInfo.class, document);

        if (serviceInfo
                .supportsService("com.sun.star.text.GenericTextDocument")) {
            /*
             * NOTE: a GenericTextDocument is either a TextDocument, a
             * WebDocument, or a GlobalDocument but this further distinction
             * doesn't seem to matter for conversions.
             */
            return SOfficeDocFamilyEnum.TEXT;
        }
        if (serviceInfo
                .supportsService("com.sun.star.sheet.SpreadsheetDocument")) {
            return SOfficeDocFamilyEnum.SPREADSHEET;
        }
        if (serviceInfo.supportsService(
                "com.sun.star.presentation.PresentationDocument")) {
            return SOfficeDocFamilyEnum.PRESENTATION;
        }
        if (serviceInfo
                .supportsService("com.sun.star.drawing.DrawingDocument")) {
            return SOfficeDocFamilyEnum.DRAWING;
        }

        throw new SOfficeException("Document of unknown family: "
                + serviceInfo.getImplementationName());
    }

    /**
     * Finds out if LibreOffice is installed using the indication from cache,
     * i.e. the result of the last {@link #getLibreOfficeVersion()} call. If the
     * cache is null {@link #getLibreOfficeVersion()} is called ad-hoc to find
     * out.
     *
     * @return {@code true} if installed.
     */
    public static boolean lazyIsInstalled() {
        if (cachedInstallIndication == null) {
            getLibreOfficeVersion();
        }
        return cachedInstallIndication;
    }

    /**
     * Retrieves the LibreOffice version from the system.
     *
     * @return The version string(s) or {@code null} when LibreOffice is not
     *         installed.
     */
    public static String getLibreOfficeVersion() {

        final String cmd = SystemInfo.Command.LIBREOFFICE.cmdLine("--version");

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {

            final int rc = exec.executeCommand();

            cachedInstallIndication = (rc == 0);

            if (!cachedInstallIndication) {
                return null;
            }
            return exec.getStandardOutput();

        } catch (Exception e) {
            throw new SpException(e);
        }
    }
}
