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
package org.savapage.core.reports.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ReservedUserGroupEnum;
import org.savapage.core.dao.helpers.UserPagerReq;
import org.savapage.core.dto.AccountDisplayInfoDto;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.reports.AbstractJrDataSource;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.ServiceContext;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserDataSource extends AbstractJrDataSource
        implements JRDataSource {

    private static final int CHUNK_SIZE = 100;

    private List<User> entryList = null;
    private Iterator<User> iterator;

    private User userWlk = null;
    private AccountDisplayInfoDto accountInfoWlk = null;

    private int counter = 1;
    private int chunkCounter = CHUNK_SIZE;

    private final UserDao.Field sortField;
    private final Boolean sortAscending;
    private final UserDao.ListFilter filter;

    private final AccountingService accountingService;

    /**
     *
     * @param req
     */
    public UserDataSource(final UserPagerReq req, final Locale locale) {

        super(locale);

        this.accountingService =
                ServiceContext.getServiceFactory().getAccountingService();

        this.sortField = UserDao.Field.USERID; // TEST
        this.sortAscending = true;
        this.filter = new UserDao.ListFilter();

        //
        final Long userGroupId = req.getSelect().getUserGroupId();

        if (userGroupId != null) {

            final UserGroupDao userGroupDao =
                    ServiceContext.getDaoContext().getUserGroupDao();

            final ReservedUserGroupEnum reservedGroup =
                    userGroupDao.findReservedGroup(userGroupId);

            if (reservedGroup == null) {
                filter.setUserGroupId(req.getSelect().getUserGroupId());
            } else {
                filter.setInternal(reservedGroup.isInternalExternal());
            }
        }

        filter.setContainingNameOrIdText(
                req.getSelect().getNameIdContainingText());
        filter.setContainingEmailText(req.getSelect().getEmailContainingText());
        filter.setAdmin(req.getSelect().getAdmin());
        filter.setPerson(req.getSelect().getPerson());
        filter.setDisabled(req.getSelect().getDisabled());
        filter.setDeleted(req.getSelect().getDeleted());

        this.counter = 0;
        this.chunkCounter = CHUNK_SIZE;
    }

    /**
     *
     * @return The Balance header text with the currency code.
     */
    public String getBalanceHeaderText() {
        return localized("userlist-header-balance",
                ConfigManager.getAppCurrencyCode());
    }

    /**
     *
     * @return The selection info.
     */
    public String getSelectionInfo() {

        final StringBuilder where = new StringBuilder();

        int nSelect = 0;

        if (filter.getContainingNameOrIdText() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("userlist-sel-username",
                    filter.getContainingNameOrIdText()));
        }

        if (filter.getContainingEmailText() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            where.append(localized("userlist-sel-email",
                    filter.getContainingEmailText()));
        }

        if (filter.getInternal() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            if (filter.getInternal()) {
                where.append(localized("userlist-sel-internal-users"));
            } else {
                where.append(localized("userlist-sel-external-users"));
            }
        } else if (filter.getUserGroupId() != null) {
            final UserGroup group = ServiceContext.getDaoContext()
                    .getUserGroupDao().findById(filter.getUserGroupId());
            if (group != null) {
                if (nSelect > 0) {
                    where.append(", ");
                }
                nSelect++;
                where.append(localized("userlist-sel-user-group",
                        group.getGroupName()));
            }
        }

        if (filter.getPerson() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            if (filter.getPerson()) {
                where.append(localized("userlist-sel-persons"));
            } else {
                where.append(localized("userlist-sel-abstract-users"));
            }
        }

        if (filter.getAdmin() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            if (filter.getAdmin()) {
                where.append(localized("userlist-sel-admins"));
            } else {
                where.append(localized("userlist-sel-non-admins"));
            }
        }

        if (filter.getDisabled() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            if (filter.getDisabled()) {
                where.append(localized("userlist-sel-disabled-users"));
            } else {
                where.append(localized("userlist-sel-enabled-users"));
            }
        }

        if (filter.getDeleted() != null) {
            if (nSelect > 0) {
                where.append(", ");
            }
            nSelect++;
            if (filter.getDeleted()) {
                where.append(localized("userlist-sel-deleted-users"));
            } else {
                where.append(localized("userlist-sel-active-users"));
            }
        }

        return where.toString();
    }

    /**
     *
     * @param startPosition
     * @param maxResults
     */
    private void getNextChunk(Integer startPosition, Integer maxResults) {

        this.entryList = ServiceContext.getDaoContext().getUserDao()
                .getListChunk(this.filter, startPosition, maxResults,
                        this.sortField, this.sortAscending);

        this.chunkCounter = 0;
        this.iterator = this.entryList.iterator();

    }

    @Override
    public Object getFieldValue(final JRField jrField) throws JRException {

        switch (jrField.getName()) {
        case "USER_NAME":
            return userWlk.getUserId();
        case "FULL_NAME":
            return userWlk.getFullName();
        case "BALANCE":
            return accountInfoWlk.getBalance();
        case "HAS_CREDIT":
            String creditLimit = accountInfoWlk.getCreditLimit();
            if (creditLimit == null) {
                creditLimit = "-";
            }
            return creditLimit;
        case "PAGE_TOTAL":
            return this.userWlk.getNumberOfPrintOutPages();
        case "JOB_TOTAL":
            return this.userWlk.getNumberOfPrintOutJobs();
        case "EMAIL":
            if (userWlk.getEmails() == null || userWlk.getEmails().isEmpty()) {
                return "";
            }
            return userWlk.getEmails().get(0).getAddress();
        default:
            return null;
        }
    }

    @Override
    public boolean next() throws JRException {

        if (this.chunkCounter == CHUNK_SIZE) {
            getNextChunk(this.counter, CHUNK_SIZE);
        }

        if (!this.iterator.hasNext()) {
            return false;
        }

        this.userWlk = this.iterator.next();

        this.counter++;
        this.chunkCounter++;

        this.accountInfoWlk = accountingService.getAccountDisplayInfo(userWlk,
                getLocale(), null);

        return true;
    }
}
