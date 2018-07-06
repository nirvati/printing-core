/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.template.dto;

import java.util.List;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TemplateAdminFeedDto implements TemplateDto {

    private String member;

    private String membership;
    private boolean membershipWarning;

    private String participants;

    private Long daysTillExpiry;
    private boolean daysTillExpiryWarning;

    private String userCount;
    private String activeUserCount;
    private String systemMode;
    private String systemStatus;
    private String uptime;
    private Long errorCount;
    private Long warningCount;
    private Long ticketCount;

    private Long pagesReceived;
    private Long pagesPrinted;
    private Long pagesDownloaded;

    private Long daysSinceLastBackup;
    private boolean backupWarning;

    private List<TemplatePrinterSnmpDto> printersSnmp;

    private TemplateSslCertDto sslCert;

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getMembership() {
        return membership;
    }

    public void setMembership(String membership) {
        this.membership = membership;
    }

    public boolean isMembershipWarning() {
        return membershipWarning;
    }

    public void setMembershipWarning(boolean membershipWarning) {
        this.membershipWarning = membershipWarning;
    }

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public Long getDaysTillExpiry() {
        return daysTillExpiry;
    }

    public void setDaysTillExpiry(Long daysTillExpiry) {
        this.daysTillExpiry = daysTillExpiry;
    }

    public boolean isDaysTillExpiryWarning() {
        return daysTillExpiryWarning;
    }

    public void setDaysTillExpiryWarning(boolean daysTillExpiryWarning) {
        this.daysTillExpiryWarning = daysTillExpiryWarning;
    }

    public String getUserCount() {
        return userCount;
    }

    public void setUserCount(String userCount) {
        this.userCount = userCount;
    }

    public String getActiveUserCount() {
        return activeUserCount;
    }

    public void setActiveUserCount(String activeUserCount) {
        this.activeUserCount = activeUserCount;
    }

    public String getSystemMode() {
        return systemMode;
    }

    public void setSystemMode(String systemMode) {
        this.systemMode = systemMode;
    }

    public String getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }

    public Long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Long warningCount) {
        this.warningCount = warningCount;
    }

    public Long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Long ticketCount) {
        this.ticketCount = ticketCount;
    }

    public List<TemplatePrinterSnmpDto> getPrintersSnmp() {
        return printersSnmp;
    }

    public void setPrintersSnmp(List<TemplatePrinterSnmpDto> printersSnmp) {
        this.printersSnmp = printersSnmp;
    }

    public Long getPagesReceived() {
        return pagesReceived;
    }

    public void setPagesReceived(Long pagesReceived) {
        this.pagesReceived = pagesReceived;
    }

    public Long getPagesPrinted() {
        return pagesPrinted;
    }

    public void setPagesPrinted(Long pagesPrinted) {
        this.pagesPrinted = pagesPrinted;
    }

    public Long getPagesDownloaded() {
        return pagesDownloaded;
    }

    public void setPagesDownloaded(Long pagesDownloaded) {
        this.pagesDownloaded = pagesDownloaded;
    }

    public TemplateSslCertDto getSslCert() {
        return sslCert;
    }

    public void setSslCert(TemplateSslCertDto sslCert) {
        this.sslCert = sslCert;
    }

    public Long getDaysSinceLastBackup() {
        return daysSinceLastBackup;
    }

    public void setDaysSinceLastBackup(Long daysSinceLastBackup) {
        this.daysSinceLastBackup = daysSinceLastBackup;
    }

    public boolean isBackupWarning() {
        return backupWarning;
    }

    public void setBackupWarning(boolean backupWarning) {
        this.backupWarning = backupWarning;
    }

}
