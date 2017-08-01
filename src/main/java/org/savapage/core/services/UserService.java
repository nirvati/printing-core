/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.UserDto;
import org.savapage.core.dto.UserPropertiesDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.users.IUserSource;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface UserService {

    /**
     *
     */
    int MAX_TIME_SERIES_INTERVALS_WEEK = 4;

    /**
     *
     */
    int MAX_TIME_SERIES_INTERVALS_MONTH = 4;

    /**
     *
     * @param cardNumber
     *            The card number.
     * @return {@code true} when user card is registered.
     */
    boolean isCardRegistered(String cardNumber);

    /**
     *
     * @param user
     *            The {@link User}.
     * @return {@code true} when user has an associated primary card.
     */
    boolean hasAssocPrimaryCard(User user);

    /**
     *
     * @param user
     *            The {@link User}.
     * @return {@code true} when user has an associated primary email address.
     */
    boolean hasAssocPrimaryEmail(User user);

    /**
     *
     * @param user
     *            The {@link User}.
     * @return {@code true} when user has an associated secundary email address.
     */
    boolean hasAssocSecondaryEmail(User user);

    /**
     *
     * @param user
     *            The {@link User}.
     * @return {@code true} when user has an associated primary ID number.
     */
    boolean hasAssocPrimaryNumber(User user);

    /**
     *
     * @param user
     *            The {@link User}.
     * @return {@code true} when (internal) user has a password.
     */
    boolean hasInternalPassword(User user);

    /**
     * Gets the Primary Card number of a User.
     *
     * @param user
     *            The {@link User}.
     * @return {@code blank} when not found.
     */
    String getPrimaryCardNumber(User user);

    /**
     * Gets the Primary ID number of a User.
     *
     * @param user
     *            The {@link User}.
     * @return {@code blank} when not found.
     */
    String getPrimaryIdNumber(User user);

    /**
     * Gets the YubiKey Public ID of a User.
     *
     * @param user
     *            The {@link User}.
     * @return {@code blank} when not found.
     */
    String getYubiKeyPubID(User user);

    /**
     * Gets the PGP Public Key ID of a User.
     *
     * @param user
     *            The {@link User}.
     * @return {@code blank} when not found.
     */
    String getPGPPubKeyID(User user);

    /**
     * Gets the Primary email address of a User.
     *
     * @param user
     *            The {@link User}.
     * @return {@code null} when not found.
     */
    String getPrimaryEmailAddress(User user);

    /**
     * Creates a {@link UserDto} from a {@link User}.
     *
     * @param user
     *            The User.
     * @return The dto.
     */
    UserDto createUserDto(User user);

    /**
     * Updates an External User or Inserts an Internal User.
     *
     * @param userDto
     *            The {@link UserDto}.
     * @param isNewInternalUser
     *            {@code true} if this is a new Internal User that needs to be
     *            inserted.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something went wrong.
     */
    AbstractJsonRpcMethodResponse setUser(UserDto userDto,
            boolean isNewInternalUser) throws IOException;

    /**
     * Creates a new or Updates an existing Internal User.
     *
     * @param dto
     *            The {@link UserDto}.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something went wrong.
     */
    AbstractJsonRpcMethodResponse addInternalUser(UserDto dto)
            throws IOException;

    /**
     * Updates one or more properties for an existing Internal or External User.
     *
     * @param dto
     *            The {@link UserPropertiesDto}.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something went wrong.
     */
    AbstractJsonRpcMethodResponse setUserProperties(final UserPropertiesDto dto)
            throws IOException;

    /**
     * Logically deletes a User.
     *
     * @param userIdToDelete
     *            The unique user name to delete.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something went wrong.
     */
    AbstractJsonRpcMethodResponse deleteUser(final String userIdToDelete)
            throws IOException;

    /**
     * Logically deletes a user and auto-corrects the inconsistent situation
     * where multiple active instances with same user name exist (in that case
     * all instances are logically deleted).
     * <p>
     * NOTE: Any inconsistent situation is caused by a program bug, which of
     * course needs to be fixed. In the mean time this method can be used to
     * ad-hoc fix inconsistencies by deleting all user instances.
     * </p>
     *
     * @param userIdToDelete
     *            The unique user name to delete.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something went wrong.
     */
    AbstractJsonRpcMethodResponse deleteUserAutoCorrect(
            final String userIdToDelete) throws IOException;

    /**
     * Lists Users sorted by user name.
     *
     * @param startIndex
     *            0-based index of the first item in the resulting data.items.
     * @param itemsPerPage
     *            The number of items in the result. This is not necessarily the
     *            size of the data.items array. I.e. in the last page of items,
     *            the size of data.items may be less than itemsPerPage. However
     *            the size of data.items should not exceed itemsPerPage.
     * @return The JSON-RPC Return message (either a result or an error);
     * @throws IOException
     *             When something goes wrong.
     */
    AbstractJsonRpcMethodResponse listUsers(Integer startIndex,
            Integer itemsPerPage) throws IOException;

    /**
     * Finds a {@link User} by Card Number.
     * <p>
     * When offered Card Number is blank, {@code null} is returned.
     * </p>
     *
     * @param cardNumber
     *            The unique card number.
     * @return The User or {@code null} when not found.
     */
    User findUserByCardNumber(String cardNumber);

    /**
     * Finds a {@link User} by email address.
     * <p>
     * When offered email is blank, {@code null} is returned.
     * </p>
     *
     * @param email
     *            The unique email address.
     * @return The User or {@code null} when not found.
     */
    User findUserByEmail(String email);

    /**
     * Finds a {@link User} by ID number.
     * <p>
     * When offered number is blank, {@code null} is returned.
     * </p>
     *
     * @param number
     *            The unique ID number.
     * @return The User or {@code null} when not found.
     */
    User findUserByNumber(String number);

    /**
     * Finds a {@link User} by YubiKey Public ID.
     * <p>
     * When offered Public ID is blank, {@code null} is returned.
     * </p>
     *
     * @param publicID
     * @return The User or {@code null} when not found.
     */
    User findUserByYubiKeyPubID(String publicID);

    /**
     * Finds a {@link User} by ID number that has {@link UUID}.
     * <p>
     * When offered number is blank, {@code null} is returned.
     * </p>
     *
     * @param number
     *            The unique ID number.
     * @param uuid
     *            The {@link UUID}.
     * @return The User or {@code null} when not found.
     */
    User findUserByNumberUuid(final String number, final UUID uuid);

    /**
     * Add/Replace the Primary Card to/of the {@link User}.
     *
     * <p>
     * Note: Also removes any non-primary card having the same number.
     * </p>
     * <p>
     * If the cardNumber is null or blank, the current card is removed.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param primaryCardNumber
     *            The Card Number to associate.
     */
    void assocPrimaryCardNumber(User user, String primaryCardNumber);

    /**
     * Add/Replace the Primary Email address to/of the {@link User}.
     *
     * <p>
     * Note: Also removes any non-primary email having the same email address.
     * </p>
     * <p>
     * If the emailAddress is null or blank, the current primary address is
     * removed.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param primaryEmailAddress
     *            The Primary Email Address to associate.
     */
    void assocPrimaryEmail(User user, String primaryEmailAddress);

    /**
     * Add/Replace the Primary ID Number to the {@link User}.
     * <p>
     * If the ID Number is null or blank, the current ID NUmber is removed.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param primaryIdNumber
     *            The Primary ID Number to associate.
     */
    void assocPrimaryIdNumber(User user, String primaryIdNumber);

    /**
     * Adds totals of a PrintIn job to a {@link User} (database is NOT updated).
     *
     * @param user
     *            The {@link User}.
     * @param jobDate
     *            The date.
     * @param jobPages
     *            The number of pages.
     * @param jobBytes
     *            The number of bytes.
     */
    void addPrintInJobTotals(User user, Date jobDate, int jobPages,
            long jobBytes);

    /**
     * Adds totals of a PdfOut job to a {@link User} (database is NOT updated).
     *
     * @param user
     *            The {@link User}.
     * @param jobDate
     *            The date.
     * @param jobPages
     *            The number of pages.
     * @param jobBytes
     *            The number of bytes.
     */
    void addPdfOutJobTotals(User user, Date jobDate, int jobPages,
            long jobBytes);

    /**
     * Adds totals of a PrintOut job to a {@link User} (database is NOT
     * updated).
     *
     * @param user
     *            The {@link User}.
     * @param jobDate
     *            The date.
     * @param jobPages
     *            The number of pages.
     * @param jobSheets
     *            The number of sheets.
     * @param jobEsu
     *            The number of ESU.
     * @param jobBytes
     *            The number of bytes.
     */
    void addPrintOutJobTotals(User user, Date jobDate, int jobPages,
            int jobSheets, long jobEsu, long jobBytes);

    /**
     * Checks if {@link User} is fully disabled on reference date.
     *
     * @param user
     *            The {@link User}.
     * @param refDate
     *            The reference date.
     * @return {@code true} if fully disabled.
     */
    boolean isUserFullyDisabled(User user, Date refDate);

    /**
     * Checks if PrintIn function is disabled for {@link User} on reference
     * date.
     *
     * @param user
     *            The {@link User}.
     * @param refDate
     *            The reference date.
     * @return {@code true} if disabled.
     */
    boolean isUserPrintInDisabled(User user, Date refDate);

    /**
     * Checks if PdfOut function is disabled for {@link User} on reference date.
     *
     * @param user
     *            The {@link User}.
     * @param refDate
     *            The reference date.
     * @return {@code true} if disabled.
     */
    boolean isUserPdfOutDisabled(User user, Date refDate);

    /**
     * Checks if PrintOut function is disabled for {@link User} on reference
     * date.
     *
     * @param user
     *            The {@link User}.
     * @param refDate
     *            The reference date.
     * @return {@code true} if disabled.
     */
    boolean isUserPrintOutDisabled(User user, Date refDate);

    /**
     * Sets {@link User} as <i>logically</i> deleted and removes associated ID
     * Number, Card and EMail.
     * <p>
     * Also, associated {@link Account} objects (via {@link UserAccount}) that
     * are non-{@link Account.AccountTypeEnum#SHARED}, are <i>logically</i>
     * deleted.
     * </p>
     *
     * @param user
     *            The {@link User}.
     */
    void performLogicalDelete(User user);

    /**
     * Gets the (un-encrypted) {@link UserAttr#getValue()} from
     * {@link User#getAttributes()} list.
     *
     * @param user
     *            The {@link User}.
     * @param attrEnum
     *            The {@link UserAttrEnum} to search for.
     * @return The (un-encrypted) value string or {@code null} when not found.
     */
    String getUserAttrValue(final User user, final UserAttrEnum attrEnum);

    /**
     * Removes an attribute from the User's list of attributes AND from the
     * database.
     *
     * @param user
     *            The user.
     * @param name
     *            The name of the {@link UserAttr}.
     * @return The removed {@link UserAttr} or {@code null} when not found.
     */
    UserAttr removeUserAttr(User user, UserAttrEnum name);

    /**
     * Adds an attribute to the User's list of attributes (the list is lazy
     * created when it does not exist).
     * <p>
     * WARNING: This method does NOT check if the attribute is already on the
     * list.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param name
     *            The name of the {@link UserAttr}.
     * @param value
     *            The value.
     */
    void addUserAttr(User user, UserAttrEnum name, final String value);

    /**
     * Adds the {@link UserAttrEnum#UUID} to the User's list of attributes and
     * creates the {@link UserAttr} in the database when the attribute is NOT
     * already on the list (the list is lazy created when it does not exist).
     *
     * @param user
     *            The {@link User}.
     * @return The {@link UUID}.
     */
    UUID lazyAddUserAttrUuid(User user);

    /**
     * Reads the attribute value from the database.
     *
     * @param user
     *            The {@link User}.
     * @param name
     *            The name of the {@link UserAttr}.
     * @return The attribute value or {@code null} when NOT found.
     */
    String findUserAttrValue(User user, UserAttrEnum name);

    /**
     * Creates or updates the attribute value to the database.
     *
     * @param user
     *            The user.
     * @param attrEnum
     *            The name of the {@link UserAttr}.
     * @param attrValue
     *            The value.
     */
    void setUserAttrValue(User user, UserAttrEnum attrEnum, String attrValue);

    /**
     * Encrypts an (internal) user password.
     *
     * @param userid
     *            The user id.
     * @param password
     *            The plain password.
     * @return The encrypted password.
     */
    String encryptUserPassword(String userid, String password);

    /**
     * Logs a PrintIn job, by adding a data point to the time series (database
     * <b>is</b> updated).
     *
     * @param user
     *            The user.
     * @param jobTime
     *            The time of the job.
     * @param jobPages
     *            The number of pages.
     * @param jobBytes
     *            The number of bytes.
     */
    void logPrintIn(User user, Date jobTime, Integer jobPages, Long jobBytes);

    /**
     * Logs a PrintOut job for a {@link User}, by adding a data point to the
     * {@link UserAttr} time series. The database is updated.
     *
     * @param user
     *            The user.
     * @param jobTime
     *            The time of the job.
     * @param jobPages
     *            The number of pages.
     * @param jobSheets
     *            The number of sheets.
     * @param jobEsu
     *            The number of ESU.
     * @param jobBytes
     *            The number of bytes.
     */
    void logPrintOut(User user, Date jobTime, Integer jobPages,
            Integer jobSheets, Long jobEsu, Long jobBytes);

    /**
     * Logs a PdfOut job, by adding a data point to the time series (database IS
     * updated).
     *
     * @param user
     *            The user.
     * @param jobTime
     *            The time of the job.
     * @param jobPages
     *            The number of pages.
     * @param jobBytes
     *            The number of bytes.
     */
    void logPdfOut(User user, Date jobTime, Integer jobPages, Long jobBytes);

    /**
     * Inserts a new external {@link User} if it exists in the external user
     * source (group).
     * <p>
     * If the user id is already active (i.e. not logically deleted) in the
     * database, the current {@link User} instance is returned.
     * </p>
     *
     * @param userSource
     *            The {@link IUserSource} to check whether the user exists in
     *            the external user source.
     * @param userId
     *            The user id.
     * @param userGroup
     *            The {@link IConfigProp.Key#USER_SOURCE_GROUP} the user belongs
     *            to. Can be empty or {@code null} when no group is specified or
     *            known.
     * @return {@code null} if the user does NOT exist in the user source
     *         (group), or is a reserved name like 'admin'.
     */
    User lazyInsertExternalUser(IUserSource userSource, String userId,
            String userGroup);

    /**
     * Creates a user's home directory structure when it does not exist.
     * <p>
     * IMPORTANT: This method has its own database transaction. When creating
     * the home directory the {@link User} database row is locked.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @return The home directory.
     * @throws IOException
     *             If an I/O error occurs.
     */
    File lazyUserHomeDir(User user) throws IOException;

    /**
     * Creates a user's home directory structure when it does not exist.
     *
     * <p>
     * IMPORTANT: This method does NOT have its own database transaction. Any
     * client should lock the {@link User} database row before calling this
     * method.
     * </p>
     *
     * @param userId
     *            The unique id of the user.
     * @throws IOException
     *             If an I/O error occurs.
     */
    void lazyUserHomeDir(String userId) throws IOException;

    /**
     * Gets the saved PDF properties for a {@link User} from the database or by
     * supplying a default.
     * <p>
     * NOTE: After reading from the database any passwords are <i>decrypted</i>.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @return The {@link PdfProperties}.
     * @throws Exception
     */
    PdfProperties getPdfProperties(User user) throws Exception;

    /**
     * Stores the PDF properties for the SafePages of a user to the database.
     * <p>
     * NOTE: Before writing to the database any passwords are <i>encrypted</i>.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param objProps
     *            The {@link PdfProperties}.
     * @throws IOException
     *             When JSON things go wrong.
     */
    void setPdfProperties(User user, PdfProperties objProps) throws IOException;

}
