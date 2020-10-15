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
package org.savapage.core.services.helpers.feed;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.services.ServiceContext;
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
public final class AdminAtomFeedWriter extends AtomFeedWriter {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminAtomFeedWriter.class);

    private int iEntry = 0;

    private final URI linkSelf;

    private final List<Path> feedEntryFiles;

    private final List<Path> filesToDelete = new ArrayList<>();

    /** */
    public static final String FILENAME_EXT_XHTML = "xhtml";

    /**
     *
     * @param requestURI
     *            The requester URI.
     * @param ostr
     *            OutputStream to write to.
     * @param feedEntryFiles
     *            List of JSON files.
     * @throws FeedException
     *             When error.
     */
    public AdminAtomFeedWriter(final URI requestURI, final OutputStream ostr,
            final List<Path> feedEntryFiles) throws FeedException {
        super(ostr);
        this.feedEntryFiles = feedEntryFiles;
        this.linkSelf = requestURI;
    }

    @Override
    protected UUID getFeedUuid() {
        return UUID.fromString(ConfigManager.instance()
                .getConfigValue(Key.FEED_ATOM_ADMIN_UUID));
    }

    @Override
    protected String getFeedTitle() {
        return CommunityDictEnum.SAVAPAGE.getWord();
    }

    @Override
    protected Date getFeedUpdated() {
        return new Date(); // TODO
    }

    @Override
    protected String getFeedAuthorName() {
        return ServiceContext.getActor();
    }

    @Override
    protected String getFeedAuthorEmail() {
        return null;
    }

    @Override
    protected URI getFeedAuthorUri() {
        return null;
    }

    @Override
    protected URI getFeedLinkSelf() {
        return this.linkSelf;
    }

    @Override
    protected String getFeedGenerator() {
        return String.format("%s %s v%s", CommunityDictEnum.SAVAPAGE.getWord(),
                CommunityDictEnum.SAVAPAGE_SLOGAN.getWord(),
                ConfigManager.getAppVersion());
    }

    @Override
    protected FeedEntryDto getFeedEntry(final StringBuilder xhtml) {

        FeedEntryDto dto = null;

        while (dto == null) {

            if (this.iEntry >= this.feedEntryFiles.size()) {
                return dto;
            }

            final Path file = this.feedEntryFiles.get(iEntry);
            this.iEntry++;

            final Path parent = file.getParent();

            if (parent == null) {
                throw new IllegalStateException();
            }

            // Find sibling file.
            final Path pathXhtml = Paths.get(parent.toString(),
                    String.format("%s.%s",
                            FilenameUtils.getBaseName(file.toString()),
                            FILENAME_EXT_XHTML));

            try {

                dto = JsonHelper.read(FeedEntryDto.class, file.toFile());

                final File fileXhtml = pathXhtml.toFile();

                if (fileXhtml.exists()) {
                    xhtml.append(FileUtils.readFileToString(fileXhtml,
                            Charset.forName("UTF-8")));
                }

            } catch (IOException e) {
                /*
                 * There has been a change in layout of the JSON file...
                 */
                this.filesToDelete.add(file);
                this.filesToDelete.add(pathXhtml);

                dto = null;
            }
        }
        return dto;
    }

    @Override
    protected void onStart() {
        this.iEntry = 0;
    }

    @Override
    protected void onEnd() {
        for (final Path path : this.filesToDelete) {
            path.toFile().delete();
            LOGGER.warn("Atom Feed JSON error: deleted [{}]", path.toString());
        }
    }

}
