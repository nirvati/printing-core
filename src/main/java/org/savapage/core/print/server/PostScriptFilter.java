/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.print.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class PostScriptFilter {

    public static enum Result {
        /**
         * No DRM encountered.
         */
        DRM_NO,
        /**
         * DRM encountered: resulting output is broken (invalid).
         */
        DRM_YES,
        /**
         * DRM encountered but neglected (removed).
         */
        DRM_NEGLECTED
    }

    private static final String DRM_SIGNATURE[] =
            {
                    //
                    "%ADOBeginClientInjection: DocumentSetup Start \"No Re-Distill\"",
                    "%% Removing the following eleven lines is illegal, subject to the Digital Copyright Act of 1998.",
                    "mark currentfile eexec",
                    "54dc5232e897cbaaa7584b7da7c23a6c59e7451851159cdbf40334cc2600",
                    "30036a856fabb196b3ddab71514d79106c969797b119ae4379c5ac9b7318",
                    "33471fc81a8e4b87bac59f7003cddaebea2a741c4e80818b4b136660994b",
                    "18a85d6b60e3c6b57cc0815fe834bc82704ac2caf0b6e228ce1b2218c8c7",
                    "67e87aef6db14cd38dda844c855b4e9c46d510cab8fdaa521d67cbb83ee1",
                    "af966cc79653b9aca2a5f91f908bbd3f06ecc0c940097ec77e210e6184dc",
                    "2f5777aacfc6907d43f1edb490a2a89c9af5b90ff126c0c3c5da9ae99f59",
                    "d47040be1c0336205bf3c6169b1b01cd78f922ec384cd0fcab955c0c20de",
                    "000000000000000000000000000000000000000000000000000000000000",
                    "cleartomark",
                    "%ADOEndClientInjection: DocumentSetup Start \"No Re-Distill\""
            //
            };

    /**
     * Streams the lines from the PostScript reader to the writer.
     *
     * @param reader
     *            The PostScript reader.
     * @param writer
     *            The PostScript writer.
     * @param fRespectDRM
     *            If {@code false}, any DRM signature is omitted from the
     *            stream. If {@code true}, the function immediately returns
     *            {@link EXIT_FAILURE} when a DRM signature is encountered.
     * @return The process exit code.
     * @throws IOException
     */
    public static Result process(BufferedReader reader, BufferedWriter writer,
            boolean fRespectDRM) throws IOException {

        Result ret = Result.DRM_NO;

        /*
         * Calculate the minimum signature string length to compare.
         */
        int minStrLenSig = 0;

        for (String line : DRM_SIGNATURE) {
            int len = line.length();
            if (minStrLenSig == 0 || minStrLenSig > len) {
                minStrLenSig = len;
            }
        }

        final int nSigLines = DRM_SIGNATURE.length;

        /*
         * Initial read.
         */
        int iSigLine = 0;

        String line = reader.readLine();

        int nFlushLineCounter = 0;
        final int nFlushLinesThreshold = 500;

        while (line != null) {

            int bytesRead = line.length();

            /*
             *
             */
            if (iSigLine < nSigLines && bytesRead >= minStrLenSig) {

                /*
                 * The line read does NOT have "0x0D 0x0A" or "0x0A" at the end.
                 */
                int bytesCompare = bytesRead;

                /*
                 * Do we have a DRM signature line?
                 */
                if (DRM_SIGNATURE[iSigLine].length() == bytesCompare
                        && line.equals(DRM_SIGNATURE[iSigLine])) {

                    if (fRespectDRM) {
                        /*
                         * This makes any PostScript processor like 'ps2pdf'
                         * return an error when processing this stream.
                         */
                        writer.newLine();
                        writer.write("[Error enforced BY SavaPage}"); // syntax
                                                                      // error
                        writer.newLine();
                        writer.flush(); // !!!
                        return Result.DRM_YES;
                    }

                    ret = Result.DRM_NEGLECTED;

                    if (iSigLine < 2 || iSigLine > 12) {
                        writer.write(line);
                        writer.newLine();
                    }

                    iSigLine++;

                } else {
                    writer.write(line);
                    writer.newLine();
                }

            } else {
                writer.write(line);
                writer.newLine();
            }

            nFlushLineCounter++;
            if (nFlushLineCounter > nFlushLinesThreshold) {
                writer.flush();
                nFlushLineCounter = 0;
            }

            /*
             * Read next
             */
            line = reader.readLine();
        }

        writer.flush();

        return ret;
    }

}
