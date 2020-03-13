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
package org.savapage.core.users;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.IConfigProp.LdapTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.net.TrustSelfSignedCertSocketFactory;
import org.savapage.core.rfid.RfidNumberFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract LDAP User Source.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class LdapUserSourceMixin extends AbstractUserSource
        implements IUserSource, IExternalUserAuthenticator {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LdapUserSourceMixin.class);

    /**
     *
     */
    private final String attrIdUserName;

    /**
     *
     */
    private final String attrIdUserEmail;

    /**
     *
     */
    private final String attrIdUserFullName;

    /**
     *
     */
    private final String batchsize;

    /**
     * The LDAP field to retrieve the ID Number. When {@code null} no ID Number
     * is provided by this user source.
     */
    private String attrIdUserIdNumber = null;

    /**
     * The LDAP field to retrieve the Card Number. When {@code null} no Card
     * Number is provided by this user source.
     */
    private String attrIdUserCardNumber = null;

    /**
     * The page size for the pager.
     */
    private final int pageSize;

    /**
     *
     */
    private final boolean isPosixGroups;

    /**
     *
     */
    private final String baseDN;

    /**
     *
     */
    private final LdapTypeEnum ldapType;

    /**
     * The lazy initialized LDAP search filter pattern to select a group.
     */
    private String groupSearchPattern;

    /**
     * The lazy initialized LDAP search filter pattern to select a user name.
     */
    private String userNameSearchPattern;

    /**
     *
     */
    private static final String LDAP_SEARCH_WILDCARD_ALL = "*";

    /**
     *
     */
    private static final String LDAP_ATTRID_SUPPORTED_CONTROL =
            "supportedcontrol";

    /**
     * Base OID of the LDAP Controls introduced by / specific to Microsoft
     * Active Directory (assigned to Microsoft).
     */
    private static final String OID_AD_BASE = "1.2.840.113556";

    /**
     * Range property.
     */
    private static final String OID_RANGE_PROPERTY = OID_AD_BASE + ".1.4.802";

    //
    private static final String JAVA_NAMING_LDAP_FACTORY_SOCKET =
            "java.naming.ldap.factory.socket";
    //
    private static final String JAVA_NAMING_BATCHSIZE = "java.naming.batchsize";

    //
    private static final String CONTEXT_SECURITY_PROTOCOL_SSL = "ssl";
    //
    private static final String CONTEXT_SECURITY_AUTHENTICATION_SIMPLE =
            "simple";
    //
    private static final String CONTEXT_INITIAL_CONTEXT_FACTORY_CLASS_NAME =
            "com.sun.jndi.ldap.LdapCtxFactory";

    /**
     * The lazy initialized supported controls.
     */
    private Set<String> supportedControlSet;

    /**
     *
     */
    public LdapUserSourceMixin(final LdapTypeEnum ldapType) {

        final ConfigManager cm = ConfigManager.instance();

        this.ldapType = ldapType;

        this.baseDN = cm.getConfigValue(IConfigProp.Key.AUTH_LDAP_BASE_DN);

        this.batchsize = cm.getConfigValue(Key.LDAP_BATCH_SIZE);

        /*
         * Mantis #393: page size MUST be LT the batch size!
         */
        this.pageSize = cm.getConfigInt(Key.LDAP_BATCH_SIZE) - 1;

        this.attrIdUserName = cm.getConfigValue(this.ldapType,
                Key.LDAP_SCHEMA_USER_NAME_FIELD);

        this.attrIdUserEmail = cm.getConfigValue(this.ldapType,
                Key.LDAP_SCHEMA_USER_EMAIL_FIELD);

        this.attrIdUserFullName = cm.getConfigValue(this.ldapType,
                Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD);

        this.isPosixGroups =
                cm.getConfigValue(this.ldapType, Key.LDAP_SCHEMA_POSIX_GROUPS)
                        .equals(IConfigProp.V_YES);

        if (isExtraUserAttributes()) {
            // optional
            this.attrIdUserIdNumber =
                    cm.getConfigValue(Key.LDAP_SCHEMA_USER_ID_NUMBER_FIELD);

            if (StringUtils.isBlank(this.attrIdUserIdNumber)) {
                this.attrIdUserIdNumber = null;
            }
            // optional
            this.attrIdUserCardNumber =
                    cm.getConfigValue(Key.LDAP_SCHEMA_USER_CARD_NUMBER_FIELD);

            if (StringUtils.isBlank(this.attrIdUserCardNumber)) {
                this.attrIdUserCardNumber = null;
            }
        } else {
            this.attrIdUserIdNumber = null;
            this.attrIdUserCardNumber = null;
        }
    }

    /**
     * @return {@code true} if extra User LDAP attributes can be used.
     */
    protected boolean isExtraUserAttributes() {
        return true;
    }

    /**
     *
     * @return {@true} when disabled users from LDAP are allowed.
     */
    protected abstract boolean allowDisabledUsers();

    /**
     *
     * @param key
     * @return
     */
    protected final String getLdapConfigValue(final Key key) {
        return ConfigManager.instance().getConfigValue(getLdapType(), key);
    }

    /**
     *
     * @param key
     * @return
     */
    protected final Boolean getLdapConfigBoolean(final Key key) {
        return ConfigManager.instance().isConfigValue(getLdapType(), key);
    }

    /**
     *
     * @param key
     * @return
     */
    protected final Boolean getLdapConfigBoolean(final Key key,
            final Boolean defaultValue) {

        Boolean value =
                ConfigManager.instance().isConfigValue(getLdapType(), key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     *
     * @return The {@link LdapTypeEnum}.
     */
    protected final IConfigProp.LdapTypeEnum getLdapType() {
        return this.ldapType;
    }

    /**
     * Gets the page size to be used with {@link LdapSearchResultPager}.
     *
     * @param ctx
     *            The {@link LdapContext}.
     * @return The number of entries on a paged result or
     *         {@link LdapSearchResultPager#NO_PAGING} when no paging is
     *         supported by the {@link LdapContext}.
     * @throws NamingException
     *             If a naming exception is encountered.
     */
    protected final int getPageSize(final LdapContext ctx)
            throws NamingException {
        if (isPagedResultControlSupported(ctx)) {
            return this.pageSize;
        } else {
            return LdapSearchResultPager.NO_PAGING;
        }
    }

    /**
     * Gets the range step size to be used with {@link LdapMultiValuePager}.
     *
     * @param ctx
     *            The {@link LdapContext}.
     * @return The number of values in retrieved range or
     *         {@link LdapMultiValuePager.NO_RANGE} when no range property is
     *         supported by the {@link LdapContext}.
     * @throws NamingException
     *             If a naming exception is encountered.
     */
    protected final int getRangeStepSize(final LdapContext ctx)
            throws NamingException {
        if (isRangePropertyControlSupported(ctx)) {
            return this.pageSize;
        } else {
            return LdapMultiValuePager.NO_RANGE;
        }
    }

    /**
     *
     * @return
     */
    protected final String getBaseDN() {
        return this.baseDN;
    }

    /**
     *
     * @return
     */
    protected final String getAttrIdUserName() {
        return this.attrIdUserName;
    }

    /**
     * @return LDAP host.
     */
    protected String getLdapHost() {
        return ConfigManager.instance()
                .getConfigValue(IConfigProp.Key.AUTH_LDAP_HOST);
    }

    /**
     * @return LDAP port.
     */
    protected String getLdapPort() {
        return ConfigManager.instance()
                .getConfigValue(IConfigProp.Key.AUTH_LDAP_PORT);
    }

    /**
     * @return {@code true} if SSL.
     */
    protected boolean isLdapSSL() {
        return ConfigManager.instance()
                .isConfigValue(IConfigProp.Key.AUTH_LDAP_USE_SSL);
    }

    /**
     * @return {@code true} if self-signed certificate for LDAP SSL is trusted.
     */
    protected boolean isLdapUseTrustSelfSignedSSL() {
        return ConfigManager.instance()
                .isConfigValue(Key.AUTH_LDAP_USE_SSL_TRUST_SELF_SIGNED);
    }

    /**
     *
     * @return The URL.
     */
    private String getProviderUrl() {

        final StringBuilder schema = new StringBuilder();

        schema.append("ldap");

        if (this.isLdapSSL()) {
            schema.append("s");
        }

        schema.append("://").append(this.getLdapHost()).append(":")
                .append(this.getLdapPort());

        return schema.toString();
    }

    /**
     *
     * @return The URL.
     */
    protected String getProviderUrlBaseDn() {

        return getProviderUrl() + "/" + ConfigManager.instance()
                .getConfigValue(IConfigProp.Key.AUTH_LDAP_BASE_DN);
    }

    /**
     * Creates a {@link User} from LDAP {@link Attributes}.
     *
     * @param attributes
     *            The attributes.
     * @return The {@link User} or {@code null} when identifying UserName is not
     *         found in the LDAP {@link Attributes}.
     *
     * @throws NamingException
     *             When LDAP error.
     */
    private User createUser(final Attributes attributes)
            throws NamingException {
        return createCommonUser(attributes).createUser();
    }

    /**
     * Checks the user attributes to see if the user is enabled.
     *
     * @param userAttributes
     *            The attributes of the LDAP user.
     * @return {@code true} is user is enabled.
     * @throws NamingException
     */
    protected abstract boolean isUserEnabled(Attributes userAttributes)
            throws NamingException;

    /**
     * Creates a {@link CommonUser} from LDAP {@link Attributes}.
     *
     * @param attributes
     *            The attributes.
     * @return The {@link CommonUser} or {@code null} when identifying UserName
     *         is not found in the LDAP {@link Attributes}.
     * @throws NamingException
     *             When LDAP error.
     */
    private CommonUser createCommonUser(final Attributes attributes)
            throws NamingException {

        /*
         * User Name.
         */
        Attribute attrWlk = attributes.get(this.attrIdUserName);

        if (attrWlk == null || attrWlk.get() == null) {
            return null;
        }

        final CommonUser user = new CommonUser();

        String attrValueWlk = attrWlk.get().toString();

        user.setExternalUserName(attrValueWlk);
        user.setUserName(asDbUserId(attrValueWlk));

        /*
         * Email, multiple values ...
         */
        attrWlk = attributes.get(this.attrIdUserEmail);

        if (attrWlk != null) {
            NamingEnumeration<?> values = attrWlk.getAll();
            while (values.hasMore()) {
                user.setEmail(values.next().toString());
            }
        }

        /*
         * Full Name
         */
        attrWlk = attributes.get(this.attrIdUserFullName);

        if (attrWlk != null && attrWlk.get() != null) {
            user.setFullName(attrWlk.get().toString());
        }

        /*
         * ID Number
         */
        if (isIdNumberProvided()) {
            attrWlk = attributes.get(this.attrIdUserIdNumber);
            if (attrWlk != null && attrWlk.get() != null) {
                user.setIdNumber(attrWlk.get().toString());
            }
        }

        /*
         * Card Number
         */
        if (isCardNumberProvided()) {
            attrWlk = attributes.get(this.attrIdUserCardNumber);
            if (attrWlk != null && attrWlk.get() != null) {
                user.setCardNumber(attrWlk.get().toString());
            }
        }

        /*
         * Enabled?
         */
        user.setEnabled(isUserEnabled(attributes));

        return user;
    }

    @Override
    public final String asDbUserId(final String userId) {
        return asDbUserId(userId, true);
    }

    @Override
    public final User authenticate(final String uid, final String password) {

        if (StringUtils.isBlank(uid) || StringUtils.isBlank(password)) {
            return null;
        }

        final String providerUrl = getProviderUrlBaseDn();

        /*
         * e.g. (uid=rijk) for OpenLDAP
         */
        final String ldapFilterExpression = getUserNameSearchExpression(uid);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("authenticate user [" + uid + "] at [" + providerUrl
                    + "] : " + ldapFilterExpression);
        }
        /*
         * To verify that someone has an account is first search for their uid
         * (or cn or whatever) then log in as them with there userdn (which you
         * get after searching for them) and password, each user should be able
         * to log in to their own account and read everything in it, but
         * shouldn't be able to write to it.
         */
        User user = null;

        /*
         * Search with administrator credentials for the user.
         */
        DirContext ctx = createLdapContextForAdmin();

        NamingEnumeration<SearchResult> results = null;

        try {

            final SearchControls controls = new SearchControls();

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Just one (1) user expected.
            controls.setCountLimit(1);

            results = ctx.search("", ldapFilterExpression, controls);

            if (results.hasMore()) {
                SearchResult searchResult = results.next();

                String dn = searchResult.getName();
                if (searchResult.isRelative()) {
                    dn += "," + this.baseDN;
                }

                try {
                    /*
                     * New context with the credentials of the user to
                     * authenticate.
                     */
                    ctx.close();
                    ctx = null;

                    final Hashtable<String, String> env = new Hashtable<>();

                    env.put(Context.INITIAL_CONTEXT_FACTORY,
                            CONTEXT_INITIAL_CONTEXT_FACTORY_CLASS_NAME);
                    env.put(Context.PROVIDER_URL, providerUrl);

                    env.put(JAVA_NAMING_BATCHSIZE, this.batchsize);

                    env.put(Context.SECURITY_AUTHENTICATION,
                            CONTEXT_SECURITY_AUTHENTICATION_SIMPLE);
                    env.put(Context.SECURITY_PRINCIPAL, dn);
                    env.put(Context.SECURITY_CREDENTIALS, password);

                    this.setInitialLdapSSLContext(ConfigManager.instance(),
                            env);

                    ctx = new InitialDirContext(env);
                    /*
                     *
                     */
                    Attributes attributes = searchResult.getAttributes();

                    if (LOGGER.isDebugEnabled()) {
                        debugLogAttributes(dn, attributes);
                    }

                    user = createUser(attributes);

                } catch (AuthenticationException authEx) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Authentication for [" + uid + "] failed!");
                    }
                } catch (NamingException namEx) {
                    throw new SpException("LDAP NamingException", namEx);
                }

            }
        } catch (NameNotFoundException e) {
            /*
             * The base context was not found.
             */
            throw new SpException(
                    "LDAP base context [" + this.baseDN + "] not found", e);

        } catch (NamingException e) {

            throw new SpException("LDAP NamingException", e);

        } finally {
            closeResources(results, ctx);
        }

        return user;
    }

    /**
     * Helper method to log attributes.
     *
     * @param dn
     *            The DN.
     * @param attributes
     *            The attributes.
     * @throws NamingException
     *             On LDAP error.
     */
    private void debugLogAttributes(final String dn,
            final Attributes attributes) throws NamingException {

        final StringBuilder output = new StringBuilder();

        output.append(dn + "\n");
        NamingEnumeration<? extends Attribute> eAtts = attributes.getAll();

        while (eAtts.hasMore()) {
            Object att = eAtts.next().toString();
            output.append(att + "");
            /*
             * NamingEnumeration values = ((BasicAttribute) attributes
             * .get(att)).getAll(); while (values.hasMore()) {
             * output.append(values.next().toString()); }
             */
            output.append("\n");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(output.toString());
        }
    }

    /**
     * Gets the custom {@link SSLSocketFactory} class.
     *
     * @return {@code null} if no custom class is present.
     */
    protected Class<?> getCustomSSLSocketFactoryClass() {
        if (this.isLdapUseTrustSelfSignedSSL()) {
            return TrustSelfSignedCertSocketFactory.class;
        }
        return null;
    }

    /**
     * Sets SSL environment for {@link InitialLdapContext}.
     *
     * @param cm
     *            Configuration manager.
     * @param env
     *            Environment for {@link InitialLdapContext}.
     */
    private void setInitialLdapSSLContext(final ConfigManager cm,
            final Hashtable<String, String> env) {

        if (this.isLdapSSL()) {

            env.put(Context.SECURITY_PROTOCOL, CONTEXT_SECURITY_PROTOCOL_SSL);

            final Class<?> customClass = this.getCustomSSLSocketFactoryClass();
            if (customClass != null) {
                env.put(JAVA_NAMING_LDAP_FACTORY_SOCKET, customClass.getName());
            }
        }
    }

    /**
     * Creates the LDAP directory context with security credentials for the
     * Admin DN.
     *
     * @return The {@link InitialLdapContext}.
     */
    protected final InitialLdapContext createLdapContextForAdmin() {

        final Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                CONTEXT_INITIAL_CONTEXT_FACTORY_CLASS_NAME);

        env.put(Context.PROVIDER_URL, getProviderUrlBaseDn());

        env.put(JAVA_NAMING_BATCHSIZE, this.batchsize);

        /*
         * Use the credentials of the administrator (if present)
         */
        final ConfigManager cm = ConfigManager.instance();
        final String adminDn = cm.getConfigValue(Key.AUTH_LDAP_ADMIN_DN).trim();

        if (!adminDn.isEmpty()) {
            env.put(Context.SECURITY_AUTHENTICATION,
                    CONTEXT_SECURITY_AUTHENTICATION_SIMPLE);
            env.put(Context.SECURITY_PRINCIPAL, adminDn);
            env.put(Context.SECURITY_CREDENTIALS,
                    cm.getConfigValue(Key.AUTH_LDAP_ADMIN_PASSWORD));
        }

        this.setInitialLdapSSLContext(cm, env);

        final InitialLdapContext ctx;

        try {
            ctx = new InitialLdapContext(env, null);
        } catch (NamingException e) {
            if (e.getCause() == null) {
                throw new SpException(e.getMessage(), e);
            }
            throw new SpException(String.format("%s [%s]", e.getMessage(),
                    e.getCause().getMessage()), e);
        }

        return ctx;
    }

    /**
     * Creates the LDAP filter expression to select one or all groups.
     *
     * @param groupName
     *            The group name, or {@link #LDAP_SEARCH_WILDCARD_ALL}.
     * @return The LDAP filter expression.
     */
    protected final String getGroupSearchExpression(final String groupName) {

        if (this.groupSearchPattern == null) {

            this.groupSearchPattern =
                    getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_SEARCH);

            if (StringUtils.isBlank(this.groupSearchPattern)) {

                final String groupMemberField =
                        getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD);

                final StringBuilder builder = new StringBuilder();
                builder.append("(").append(groupMemberField).append("={0})");

                this.groupSearchPattern = builder.toString();
            }
        }

        return MessageFormat.format(this.groupSearchPattern, groupName);
    }

    /**
     * Creates the LDAP filter pattern to select a user name.
     *
     * @return The LDAP filter pattern.
     */
    protected abstract String createUserNameSearchPattern();

    /**
     * Gets the LDAP filter expression to select one or all user names.
     *
     * @param userName
     *            The user name, or {@link #LDAP_SEARCH_WILDCARD_ALL}.
     * @return The LDAP filter expression.
     */
    protected abstract String
            getUserNameSearchExpression(final String userName);

    /**
     *
     * @return
     */
    protected final String getUserNameSearchPattern() {

        if (this.userNameSearchPattern == null) {
            this.userNameSearchPattern = createUserNameSearchPattern();
        }
        return this.userNameSearchPattern;
    }

    @Override
    public final SortedSet<CommonUserGroup> getGroups() {

        final SortedSet<CommonUserGroup> sset =
                new TreeSet<>(new CommonUserGroupComparator());

        final String ldapFilterExpression =
                getGroupSearchExpression(LDAP_SEARCH_WILDCARD_ALL);

        final String providerUrl = getProviderUrlBaseDn();

        final InitialLdapContext ctx = createLdapContextForAdmin();

        try {

            final String groupNameField =
                    getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_NAME_FIELD);

            final String groupFullNameField =
                    getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_FULL_NAME_FIELD);

            final SearchControls searchControls = new SearchControls();

            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setCountLimit(0);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("get groups at [" + providerUrl + "] : "
                        + ldapFilterExpression);
            }
            /*
             * Paging the results.
             */
            final LdapSearchResultPager ldapPager =
                    new LdapSearchResultPager(ctx, searchControls,
                            ldapFilterExpression, getPageSize(ctx));

            boolean hasNextPage = true;

            while (hasNextPage) {

                final NamingEnumeration<SearchResult> results =
                        ldapPager.nextPage();

                try {
                    while (results.hasMoreElements()) {
                        sset.add(getCommonUserGroup(groupNameField,
                                groupFullNameField, results.next()));
                    }
                } finally {
                    closeResources(results, null);
                }

                hasNextPage = ldapPager.hasNextPage();
            }

        } catch (NameNotFoundException e) {
            throw new SpException(
                    "LDAP base context [" + this.baseDN + "] not found", e);
        } catch (NamingException e) {
            throw new SpException(e.getMessage(), e);
        } finally {
            closeResources(null, ctx);
        }

        return sset;
    }

    /**
     *
     * @param groupNameField
     *            The LDAP field that contains the group's name.
     * @param groupFullNameField
     *            The LDAP field that contains the group's full name.
     * @param searchResult
     *            The LDAP search result.
     * @return The {@link CommonUserGroup}
     * @throws NamingException
     *             When LDAP error.
     */
    private static CommonUserGroup getCommonUserGroup(
            final String groupNameField, final String groupFullNameField,
            final SearchResult searchResult) throws NamingException {

        final Attributes attributes = searchResult.getAttributes();

        final String groupName =
                attributes.get(groupNameField).get().toString();

        final String groupFullName;

        if (attributes.get(groupFullNameField) == null) {
            groupFullName = null;
        } else {
            groupFullName = attributes.get(groupFullNameField).get().toString();
        }

        return new CommonUserGroup(groupName, groupFullName);
    }

    @Override
    public final boolean isGroupPresent(final String groupName) {
        return this.getGroup(groupName) != null;
    }

    @Override
    public final CommonUserGroup getGroup(final String groupName) {

        CommonUserGroup commonUserGroup = null;

        final String providerUrl = getProviderUrlBaseDn();

        final String ldapFilterExpression = getGroupSearchExpression(groupName);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("find group [" + groupName + "] at [" + providerUrl
                    + "] : " + ldapFilterExpression);
        }

        final DirContext ctx = createLdapContextForAdmin();

        NamingEnumeration<SearchResult> results = null;

        try {

            final SearchControls controls = new SearchControls();

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Just one (1) group expected.
            controls.setCountLimit(1);

            results = ctx.search("", ldapFilterExpression, controls);

            if (results.hasMoreElements()) {
                commonUserGroup = getCommonUserGroup(
                        getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_NAME_FIELD),
                        getLdapConfigValue(
                                Key.LDAP_SCHEMA_GROUP_FULL_NAME_FIELD),
                        results.next());
            }

        } catch (NamingException e) {

            LOGGER.error(String.format("isGroupPresent(\"%s\"): %s", groupName,
                    e.getMessage()));

            commonUserGroup = null;

        } finally {
            closeResources(results, ctx);
        }

        return commonUserGroup;
    }

    /**
     *
     * @param ldapFilterExpression
     *            The LDAP expression.
     * @return The sorted users.
     */
    private SortedSet<CommonUser> getUsers(final String ldapFilterExpression) {

        final String providerUrl = getProviderUrlBaseDn();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getUsers() from [" + providerUrl + "] : "
                    + ldapFilterExpression);
        }

        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());

        final InitialLdapContext ctx = createLdapContextForAdmin();

        try {

            final SearchControls searchControls = new SearchControls();

            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setCountLimit(0);

            /*
             * Paging the results.
             */
            final LdapSearchResultPager ldapPager =
                    new LdapSearchResultPager(ctx, searchControls,
                            ldapFilterExpression, getPageSize(ctx));

            boolean hasNextPage = true;

            while (hasNextPage) {

                final NamingEnumeration<SearchResult> results =
                        ldapPager.nextPage();

                try {

                    while (results.hasMoreElements()) {

                        final SearchResult searchResult = results.next();

                        final Attributes attributes =
                                searchResult.getAttributes();

                        final CommonUser cuser = createCommonUser(attributes);

                        if (cuser != null && (cuser.isEnabled()
                                || this.allowDisabledUsers())) {
                            sset.add(cuser);
                        }
                    }

                } finally {
                    closeResources(results, null);
                }

                hasNextPage = ldapPager.hasNextPage();
            }

        } catch (

        NameNotFoundException e) {
            throw new SpException(
                    "LDAP base context [" + this.baseDN + "] not found", e);
        } catch (NamingException e) {
            throw new SpException(e.getMessage(), e);
        } finally {
            closeResources(null, ctx);
        }
        return sset;
    }

    @Override
    public final SortedSet<CommonUser> getUsers() {
        return getUsers(getUserNameSearchExpression(LDAP_SEARCH_WILDCARD_ALL));
    }

    @Override
    public final SortedSet<CommonUser> getUsersInGroup(final String groupName) {

        final ConfigManager cm = ConfigManager.instance();
        final String providerUrl = getProviderUrlBaseDn();

        final String ldapUserFullNameField = cm.getConfigValue(getLdapType(),
                Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD);

        final String ldapFilterExpression = getGroupSearchExpression(groupName);

        final SortedSet<CommonUser> sset =
                new TreeSet<>(new CommonUserComparator());

        final InitialLdapContext ctx = createLdapContextForAdmin();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getUsersInGroup() from [" + providerUrl + "] : "
                    + ldapFilterExpression);
        }

        try {

            final SearchControls searchControls = new SearchControls();

            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            // One group expected
            searchControls.setCountLimit(1);

            final String groupMemberField =
                    getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD);

            /*
             * Paged retrieval of group members attribute values.
             */
            final LdapMultiValuePager ldapPager = new LdapMultiValuePager(ctx,
                    searchControls, ldapFilterExpression, groupMemberField,
                    getRangeStepSize(ctx));

            while (ldapPager.hasNextRange()) {

                for (final String groupMember : ldapPager.nextRange()) {

                    final CommonUser cuser = commonUserFromGroupMember(ctx,
                            ldapUserFullNameField, groupMember);

                    if (cuser == null) {
                        LOGGER.trace("Group member [{}] is not a user.",
                                groupMember);
                    } else if (cuser.isEnabled() || this.allowDisabledUsers()) {
                        sset.add(cuser);
                    } else {
                        LOGGER.trace("User [{}] is disabled.", groupMember);
                    }
                }
            }

        } catch (NamingException e) {
            throw new SpException(String.format("%s | %s", e.getMessage(),
                    getResultControlSupportDiagnostic(ctx)), e);
        } finally {
            closeResources(null, ctx);
        }
        return sset;
    }

    /**
     * Creates a diagnostic message about LDAP ResultControlSupport.
     *
     * @param ctx
     *            The {@link InitialLdapContext}.
     * @return The diagnostic message string.
     */
    protected final String
            getResultControlSupportDiagnostic(final InitialLdapContext ctx) {
        String support = "[SavaPage Diagnostics for ResultControlSupport";

        try {
            support += ": PagedResultControl supported ("
                    + isPagedResultControlSupported(ctx) + ")";
            support += ", RangePropertyControl supported ("
                    + isRangePropertyControlSupported(ctx) + ")";

        } catch (NamingException e) {
            support = String.format(": Error %s: ", e.getMessage());
        }
        support += "]";

        return support;
    }

    /**
     * Creates the LDAP RDN from a full DN and a base DN.
     *
     * @param dnName
     *            Full DN.
     * @return The RDN.
     * @throws InvalidNameException
     *             When one of the DNs has bad syntax.
     */
    private LdapName ldapRdnFromDn(final String dnName)
            throws InvalidNameException {

        LdapName dn = new LdapName(dnName);
        LdapName basedn = new LdapName(this.baseDN);

        final List<Rdn> rdns = new ArrayList<>();

        for (int i = basedn.size(); i < dn.size(); i++) {
            rdns.add(dn.getRdn(i));
        }
        return new LdapName(rdns);
    }

    /**
     * Checks if group member is a user.
     *
     * @param attributes
     *            The LDAP group member attributes
     * @return {@code true} if member is a user (and not a nested group).
     * @throws NamingException
     *             When a LDAP error.
     */
    protected abstract boolean isUserGroupMember(final Attributes attributes)
            throws NamingException;

    /**
     * Creates a CommonUser object from an LDAP group member and places it on
     * the SortedSet.
     *
     * @param ctx
     *            The LDAP directory context (technical object).
     * @param ldapUserFullNameField
     *            The name of the LDAP field holding the full user name.
     * @param member
     *            If isPosixGroups is {@code true}, this member field contains
     *            the user's username. If {@code false}, then it contains the
     *            user's DN.
     * @return The CommonUser or {@code null} when the member was not found or
     *         is not a user.
     * @throws NamingException
     *             When LDAP errors.
     */
    protected final CommonUser commonUserFromGroupMember(final DirContext ctx,
            final String ldapUserFullNameField, final String member)
            throws NamingException {

        CommonUser cuser = null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("member [" + member + "]");
        }

        NamingEnumeration<SearchResult> results = null;

        try {

            Attributes attributes = null;

            if (this.isPosixGroups) {
                /*
                 * Group member field contains the user's username.
                 */
                final String ldapFilterExpression =
                        String.format("(%s=%s)", ldapUserFullNameField, member);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("filter [" + ldapFilterExpression + "]");
                }

                final SearchControls controls = new SearchControls();
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                controls.setCountLimit(0);

                results = ctx.search("", ldapFilterExpression, controls);

                if (results.hasMore()) {
                    SearchResult searchResult = results.next();
                    attributes = searchResult.getAttributes();
                }

            } else {
                /*
                 * Group member field contains the user's DN.
                 *
                 * Example:
                 *
                 * cn=Folkert Ravestein,ou=Studenten,dc=datraverse,dc=nl
                 *
                 * Create RDN from DN:
                 *
                 * cn=Folkert Ravestein,ou=Studenten
                 */
                final LdapName ldapName = ldapRdnFromDn(member);

                try {
                    attributes = ctx.getAttributes(ldapName);
                } catch (NamingException e) {
                    /*
                     * When member is outside the BaseDN scope.
                     */
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format(
                                "BaseDN [%s]: failed to get attributes "
                                        + "for member [%s] : %s",
                                this.getBaseDN(), member, e.getMessage()));
                    }
                }
            }

            if (attributes != null && isUserGroupMember(attributes)) {
                cuser = createCommonUser(attributes);
                if (cuser != null && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("member [" + cuser.getUserName() + "]");
                }
            }

        } finally {
            closeResources(results, null);
        }
        return cuser;
    }

    @Override
    public final boolean isUserInGroup(final String uid, final String group) {

        boolean found = false;

        final String providerUrl = getProviderUrlBaseDn();

        final String ldapFilterExpression = getUserNameSearchExpression(uid);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("find user [" + uid + "] at [" + providerUrl + "] : "
                    + ldapFilterExpression);
        }

        final DirContext ctx = createLdapContextForAdmin();

        NamingEnumeration<SearchResult> results = null;

        try {
            /*
             * Example:
             *
             * Find "(uid=folkert)" and get the DN
             *
             * dn: cn=Folkert Ravestein,ou=Studenten,dc=datraverse,dc=nl
             */
            final SearchControls controls = new SearchControls();

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Just one (1) user expected.
            controls.setCountLimit(1);

            results = ctx.search("", ldapFilterExpression, controls);

            if (results.hasMore()) {

                SearchResult searchResult = results.next();

                String dn = searchResult.getName();

                if (searchResult.isRelative()) {
                    dn += "," + this.baseDN;
                }

                /*
                 * Is DN part of the group?
                 */
                final String groupFilter = "(&("
                        + getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_NAME_FIELD)
                        + "=" + group + ")("
                        + getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD)
                        + "=" + dn + "))";

                // re-use the results
                results.close();
                results = null;

                results = ctx.search("", groupFilter, controls);
                found = results.hasMore();
            }

        } catch (NamingException e) {

            LOGGER.error(e.getMessage());

            found = false;

        } finally {
            closeResources(results, ctx);
        }
        return found;
    }

    @Override
    public final CommonUser getUser(final String uid) {

        CommonUser cuser = null;

        final String providerUrl = getProviderUrlBaseDn();

        /*
         * e.g. (uid=rijk) for OpenLDAP
         */
        final String ldapFilterExpression = getUserNameSearchExpression(uid);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("find user [" + uid + "] at [" + providerUrl + "] : "
                    + ldapFilterExpression);
        }

        final DirContext ctx = createLdapContextForAdmin();

        NamingEnumeration<SearchResult> results = null;

        try {
            /*
             * Example:
             *
             * Find "(uid=folkert)" and get the DN
             *
             * dn: cn=Folkert Ravestein,ou=Studenten,dc=datraverse,dc=nl
             */
            final SearchControls controls = new SearchControls();

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Just one (1) user expected.
            controls.setCountLimit(1);

            results = ctx.search("", ldapFilterExpression, controls);

            if (results.hasMore()) {

                final SearchResult searchResult = results.next();

                final String dn = searchResult.getName();
                String rdn = null;

                if (searchResult.isRelative()) {
                    rdn = dn;
                } else {
                    rdn = ldapRdnFromDn(dn).toString();
                }

                final Attributes attributes = ctx.getAttributes(rdn);

                cuser = createCommonUser(attributes);
            }

        } catch (NamingException e) {

            cuser = null;

        } finally {
            closeResources(results, ctx);
        }
        return cuser;
    }

    @Override
    public final RfidNumberFormat createRfidNumberFormat() {

        if (!isCardNumberProvided()) {
            return null;
        }

        RfidNumberFormat.FirstByte firstByte;
        RfidNumberFormat.Format format;

        ConfigManager cm = ConfigManager.instance();

        if (cm.getConfigValue(Key.LDAP_SCHEMA_USER_CARD_NUMBER_FIRST_BYTE)
                .equals(IConfigProp.CARD_NUMBER_FIRSTBYTE_V_LSB)) {
            firstByte = RfidNumberFormat.FirstByte.LSB;
        } else {
            firstByte = RfidNumberFormat.FirstByte.MSB;
        }

        if (cm.getConfigValue(Key.LDAP_SCHEMA_USER_CARD_NUMBER_FORMAT)
                .equals(IConfigProp.CARD_NUMBER_FORMAT_V_HEX)) {
            format = RfidNumberFormat.Format.HEX;
        } else {
            format = RfidNumberFormat.Format.DEC;
        }
        return new RfidNumberFormat(format, firstByte);
    }

    @Override
    public final boolean isIdNumberProvided() {
        return this.attrIdUserIdNumber != null;
    }

    @Override
    public final boolean isCardNumberProvided() {
        return this.attrIdUserCardNumber != null;
    }

    @Override
    public final boolean isEmailProvided() {
        return true;
    }

    /**
     * Closes the resources.
     *
     * @param results
     *            The results.
     * @param ctx
     *            The {@link DirContext}.
     */
    protected final void closeResources(
            final NamingEnumeration<SearchResult> results,
            final DirContext ctx) {

        if (results != null) {
            try {
                results.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        if (ctx != null) {
            try {
                ctx.close();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

    }

    /**
     * Is paged result control supported?
     *
     * @param ctx
     *            The {@link LdapContext}.
     * @return {@code true} if supported.
     * @throws NamingException
     *             When LDAP errors.
     */
    protected final boolean isPagedResultControlSupported(final LdapContext ctx)
            throws NamingException {
        return isControlSupported(ctx, PagedResultsControl.OID);
    }

    /**
     * Is control supported?
     *
     * @param ctx
     *            The {@link LdapContext}.
     * @param control
     *            The control to check.
     * @return {@code true} if supported.
     * @throws NamingException
     *             When LDAP errors.
     */
    private boolean isControlSupported(final LdapContext ctx,
            final String control) throws NamingException {
        return getSupportedControls(ctx).contains(control);
    }

    /**
     * Is range control supported?
     *
     * @param ctx
     *            The {@link LdapContext}.
     * @return {@code true} if supported.
     * @throws NamingException
     *             When LDAP errors.
     */
    protected final boolean isRangePropertyControlSupported(
            final LdapContext ctx) throws NamingException {
        return isControlSupported(ctx, OID_RANGE_PROPERTY);
    }

    /**
     *
     * @param ctx
     *            The {@link LdapContext}.
     * @return
     * @throws NamingException
     */
    private Set<String> getSupportedControls(final LdapContext ctx)
            throws NamingException {

        if (this.supportedControlSet != null) {
            return this.supportedControlSet;
        }

        this.supportedControlSet = new HashSet<>();

        final Attributes attrs = ctx.getAttributes(getProviderUrl(),
                new String[] { LDAP_ATTRID_SUPPORTED_CONTROL });

        if (attrs == null) {
            return this.supportedControlSet;
        }

        final Attribute attr = attrs.get(LDAP_ATTRID_SUPPORTED_CONTROL);

        if (attr == null) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("[" + LDAP_ATTRID_SUPPORTED_CONTROL
                        + "] field NOT found: do you have  "
                        + "permission to query the controls?");
            }

        } else {

            NamingEnumeration<?> controls = null;

            try {
                controls = attr.getAll();
                while (controls.hasMoreElements()) {
                    this.supportedControlSet.add(controls.next().toString());
                }
            } finally {
                closeQuietly(controls);
            }
        }

        return supportedControlSet;
    }

    /**
     *
     * @param enumeration
     */
    private void closeQuietly(final NamingEnumeration<?> enumeration) {

        if (enumeration != null) {
            try {
                enumeration.close();
            } catch (NamingException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(e.getMessage(), e);
                }
            }
        }
    }

}
