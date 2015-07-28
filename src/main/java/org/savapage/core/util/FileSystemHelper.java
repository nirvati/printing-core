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
package org.savapage.core.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 *
 * @author Datraverse B.V.
 */
public final class FileSystemHelper {

    /**
     *
     */
    private FileSystemHelper() {
    }

    /**
     * Performs a file move as an ATOMIC_MOVE file operation. If the file system
     * does not support an atomic move, an exception is thrown. With an
     * ATOMIC_MOVE you can move a file into a directory and be guaranteed that
     * any process watching the directory accesses a complete file.
     *
     * @param source
     *            The source path.
     * @param target
     *            The target path.
     * @throws IOException
     *             If any IO error occurs.
     */
    public static void doAtomicFileMove(final Path source, final Path target)
            throws IOException {

        java.nio.file.Files.move(source, target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }

}
