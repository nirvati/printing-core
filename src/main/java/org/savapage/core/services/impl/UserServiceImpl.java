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
package org.savapage.core.services.impl;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.SpException;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserEmailDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserDto;
import org.savapage.core.dto.UserEmailDto;
import org.savapage.core.dto.UserPropertiesDto;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserCard;
import org.savapage.core.jpa.UserEmail;
import org.savapage.core.jpa.UserNumber;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.json.rpc.impl.ResultListUsers;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.users.CommonUser;
import org.savapage.core.users.IUserSource;
import org.savapage.core.util.EmailValidator;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class UserServiceImpl extends AbstractService
        implements UserService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public AbstractJsonRpcMethodResponse
            setUserProperties(final UserPropertiesDto dto) throws IOException {

        final String userid = dto.getUserName();

        /*
         * INVARIANT: username MUST be present.
         */
        if (StringUtils.isBlank(userid)) {
            return createError("msg-user-userid-is-empty");
        }

        /*
         * INVARIANT: Internal admin is a reserved user.
         */
        if (ConfigManager.isInternalAdmin(userid)) {
            return createError("msg-username-reserved", userid);
        }

        final User jpaUser = userDAO().findActiveUserByUserId(userid);

        /*
         * INVARIANT: User MUST exist.
         */
        if (jpaUser == null) {
            return createError("msg-user-not-found", userid);
        }

        /*
         * INVARIANT: (Remove/Keep) password is for Internal User only.
         */
        if ((dto.getPassword() != null || dto.getKeepPassword()
                || dto.getRemovePassword()) && !jpaUser.getInternal()) {
            return createError("msg-user-not-internal", userid);
        }

        /*
         *
         */
        final String pin = dto.getPin();
        final String uuid = dto.getUuid();
        final String password = dto.getPassword();
        final String primaryEmail = dto.getEmail();
        final String cardNumber = dto.getCard();
        final String idNumber = dto.getId();

        boolean isUpdated = false;

        /*
         * Fullname
         */
        if (!StringUtils.isBlank(dto.getFullName())) {
            jpaUser.setFullName(dto.getFullName());
            isUpdated = true;
        }

        /*
         * Primary Email (remove).
         */
        if (StringUtils.isBlank(primaryEmail)) {

            if (dto.getRemoveEmail()) {
                this.assocPrimaryEmail(jpaUser, null);
                isUpdated = true;
            }

        } else {

            EmailValidator validator = new EmailValidator();
            if (!validator.validate(primaryEmail)) {
                /*
                 * INVARIANT: Email format MUST be valid.
                 */
                return createError("msg-email-invalid", primaryEmail);
            }

            final User jpaUserDuplicate = this.findUserByEmail(primaryEmail);

            if (jpaUserDuplicate != null
                    && !jpaUserDuplicate.getUserId().equals(userid)) {

                /*
                 * INVARIANT: Email MUST be unique.
                 */
                return createError("msg-user-duplicate-user-email",
                        primaryEmail, jpaUserDuplicate.getUserId());
            }

            this.assocPrimaryEmail(jpaUser, primaryEmail);
            isUpdated = true;
        }

        /*
         * Secondary Emails (remove).
         */
        boolean updateEmailOther = false;

        if (dto.getEmailOther() == null) {
            dto.importEmailOther("");
        }

        if (dto.getEmailOther().isEmpty()) {

            updateEmailOther = dto.getRemoveEmailOther();

        } else {

            if (dto.getKeepEmailOther()) {
                updateEmailOther = !hasAssocSecondaryEmail(jpaUser);
            } else {
                updateEmailOther = true;
            }
        }

        if (updateEmailOther) {

            JsonRpcMethodError error = setSecondaryEmail(false, primaryEmail,
                    jpaUser, dto.getEmailOther());

            if (error != null) {
                return error;
            }

            isUpdated = true;
        }

        /*
         * Primary Card Number (keep/remove).
         */
        if (StringUtils.isBlank(cardNumber)) {

            if (dto.getRemoveCard()) {
                this.assocPrimaryCardNumber(jpaUser, null);
                isUpdated = true;
            }
        } else {

            boolean doUpdate;

            if (dto.getKeepCard()) {
                doUpdate = !hasAssocPrimaryCard(jpaUser);
            } else {
                doUpdate = true;
            }

            if (doUpdate) {

                String normalizedCardNumber = null;

                final RfidNumberFormat formatDefault = new RfidNumberFormat();

                RfidNumberFormat.Format format;
                RfidNumberFormat.FirstByte firstByte;

                if (StringUtils.isBlank(dto.getCardFormat())) {
                    format = formatDefault.getFormat();
                } else {
                    format = RfidNumberFormat.toFormat(dto.getCardFormat());
                }

                if (StringUtils.isBlank(dto.getCardFirstByte())) {
                    firstByte = formatDefault.getFirstByte();
                } else {
                    firstByte = RfidNumberFormat
                            .toFirstByte(dto.getCardFirstByte());
                }

                final RfidNumberFormat rfidNumberFormat =
                        new RfidNumberFormat(format, firstByte);

                if (!rfidNumberFormat.isNumberValid(cardNumber)) {
                    /*
                     * INVARIANT: Card Number format MUST be valid.
                     */
                    return createError("msg-card-number-invalid", cardNumber);
                }

                final User jpaUserDuplicate =
                        this.findUserByCardNumber(dto.getCard());

                if (jpaUserDuplicate != null
                        && !jpaUserDuplicate.getUserId().equals(userid)) {

                    /*
                     * INVARIANT: Card Number MUST be unique.
                     */
                    return createError("msg-user-duplicate-user-card-number",
                            dto.getCard(), jpaUserDuplicate.getUserId());
                }

                normalizedCardNumber =
                        rfidNumberFormat.getNormalizedNumber(cardNumber);

                this.assocPrimaryCardNumber(jpaUser, normalizedCardNumber);
                isUpdated = true;
            }
        }

        /*
         * Primary ID Number (remove).
         */
        if (StringUtils.isBlank(idNumber)) {

            if (dto.getRemoveId()) {
                this.assocPrimaryIdNumber(jpaUser, null);
                isUpdated = true;
            }

        } else {

            final int lengthMin = ConfigManager.instance()
                    .getConfigInt(Key.USER_ID_NUMBER_LENGTH_MIN);

            if (idNumber.length() < lengthMin) {
                /*
                 * INVARIANT: ID Number format MUST be valid.
                 */
                return createError("msg-id-number-length-error",
                        String.valueOf(lengthMin));
            }

            final User jpaUserDuplicate = this.findUserByNumber(dto.getId());

            if (jpaUserDuplicate != null
                    && !jpaUserDuplicate.getUserId().equals(userid)) {

                /*
                 * INVARIANT: ID Number MUST be unique.
                 */
                return createError("msg-user-duplicate-user-id-number",
                        dto.getId(), jpaUserDuplicate.getUserId());
            }

            this.assocPrimaryIdNumber(jpaUser, idNumber);
            isUpdated = true;
        }

        /*
         * PIN (keep/remove)
         */
        if (StringUtils.isBlank(pin)) {

            if (dto.getRemovePin()) {
                if (this.removeUserAttr(jpaUser, UserAttrEnum.PIN) != null) {
                    isUpdated = true;
                }
            }

        } else {

            boolean doUpdate;

            if (dto.getKeepPin()) {
                doUpdate = this.findUserAttrValue(jpaUser,
                        UserAttrEnum.PIN) == null;
            } else {
                doUpdate = true;
            }

            if (doUpdate) {

                JsonRpcMethodError error = validateUserPin(dto.getPin());

                if (error != null) {
                    /*
                     * INVARIANT: PIN format MUST be valid.
                     */
                    return error;
                }
                this.encryptStoreUserAttr(jpaUser, UserAttrEnum.PIN, pin);
                isUpdated = true;
            }
        }

        /*
         * UUID (keep/remove)
         */
        if (StringUtils.isBlank(uuid)) {

            if (dto.getRemoveUuid()) {
                if (this.removeUserAttr(jpaUser, UserAttrEnum.UUID) != null) {
                    isUpdated = true;
                }
            }

        } else {

            boolean doUpdate;

            if (dto.getKeepUuid()) {
                doUpdate = this.findUserAttrValue(jpaUser,
                        UserAttrEnum.UUID) == null;
            } else {
                doUpdate = true;
            }

            if (doUpdate) {

                JsonRpcMethodError error = validateUserUuid(dto.getUuid());

                if (error != null) {
                    /*
                     * INVARIANT: UUID format MUST be valid.
                     */
                    return error;
                }
                this.encryptStoreUserAttr(jpaUser, UserAttrEnum.UUID, uuid);
                isUpdated = true;
            }
        }

        /*
         * Internal Password (keep/remove).
         */
        if (StringUtils.isBlank(password)) {

            if (dto.getRemovePassword()) {
                if (this.removeUserAttr(jpaUser,
                        UserAttrEnum.INTERNAL_PASSWORD) != null) {
                    isUpdated = true;
                }
            }
        } else {

            boolean doUpdate;

            if (dto.getKeepPassword()) {
                doUpdate = (this.findUserAttrValue(jpaUser,
                        UserAttrEnum.INTERNAL_PASSWORD) == null);
            } else {
                doUpdate = true;
            }

            if (doUpdate) {

                JsonRpcMethodError error =
                        validateInternalUserPassword(password);

                if (error != null) {
                    return error;
                }

                storeInternalUserPassword(jpaUser, password);

                isUpdated = true;
            }
        }

        /*
         * Anything changed?
         */
        if (isUpdated) {

            final Date now = new Date();
            jpaUser.setModifiedBy(ServiceContext.getActor());
            jpaUser.setModifiedDate(now);

            userDAO().update(jpaUser);
        }

        /*
         * Accounting.
         */
        final UserAccountingDto accountingDto = dto.getAccounting();

        if (accountingDto != null) {

            final AbstractJsonRpcMethodResponse rsp = accountingService()
                    .setUserAccounting(jpaUser, accountingDto);

            if (rsp.isError()) {
                return rsp;
            }
        }

        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public UserDto createUserDto(final User user) {

        final UserDto dto = new UserDto();

        dto.setDatabaseId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setAdmin(user.getAdmin());
        dto.setPerson(user.getPerson());
        dto.setUserName(user.getUserId());
        dto.setInternal(user.getInternal());

        dto.setId(this.getPrimaryIdNumber(user));
        dto.setEmail(this.getPrimaryEmailAddress(user));
        dto.setCard(this.getPrimaryCardNumber(user));

        final RfidNumberFormat rfidNumberFormat = new RfidNumberFormat();

        dto.setCardFirstByte(rfidNumberFormat.getFirstByte().toString());
        dto.setCardFormat(rfidNumberFormat.getFormat().toString());

        /*
         * Email other.
         */
        final ArrayList<UserEmailDto> emailsDto = new ArrayList<>();

        if (user.getEmails() != null) {

            for (final UserEmail email : user.getEmails()) {

                if (!userEmailDAO().isPrimaryEmail(email)) {
                    UserEmailDto emailDto = new UserEmailDto();
                    emailDto.setAddress(email.getAddress());
                    emailsDto.add(emailDto);
                }
            }
        }
        dto.setEmailOther(emailsDto);

        /*
         * PIN.
         */
        final String encryptedPin =
                this.findUserAttrValue(user, UserAttrEnum.PIN);

        String pin = "";
        if (encryptedPin != null) {
            pin = CryptoUser.decryptUserAttr(user.getId(), encryptedPin);
        }

        dto.setPin(pin);

        /*
         * UUID.
         */
        final String encryptedIppInternetUuid =
                this.findUserAttrValue(user, UserAttrEnum.UUID);

        String ippInternetUuid = "";
        if (encryptedIppInternetUuid != null) {
            ippInternetUuid = CryptoUser.decryptUserAttr(user.getId(),
                    encryptedIppInternetUuid);
        }

        dto.setUuid(ippInternetUuid);

        /*
         * As indication for all.
         */
        dto.setDisabled(user.getDisabledPrintIn());

        /*
         * Accounting.
         */
        dto.setAccounting(accountingService().getUserAccounting(user));

        /*
         * ACL Roles.
         */
        final UserAttr aclAttr =
                userAttrDAO().findByName(user, UserAttrEnum.ACL_ROLES);

        Map<ACLRoleEnum, Boolean> aclRoles;

        if (aclAttr == null) {
            aclRoles = null;
        } else {
            aclRoles = JsonHelper.createEnumBooleanMapOrNull(ACLRoleEnum.class,
                    aclAttr.getValue());
        }

        if (aclRoles == null) {
            aclRoles = new HashMap<ACLRoleEnum, Boolean>();
        }

        dto.setAclRoles(aclRoles);

        //
        return dto;

    }

    @Override
    public AbstractJsonRpcMethodResponse setUser(final UserDto userDto,
            final boolean isNewInternalUser) throws IOException {

        final Date now = new Date();

        final String userid = userDto.getUserName();

        if (StringUtils.isBlank(userid)) {
            return createError("msg-user-userid-is-empty");
        }

        if (ConfigManager.isInternalAdmin(userid)) {
            return createError("msg-username-reserved", userid);
        }

        String cardNumber = userDto.getCard();
        if (cardNumber != null) {
            cardNumber = cardNumber.toLowerCase();
        }
        final String idNumber = userDto.getId();
        final String primaryEmail = userDto.getEmail();

        /*
         * PIN
         */
        String pin = userDto.getPin();
        final boolean hasPIN = StringUtils.isNotBlank(pin);

        if (hasPIN) {
            JsonRpcMethodError error = validateUserPin(pin);
            if (error != null) {
                return error;
            }
        }

        /*
         * UUID
         */
        String uuid = userDto.getUuid();
        final boolean hasUuid = StringUtils.isNotBlank(uuid);

        if (hasUuid) {
            JsonRpcMethodError error = validateUserUuid(uuid);
            if (error != null) {
                return error;
            }
        }

        /*
         * Find duplicates for userid, ID Number and Card Number.
         *
         * NOTE: The finds return null when instance is logically deleted!
         */
        final User jpaUserDuplicate =
                userDAO().findActiveUserByUserId(userDto.getUserName());

        final User jpaUserIdNumberDuplicate = this.findUserByNumber(idNumber);

        final User jpaUserCardNumberDuplicate =
                this.findUserByCardNumber(cardNumber);

        final User jpaUserEmailDuplicate = this.findUserByEmail(primaryEmail);

        User jpaUser = null;

        if (isNewInternalUser) {

            if (jpaUserDuplicate != null) {
                return createError("msg-user-duplicate-userid", userid);
            }

            if (jpaUserIdNumberDuplicate != null) {
                return createError("msg-user-duplicate-user-id-number",
                        idNumber, jpaUserIdNumberDuplicate.getUserId());
            }

            if (jpaUserCardNumberDuplicate != null) {
                return createError("msg-user-duplicate-user-card-number",
                        cardNumber, jpaUserCardNumberDuplicate.getUserId());
            }

            if (jpaUserEmailDuplicate != null) {
                return createError("msg-user-duplicate-user-email",
                        primaryEmail, jpaUserEmailDuplicate.getUserId());
            }

            jpaUser = new User();
            jpaUser.setInternal(Boolean.TRUE);

            jpaUser.setUserId(userDto.getUserName());
            jpaUser.setExternalUserName("");

            final String password = userDto.getPassword();

            if (StringUtils.isNotBlank(password)) {
                JsonRpcMethodError error =
                        validateInternalUserPassword(password);
                if (error != null) {
                    return error;
                }
                addInternalUserPassword(jpaUser, password);
            }

            jpaUser.setCreatedBy(ServiceContext.getActor());
            jpaUser.setCreatedDate(now);

        } else {

            if (jpaUserDuplicate == null) {
                return createError("msg-user-not-found", userDto.getUserName());
            }

            if (jpaUserIdNumberDuplicate != null && !jpaUserIdNumberDuplicate
                    .getUserId().equals(jpaUserDuplicate.getUserId())) {
                return createError("msg-user-duplicate-user-id-number",
                        idNumber, jpaUserIdNumberDuplicate.getUserId());
            }

            if (jpaUserCardNumberDuplicate != null
                    && !jpaUserCardNumberDuplicate.getUserId()
                            .equals(jpaUserDuplicate.getUserId())) {
                return createError("msg-user-duplicate-user-card-number",
                        cardNumber, jpaUserCardNumberDuplicate.getUserId());
            }

            if (jpaUserEmailDuplicate != null && !jpaUserEmailDuplicate
                    .getUserId().equals(jpaUserDuplicate.getUserId())) {
                return createError("msg-user-duplicate-user-email",
                        primaryEmail, jpaUserEmailDuplicate.getUserId());
            }

            jpaUser = jpaUserDuplicate;

            jpaUser.setModifiedBy(ServiceContext.getActor());
            jpaUser.setModifiedDate(now);

        }

        /*
         * Primary Email.
         */
        if (StringUtils.isNotBlank(primaryEmail)) {
            EmailValidator validator = new EmailValidator();
            if (!validator.validate(primaryEmail)) {
                return createError("msg-email-invalid", primaryEmail);
            }
        }
        this.assocPrimaryEmail(jpaUser, primaryEmail);

        /*
         * Secondary Email.
         */
        final ArrayList<UserEmailDto> secondaryEmail = userDto.getEmailOther();

        if (secondaryEmail != null) {

            JsonRpcMethodError error = setSecondaryEmail(isNewInternalUser,
                    primaryEmail, jpaUser, secondaryEmail);

            if (error != null) {
                return error;
            }
        }

        /*
         * Card Number.
         */
        String normalizedCardNumber = null;

        if (StringUtils.isNotBlank(cardNumber)) {

            final RfidNumberFormat rfidNumberFormat;

            if (StringUtils.isBlank(userDto.getCardFormat())
                    || StringUtils.isBlank(userDto.getCardFirstByte())) {

                rfidNumberFormat = new RfidNumberFormat();

            } else {

                final RfidNumberFormat.Format format =
                        RfidNumberFormat.toFormat(userDto.getCardFormat());

                final RfidNumberFormat.FirstByte firstByte = RfidNumberFormat
                        .toFirstByte(userDto.getCardFirstByte());

                rfidNumberFormat = new RfidNumberFormat(format, firstByte);
            }

            if (!rfidNumberFormat.isNumberValid(cardNumber)) {
                return createError("msg-card-number-invalid", cardNumber);
            }

            normalizedCardNumber =
                    rfidNumberFormat.getNormalizedNumber(cardNumber);
        }

        this.assocPrimaryCardNumber(jpaUser, normalizedCardNumber);

        /*
         * ID Number.
         */
        if (StringUtils.isNotBlank(idNumber)) {

            final int lengthMin = ConfigManager.instance()
                    .getConfigInt(Key.USER_ID_NUMBER_LENGTH_MIN);

            if (idNumber.length() < lengthMin) {
                return createError("msg-id-number-length-error",
                        String.valueOf(lengthMin));
            }
        }

        this.assocPrimaryIdNumber(jpaUser, idNumber);

        /*
         *
         */
        jpaUser.setAdmin(userDto.getAdmin());
        jpaUser.setPerson(userDto.getPerson());
        jpaUser.setFullName(userDto.getFullName());

        /*
         * As indication for all.
         */
        jpaUser.setDisabledPdfOut(userDto.getDisabled());
        jpaUser.setDisabledPrintIn(userDto.getDisabled());
        jpaUser.setDisabledPrintOut(userDto.getDisabled());

        /*
         *
         */
        if (isNewInternalUser) {

            userDAO().create(jpaUser);

            if (hasPIN || hasUuid) {
                /*
                 * For a new User a create (persist()) is needed first, cause we
                 * need the generated primary key to encrypt the PIN / UUID.
                 */
                if (hasPIN) {
                    this.encryptStoreUserAttr(jpaUser, UserAttrEnum.PIN, pin);
                }
                if (hasUuid) {
                    this.encryptStoreUserAttr(jpaUser, UserAttrEnum.UUID, uuid);
                }

                userDAO().update(jpaUser);
            }

        } else {

            if (!hasPIN) {
                this.removeUserAttr(jpaUser, UserAttrEnum.PIN);
            }

            if (!hasUuid) {
                this.removeUserAttr(jpaUser, UserAttrEnum.UUID);
            }

            userDAO().update(jpaUser);

            if (!jpaUser.getPerson()) {
                ConfigManager.removeUserHomeDir(jpaUser.getUserId());
            }

            if (hasPIN) {
                this.encryptStoreUserAttr(jpaUser, UserAttrEnum.PIN, pin);
            }

            if (hasUuid) {
                this.encryptStoreUserAttr(jpaUser, UserAttrEnum.UUID, uuid);
            }
        }

        /*
         * Accounting.
         */
        final UserAccountingDto accountingDto = userDto.getAccounting();

        if (accountingDto != null) {

            final AbstractJsonRpcMethodResponse rsp = accountingService()
                    .setUserAccounting(jpaUser, accountingDto);

            if (rsp.isError()) {
                return rsp;
            }
        }

        /*
         * ACL Roles.
         */
        final Map<ACLRoleEnum, Boolean> aclRoles = userDto.getAclRoles();

        if (aclRoles != null) {
            setAclRoles(userAttrDAO(), jpaUser, aclRoles);
        }

        /*
         * Re-initialize Member Card information.
         */
        if (isNewInternalUser) {
            MemberCard.instance().init();
        }

        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Sets the ACL roles of a user.
     *
     * @param daoAttr
     *            The {@link UserAttrDao}.
     * @param user
     *            The user.
     * @param aclRoles
     *            The ACL roles.
     * @throws IOException
     *             When JSON errors.
     */
    private static void setAclRoles(final UserAttrDao daoAttr, final User user,
            final Map<ACLRoleEnum, Boolean> aclRoles) throws IOException {

        final String jsonRoles;

        if (aclRoles.isEmpty()) {
            jsonRoles = null;
        } else {
            jsonRoles = JsonHelper.stringifyObject(aclRoles);
        }

        final UserAttrEnum attrEnum = UserAttrEnum.ACL_ROLES;

        UserAttr attr = daoAttr.findByName(user, attrEnum);

        if (attr == null) {
            if (jsonRoles != null) {
                attr = new UserAttr();
                attr.setUser(user);
                attr.setName(attrEnum.getName());
                attr.setValue(jsonRoles);
                daoAttr.create(attr);
            }
        } else if (jsonRoles == null) {
            daoAttr.delete(attr);
        } else if (!attr.getValue().equals(jsonRoles)) {
            attr.setValue(jsonRoles);
            daoAttr.update(attr);
        }
    }

    @Override
    public AbstractJsonRpcMethodResponse deleteUser(final String userIdToDelete)
            throws IOException {

        User user = userDAO().lockByUserId(userIdToDelete);

        if (user == null) {
            return createError("msg-user-not-found", userIdToDelete);

        }

        this.performLogicalDelete(user);

        userDAO().update(user);

        MemberCard.instance().init();

        long nBytes = 0L;

        try {
            nBytes = ConfigManager.getUserHomeDirSize(userIdToDelete);
            ConfigManager.removeUserHomeDir(userIdToDelete);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return createError("msg-user-delete-safepages-error",
                    userIdToDelete);
        }

        if (nBytes == 0) {
            return createOkResult("msg-user-deleted-ok");
        }
        return createOkResult("msg-user-deleted-ok-inc-safepages",
                NumberUtil.humanReadableByteCount(nBytes, true));
    }

    /**
     * Encrypts the user password.
     *
     * @param userid
     * @param password
     * @return
     */
    private static String encryptUserPassword(final String userid,
            final String password) {
        return CryptoUser.getHashedUserPassword(userid, password);
    }

    /**
     * Checks if Internal User Password is valid.
     *
     * @param plainPassword
     *            The password in plain text.
     * @return {@code null} if valid.
     */
    private JsonRpcMethodError
            validateInternalUserPassword(final String plainPassword) {
        final int minPwLength = ConfigManager.instance()
                .getConfigInt(Key.INTERNAL_USERS_PW_LENGTH_MIN);

        if (plainPassword == null || plainPassword.length() < minPwLength) {
            return createError("msg-password-length-error",
                    String.valueOf(minPwLength));
        }
        return null;
    }

    /**
     * Checks if User UUID is valid.
     *
     * @param uuid
     *            The UUID.
     * @return {@code null} if valid.
     */
    private JsonRpcMethodError validateUserUuid(final String uuid) {

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            return createError("msg-user-uuid-invalid");
        }
        return null;
    }

    /**
     * Checks if User PIN is valid.
     *
     * @param pin
     *            The PIN.
     * @return {@code null} if valid.
     */
    private JsonRpcMethodError validateUserPin(final String pin) {

        JsonRpcMethodError methodError = null;

        final int lengthMin =
                ConfigManager.instance().getConfigInt(Key.USER_PIN_LENGTH_MIN);

        final int lengthMax =
                ConfigManager.instance().getConfigInt(Key.USER_PIN_LENGTH_MAX);

        if (!StringUtils.isNumeric(pin)) {

            methodError = createError("msg-user-pin-not-numeric");

        } else if (pin.length() < lengthMin
                || (lengthMax > 0 && pin.length() > lengthMax)) {

            if (lengthMin == lengthMax) {

                methodError = createError("msg-user-pin-length-error",
                        String.valueOf(lengthMin));

            } else if (lengthMax == 0) {

                methodError = createError("msg-user-pin-length-error-min",
                        String.valueOf(lengthMin));

            } else {

                methodError = createError("msg-user-pin-length-error-min-max",
                        String.valueOf(lengthMin), String.valueOf(lengthMax));
            }

        } else {
            methodError = null;
        }

        return methodError;
    }

    @Override
    public AbstractJsonRpcMethodResponse listUsers(final Integer startIndex,
            final Integer itemsPerPage) throws IOException {

        final UserDao.ListFilter filter = new UserDao.ListFilter();

        filter.setDeleted(Boolean.FALSE);

        final List<User> list = userDAO().getListChunk(filter, startIndex,
                itemsPerPage, UserDao.Field.USERID, true);

        final List<UserDto> items = new ArrayList<>();

        for (final User user : list) {

            final UserDto dto = new UserDto();

            dto.setUserName(user.getUserId());
            dto.setFullName(user.getFullName());
            dto.setAdmin(user.getAdmin());
            dto.setPerson(user.getPerson());
            dto.setCard(getPrimaryIdNumber(user));
            dto.setEmail(getPrimaryEmailAddress(user));
            dto.setId(getPrimaryIdNumber(user));

            items.add(dto);
        }

        final ResultListUsers data = new ResultListUsers();
        data.setItems(items);

        return JsonRpcMethodResult.createResult(data);
    }

    @Override
    public boolean isCardRegistered(final String cardNumber) {
        return this.findUserByCardNumber(cardNumber) != null;
    }

    @Override
    public AbstractJsonRpcMethodResponse addInternalUser(final UserDto dto)
            throws IOException {

        User user = userDAO().findActiveUserByUserId(dto.getUserName());
        if (user == null) {
            return setUser(dto, true);
        } else {
            /*
             * INVARIANT: Internal User only.
             */
            if (!user.getInternal()) {
                return createError("msg-user-not-internal", dto.getUserName());
            }
            return setUserProperties(new UserPropertiesDto(dto));
        }
    }

    /**
     * Encrypts and writes the {@link UserAttr} value to the database.
     *
     * @param jpaUser
     *            The {@link User}.
     * @param attrEnum
     *            The {@link UserAttrEnum}.
     * @param plainValue
     *            The plain (unencrypted) value.
     */
    private void encryptStoreUserAttr(final User jpaUser,
            final UserAttrEnum attrEnum, final String plainValue) {
        this.setUserAttrValue(jpaUser, attrEnum,
                CryptoUser.encryptUserAttr(jpaUser.getId(), plainValue));
    }

    /**
     * Encrypts and stores the Internal User Password to the database.
     *
     * @param jpaUser
     *            The User.
     * @param plainPassword
     *            Password in plain text.
     */
    private void storeInternalUserPassword(User jpaUser, String plainPassword) {

        this.setUserAttrValue(jpaUser, UserAttrEnum.INTERNAL_PASSWORD,
                encryptUserPassword(jpaUser.getUserId(), plainPassword));
    }

    /**
     * Encrypts and adds the Internal User Password attribute to the User
     * object.
     *
     * @param jpaUser
     *            The User.
     * @param plainPassword
     *            Password in plain text.
     */
    private void addInternalUserPassword(User jpaUser, String plainPassword) {

        this.addUserAttr(jpaUser, UserAttrEnum.INTERNAL_PASSWORD,
                encryptUserPassword(jpaUser.getUserId(), plainPassword));
    }

    /**
     * Replaces the secondary email addresses by new ones and removes the
     * obsolete ones.
     *
     * @param jpaUser
     * @param secondaryEmailList
     */
    private JsonRpcMethodError setSecondaryEmail(boolean isNewInternalUser,
            String primaryEmail, User jpaUser,
            List<UserEmailDto> secondaryEmailList) {

        final EmailValidator validator = new EmailValidator();

        /*
         * Sorted map of new secondary e-mails.
         */
        final SortedMap<String, UserEmailDto> sortedUserEmailDto =
                new TreeMap<>();

        /*
         * Validate.
         */
        for (UserEmailDto dto : secondaryEmailList) {

            final String address = dto.getAddress().trim();

            /*
             * INVARIANT: secondary email address MUST be different from primary
             * email.
             */
            if (StringUtils.isNotBlank(primaryEmail)
                    && address.equalsIgnoreCase(primaryEmail)) {
                return createError("msg-user-email-used-as-primary", address);
            }

            /*
             * INVARIANT: email address MUST be valid.
             */
            if (!validator.validate(address)) {
                return createError("msg-email-invalid", address);
            }

            final String key = address.toLowerCase();

            final User jpaUserEmailDuplicate = this.findUserByEmail(key);

            if (jpaUserEmailDuplicate != null) {

                if (isNewInternalUser) {
                    /*
                     * INVARIANT: email address MUST not yet exist.
                     */
                    return createError("msg-user-duplicate-user-email", address,
                            jpaUserEmailDuplicate.getUserId());

                } else if (!jpaUserEmailDuplicate.getUserId()
                        .equals(jpaUser.getUserId())) {
                    /*
                     * INVARIANT: email address MUST not be associated to
                     * another user.
                     */
                    return createError("msg-user-duplicate-user-email", address,
                            jpaUserEmailDuplicate.getUserId());
                }
            }

            sortedUserEmailDto.put(key, dto);
        }

        /*
         * Get (lazy initialize) the current email list.
         */
        List<UserEmail> userEmailList = jpaUser.getEmails();

        if (userEmailList == null) {
            userEmailList = new ArrayList<>();
            jpaUser.setEmails(userEmailList);
        }

        /*
         * Sorted map of current secondary UserEmail objects
         */
        final SortedMap<String, UserEmail> sortedUserEmail = new TreeMap<>();

        int indexNumberWlk = UserEmailDao.INDEX_NUMBER_PRIMARY_EMAIL;

        for (UserEmail userEmail : userEmailList) {

            if (!userEmailDAO().isPrimaryEmail(userEmail)) {

                /*
                 * Save the HIGHEST current index number, to use as base for new
                 * entries.
                 *
                 * IMPORTANT: we cannot delete a UserEmail row and re-use its
                 * index number in one (1) transaction, cause this causes a
                 * unique index violation (on "user_id", "index_number").
                 */
                if (indexNumberWlk < userEmail.getIndexNumber().intValue()) {
                    indexNumberWlk = userEmail.getIndexNumber().intValue();
                }

                sortedUserEmail.put(userEmail.getAddress().toLowerCase(),
                        userEmail);
            }
        }

        /*
         * Balanced line: init
         */
        final Iterator<Entry<String, UserEmail>> iterUserEmail =
                sortedUserEmail.entrySet().iterator();

        final Iterator<Entry<String, UserEmailDto>> iterUserEmailDto =
                sortedUserEmailDto.entrySet().iterator();

        /*
         * Balanced line: initial read.
         */
        UserEmailDto dtoEmailWlk = null;
        UserEmail userEmailWlk = null;

        if (iterUserEmail.hasNext()) {
            userEmailWlk = iterUserEmail.next().getValue();
        }

        if (iterUserEmailDto.hasNext()) {
            dtoEmailWlk = iterUserEmailDto.next().getValue();
        }

        boolean emailChanges = false;

        /*
         * Balanced line: process.
         */
        while (userEmailWlk != null || dtoEmailWlk != null) {

            boolean readNextUserMail = false;
            boolean readNextDto = false;

            if (dtoEmailWlk != null && userEmailWlk != null) {

                final String keyUserEmail =
                        userEmailWlk.getAddress().toLowerCase();

                final String keyDto = dtoEmailWlk.getAddress().toLowerCase();

                final int compare = keyUserEmail.compareToIgnoreCase(keyDto);

                if (compare < 0) {
                    /*
                     * keyUserEmail < keyDto : Remove UserEmail.
                     */
                    userEmailDAO().delete(userEmailWlk);
                    userEmailList.remove(userEmailWlk);

                    readNextUserMail = true;

                    emailChanges = true;

                } else if (compare > 0) {
                    /*
                     * keyUserEmail > keyDto : Add UserEmail from dto
                     */
                    ++indexNumberWlk;

                    final UserEmail userEmail = new UserEmail();

                    userEmail.setUser(jpaUser);
                    userEmail.setIndexNumber(Integer.valueOf(indexNumberWlk));
                    userEmail.setAddress(keyDto);
                    userEmail.setDisplayName(keyDto);

                    userEmailDAO().create(userEmail);
                    userEmailList.add(userEmail);

                    readNextDto = true;
                    emailChanges = true;

                } else {
                    /*
                     * keyUserEmail == keyDto : no update.
                     */
                    readNextDto = true;
                    readNextUserMail = true;
                }

            } else if (dtoEmailWlk != null) {
                /*
                 * Add UserEmail.
                 */
                ++indexNumberWlk;

                final String keyDto = dtoEmailWlk.getAddress().toLowerCase();
                final UserEmail userEmail = new UserEmail();

                userEmail.setUser(jpaUser);
                userEmail.setIndexNumber(Integer.valueOf(indexNumberWlk));
                userEmail.setAddress(keyDto);
                userEmail.setDisplayName(keyDto);

                userEmailDAO().create(userEmail);
                userEmailList.add(userEmail);

                readNextDto = true;
                emailChanges = true;

            } else {
                /*
                 * Remove UserEmail.
                 */
                userEmailDAO().delete(userEmailWlk);
                userEmailList.remove(userEmailWlk);

                readNextUserMail = true;
                emailChanges = true;
            }

            /*
             * Next read(s).
             */
            if (readNextUserMail) {
                userEmailWlk = null;
                if (iterUserEmail.hasNext()) {
                    userEmailWlk = iterUserEmail.next().getValue();
                }
            }
            if (readNextDto) {
                dtoEmailWlk = null;
                if (iterUserEmailDto.hasNext()) {
                    dtoEmailWlk = iterUserEmailDto.next().getValue();
                }
            }

        } // end-while

        if (!emailChanges) {
            // TODO: lazy re-align the index numbers.

        }

        return null;
    }

    @Override
    public User findUserByCardNumber(final String cardNumber) {

        User user = null;

        if (StringUtils.isNotBlank(cardNumber)) {

            final UserCard card =
                    userCardDAO().findByCardNumber(cardNumber.toLowerCase());

            if (card != null) {
                user = card.getUser();
            }
        }
        return user;
    }

    @Override
    public boolean hasAssocPrimaryCard(final User user) {

        final List<UserCard> list = user.getCards();

        if (list != null) {

            for (final UserCard obj : list) {

                if (userCardDAO().isPrimaryCard(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public User findUserByEmail(final String emailAddress) {

        User user = null;

        if (StringUtils.isNotBlank(emailAddress)) {

            final UserEmail userEmail =
                    userEmailDAO().findByEmail(emailAddress.toLowerCase());

            if (userEmail != null) {
                user = userEmail.getUser();
            }
        }
        return user;
    }

    @Override
    public boolean hasAssocPrimaryEmail(final User user) {

        final List<UserEmail> list = user.getEmails();

        if (list != null) {
            for (final UserEmail obj : list) {
                if (userEmailDAO().isPrimaryEmail(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAssocSecondaryEmail(final User user) {

        final List<UserEmail> list = user.getEmails();

        if (list != null) {

            for (final UserEmail obj : list) {

                if (!userEmailDAO().isPrimaryEmail(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasAssocPrimaryNumber(final User user) {

        final List<UserNumber> list = user.getIdNumbers();

        if (list != null) {

            for (final UserNumber obj : list) {

                if (userNumberDAO().isPrimaryNumber(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public User findUserByNumber(final String number) {

        User user = null;

        if (StringUtils.isNotBlank(number)) {

            final UserNumber userNumber = userNumberDAO().findByNumber(number);

            if (userNumber != null) {
                user = userNumber.getUser();
            }
        }
        return user;
    }

    /**
     * Gets the {@link UserAttr} from {@link User#getAttributes()} list.
     *
     * @param user
     *            The {@link User}.
     * @param attrEnum
     *            The {@link UserAttrEnum} to search for.
     * @return The {@link UserAttr} or {@code null} when not found.
     */
    private UserAttr getUserAttr(final User user, final UserAttrEnum attrEnum) {

        if (user.getAttributes() != null) {
            for (final UserAttr attr : user.getAttributes()) {
                if (attr.getName().equals(attrEnum.getName())) {
                    return attr;
                }
            }
        }
        return null;
    }

    @Override
    public String getUserAttrValue(final User user,
            final UserAttrEnum attrEnum) {

        if (user.getAttributes() != null) {
            for (final UserAttr attr : user.getAttributes()) {
                if (attr.getName().equals(attrEnum.getName())) {
                    if (attrEnum == UserAttrEnum.UUID) {
                        return CryptoUser.decryptUserAttr(user.getId(),
                                attr.getValue());
                    }
                    return attr.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public User findUserByNumberUuid(final String number, final UUID uuid) {

        final User user = this.findUserByNumber(number);

        if (user != null && user.getAttributes() != null) {

            final UserAttr attr = this.getUserAttr(user, UserAttrEnum.UUID);

            if (attr != null) {

                final String encryptedUuid = CryptoUser
                        .encryptUserAttr(user.getId(), uuid.toString());

                if (attr.getValue().equals(encryptedUuid)) {
                    return user;
                }
            }
        }
        return null;
    }

    @Override
    public void assocPrimaryCardNumber(final User user,
            final String primaryCardNumber) {

        List<UserCard> cardList = user.getCards();

        if (StringUtils.isBlank(primaryCardNumber)) {
            /*
             * Find and remove the primary card.
             */
            if (cardList != null) {

                final Iterator<UserCard> iter = cardList.iterator();

                while (iter.hasNext()) {

                    final UserCard card = iter.next();

                    if (userCardDAO().isPrimaryCard(card)) {
                        userCardDAO().delete(card);
                        iter.remove();
                        break;
                    }
                }
            }

        } else {

            /*
             * Lazy create the list...
             */
            if (cardList == null) {
                cardList = new ArrayList<>();
                user.setCards(cardList);
            }

            /*
             * Find the primary card, and remove any non-primary card having the
             * same number.
             */
            UserCard primaryCard = null;

            final Iterator<UserCard> iter = cardList.iterator();

            while (iter.hasNext()) {

                final UserCard card = iter.next();

                if (userCardDAO().isPrimaryCard(card)) {
                    primaryCard = card;
                } else if (card.getNumber()
                        .equalsIgnoreCase(primaryCardNumber)) {
                    userCardDAO().delete(card);
                    iter.remove();
                }
            }

            /*
             * Create or update.
             */
            if (primaryCard == null) {

                primaryCard = new UserCard();
                primaryCard.setUser(user);
                userCardDAO().assignPrimaryCard(primaryCard);

                cardList.add(primaryCard);
            }

            primaryCard.setNumber(primaryCardNumber.toLowerCase());
        }

    }

    @Override
    public void assocPrimaryEmail(final User user,
            final String primaryEmailAddress) {

        List<UserEmail> emailList = user.getEmails();

        if (StringUtils.isBlank(primaryEmailAddress)) {

            /*
             * Find and remove the primary email.
             */
            if (emailList != null) {

                final Iterator<UserEmail> iter = emailList.iterator();

                while (iter.hasNext()) {

                    final UserEmail email = iter.next();

                    if (userEmailDAO().isPrimaryEmail(email)) {
                        userEmailDAO().delete(email);
                        iter.remove();
                        break;
                    }
                }
            }

        } else {

            /*
             * Lazy create the list...
             */
            if (emailList == null) {
                emailList = new ArrayList<>();
                user.setEmails(emailList);
            }

            /*
             * Find the primary email, and remove any non-primary email having
             * the same email address.
             */
            UserEmail primaryEmail = null;

            final Iterator<UserEmail> iter = emailList.iterator();

            while (iter.hasNext()) {

                final UserEmail email = iter.next();

                if (userEmailDAO().isPrimaryEmail(email)) {
                    primaryEmail = email;
                } else if (email.getAddress()
                        .equalsIgnoreCase(primaryEmailAddress)) {
                    userEmailDAO().delete(email);
                    iter.remove();
                }
            }

            /*
             * Create or update.
             */
            if (primaryEmail == null) {

                primaryEmail = new UserEmail();
                primaryEmail.setUser(user);
                userEmailDAO().assignPrimaryEmail(primaryEmail);

                emailList.add(primaryEmail);
            }

            primaryEmail.setAddress(primaryEmailAddress.toLowerCase());
        }

    }

    @Override
    public void assocPrimaryIdNumber(final User user,
            final String primaryIdNumber) {

        List<UserNumber> numberList = user.getIdNumbers();

        if (StringUtils.isBlank(primaryIdNumber)) {

            /*
             * Find and remove the primary ID number.
             */
            if (numberList != null) {

                final Iterator<UserNumber> iter = numberList.iterator();

                while (iter.hasNext()) {

                    final UserNumber number = iter.next();

                    if (userNumberDAO().isPrimaryNumber(number)) {
                        userNumberDAO().delete(number);
                        iter.remove();
                        break;
                    }
                }
            }

        } else {

            /*
             * Lazy create the list...
             */
            if (numberList == null) {
                numberList = new ArrayList<>();
                user.setIdNumbers(numberList);
            }

            /*
             * Find the primary ID number, and remove any non-primary number
             * having the same number.
             */
            UserNumber primaryNumber = null;

            final Iterator<UserNumber> iter = numberList.iterator();

            while (iter.hasNext()) {

                UserNumber number = iter.next();

                if (userNumberDAO().isPrimaryNumber(number)) {
                    primaryNumber = number;
                } else if (number.getNumber()
                        .equalsIgnoreCase(primaryIdNumber)) {
                    userNumberDAO().delete(number);
                    iter.remove();
                }
            }

            /*
             * Create or update.
             */
            if (primaryNumber == null) {

                primaryNumber = new UserNumber();
                primaryNumber.setUser(user);
                userNumberDAO().assignPrimaryNumber(primaryNumber);

                numberList.add(primaryNumber);
            }

            primaryNumber.setNumber(primaryIdNumber.toLowerCase());
        }

    }

    @Override
    public String getPrimaryCardNumber(final User user) {

        String cardNumber = "";

        final List<UserCard> cardList = user.getCards();

        if (cardList != null) {

            final Iterator<UserCard> iter = cardList.iterator();

            while (iter.hasNext()) {

                final UserCard card = iter.next();

                if (userCardDAO().isPrimaryCard(card)) {
                    cardNumber = card.getNumber();
                    break;
                }
            }
        }
        return cardNumber;
    }

    @Override
    public String getPrimaryIdNumber(final User user) {

        String idNumber = "";

        final List<UserNumber> numberList = user.getIdNumbers();

        if (numberList != null) {

            final Iterator<UserNumber> iter = numberList.iterator();

            while (iter.hasNext()) {

                final UserNumber number = iter.next();

                if (userNumberDAO().isPrimaryNumber(number)) {
                    idNumber = number.getNumber();
                    break;
                }
            }
        }
        return idNumber;
    }

    @Override
    public String getPrimaryEmailAddress(final User user) {

        String address = null;

        final List<UserEmail> emailList = user.getEmails();

        if (emailList != null) {

            final Iterator<UserEmail> iter = emailList.iterator();

            while (iter.hasNext()) {

                final UserEmail mail = iter.next();

                if (userEmailDAO().isPrimaryEmail(mail)) {
                    address = mail.getAddress();
                    break;
                }
            }
        }
        return address;
    }

    @Override
    public void addPrintInJobTotals(final User user, final Date jobDate,
            final int jobPages, final long jobBytes) {

        user.setNumberOfPrintInJobs(
                user.getNumberOfPrintInJobs().intValue() + 1);
        user.setNumberOfPrintInPages(
                user.getNumberOfPrintInPages().intValue() + jobPages);
        user.setNumberOfPrintInBytes(
                user.getNumberOfPrintInBytes().longValue() + jobBytes);

        user.setLastUserActivity(jobDate);

    }

    @Override
    public void addPdfOutJobTotals(final User user, final Date jobDate,
            final int jobPages, final long jobBytes) {

        user.setNumberOfPdfOutJobs(user.getNumberOfPdfOutJobs().intValue() + 1);
        user.setNumberOfPdfOutPages(
                user.getNumberOfPdfOutPages().intValue() + jobPages);
        user.setNumberOfPdfOutBytes(
                user.getNumberOfPdfOutBytes().longValue() + jobBytes);

        user.setLastUserActivity(jobDate);
    }

    @Override
    public void addPrintOutJobTotals(final User user, final Date jobDate,
            final int jobPages, final int jobSheets, final long jobEsu,
            final long jobBytes) {

        user.setNumberOfPrintOutJobs(
                user.getNumberOfPrintOutJobs().intValue() + 1);
        user.setNumberOfPrintOutPages(
                user.getNumberOfPrintOutPages().intValue() + jobPages);
        user.setNumberOfPrintOutSheets(
                user.getNumberOfPrintOutSheets().intValue() + jobSheets);
        user.setNumberOfPrintOutEsu(
                user.getNumberOfPrintOutEsu().intValue() + jobEsu);

        user.setNumberOfPrintOutBytes(
                user.getNumberOfPrintOutBytes().longValue() + jobBytes);

        user.setLastUserActivity(jobDate);
    }

    @Override
    public boolean isUserFullyDisabled(final User user, final Date refDate) {

        return isUserPdfOutDisabled(user, refDate)
                && isUserPrintInDisabled(user, refDate)
                && isUserPrintOutDisabled(user, refDate);
    }

    @Override
    public boolean isUserPrintInDisabled(final User user, final Date refDate) {
        return isDisabled(user.getDisabledPrintIn(), refDate,
                user.getDisabledPrintInUntil());
    }

    @Override
    public boolean isUserPdfOutDisabled(final User user, final Date refDate) {
        return isDisabled(user.getDisabledPdfOut(), refDate,
                user.getDisabledPdfOutUntil());
    }

    @Override
    public boolean isUserPrintOutDisabled(final User user, final Date refDate) {
        return isDisabled(user.getDisabledPrintOut(), refDate,
                user.getDisabledPrintOutUntil());
    }

    /**
     * Checks if a disabled state is active on reference date.
     *
     * @param disabled
     *            The disabled state.
     * @param onDate
     *            The reference date.
     * @param disabledUtil
     *            The end date if the disabled state. {@code null} is no end
     *            date for disabled state is present.
     * @return {@code true} if disabled.
     */
    private boolean isDisabled(final boolean disabled, final Date onDate,
            final Date disabledUtil) {

        if (!disabled || disabledUtil == null) {
            return disabled;
        }
        return disabled && DateUtils.truncatedCompareTo(onDate, disabledUtil,
                Calendar.DAY_OF_MONTH) < 0;
    }

    /**
     * Removes all User email addresses from the database.
     *
     * @param user
     *            The {@link User}.
     */
    private void removeAllEmails(final User user) {

        final List<UserEmail> emails = user.getEmails();

        if (emails != null) {
            for (final UserEmail email : emails) {
                userEmailDAO().delete(email);
            }
            emails.clear();
        }
    }

    /**
     * Removes all User cards from the database.
     *
     * @param user
     *            The {@link User}.
     */
    private void removeAllCards(final User user) {

        final List<UserCard> cards = user.getCards();

        if (cards != null) {
            for (final UserCard card : cards) {
                userCardDAO().delete(card);
            }
            cards.clear();
        }
    }

    /**
     * Removes all User ID Numbers from the database.
     *
     * @param user
     *            The {@link User}.
     */
    private void removeAllIdNumbers(final User user) {

        final List<UserNumber> numbers = user.getIdNumbers();

        if (numbers != null) {
            for (final UserNumber number : numbers) {
                userNumberDAO().delete(number);
            }
            numbers.clear();
        }
    }

    @Override
    public void performLogicalDelete(final User user) {

        final Date trxDate = ServiceContext.getTransactionDate();

        user.setDeleted(true);
        user.setDeletedDate(trxDate);
        user.setModifiedBy(ServiceContext.getActor());
        user.setModifiedDate(trxDate);

        removeAllCards(user);
        removeAllIdNumbers(user);
        removeAllEmails(user);

        final List<UserAccount> userAccountList = user.getAccounts();

        if (userAccountList != null) {

            for (final UserAccount userAccount : userAccountList) {

                final Account account = userAccount.getAccount();

                final AccountTypeEnum accountType =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountType != Account.AccountTypeEnum.SHARED) {

                    account.setDeleted(true);
                    account.setDeletedDate(trxDate);
                    account.setModifiedBy(ServiceContext.getActor());
                    account.setModifiedDate(trxDate);
                }
            }
        }
    }

    @Override
    public UserAttr removeUserAttr(final User user, final UserAttrEnum name) {

        UserAttr removedAttr = null;

        if (user.getAttributes() != null) {

            final String strName = name.getName();

            final Iterator<UserAttr> iter = user.getAttributes().iterator();

            while (iter.hasNext()) {
                final UserAttr attr = iter.next();
                if (attr.getName().equals(strName)) {
                    removedAttr = attr;
                    iter.remove();
                    break;
                }
            }
        }

        if (removedAttr != null) {
            userAttrDAO().delete(removedAttr);
        }
        return removedAttr;
    }

    @Override
    public void addUserAttr(final User user, final UserAttrEnum name,
            final String value) {

        List<UserAttr> list = user.getAttributes();

        if (list == null) {
            list = new ArrayList<>();
            user.setAttributes(list);
        }

        final UserAttr attr = new UserAttr();

        attr.setUser(user);
        attr.setName(name.getName());
        attr.setValue(value);

        list.add(attr);
    }

    @Override
    public UUID lazyAddUserAttrUuid(final User user) {

        final List<UserAttr> list = user.getAttributes();

        if (list != null) {

            final UserAttr attr = this.getUserAttr(user, UserAttrEnum.UUID);

            if (attr != null) {
                final String decryptedUuid = CryptoUser
                        .decryptUserAttr(user.getId(), attr.getValue());
                return UUID.fromString(decryptedUuid);
            }
        }

        final UUID uuid = UUID.randomUUID();

        final String encryptedUuid =
                CryptoUser.encryptUserAttr(user.getId(), uuid.toString());

        this.addUserAttr(user, UserAttrEnum.UUID, encryptedUuid);
        this.setUserAttrValue(user, UserAttrEnum.UUID, encryptedUuid);

        return uuid;
    }

    @Override
    public void setUserAttrValue(final User user, final UserAttrEnum attrEnum,
            final String attrValue) {

        this.setUserAttrValue(user, userAttrDAO().findByName(user, attrEnum),
                attrEnum, attrValue);
    }

    /**
     * Creates or updates a {@link UserAttr} value to the database.
     *
     * @param user
     *            The {@link User}.
     * @param userAttr
     *            The {@link UserAttr}. When {@code null} the attribute is
     *            created.
     * @param attrEnum
     *            The {@link UserAttrEnum} (used when {@link UserAttr} is
     *            {@code null}).
     * @param attrValue
     *            The attribute value (used when {@link UserAttr} is
     *            {@code null}).
     */
    private void setUserAttrValue(final User user, final UserAttr userAttr,
            final UserAttrEnum attrEnum, final String attrValue) {

        if (userAttr == null) {

            final UserAttr attrNew = new UserAttr();

            attrNew.setUser(user);

            attrNew.setName(attrEnum.getName());
            attrNew.setValue(attrValue);

            userAttrDAO().create(attrNew);

        } else {

            userAttr.setValue(attrValue);

            userAttrDAO().update(userAttr);
        }
    }

    @Override
    public String findUserAttrValue(final User user, final UserAttrEnum name) {

        final UserAttr attr = userAttrDAO().findByName(user, name);

        if (attr == null) {
            return null;
        }

        return attr.getValue();
    }

    @Override
    public void logPrintIn(final User user, final Date jobTime,
            final Integer jobPages, final Long jobBytes) {

        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_IN_ROLLING_MONTH_BYTES,
                jobTime, jobBytes);
        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_IN_ROLLING_WEEK_BYTES,
                jobTime, jobBytes);

        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_IN_ROLLING_MONTH_PAGES,
                jobTime, jobPages);
        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_IN_ROLLING_WEEK_PAGES,
                jobTime, jobPages);
    }

    @Override
    public void logPrintOut(final User user, final Date jobTime,
            final Integer jobPages, final Integer jobSheets, final Long jobEsu,
            final Long jobBytes) {

        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_MONTH_BYTES,
                jobTime, jobBytes);
        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_WEEK_BYTES,
                jobTime, jobBytes);

        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_MONTH_PAGES,
                jobTime, jobPages);
        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_WEEK_PAGES,
                jobTime, jobPages);

        addTimeSeriesDataPoint(user,
                UserAttrEnum.PRINT_OUT_ROLLING_MONTH_SHEETS, jobTime,
                jobSheets);
        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_WEEK_SHEETS,
                jobTime, jobSheets);

        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_MONTH_ESU,
                jobTime, jobEsu);
        addTimeSeriesDataPoint(user, UserAttrEnum.PRINT_OUT_ROLLING_WEEK_ESU,
                jobTime, jobEsu);

    }

    @Override
    public void logPdfOut(final User user, final Date jobTime,
            final Integer jobPages, final Long jobBytes) {

        addTimeSeriesDataPoint(user, UserAttrEnum.PDF_OUT_ROLLING_MONTH_BYTES,
                jobTime, jobBytes);
        addTimeSeriesDataPoint(user, UserAttrEnum.PDF_OUT_ROLLING_WEEK_BYTES,
                jobTime, jobBytes);

        addTimeSeriesDataPoint(user, UserAttrEnum.PDF_OUT_ROLLING_MONTH_PAGES,
                jobTime, jobPages);
        addTimeSeriesDataPoint(user, UserAttrEnum.PDF_OUT_ROLLING_WEEK_PAGES,
                jobTime, jobPages);

    }

    /**
     * Creates or updates a {@link UserAttr} time series data {@link Long}
     * point.
     *
     * @param user
     *            The {@link User}.
     * @param name
     *            The {@link UserAttrEnum}.
     * @param observationTime
     *            The observation time.
     * @param observation
     *            The observation value.
     */
    private void addTimeSeriesDataPoint(final User user,
            final UserAttrEnum name, final Date observationTime,
            final Long observation) {

        JsonRollingTimeSeries<Long> statsPages = null;

        if (name == UserAttrEnum.PRINT_IN_ROLLING_MONTH_BYTES
                || name == UserAttrEnum.PDF_OUT_ROLLING_MONTH_BYTES
                || name == UserAttrEnum.PRINT_OUT_ROLLING_MONTH_ESU
                || name == UserAttrEnum.PRINT_OUT_ROLLING_MONTH_BYTES) {
            statsPages = new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                    MAX_TIME_SERIES_INTERVALS_MONTH, 0L);
        } else if (name == UserAttrEnum.PRINT_IN_ROLLING_WEEK_BYTES
                || name == UserAttrEnum.PDF_OUT_ROLLING_WEEK_BYTES
                || name == UserAttrEnum.PRINT_OUT_ROLLING_WEEK_ESU
                || name == UserAttrEnum.PRINT_OUT_ROLLING_WEEK_BYTES) {
            statsPages = new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                    MAX_TIME_SERIES_INTERVALS_WEEK, 0L);
        } else {
            throw new SpException("time series for attribute [" + name
                    + "] is not supported");
        }

        final UserAttr attr = userAttrDAO().findByName(user, name);

        String json = null;

        if (attr != null) {
            json = attr.getValue();
        }

        try {

            if (StringUtils.isNotBlank(json)) {
                statsPages.init(json);
            }

            statsPages.addDataPoint(observationTime, observation);
            this.setUserAttrValue(user, attr, name, statsPages.stringify());

        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Creates or updates a {@link UserAttr} time series data {@link Integer}
     * point.
     *
     * @param user
     *            The {@link User}.
     * @param name
     *            The {@link UserAttrEnum}.
     * @param observationTime
     *            The observation time.
     * @param observation
     *            The observation value.
     */
    private void addTimeSeriesDataPoint(final User user,
            final UserAttrEnum name, final Date observationTime,
            final Integer observation) {

        JsonRollingTimeSeries<Integer> statsPages = null;

        if (name == UserAttrEnum.PRINT_IN_ROLLING_MONTH_PAGES
                || name == UserAttrEnum.PDF_OUT_ROLLING_MONTH_PAGES
                || name == UserAttrEnum.PRINT_OUT_ROLLING_MONTH_PAGES
                || name == UserAttrEnum.PRINT_OUT_ROLLING_MONTH_SHEETS) {
            statsPages = new JsonRollingTimeSeries<>(TimeSeriesInterval.MONTH,
                    MAX_TIME_SERIES_INTERVALS_MONTH, 0);
        } else if (name == UserAttrEnum.PRINT_IN_ROLLING_WEEK_PAGES
                || name == UserAttrEnum.PDF_OUT_ROLLING_WEEK_PAGES
                || name == UserAttrEnum.PRINT_OUT_ROLLING_WEEK_PAGES
                || name == UserAttrEnum.PRINT_OUT_ROLLING_WEEK_SHEETS) {
            statsPages = new JsonRollingTimeSeries<>(TimeSeriesInterval.WEEK,
                    MAX_TIME_SERIES_INTERVALS_WEEK, 0);
        } else {
            throw new SpException("time series for attribute [" + name
                    + "] is not supported");
        }

        final UserAttr attr = userAttrDAO().findByName(user, name);

        String json = null;

        if (attr != null) {
            json = attr.getValue();
        }

        try {

            if (StringUtils.isNotBlank(json)) {
                statsPages.init(json);
            }

            statsPages.addDataPoint(observationTime, observation);
            this.setUserAttrValue(user, attr, name, statsPages.stringify());

        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }

    }

    @Override
    public File lazyUserHomeDir(final User user) throws IOException {

        final String uid = user.getUserId();
        final String homeDir = ConfigManager.getUserHomeDir(uid);
        final File fileHomeDir = new File(homeDir);

        if (!fileHomeDir.exists()) {

            final Date perfStartTime = PerformanceLogger.startTime();

            final DaoContext daoContext = ServiceContext.getDaoContext();

            daoContext.beginTransaction();

            try {
                userDAO().lock(user.getId());
                lazyUserHomeDir(uid);
            } finally {
                /*
                 * unlock
                 */
                daoContext.rollback();
            }

            PerformanceLogger.log(this.getClass(), "lazyUserHomeDir",
                    perfStartTime, user.getUserId());
        }
        return fileHomeDir;
    }

    @Override
    public User lazyInsertExternalUser(final IUserSource userSource,
            final String userId, final String userGroup) {

        final User user;

        if (userGroup.isEmpty()
                || userSource.isUserInGroup(userId, userGroup)) {

            final CommonUser commonUser = userSource.getUser(userId);

            if (commonUser != null) {

                user = userDAO().findActiveUserByUserIdInsert(
                        commonUser.createUser(),
                        ServiceContext.getTransactionDate(),
                        ServiceContext.getActor());

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            String.format("Lazy inserted user [%s].", userId));
                }

            } else {

                user = null;

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format(
                            "User [%s] NOT lazy inserted: not found.", userId));
                }
            }

        } else {

            user = null;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format(
                        "User [%s] NOT lazy inserted: not member of group [%s].",
                        userId, userGroup));
            }
        }

        return user;
    }

    @Override
    public void lazyUserHomeDir(final String uid) throws IOException {
        lazyCreateDir(new File(ConfigManager.getUserTempDir(uid)));
        lazyCreateDir(outboxService().getUserOutboxDir(uid));
    }

    /**
     *
     * @param dir
     * @throws IOException
     */
    private void lazyCreateDir(final File dir) throws IOException {

        if (!dir.exists()) {

            final FileSystem fs = FileSystems.getDefault();
            final Path p = fs.getPath(dir.getCanonicalPath());

            final Set<PosixFilePermission> permissions =
                    EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);

            final FileAttribute<Set<PosixFilePermission>> attr =
                    PosixFilePermissions.asFileAttribute(permissions);

            java.nio.file.Files.createDirectories(p, attr);
        }
    }

    @Override
    public PdfProperties getPdfProperties(final User user) throws Exception {

        PdfProperties props = null;
        String json = this.findUserAttrValue(user, UserAttrEnum.PDF_PROPS);
        try {
            if (json != null) {
                props = PdfProperties.create(json);
                props.getPw().decrypt();
            }
        } catch (Exception e) {
            /*
             * Be forgiving ...
             */
            json = null;
            LOGGER.warn("PDF Properties of user [" + user.getUserId()
                    + "] are reset, because: " + e.getMessage());
        }

        if (json == null) {
            props = new PdfProperties();
        }

        if (props.getDesc().getAuthor().isEmpty()) {
            props.getDesc().setAuthor(user.getFullName());
        }

        return props;
    }

    @Override
    public void setPdfProperties(final User user, final PdfProperties objProps)
            throws IOException {

        objProps.getPw().encrypt();

        this.setUserAttrValue(user, UserAttrEnum.PDF_PROPS,
                objProps.stringify());
    }

}
