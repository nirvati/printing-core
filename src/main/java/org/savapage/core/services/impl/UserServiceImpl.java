/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.OutOfBoundsException;
import org.savapage.core.PerformanceLogger;
import org.savapage.core.SpException;
import org.savapage.core.auth.YubiKeyOTP;
import org.savapage.core.community.MemberCard;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.GenericDao;
import org.savapage.core.dao.IAttrDao;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserEmailDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dao.helpers.PGPPubRingKeyDto;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserDto;
import org.savapage.core.dto.UserEmailDto;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.dto.UserPropertiesDto;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserCard;
import org.savapage.core.jpa.UserEmail;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.jpa.UserNumber;
import org.savapage.core.json.JobTicketProperties;
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
import org.savapage.core.users.conf.InternalGroupList;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.EmailValidator;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.NumberUtil;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPKeyID;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserServiceImpl extends AbstractService
        implements UserService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserServiceImpl.class);

    /** */
    private static final int MAX_USER_ID_NUMBER_GENERATE_TRIALS = 10;

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
         * INVARIANT: username MUST NOT be reserved.
         */
        if (isReserverUserId(userid)) {
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
        final String yubiKeyPubId = dto.getYubiKeyPubId();

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

            if (!EmailValidator.validate(primaryEmail)) {
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

            boolean doUpdate;

            if (dto.getKeepId()) {
                doUpdate = !hasAssocPrimaryNumber(jpaUser);
            } else {
                doUpdate = true;
            }

            if (doUpdate) {

                final int lengthMin = ConfigManager.instance()
                        .getConfigInt(Key.USER_ID_NUMBER_LENGTH_MIN);

                if (idNumber.length() < lengthMin) {
                    /*
                     * INVARIANT: ID Number format MUST be valid.
                     */
                    return createError("msg-id-number-length-error",
                            String.valueOf(lengthMin));
                }

                final User jpaUserDuplicate =
                        this.findUserByNumber(dto.getId());

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
        }

        /*
         * YubiKey Public ID (remove).
         */
        if (StringUtils.isBlank(yubiKeyPubId)) {

            if (dto.getRemoveYubiKey()) {
                this.assocYubiKeyPubId(jpaUser, null);
                isUpdated = true;
            }

        } else {

            final int lengthYubiKey = YubiKeyOTP.PUBLIC_ID_LENGTH;

            if (yubiKeyPubId.length() != lengthYubiKey) {
                /*
                 * INVARIANT: YubiKey Public ID MUST be valid.
                 */
                return createError("msg-yubikey-length-error",
                        String.valueOf(lengthYubiKey));
            }

            final User jpaUserDuplicate =
                    this.findUserByYubiKeyPubID(yubiKeyPubId);

            if (jpaUserDuplicate != null
                    && !jpaUserDuplicate.getUserId().equals(userid)) {

                /*
                 * INVARIANT: YubiKey Public ID MUST be unique.
                 */
                return createError("msg-user-duplicate-user-yubikey",
                        dto.getId(), jpaUserDuplicate.getUserId());
            }

            this.assocYubiKeyPubId(jpaUser, yubiKeyPubId);
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
                doUpdate = this.findUserAttrValue(jpaUser.getId(),
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
                doUpdate = this.findUserAttrValue(jpaUser.getId(),
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
                doUpdate = (this.findUserAttrValue(jpaUser.getId(),
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
        dto.setInternalPw(user.getInternal().booleanValue()
                && this.hasInternalPassword(user));

        dto.setId(this.getPrimaryIdNumber(user));
        dto.setYubiKeyPubId(this.getYubiKeyPubID(user));

        final PGPKeyID pgpKeyID = this.getPGPPubKeyID(user);
        if (pgpKeyID != null) {
            dto.setPgpPubKeyId(pgpKeyID.toHex());
        }

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
                this.findUserAttrValue(user.getId(), UserAttrEnum.PIN);

        String pin = "";
        if (encryptedPin != null) {
            pin = CryptoUser.decryptUserAttr(user.getId(), encryptedPin);
        }

        dto.setPin(pin);

        /*
         * UUID.
         */
        final String encryptedIppInternetUuid =
                this.findUserAttrValue(user.getId(), UserAttrEnum.UUID);

        String ippInternetUuid = "";
        if (encryptedIppInternetUuid != null) {
            try {
                ippInternetUuid = CryptoUser.decryptUserAttr(user.getId(),
                        encryptedIppInternetUuid);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
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
        UserAttr aclAttr =
                userAttrDAO().findByName(user.getId(), UserAttrEnum.ACL_ROLES);

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

        /*
         * OIDS (User).
         */
        aclAttr = userAttrDAO().findByName(user.getId(),
                UserAttrEnum.ACL_OIDS_USER);
        Map<ACLOidEnum, Integer> aclOids;

        if (aclAttr == null) {
            aclOids = null;
        } else {
            aclOids = JsonHelper.createEnumIntegerMapOrNull(ACLOidEnum.class,
                    aclAttr.getValue());
        }

        if (aclOids == null) {
            aclOids = new HashMap<ACLOidEnum, Integer>();
        }

        dto.setAclOidsUser(ACLOidEnum.asMapPerms(aclOids));

        /*
         * OIDS (Admin).
         */
        aclAttr = userAttrDAO().findByName(user.getId(),
                UserAttrEnum.ACL_OIDS_ADMIN);

        if (aclAttr == null) {
            aclOids = null;
        } else {
            aclOids = JsonHelper.createEnumIntegerMapOrNull(ACLOidEnum.class,
                    aclAttr.getValue());
        }

        if (aclOids == null) {
            aclOids = new HashMap<ACLOidEnum, Integer>();
        }

        dto.setAclOidsAdmin(ACLOidEnum.asMapPerms(aclOids));
        //
        return dto;
    }

    /**
     *
     * @param userid
     *            Unique User ID.
     * @return {@code true} when User ID is reserved.
     */
    private static boolean isReserverUserId(final String userid) {
        return ConfigManager.isInternalAdmin(userid)
                || userid.equalsIgnoreCase(User.ERASED_USER_ID);
    }

    @Override
    public AbstractJsonRpcMethodResponse setUser(final UserDto userDto,
            final boolean isNewInternalUser) throws IOException {

        final Date now = new Date();

        final String userid = userDto.getUserName();

        if (StringUtils.isBlank(userid)) {
            return createError("msg-user-userid-is-empty");
        }

        if (isReserverUserId(userid)) {
            return createError("msg-username-reserved", userid);
        }

        String cardNumber = userDto.getCard();
        if (cardNumber != null) {
            cardNumber = cardNumber.toLowerCase();
        }
        final String idNumber = userDto.getId();
        final String yubiKeyPubId = userDto.getYubiKeyPubId();
        final String primaryEmail = userDto.getEmail();

        /*
         * PGP Public Key ID.
         */
        final PGPKeyID pgpPubKeyId;

        if (StringUtils.isBlank(userDto.getPgpPubKeyId())) {
            pgpPubKeyId = null;
        } else {
            pgpPubKeyId = new PGPKeyID(userDto.getPgpPubKeyId());
        }

        final boolean hasPgpPubKeyId = pgpPubKeyId != null;

        /*
         * PIN
         */
        final String pin = userDto.getPin();
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
         * Find duplicates for userid, ID Number, YubiKey Public ID, and Card
         * Number.
         *
         * NOTE: The find returns null when instance is logically deleted!
         */
        final User jpaUserDuplicate =
                userDAO().findActiveUserByUserId(userDto.getUserName());

        final User jpaUserIdNumberDuplicate = this.findUserByNumber(idNumber);

        final User jpaUserYubiKeyuplicate =
                this.findUserByYubiKeyPubID(yubiKeyPubId);

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

            if (jpaUserYubiKeyuplicate != null) {
                return createError("msg-user-duplicate-user-yubikey",
                        yubiKeyPubId, jpaUserYubiKeyuplicate.getUserId());
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
            // Mantis #1105
            jpaUser.setExternalUserName(userDto.getUserName());

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

            if (jpaUserYubiKeyuplicate != null && !jpaUserYubiKeyuplicate
                    .getUserId().equals(jpaUserDuplicate.getUserId())) {
                return createError("msg-user-duplicate-user-yubikey",
                        yubiKeyPubId, jpaUserYubiKeyuplicate.getUserId());
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
            if (!EmailValidator.validate(primaryEmail)) {
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
         * YubiKey Public ID.
         */
        if (StringUtils.isNotBlank(yubiKeyPubId)) {

            if (yubiKeyPubId.length() != YubiKeyOTP.PUBLIC_ID_LENGTH) {
                return createError("msg-yubikey-length-error",
                        String.valueOf(YubiKeyOTP.PUBLIC_ID_LENGTH));
            }
        }

        this.assocYubiKeyPubId(jpaUser, yubiKeyPubId);

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

            if (hasPIN || hasUuid || hasPgpPubKeyId) {
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
                if (hasPgpPubKeyId) {

                    this.setUserAttrValue(jpaUser, UserAttrEnum.PGP_PUBKEY_ID,
                            pgpPubKeyId.toHex());
                    try {
                        setUserAttrValue(jpaUser, pgpPubKeyId);
                    } catch (PGPBaseException e) {
                        throw new IOException(e.getMessage());
                    }
                }

                userDAO().update(jpaUser);
            }

        } else {

            final String hexIdPriv =
                    this.getUserAttrValue(jpaUser, UserAttrEnum.PGP_PUBKEY_ID);

            final PGPKeyID keyIdPrev;

            if (hexIdPriv == null) {
                keyIdPrev = null;
            } else {
                keyIdPrev = new PGPKeyID(hexIdPriv);
            }

            if (!hasPIN) {
                this.removeUserAttr(jpaUser, UserAttrEnum.PIN);
            }

            if (!hasUuid) {
                this.removeUserAttr(jpaUser, UserAttrEnum.UUID);
            }

            if (!hasPgpPubKeyId) {
                this.removeUserAttr(jpaUser, UserAttrEnum.PGP_PUBKEY_ID);
                if (keyIdPrev != null) {
                    removeUserAttr(jpaUser,
                            UserAttrEnum.getPgpPubRingDbKey(keyIdPrev));
                    pgpPublicKeyService().deleteRingEntry(jpaUser, keyIdPrev);
                }
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

            if (hasPgpPubKeyId) {

                this.setUserAttrValue(jpaUser, UserAttrEnum.PGP_PUBKEY_ID,
                        pgpPubKeyId.toHex());

                if (keyIdPrev == null
                        || keyIdPrev.getId() != pgpPubKeyId.getId()) {
                    try {
                        setUserAttrValue(jpaUser, pgpPubKeyId);
                    } catch (PGPBaseException e) {
                        return createErrorMsg(e.getMessage());
                    }
                }
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
         * ACL OIDS
         */
        setAclOids(userAttrDAO(), jpaUser, UserAttrEnum.ACL_OIDS_USER,
                ACLOidEnum.asMapPrivilege(userDto.getAclOidsUser()));

        setAclOids(userAttrDAO(), jpaUser, UserAttrEnum.ACL_OIDS_ADMIN,
                ACLOidEnum.asMapPrivilege(userDto.getAclOidsAdmin()));

        /*
         * Re-initialize Member Card information.
         */
        if (isNewInternalUser) {
            MemberCard.instance().init();
        }

        return JsonRpcMethodResult.createOkResult();
    }

    /**
     * Creates or updates the {@link UserAttrEnum#PFX_PGP_PUBRING_KEY_ID}
     * attribute value to the database.
     *
     * @param user
     *            User.
     * @param pubKeyId
     *            Public Key ID.
     * @throws PGPBaseException
     *             If PGP error.
     * @throws IOException
     *             If IO error.
     */
    private void setUserAttrValue(final User user, PGPKeyID pubKeyId)
            throws PGPBaseException, IOException {

        final PGPPublicKeyInfo pubKeyInfo =
                pgpPublicKeyService().lazyAddRingEntry(user, pubKeyId);

        this.setUserAttrValue(user, UserAttrEnum.getPgpPubRingDbKey(pubKeyId),
                PGPPubRingKeyDto.toDbJson(pubKeyInfo));
    }

    /**
     * Creates, updates or deletes the ACL roles of a user.
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

        crudUserAttr(daoAttr, user, UserAttrEnum.ACL_ROLES, jsonRoles);
    }

    /**
     * Creates, updates or deletes a {@link UserAttr}.
     *
     * @param daoAttr
     *            The {@link UserAttrDao}.
     * @param user
     *            The user.
     * @param attrEnum
     *            The attribute key.
     * @param aclOids
     *            The OIDs
     * @throws IOException
     *             When JSON errors.
     */
    private static void setAclOids(final UserAttrDao daoAttr, final User user,
            final UserAttrEnum attrEnum, final Map<ACLOidEnum, Integer> aclOids)
            throws IOException {

        final String jsonOids;

        if (aclOids.isEmpty()) {
            jsonOids = null;
        } else {
            jsonOids = JsonHelper.stringifyObject(aclOids);
        }
        crudUserAttr(daoAttr, user, attrEnum, jsonOids);
    }

    /**
     * Creates, updates or deletes a {@link UserAttr}.
     *
     * @param daoAttr
     *            The {@link UserAttrDao}.
     * @param user
     *            The user.
     * @param attrEnum
     *            The attribute key.
     * @param attrValue
     *            The attribute value. When {@code null} an existing attribute
     *            is deleted.
     */
    private static void crudUserAttr(final UserAttrDao daoAttr, final User user,
            final UserAttrEnum attrEnum, final String attrValue) {

        UserAttr attr = daoAttr.findByName(user.getId(), attrEnum);

        if (attr == null) {
            if (attrValue != null) {
                attr = new UserAttr();
                attr.setUser(user);
                attr.setName(attrEnum.getName());
                attr.setValue(attrValue);
                daoAttr.create(attr);
            }
        } else if (attrValue == null) {

            final Iterator<UserAttr> iter = user.getAttributes().iterator();

            while (iter.hasNext()) {
                final UserAttr attrWlk = iter.next();
                if (attrWlk.getName().equals(attrEnum.getName())) {
                    iter.remove();
                    break;
                }
            }
            userDAO().update(user);
            daoAttr.delete(attr);

        } else if (!attr.getValue().equals(attrValue)) {
            attr.setValue(attrValue);
            daoAttr.update(attr);
        }
    }

    /**
     * Perform the final actions after a user delete (clean SafePages).
     *
     * @param userIdToDelete
     *            The unique user name to delete.
     * @return The JSON-RPC Return message (either a result or an error);
     */
    private AbstractJsonRpcMethodResponse
            deleteUserFinalAction(final String userIdToDelete) {

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

        return createOkResult("msg-user-deleted-ok-inc-safepages", NumberUtil
                .humanReadableByteCountSI(ServiceContext.getLocale(), nBytes));
    }

    @Override
    public AbstractJsonRpcMethodResponse deleteUser(final String uid) {

        final User user = this.lockByUserId(uid);
        if (user == null) {
            return createError("msg-user-not-found", uid);
        }

        final AbstractJsonRpcMethodResponse rsp = this.deleteUser(user);
        if (rsp.isError()) {
            return rsp;
        }
        userDAO().update(user);
        return rsp;
    }

    /**
     * Logically deletes a user.
     *
     * @param user
     *            The user.
     * @return The JSON-RPC Return message (either a result or an error);
     */
    private AbstractJsonRpcMethodResponse deleteUser(final User user) {
        this.performLogicalDelete(user);
        return this.deleteUserFinalAction(user.getUserId());
    }

    @Override
    public AbstractJsonRpcMethodResponse eraseUser(final String uid) {

        // Erase active user.
        final User user = this.lockByUserId(uid);
        if (user != null) {
            final AbstractJsonRpcMethodResponse rsp = this.deleteUser(user);
            if (rsp.isError()) {
                return rsp;
            }
            this.performErase(user);
            userDAO().update(user);
        }

        // Erase deleted users.
        for (final User userWlk : userDAO().findDeletedUsersByUserId(uid)) {
            this.performErase(userWlk);
            userDAO().update(userWlk);
        }

        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public AbstractJsonRpcMethodResponse deleteUserAutoCorrect(
            final String userIdToDelete) throws IOException {

        final List<User> users =
                userDAO().checkActiveUserByUserId(userIdToDelete);

        if (users.isEmpty()) {
            return createError("msg-user-not-found", userIdToDelete);
        }

        if (users.size() == 1) {
            this.lockUser(users.get(0).getId());
        } else {
            LOGGER.warn(String.format(
                    "Inconsistency corrected when deleting user [%s]: "
                            + "[%d] instances found and deleted.",
                    userIdToDelete, users.size()));
        }

        for (final User user : users) {
            this.performLogicalDelete(user);
            userDAO().update(user);
        }

        return this.deleteUserFinalAction(userIdToDelete);
    }

    @Override
    public String encryptUserPassword(final String userid,
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
            dto.setYubiKeyPubId(getYubiKeyPubID(user));

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
            if (!EmailValidator.validate(address)) {
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

                    if (!isNewInternalUser) {
                        userEmailDAO().create(userEmail);
                    }
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

                if (!isNewInternalUser) {
                    userEmailDAO().create(userEmail);
                }
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
    public boolean hasInternalPassword(final User user) {
        return this.getUserAttrValue(user,
                UserAttrEnum.INTERNAL_PASSWORD) != null;
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

    @Override
    public User findUserByYubiKeyPubID(final String publicID) {

        User user = null;

        if (StringUtils.isNotBlank(publicID)) {

            final UserNumber userNumber =
                    userNumberDAO().findByYubiKeyPubID(publicID);

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
                    if (attrEnum.isEncrypted()) {
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
    public boolean isUserAttrValue(final User user,
            final UserAttrEnum attrEnum) {
        return StringUtils.defaultString(this.getUserAttrValue(user, attrEnum),
                IAttrDao.V_NO).equals(IAttrDao.V_YES);
    }

    @Override
    public User findUserByNumberUuid(final String number, final UUID uuid) {
        return this.checkUserByUuid(this.findUserByNumber(number), uuid);
    }

    @Override
    public boolean isUserUuidPresent(final User user, final UUID uuid) {
        return this.checkUserByUuid(user, uuid) != null;
    }

    /**
     * Checks if {@link User} has {@link UUID}.
     *
     * @param user
     *            The user.
     * @param uuid
     *            The {@link UUID}.
     * @return The User or {@code null} when not found.
     */
    private User checkUserByUuid(final User user, final UUID uuid) {

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
                    // Prevent JPA/Hibernate "duplicate key value" exception,
                    // when adding same card number as primary.
                    ServiceContext.getDaoContext().commitInBetween();
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
                    // Prevent JPA/Hibernate "duplicate key value" exception,
                    // when adding same email address as primary.
                    ServiceContext.getDaoContext().commitInBetween();
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

    /**
     *
     * @param user
     * @param yubiKeyPubId
     */
    public void assocYubiKeyPubId(final User user, final String yubiKeyPubId) {

        List<UserNumber> numberList = user.getIdNumbers();

        if (StringUtils.isBlank(yubiKeyPubId)) {

            /*
             * Find and remove the YubiKey Public ID.
             */
            if (numberList != null) {

                final Iterator<UserNumber> iter = numberList.iterator();

                while (iter.hasNext()) {

                    final UserNumber number = iter.next();

                    if (userNumberDAO().isYubiKeyPubID(number)) {
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
             * Find the YubiKey Public ID.
             */
            UserNumber yubikeyNumber = null;

            final Iterator<UserNumber> iter = numberList.iterator();

            while (iter.hasNext()) {

                final UserNumber number = iter.next();

                if (userNumberDAO().isYubiKeyPubID(number)) {
                    yubikeyNumber = number;
                    break;
                }
            }

            /*
             * Create or update.
             */
            if (yubikeyNumber == null) {

                yubikeyNumber = new UserNumber();
                yubikeyNumber.setUser(user);
                userNumberDAO().assignYubiKeyNumber(yubikeyNumber);

                numberList.add(yubikeyNumber);
            }

            yubikeyNumber.setNumber(userNumberDAO()
                    .composeYubiKeyDbNumber(yubiKeyPubId.toLowerCase()));
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
                    // Prevent JPA/Hibernate "duplicate key value" exception,
                    // when adding same ID number as primary.
                    ServiceContext.getDaoContext().commitInBetween();
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
    public String getYubiKeyPubID(final User user) {
        String publicID = "";

        final List<UserNumber> numberList = user.getIdNumbers();

        if (numberList != null) {

            final Iterator<UserNumber> iter = numberList.iterator();

            while (iter.hasNext()) {

                final UserNumber number = iter.next();
                final String id = userNumberDAO().getYubiKeyPubID(number);

                if (id != null) {
                    publicID = id;
                    break;
                }
            }
        }
        return publicID;
    }

    @Override
    public PGPKeyID getPGPPubKeyID(final User user) {

        final String hexKeyID;

        final UserAttr attr = userAttrDAO().findByName(user.getId(),
                UserAttrEnum.PGP_PUBKEY_ID);

        if (attr == null) {
            hexKeyID = null;
        } else {
            hexKeyID = attr.getValue();
        }

        if (StringUtils.isBlank(hexKeyID)) {
            return null;
        }

        return new PGPKeyID(hexKeyID);
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
     * Removes all User group memberships from the database.
     *
     * @param user
     *            The {@link User}.
     */
    private void removeAllGroupMemberShips(final User user) {

        final List<UserGroupMember> memberships = user.getGroupMembership();

        if (memberships != null) {
            for (final UserGroupMember membership : memberships) {
                userGroupMemberDAO().delete(membership);
            }
            memberships.clear();
        }
    }

    /**
     * Removes identifying {@link UserAttr} from the database.
     *
     * @param user
     *            The {@link User}.
     */
    private void eraseUserAttr(final User user) {

        final List<UserAttr> attrList = user.getAttributes();
        if (attrList == null) {
            return;
        }
        final Iterator<UserAttr> iter = attrList.iterator();

        while (iter.hasNext()) {

            final UserAttr attr = iter.next();

            switch (UserAttrEnum.asEnum(attr.getName())) {
            case ACL_OIDS_ADMIN:
            case ACL_OIDS_USER:
            case ACL_ROLES:
            case BITCOIN_PAYMENT_ADDRESS:
            case INTERNAL_PASSWORD:
            case PIN:
            case PGP_PUBKEY_ID:
            case PDF_PROPS:
            case JOBTICKET_PROPS_LATEST:
            case UUID:
                userAttrDAO().delete(attr);
                iter.remove();
                break;
            default:
                break;
            }
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

                if (accountType == Account.AccountTypeEnum.SHARED) {
                    continue;
                }

                account.setDeleted(true);
                account.setDeletedDate(trxDate);
                account.setModifiedBy(ServiceContext.getActor());
                account.setModifiedDate(trxDate);
            }
        }
    }

    /**
     * Erases a user.
     *
     * @param user
     *            The user.
     */
    private void performErase(final User user) {

        final Date trxDate = ServiceContext.getTransactionDate();

        user.setModifiedBy(ServiceContext.getActor());
        user.setModifiedDate(trxDate);

        user.setUserId(User.ERASED_USER_ID);
        user.setExternalUserName(User.ERASED_USER_ID);

        user.setFullName(null);
        user.setDepartment(null);
        user.setOffice(null);

        eraseUserAttr(user);

        final List<UserAccount> userAccountList = user.getAccounts();

        if (userAccountList != null) {

            for (final UserAccount userAccount : userAccountList) {

                final Account account = userAccount.getAccount();
                final AccountTypeEnum accountType =
                        AccountTypeEnum.valueOf(account.getAccountType());

                if (accountType == Account.AccountTypeEnum.SHARED) {
                    continue;
                }

                account.setModifiedBy(ServiceContext.getActor());
                account.setModifiedDate(trxDate);

                account.setName(User.ERASED_USER_ID);
                account.setNameLower(User.ERASED_USER_ID.toLowerCase());
                account.setNotes(null);
                account.setPin(null);

                if (account.getSubName() != null) {
                    account.setSubName(User.ERASED_USER_ID);
                    account.setSubNameLower(User.ERASED_USER_ID.toLowerCase());
                    account.setSubPin(null);
                }
            }
        }

        accountTrxDAO().eraseUser(user);
        costChangeDAO().eraseUser(user);
        purchaseDAO().eraseUser(user);

        docLogDAO().eraseUser(user);
        docInDAO().eraseUser(user);
        docOutDAO().eraseUser(user);
        pdfOutDAO().eraseUser(user);
    }

    @Override
    public UserAttr removeUserAttr(final User user, final UserAttrEnum name) {
        return removeUserAttr(user, name.getName());
    }

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
    private static UserAttr removeUserAttr(final User user, final String name) {

        UserAttr removedAttr = null;

        if (user.getAttributes() != null) {

            final Iterator<UserAttr> iter = user.getAttributes().iterator();

            while (iter.hasNext()) {
                final UserAttr attr = iter.next();
                if (attr.getName().equals(name)) {
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
    public String lazyAddUserPrimaryIdNumber(final User user) {

        final String idNumber = this.getPrimaryIdNumber(user);

        if (StringUtils.isNotBlank(idNumber)) {
            return idNumber;
        }

        final ConfigManager cm = ConfigManager.instance();

        if (!cm.isConfigValue(Key.USER_ID_NUMBER_GENERATE_ENABLE)) {
            return null;
        }

        final int length = cm.getConfigInt(Key.USER_ID_NUMBER_GENERATE_LENGTH);

        final DaoContext ctx = ServiceContext.getDaoContext();

        for (int i = 0; i < MAX_USER_ID_NUMBER_GENERATE_TRIALS; i++) {

            final String random = RandomStringUtils.randomNumeric(length);

            if (userNumberDAO().findByNumber(random) == null) {
                try {
                    if (!ctx.isTransactionActive()) {
                        ctx.beginTransaction();
                    }
                    this.assocPrimaryIdNumber(user, random);
                    ctx.commit();
                    if (i > 0) {
                        LOGGER.warn("User [{}] Number ID generated: trial [{}]",
                                user.getUserId(), i + 1);
                    }
                    return random;
                } catch (Exception e) {
                    // Highly unlikely duplicate collision.
                } finally {
                    ctx.rollback();
                }
            }
        }

        LOGGER.warn("User [{}] Number ID could not be generated: trial [{}]",
                user.getUserId(), MAX_USER_ID_NUMBER_GENERATE_TRIALS);

        return null;
    }

    @Override
    public void setUserAttrValue(final User user, final UserAttrEnum attrEnum,
            final String attrValue) {

        this.setUserAttrValue(user,
                userAttrDAO().findByName(user.getId(), attrEnum), attrEnum,
                attrValue);
    }

    @Override
    public void setUserAttrValue(final User user, final UserAttrEnum attrEnum,
            final boolean attrValue) {
        this.setUserAttrValue(user, attrEnum,
                userAttrDAO().getDbBooleanValue(attrValue));
    }

    /**
     * Creates or updates the attribute value to the database.
     *
     * @param user
     *            The user.
     * @param attrName
     *            The name of the {@link UserAttr}.
     * @param attrValue
     *            The value.
     */
    private void setUserAttrValue(final User user, final String attrName,
            final String attrValue) {

        this.setUserAttrValue(user,
                userAttrDAO().findByName(user.getId(), attrName), attrName,
                attrValue);
    }

    /**
     * Creates or updates a {@link UserAttr} value to the database.
     * <p>
     * Value encryption is responsibility of client.
     * </p>
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
        this.setUserAttrValue(user, userAttr, attrEnum.getName(), attrValue);
    }

    /**
     * Creates or updates a {@link UserAttr} value to the database.
     * <p>
     * Value encryption is responsibility of client.
     * </p>
     *
     * @param user
     *            The {@link User}.
     * @param userAttr
     *            The {@link UserAttr}. When {@code null} the attribute is
     *            created.
     * @param attrName
     *            The {@link UserAttr#getName()} (used when {@link UserAttr} is
     *            {@code null}).
     * @param attrValue
     *            The attribute value (used when {@link UserAttr} is
     *            {@code null}).
     */
    private void setUserAttrValue(final User user, final UserAttr userAttr,
            final String attrName, final String attrValue) {

        if (userAttr == null) {

            final UserAttr attrNew = new UserAttr();

            attrNew.setUser(user);

            attrNew.setName(attrName);
            attrNew.setValue(attrValue);

            userAttrDAO().create(attrNew);

        } else {

            userAttr.setValue(attrValue);

            userAttrDAO().update(userAttr);
        }
    }

    @Override
    public String findUserAttrValue(final Long userDbKey,
            final UserAttrEnum name) {
        final UserAttr attr = userAttrDAO().findByName(userDbKey, name);
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

        final UserAttr attr = userAttrDAO().findByName(user.getId(), name);

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

        final UserAttr attr = userAttrDAO().findByName(user.getId(), name);

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
                this.lockUser(user.getId());
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

                final int nMemberships =
                        addUserGroupMemberships(userSource, user);

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
                        "User [%s] NOT lazy inserted: "
                                + "not member of User Source Group [%s].",
                        userId, userGroup));
            }
        }

        return user;
    }

    /**
     * Adds a {@link UserGroupMember} to the database.
     *
     * @param userGroup
     *            The group.
     * @param user
     *            The user.
     */
    private void addUserGroupMember(final UserGroup userGroup,
            final User user) {

        final UserGroupMember member = new UserGroupMember();

        member.setGroup(userGroup);
        member.setUser(user);
        member.setCreatedBy(ServiceContext.getActor());
        member.setCreatedDate(ServiceContext.getTransactionDate());

        userGroupMemberDAO().create(member);
    }

    /**
     * Adds a {@link UserGroupMember} objects to the database.
     * <p>
     * The {@link InternalGroupList} is consulted and the {@link UserGroup}
     * objects from the database.
     * </p>
     *
     * @param userSource
     *            The {@link IUserSource}
     * @param user
     *            The {@link User}.
     * @return The number of objects added.
     */
    private int addUserGroupMemberships(final IUserSource userSource,
            final User user) {

        final String userId = user.getUserId();

        int nMemberships = 0;

        /*
         * Step 1: Process the Internal Groups the user is member of.
         */
        final Map<String, Boolean> internalGroups;

        try {
            internalGroups = InternalGroupList.getGroupsOfUser(userId);
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }

        for (final Entry<String, Boolean> entry : internalGroups.entrySet()) {
            if (entry.getValue()) {
                final UserGroup userGroup =
                        userGroupDAO().findByName(entry.getKey());
                if (userGroup != null) {
                    addUserGroupMember(userGroup, user);
                    nMemberships++;
                }
            }
        }

        /*
         * Step 2: Process the User Groups in the database.
         */
        for (final UserGroup userGroup : userGroupDAO().getListChunk(
                new UserGroupDao.ListFilter(), null, null,
                UserGroupDao.Field.ID, true)) {

            final String groupName = userGroup.getGroupName();

            if (userGroupService().isReservedGroupName(groupName)) {
                continue;
            }

            /*
             * INVARIANT: "Internal Groups should have a name distinctive to any
             * groups defined in your external user source. If case of a name
             * clash, the internal group takes precedence."
             */
            if (internalGroups.containsKey(groupName)) {
                continue;
            }

            if (userSource.isUserInGroup(userId, groupName)) {
                addUserGroupMember(userGroup, user);
                nMemberships++;
            }
        }
        return nMemberships;
    }

    @Override
    public void lazyUserHomeDir(final String uid) throws IOException {
        lazyCreateDir(new File(ConfigManager.getUserHomeDir(uid)));
        lazyCreateDir(outboxService().getUserOutboxDir(uid));
    }

    /**
     *
     * @param dir
     *            The directory to lazy create.
     * @throws IOException
     *             When file IO errors.
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
    public PdfProperties getPdfProperties(final User user) {

        PdfProperties props = null;
        String json =
                this.findUserAttrValue(user.getId(), UserAttrEnum.PDF_PROPS);
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
            LOGGER.warn("PDF Properties of user [{}] are reset, because: {}",
                    user.getUserId(), e.getMessage());
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

    @Override
    public JobTicketProperties getJobTicketPropsLatest(final User user) {

        JobTicketProperties props = null;
        String json = this.findUserAttrValue(user.getId(),
                UserAttrEnum.JOBTICKET_PROPS_LATEST);
        try {
            if (json != null) {
                props = JobTicketProperties.create(json);
            }
        } catch (Exception e) {
            // Be forgiving ...
            json = null;
            LOGGER.warn(
                    "JobTicket properties of user [{}] are reset, because: {}",
                    user.getUserId(), e.getMessage());
        }

        if (json == null) {
            props = new JobTicketProperties();
        }

        return props;
    }

    @Override
    public JobTicketProperties getJobTicketPropsLatest(final UserIdDto dto) {

        JobTicketProperties props = null;
        String json = this.findUserAttrValue(dto.getDbKey(),
                UserAttrEnum.JOBTICKET_PROPS_LATEST);
        try {
            if (json != null) {
                props = JobTicketProperties.create(json);
            }
        } catch (Exception e) {
            // Be forgiving ...
            json = null;
            LOGGER.warn(
                    "JobTicket properties of user [{}] are reset, because: {}",
                    dto.getUserId(), e.getMessage());
        }

        if (json == null) {
            props = new JobTicketProperties();
        }

        return props;
    }

    @Override
    public void setJobTicketPropsLatest(final User user,
            final JobTicketProperties objProps) throws IOException {

        this.setUserAttrValue(user, UserAttrEnum.JOBTICKET_PROPS_LATEST,
                objProps.stringify());
    }

    @Override
    public boolean isErased(final User user) {
        return user.getDeleted()
                && user.getUserId().equals(User.ERASED_USER_ID);
    }

    @Override
    public String getUserIdUi(final User user, final Locale locale) {
        if (this.isErased(user)) {
            return String.format("%s-%s",
                    AdverbEnum.ANONYMOUS.uiText(locale).toLowerCase(),
                    DateUtil.dateAsIso8601(user.getModifiedDate()));
        }
        return user.getUserId();
    }

    /**
     *
     * @param user
     *            The user (for logging).
     * @param attrName
     *            The user attribute (for logging).
     * @param jsonSet
     *            The JSON "set" string with Long values.
     * @return {@code null} when user attribute is not present, or when JSON is
     *         invalid.
     */
    private Set<Long> getLongSet(final User user, final UserAttrEnum attrName,
            final String jsonSet) {

        try {
            if (StringUtils.isNotBlank(jsonSet)) {
                return JsonHelper.createLongSet(jsonSet);
            }
        } catch (IOException e) {
            LOGGER.warn("User [{}] attribute [{}] : {}", user.getUserId(),
                    attrName.getName(), e.getMessage());
        }
        return null;
    }

    /**
     *
     * @param user
     * @param attrName
     * @return
     */
    private Set<Long> getPreferredDelegateDbKeys(final User user,
            final UserAttrEnum attrName) {
        return this.getLongSet(user, attrName,
                this.getUserAttrValue(user, attrName));
    }

    /**
     *
     * @param user
     * @param attrName
     * @param dbIds
     * @return
     * @throws OutOfBoundsException
     */
    private boolean addPreferredDelegateDbKeys(final User user,
            final UserAttrEnum attrName, final Set<Long> dbIds)
            throws OutOfBoundsException {

        final UserAttr userAttr = getUserAttr(user, attrName);

        Set<Long> groupIdsDb;

        if (userAttr == null) {
            groupIdsDb = null;
        } else {
            groupIdsDb = this.getLongSet(user, attrName, userAttr.getValue());
        }

        if (groupIdsDb == null) {
            groupIdsDb = new HashSet<>();
        }

        if (groupIdsDb.addAll(dbIds)) {

            final String json = JsonHelper.stringifyLongSet(groupIdsDb);

            if (json.length() > UserAttr.COL_ATTRIB_VALUE_LENGTH) {
                throw new OutOfBoundsException(
                        "Max reached: preference not stored.");
            }

            if (userAttr == null) {
                this.addUserAttr(user, attrName, json);
            } else {
                userAttr.setValue(json);
                this.setUserAttrValue(user, attrName, json);
            }

            return true;
        }
        return false;
    }

    /**
     *
     * @param user
     * @param attrName
     * @param dbIds
     * @return
     */
    private boolean removePreferredDelegateDbKeys(final User user,
            final UserAttrEnum attrName, final Set<Long> dbIds) {

        final UserAttr userAttr = getUserAttr(user, attrName);

        if (userAttr == null) {
            return false;
        }

        final Set<Long> groupIdsDb =
                this.getLongSet(user, attrName, userAttr.getValue());

        final boolean change;

        if (groupIdsDb == null) {

            this.removeUserAttr(user, attrName);
            change = true;

        } else if (groupIdsDb.removeAll(dbIds)) {

            if (groupIdsDb.isEmpty()) {
                this.removeUserAttr(user, attrName);
            } else {
                final String json = JsonHelper.stringifyLongSet(groupIdsDb);
                this.setUserAttrValue(user, attrName, json);
            }
            change = true;
        } else {
            change = false;
        }

        return change;
    }

    @Override
    public Set<Long> getPreferredDelegateGroups(final User user) {
        return this.getPreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_GROUPS_PREFERRED);
    }

    @Override
    public boolean addPreferredDelegateGroups(final User user,
            final Set<Long> dbIds) throws OutOfBoundsException {
        return addPreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_GROUPS_PREFERRED, dbIds);
    }

    @Override
    public boolean removePreferredDelegateGroups(final User user,
            final Set<Long> dbIds) {
        return removePreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_GROUPS_PREFERRED, dbIds);
    }

    @Override
    public Set<Long> getPreferredDelegateAccounts(final User user) {
        return this.getPreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_ACCOUNTS_PREFERRED);
    }

    @Override
    public boolean addPreferredDelegateAccounts(final User user,
            final Set<Long> dbIds) throws OutOfBoundsException {
        return addPreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_ACCOUNTS_PREFERRED, dbIds);
    }

    @Override
    public boolean removePreferredDelegateAccounts(final User user,
            final Set<Long> dbIds) {
        return removePreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_ACCOUNTS_PREFERRED, dbIds);
    }

    /**
     *
     * @param user
     * @param attrName
     * @param dao
     * @return
     */
    public Set<Long> prunePreferredDelegateDbKeys(final User user,
            final UserAttrEnum attrName, final GenericDao<?> dao) {

        final UserAttr userAttr = getUserAttr(user, attrName);

        if (userAttr != null) {
            try {
                final Set<Long> dbIds =
                        JsonHelper.createLongSet(userAttr.getValue());

                final int sizePrv = dbIds.size();
                final Iterator<Long> iter = dbIds.iterator();

                while (iter.hasNext()) {
                    final Long id = iter.next();

                    if (id == null || dao.findById(id) == null) {
                        iter.remove();
                    }
                }

                if (dbIds.isEmpty()) {
                    this.removeUserAttr(user, attrName);
                } else if (sizePrv != dbIds.size()) {
                    final String json = JsonHelper.stringifyLongSet(dbIds);
                    userAttr.setValue(json);
                    this.setUserAttrValue(user, attrName, json);
                }

                return dbIds;

            } catch (IOException e) {
                this.removeUserAttr(user, attrName);
            }
        }
        return null;
    }

    @Override
    public Set<Long> prunePreferredDelegateGroups(final User user) {
        return prunePreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_GROUPS_PREFERRED,
                userGroupDAO());
    }

    @Override
    public Set<Long> prunePreferredDelegateAccounts(final User user) {
        return prunePreferredDelegateDbKeys(user,
                UserAttrEnum.PROXY_PRINT_DELEGATE_ACCOUNTS_PREFERRED,
                accountDAO());
    }

    @Override
    public User lockByUserId(final String userId) {
        return userDAO().lockByUserId(userId);
    }

    @Override
    public User lockUser(final Long id) {
        return userDAO().lock(id);
    }

}
