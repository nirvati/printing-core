/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.ipp.attribute.IppAttrCollection;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.attribute.IppAttrValue;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.jpa.Printer;
import org.savapage.core.json.JsonPrinterDetail;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppLogger {

    /**
     *
     */
    private static final String CRLF = "\r\n";

    /**
     *
     */
    private static final String INDENT_UNIT = "    ";

    /**
    *
    */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * Produces log string for the UI and IPP properties of a {@link Printer}.
     *
     * @param printer
     *            The {@link Printer}.
     * @param locale
     *            The locale used for the UI texts.
     * @return The log string.
     */
    public static String logIppPrinterOpt(final Printer printer,
            final Locale locale) {

        /*
         * INVARIANT: CUPS printer details must exist.
         */
        final String printerName = printer.getPrinterName();

        final JsonPrinterDetail jsonPrinter =
                PROXY_PRINT_SERVICE.getPrinterDetailCopy(printerName);

        if (jsonPrinter == null) {
            throw new SpException(
                    "No details found for printer [" + printerName + "]");
        }

        /*
         * Collect printer options.
         */
        PROXY_PRINT_SERVICE.localize(locale, jsonPrinter);

        jsonPrinter.getName();

        final Writer writer = new StringWriter();

        try {

            writer.append(CRLF);
            writer.append("CUPS    : ")
                    .append(PROXY_PRINT_SERVICE.getCupsVersion()).append(CRLF);
            writer.append("Printer : ").append(printerName).append(CRLF);
            writer.append("URI     : ")
                    .append(jsonPrinter.getPrinterUri().toString()).append(CRLF)
                    .append(CRLF);

            writer.append("+--------------------------------------+")
                    .append(CRLF);
            writer.append("| UI Options                           |")
                    .append(CRLF);
            writer.append("+--------------------------------------+");

            for (final JsonProxyPrinterOptGroup group : jsonPrinter
                    .getGroups()) {

                writer.append(CRLF).append(CRLF).append("Group: ")
                        .append(group.getGroupId().toString()).append(" [")
                        .append(group.getUiText()).append("]");

                for (final JsonProxyPrinterOpt opt : group.getOptions()) {

                    writer.append(CRLF).append(CRLF).append("    ")
                            .append(opt.getKeyword());

                    if (StringUtils.isNotBlank(opt.getKeywordPpd())) {
                        writer.append(" {").append(opt.getKeywordPpd())
                                .append("}");
                    }

                    writer.append(" [").append(opt.getUiText()).append("]")
                            .append(CRLF);

                    for (final JsonProxyPrinterOptChoice choice : opt
                            .getChoices()) {

                        writer.append(CRLF);

                        if (choice.getChoice().equals(opt.getDefchoiceIpp())) {
                            writer.append("       *");
                        } else {
                            writer.append("        ");
                        }

                        writer.append(choice.getChoice());

                        if (StringUtils.isNotBlank(choice.getChoicePpd())) {
                            writer.append(" {").append(choice.getChoicePpd())
                                    .append("}");
                        }

                        writer.append(" [").append(choice.getUiText())
                                .append("]");
                    }
                }
            }

            logIppPrinterAttr(jsonPrinter, writer);

            writer.append(CRLF).append(CRLF).append("** end-of-report **")
                    .append(CRLF);

        } catch (IOException | IppConnectException e) {
            throw new SpException(e.getMessage(), e);
        }

        return writer.toString();
    }

    /**
     *
     * @param printerName
     * @param writer
     * @param crLf
     * @throws IOException
     * @throws IppConnectException
     */
    private static void logIppPrinterAttr(final JsonPrinterDetail jsonPrinter,
            final Writer writer) throws IOException, IppConnectException {

        final String printerName = jsonPrinter.getName();

        writer.append(CRLF).append(CRLF);

        writer.append("+--------------------------------------+").append(CRLF);
        writer.append("| IPP Attributes                       |").append(CRLF);
        writer.append("+--------------------------------------+");

        for (final IppAttrGroup group : PROXY_PRINT_SERVICE
                .getIppPrinterAttr(printerName, jsonPrinter.getPrinterUri())) {

            writer.append(CRLF).append(CRLF).append("Group [")
                    .append(group.getDelimiterTag().toString()).append("]");

            for (final IppAttrValue attr : group.getAttributes()) {

                writer.append(CRLF).append(CRLF).append(INDENT_UNIT)
                        .append(attr.getAttribute().getKeyword())
                        .append(" [").append(attr.getAttribute().getSyntax()
                                .getClass().getSimpleName())
                        .append("]").append(CRLF);

                for (final String value : attr.getValues()) {
                    writer.append(CRLF).append(INDENT_UNIT).append(INDENT_UNIT)
                            .append(value);
                }
            }

            for (final IppAttrCollection collection : group.getCollections()) {
                logIppAttrCollection(1, collection, writer);
            }

        }
    }

    /**
     *
     * @param nIndent
     * @param collection
     * @param writer
     * @throws IOException
     */
    private static void logIppAttrCollection(final int nIndent,
            final IppAttrCollection collection, final Writer writer)
            throws IOException {

        final String indent = StringUtils.repeat(INDENT_UNIT, nIndent);

        writer.append(CRLF).append(CRLF).append(indent).append("Collection [")
                .append(collection.getKeyword()).append("]");

        for (final IppAttrValue attr : collection.getAttributes()) {

            writer.append(CRLF).append(CRLF).append(indent).append(INDENT_UNIT)
                    .append(attr.getAttribute().getKeyword())
                    .append(" [").append(attr.getAttribute().getSyntax()
                            .getClass().getSimpleName())
                    .append("]").append(CRLF);

            for (final String value : attr.getValues()) {
                writer.append(CRLF).append(indent).append(INDENT_UNIT)
                        .append(INDENT_UNIT).append(value);
            }

        }

        for (final IppAttrCollection nestedCollection : collection
                .getCollections()) {
            logIppAttrCollection(nIndent + 1, nestedCollection, writer);
        }

    }

}
