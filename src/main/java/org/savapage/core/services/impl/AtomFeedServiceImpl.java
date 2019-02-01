/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.SslCertInfo;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.savapage.core.jpa.Printer;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.services.AtomFeedService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.services.helpers.feed.AdminAtomFeedWriter;
import org.savapage.core.snmp.SnmpPrinterErrorStateEnum;
import org.savapage.core.snmp.SnmpPrtMarkerColorantValueEnum;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.template.dto.TemplateAdminFeedDto;
import org.savapage.core.template.dto.TemplatePrinterSnmpDto;
import org.savapage.core.template.dto.TemplateSslCertDto;
import org.savapage.core.template.feed.AdminFeedTemplate;
import org.savapage.core.util.DateUtil;
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

    /** */
    private static final long BACKUP_WARN_THRESHOLD_DAYS = 6;

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
        createAdminFeedXhtml(Locale.ENGLISH, xhtml);

        //
        final FeedEntryDto dto = new FeedEntryDto();

        dto.setUuid(UUID.randomUUID());
        dto.setTitle("Metrics");
        dto.setAuthor(ServiceContext.getActor());
        dto.setCategory("statistics");
        dto.setSummary("Daily Data");
        dto.setUpdated(ServiceContext.getTransactionDate());

        try (FileWriter jsonWriter = new FileWriter(pathJson.toFile());) {

            // 1.
            FileUtils.writeStringToFile(pathXhtml.toFile(), xhtml.toString(),
                    Charset.forName("UTF-8"));
            // 2.
            JsonHelper.write(dto, jsonWriter);

        } catch (IOException e) {
            throw new FeedException(e.getMessage());
        }
    }

    /**
     * Gets the rolling pages over the last 2 days.
     *
     * @param configKey
     *            The type of pages.
     * @return Number of pages.
     */
    private static Long getRollingPages(final IConfigProp.Key configKey) {

        final String jsonSeries =
                ConfigManager.instance().getConfigValue(configKey);

        final JsonRollingTimeSeries<Long> data =
                new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY, 2, 0L);

        try {
            data.init(ServiceContext.getTransactionDate(), jsonSeries);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return null;
        }

        Long pages = 0L;

        if (!data.getData().isEmpty()) {

            pages = data.getData().get(0);

            if (data.getData().size() > 1) {
                pages += data.getData().get(1);
            }
        }

        if (pages == 0L) {
            return null;
        }
        return pages;
    }

    /**
     *
     * @return
     */
    private List<TemplatePrinterSnmpDto> getPrintersSnmp(final Locale locale) {

        final PrinterDao.ListFilter filter = new PrinterDao.ListFilter();

        filter.setDeleted(Boolean.FALSE);
        filter.setDisabled(Boolean.FALSE);
        filter.setSnmp(Boolean.TRUE);

        final List<Printer> list = printerDAO().getListChunk(filter, null, null,
                PrinterDao.Field.DISPLAY_NAME, true);

        if (list.isEmpty()) {
            return null;
        }

        final List<TemplatePrinterSnmpDto> printers = new ArrayList<>();

        final Map<String, TemplatePrinterSnmpDto> printerMap = new HashMap<>();

        for (final Printer printer : list) {

            final PrinterAttrLookup attrLookup = new PrinterAttrLookup(printer);
            final Date snmpDate = printerAttrDAO().getSnmpDate(attrLookup);

            final String json = printerAttrDAO().getSnmpJson(attrLookup);
            final ProxyPrinterSnmpInfoDto snmpInfo;

            if (json == null) {
                snmpInfo = null;
            } else {
                snmpInfo = printerService().getSnmpInfo(json);
            }

            final String mapKey;
            if (snmpInfo == null || StringUtils.isBlank(snmpInfo.getModel())
                    || StringUtils.isBlank(snmpInfo.getSerial())) {
                mapKey = printer.getPrinterName();
            } else {
                mapKey = String.format("%s%s", snmpInfo.getModel(),
                        snmpInfo.getSerial());
            }

            final TemplatePrinterSnmpDto wlk;
            final List<String> wlkNames;

            if (printerMap.containsKey(mapKey)) {
                wlk = printerMap.get(mapKey);
                wlkNames = wlk.getNames();
            } else {
                wlk = new TemplatePrinterSnmpDto();
                printerMap.put(mapKey, wlk);
                wlkNames = new ArrayList<>();
                wlk.setNames(wlkNames);
            }

            wlkNames.add(printer.getDisplayName());

            if (wlkNames.size() > 1) {
                continue;
            }

            wlk.setDate(snmpDate);

            final List<String> alerts = new ArrayList<>();

            if (snmpInfo != null) {

                wlk.setModel(snmpInfo.getModel());
                wlk.setSerial(snmpInfo.getSerial());

                final long duration =
                        snmpDate.getTime() - snmpInfo.getDate().getTime();

                if (duration == 0) {
                    if (snmpInfo.getErrorStates() != null) {
                        for (final SnmpPrinterErrorStateEnum error : snmpInfo
                                .getErrorStates()) {
                            alerts.add(error.uiText(locale));
                        }
                    }
                } else {
                    alerts.add(String.format("%s (%s)",
                            SnmpPrinterErrorStateEnum.OFFLINE.uiText(locale),
                            DateUtil.formatDuration(duration)));
                }

                if (!snmpInfo.getMarkers().isEmpty()) {
                    final List<String> markerNames = new ArrayList<>();
                    final List<Integer> markerPercs = new ArrayList<>();

                    for (final Entry<SnmpPrtMarkerColorantValueEnum, Integer> entry : snmpInfo
                            .getMarkers().entrySet()) {

                        markerNames.add(entry.getKey().uiText(locale));
                        markerPercs.add(entry.getValue());
                    }

                    wlk.setMarkerNames(markerNames);
                    wlk.setMarkerPercs(markerPercs);
                }

            } else {
                alerts.add(SnmpPrinterErrorStateEnum.OFFLINE.uiText(locale));
            }

            if (!alerts.isEmpty()) {
                wlk.setAlerts(alerts);
            }
        }

        printers.addAll(printerMap.values());

        return printers;
    }

    /**
     * @param locale
     *            The locale.
     * @param xhtml
     *            {@link StringBuilder} to append XHTML on.
     */
    private void createAdminFeedXhtml(final Locale locale,
            final StringBuilder xhtml) {

        final TemplateAdminFeedDto dto = new TemplateAdminFeedDto();

        //
        final MemberCard card = MemberCard.instance();

        dto.setMember(card.getMemberOrganisation());
        dto.setMembership(card.getStatusUserText(locale));
        dto.setMembershipWarning(card.isMembershipDesirable());
        dto.setParticipants(String.valueOf(card.getMemberParticipants()));
        dto.setDaysTillExpiry(card.getDaysTillExpiry());
        dto.setDaysTillExpiryWarning(card.isDaysTillExpiryWarning());

        dto.setUserCount(String.valueOf(userDAO().count()));
        dto.setActiveUserCount(String.valueOf(userDAO().countActiveUsers()));

        dto.setSystemMode(ConfigManager.getSystemMode().uiText(locale));
        dto.setUptime(DateUtil.formatDuration(SystemInfo.getUptime()));

        final Date now = new Date();
        final Date oneDayAgo = DateUtils.addDays(now, -1);
        final long errors = appLogService().countErrors(oneDayAgo);
        final long warnings = appLogService().countWarnings(oneDayAgo);

        if (errors != 0) {
            dto.setErrorCount(errors);
        }
        if (warnings != 0) {
            dto.setWarningCount(warnings);
        }

        final long tickets = jobTicketService().getJobTicketQueueSize();
        if (tickets != 0) {
            dto.setTicketCount(tickets);
        }

        dto.setPagesReceived(
                getRollingPages(Key.STATS_PRINT_IN_ROLLING_DAY_PAGES));
        dto.setPagesPrinted(
                getRollingPages(Key.STATS_PRINT_OUT_ROLLING_DAY_PAGES));
        dto.setPagesDownloaded(
                getRollingPages(Key.STATS_PDF_OUT_ROLLING_DAY_PAGES));

        if (ConfigManager.instance().isConfigValue(Key.PRINTER_SNMP_ENABLE)) {
            dto.setPrintersSnmp(getPrintersSnmp(locale));
        }

        //
        final long timeBackup = ConfigManager.instance()
                .getConfigLong(IConfigProp.Key.SYS_BACKUP_LAST_RUN_TIME);
        if (timeBackup > 0) {
            final long backupDays = ChronoUnit.DAYS.between(
                    new Date(timeBackup).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now());

            dto.setDaysSinceLastBackup(Long.valueOf(backupDays));
            dto.setBackupWarning(backupDays > BACKUP_WARN_THRESHOLD_DAYS);
        }

        //
        final SslCertInfo sslCert = ConfigManager.getSslCertInfo();
        if (sslCert != null) {
            dto.setSslCert(TemplateSslCertDto.create(sslCert));
            dto.getSslCert()
                    .setNotAfterWarning(sslCert.isNotAfterWithinMonth(now));
        }
        //
        xhtml.append(new AdminFeedTemplate(dto).render(locale));
    }

    @Override
    public AtomFeedWriter getAdminFeedWriter(final URI requestURI,
            final OutputStream ostr) throws FeedException {

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

        return new AdminAtomFeedWriter(requestURI, ostr, feedEntryFiles);
    }
}
