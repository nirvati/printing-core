/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.impl;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.services.AtomFeedService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.feed.AdminAtomFeedWriter;
import org.savapage.core.util.JsonHelper;
import org.savapage.lib.feed.AtomFeedWriter;
import org.savapage.lib.feed.FeedEntryDto;
import org.savapage.lib.feed.FeedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AtomFeedServiceImpl extends AbstractService
        implements AtomFeedService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AtomFeedServiceImpl.class);

    /** */
    public static final String FEED_FILE_EXT_JSON = "json";

    /** */
    public static final String FEED_FILE_EXT_XHTML = "xhtml";

    /** */
    public static final String FEED_FILE_BASENAME = "admin";

    /** */
    public static final String FEED_FILE_JSON =
            FEED_FILE_BASENAME + "." + FEED_FILE_EXT_JSON;

    /** */
    public static final String FEED_FILE_XHTML =
            FEED_FILE_BASENAME + "." + FEED_FILE_EXT_XHTML;

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void refreshAdminFeed() throws FeedException {

        final String feedHome = ConfigManager.getAtomFeedsHome().toString();

        final Path pathXhtml = Paths.get(feedHome, FEED_FILE_XHTML);
        final Path pathJson = Paths.get(feedHome, FEED_FILE_JSON);

        final StringBuilder xhtml = new StringBuilder();
        createAdminFeedXhtml(xhtml);

        //
        final FeedEntryDto dto = new FeedEntryDto();

        dto.setUuid(UUID.randomUUID());
        dto.setTitle("Metrics");
        dto.setSummary("");
        dto.setUpdated(ServiceContext.getTransactionDate());

        try {
            // 1.
            FileUtils.writeStringToFile(pathXhtml.toFile(), xhtml.toString(),
                    Charset.forName("UTF-8"));
            // 2.
            JsonHelper.write(dto, new FileWriter(pathJson.toFile()));

        } catch (IOException e) {
            throw new FeedException(e.getMessage());
        }
    }

    /**
     *
     * @param xhtml
     */
    private static void createAdminFeedXhtml(final StringBuilder xhtml) {
        xhtml.append("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
        xhtml.append("\n<p>");
        xhtml.append("Users: ").append(userDAO().count());
        xhtml.append("</p>");
        xhtml.append("\n</div>");
    }

    @Override
    public AtomFeedWriter getAdminFeedWriter(final OutputStream ostr)
            throws FeedException {

        final List<Path> feedEntryFiles = new ArrayList<>();

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {

                final String filePath = file.toString();

                if (!FilenameUtils.getExtension(filePath)
                        .equalsIgnoreCase(FEED_FILE_EXT_JSON)) {
                    return CONTINUE;
                }

                feedEntryFiles.add(file);
                return CONTINUE;
            }
        };

        final Path feedPath = ConfigManager.getAtomFeedsHome();

        if (feedPath.toFile().exists()) {
            try {
                Files.walkFileTree(feedPath, visitor);
            } catch (IOException e) {
                throw new FeedException(e.getMessage());
            }
        } else {
            LOGGER.warn("Directory [{}] does not exist.", feedPath);
        }

        return new AdminAtomFeedWriter(ostr, feedEntryFiles);
    }
}
