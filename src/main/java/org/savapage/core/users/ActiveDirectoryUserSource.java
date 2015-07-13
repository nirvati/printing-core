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
package org.savapage.core.users;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.IConfigProp.LdapType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ActiveDirectoryUserSource extends LdapUserSourceMixin {

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ActiveDirectoryUserSource.class);

    /**
     *
     */
    private static final String AD_ATTR_ACCOUNT_TYPE = "sAMAccountType";

    /**
     * sAMAccountType Value for SAM_USER_OBJECT.
     * <p>
     * See <a
     * href="http://msdn.microsoft.com/en-us/library/cc228417.aspx">here</a>.
     * </p>
     */
    private static final long SAM_USER_OBJECT = 0x30000000;

    /**
     * The matching rule is true only if all bits from the property match the
     * value. This rule is like the bitwise AND operator.
     * <p>
     * See: <a
     * href="http://support.microsoft.com/kb/269181">http://support.microsoft.
     * com/kb/269181</a>
     * </p>
     * <p>
     * See <a href="http://msdn.microsoft.com/en-us/library/cc223367.aspx">this
     * link</a>: <i>Unlike, for example, extended controls and extended
     * operations, there is no attribute exposed by the DC that specifies which
     * matching rules it supports.</i>
     * </p>
     */
    private static final String LDAP_MATCHING_RULE_BIT_AND =
            "1.2.840.113556.1.4.803";

    /**
     * The AD's "account disabled" account option.
     */
    private static final int UF_ACCOUNTDISABLE = 0x0002;

    /**
     * User Account Control (UAC) attribute.
     */
    private static final String USER_ACCOUNT_CONTROL = "userAccountControl";

    /**
     * LDAP filter to retrieve enabled users only.
     */
    private static final String LDAP_UAC_FILTER_ENABLED_USERS = "(!("
            + USER_ACCOUNT_CONTROL + ":" + LDAP_MATCHING_RULE_BIT_AND + ":="
            + UF_ACCOUNTDISABLE + "))";

    /**
     * Dummy LDAP filter to retrieve all users.
     */
    private static final String LDAP_UAC_FILTER_ALL_USERS = "";

    /**
     * The name of the LDAP attribute holding the object's DN.
     */
    private final String ldapDnField;

    /**
     * The name of the LDAP attribute holding the group name.
     */
    private final String ldapGroupField;

    /**
    *
    */
    private final String ldapUserAccountControlFilter;

    /**
     *
     */
    private final boolean filterDisabledUsersLocally;

    /**
     *
     */
    private final boolean allowDisabledUsers;

    /**
     *
     */
    public ActiveDirectoryUserSource() {

        super(LdapType.ACTD);

        final ConfigManager cm = ConfigManager.instance();

        this.ldapDnField = getLdapConfigValue(Key.LDAP_SCHEMA_DN_FIELD);

        this.ldapGroupField =
                getLdapConfigValue(Key.LDAP_SCHEMA_GROUP_NAME_FIELD);

        this.filterDisabledUsersLocally =
                getLdapConfigBoolean(Key.LDAP_FILTER_DISABLED_USERS_LOCALLY,
                        Boolean.FALSE).booleanValue();

        this.allowDisabledUsers =
                getLdapConfigBoolean(Key.LDAP_ALLOW_DISABLED_USERS,
                        Boolean.FALSE).booleanValue();

        if (this.allowDisabledUsers || this.filterDisabledUsersLocally) {
            this.ldapUserAccountControlFilter = LDAP_UAC_FILTER_ALL_USERS;
        } else {
            this.ldapUserAccountControlFilter = LDAP_UAC_FILTER_ENABLED_USERS;
        }
    }

    @Override
    protected String createUserNameSearchPattern() {
        return getLdapConfigValue(Key.LDAP_SCHEMA_USER_NAME_SEARCH);
    }

    @Override
    protected String getUserNameSearchExpression(final String userName) {
        return MessageFormat.format(this.getUserNameSearchPattern(), userName,
                this.ldapUserAccountControlFilter);
    }

    @Override
    protected boolean isUserGroupMember(final Attributes attributes)
            throws NamingException {

        final Attribute attr = attributes.get(AD_ATTR_ACCOUNT_TYPE);

        final boolean isUser;

        if (attr == null) {
            isUser = true;
        } else {
            isUser = Integer.parseInt(attr.get().toString()) == SAM_USER_OBJECT;
        }
        return isUser;
    }

    @Override
    public SortedSet<CommonUser> getUsersInGroup(final String groupName,
            final boolean nested) {

        final SortedSet<CommonUser> users;

        if (nested) {

            users = new TreeSet<>(new CommonUserComparator());

            for (final String group : getGroupHierarchy(groupName, false)) {
                users.addAll(getUsersInGroup(group));
            }

        } else {
            users = getUsersInGroup(groupName);
        }
        return users;
    }

    /**
     * Accumulates all the users from Active Directory belonging to a (nested)
     * group, sorted ascended (case insensitive) by user name.
     *
     * @deprecated See Mantis #416.
     *
     * @param groupName
     *            The parent group name.
     * @return The sorted set of users.
     */
    @Deprecated
    private SortedSet<CommonUser> getUsersInGroupNested(final String groupName) {

        final String providerUrl = getProviderUrlBaseDn();

        final String ldapUserFullNameField =
                getLdapConfigValue(Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD);

        final InitialLdapContext ctx = createLdapContextForAdmin();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getUsersInGroup(" + groupName + ") from ["
                    + providerUrl + "] : ");
        }

        final Set<String> nestedGroups = new TreeSet<>();

        final SortedSet<CommonUser> ssetUsers =
                new TreeSet<>(new CommonUserComparator());

        NamingEnumeration<SearchResult> results = null;

        try {

            final SearchControls controls = new SearchControls();

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Just one (1) group expected.
            controls.setCountLimit(1);

            final String ldapFilterExpression =
                    getGroupSearchExpression(groupName);

            results = ctx.search("", ldapFilterExpression, controls);

            if (results.hasMore()) {

                final SearchResult searchResultGroup = results.next();

                final String groupDn =
                        searchResultGroup.getAttributes().get(this.ldapDnField)
                                .get().toString();

                collectUsersFromGroup(ctx, ssetUsers, groupDn,
                        ldapUserFullNameField);

                collectNestedGroups(ctx, nestedGroups, groupDn);
            }

            closeResources(results, null);
            results = null;

            /*
             * Process the collected nested groups.
             */
            for (final String groupDn : nestedGroups) {
                collectUsersFromGroup(ctx, ssetUsers, groupDn,
                        ldapUserFullNameField);
            }

        } catch (NamingException e) {

            throw new SpException(e.getMessage(), e);

        } finally {
            closeResources(results, ctx);
        }

        return ssetUsers;

    }

    /**
     * Collects enabled users from a group.
     * <p>
     * This method uses {@link LdapSearchResultPager} which causes error 'error
     * code 12 - 00002040: SvcErr: DSID-031401F1, problem 5010
     * (UNAVAIL_EXTENSION)'. See Mantis #416.
     * </p>
     *
     * @deprecated Use {@link #getUsersInGroup(String)} instead.
     *
     * @param ldapContext
     *            The {@link InitialLdapContext};
     * @param sset
     *            The {@link SortedSet} to collect on.
     * @param groupDN
     *            The DN of the group.
     * @param ldapUserFullNameField
     *            The name of the LDAP field holding the full user name.
     * @throws NamingException
     *             When LDAP errors.
     */
    @Deprecated
    private void collectUsersFromGroup(final InitialLdapContext ldapContext,
            final SortedSet<CommonUser> sset, final String groupDN,
            final String ldapUserFullNameField) throws NamingException {

        final String ldapFilterPattern =
                getLdapConfigValue(Key.LDAP_SCHEMA_USER_NAME_GROUP_SEARCH);

        final String ldapFilterExpression =
                MessageFormat.format(ldapFilterPattern, groupDN,
                        this.ldapUserAccountControlFilter);

        final SearchControls searchControls = new SearchControls();

        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setCountLimit(0);

        searchControls.setReturningAttributes(new String[] {
                getAttrIdUserName(), this.ldapDnField });

        /*
         * Paging the results.
         */
        final LdapSearchResultPager ldapPager =
                new LdapSearchResultPager(ldapContext, searchControls,
                        ldapFilterExpression, getPageSize(ldapContext));

        boolean hasNextPage = true;

        while (hasNextPage) {

            final NamingEnumeration<SearchResult> results =
                    ldapPager.nextPage();

            try {

                while (results.hasMoreElements()) {

                    final SearchResult searchResult = results.next();

                    final String userNameOrDn =
                            searchResult.getAttributes().get(this.ldapDnField)
                                    .get().toString();

                    final CommonUser cuser =
                            commonUserFromGroupMember(ldapContext,
                                    ldapUserFullNameField, userNameOrDn);

                    if (cuser != null && cuser.isEnabled()) {
                        sset.add(cuser);
                    }

                    if (cuser != null && LOGGER.isTraceEnabled()) {

                        final StringBuilder builder = new StringBuilder();

                        builder.append("User [").append(cuser.getUserName())
                                .append("] [").append(cuser.getFullName())
                                .append("] ");

                        if (cuser.isEnabled()) {
                            builder.append("selected");
                        } else {
                            builder.append("not selected (DISABLED)");
                        }

                        LOGGER.trace(builder.toString());
                    }

                    if (cuser == null && LOGGER.isWarnEnabled()) {

                        final StringBuilder builder = new StringBuilder();

                        builder.append("No details found for group member [")
                                .append(userNameOrDn).append("].");

                        LOGGER.warn(builder.toString());
                    }

                }

            } finally {
                closeResources(results, null);
            }

            hasNextPage = ldapPager.hasNextPage();
        }

    }

    @Override
    protected boolean isUserEnabled(final Attributes userAttributes)
            throws NamingException {

        final boolean enabled;

        if (this.allowDisabledUsers) {

            enabled = true;

        } else if (this.filterDisabledUsersLocally) {

            final Attribute userAccountControl =
                    userAttributes.get(USER_ACCOUNT_CONTROL);

            if (userAccountControl == null || userAccountControl.get() == null) {
                enabled = false;
            } else {
                final int uac =
                        Integer.valueOf(userAccountControl.get().toString());
                enabled = (uac & UF_ACCOUNTDISABLE) != UF_ACCOUNTDISABLE;
            }
        } else {

            enabled = true;
        }
        return enabled;
    }

    /**
     *
     * @param ldapContext
     *            The {@link InitialLdapContext};
     * @param sset
     *            The set to collect the DN of nested group on.
     * @param groupDN
     *            The DN of the group.
     * @throws NamingException
     *             When LDAP errors.
     */
    private void collectNestedGroups(final InitialLdapContext ldapContext,
            final Set<String> sset, final String groupDN)
            throws NamingException {

        final String ldapFilterPattern =
                getLdapConfigValue(Key.LDAP_SCHEMA_NESTED_GROUP_SEARCH);

        final String ldapFilterExpression =
                MessageFormat.format(ldapFilterPattern, groupDN);

        final SearchControls searchControls = new SearchControls();

        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setCountLimit(0);

        searchControls
                .setReturningAttributes(new String[] { this.ldapDnField });

        /*
         * Paging the results.
         */
        final LdapSearchResultPager ldapPager =
                new LdapSearchResultPager(ldapContext, searchControls,
                        ldapFilterExpression, getPageSize(ldapContext));

        boolean hasNextPage = true;

        final List<String> nestedGroups = new ArrayList<>();

        while (hasNextPage) {

            final NamingEnumeration<SearchResult> results =
                    ldapPager.nextPage();

            try {

                while (results.hasMoreElements()) {

                    final SearchResult searchResult = results.next();

                    final String nestedGroupDn =
                            searchResult.getAttributes().get(this.ldapDnField)
                                    .get().toString();

                    if (!sset.contains(nestedGroupDn)) {
                        sset.add(nestedGroupDn);
                        nestedGroups.add(nestedGroupDn);
                    }
                }

            } finally {
                closeResources(results, null);
            }

            hasNextPage = ldapPager.hasNextPage();
        }

        for (final String nestedGroupDn : nestedGroups) {
            // recurse.
            collectNestedGroups(ldapContext, sset, nestedGroupDn);
        }

    }

    @Override
    public List<String> getGroupHierarchy(final String groupName,
            final boolean formatted) {

        final List<String> nestedGroupList = new ArrayList<>();
        final Set<String> collectedGroups = new HashSet<>();

        final String providerUrl = getProviderUrlBaseDn();

        final InitialLdapContext ctx = createLdapContextForAdmin();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getGroupHierachy(" + groupName + ") from ["
                    + providerUrl + "] : ");
        }

        NamingEnumeration<SearchResult> results = null;

        try {

            final SearchControls controls = new SearchControls();

            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Just one (1) group expected.
            controls.setCountLimit(1);

            controls.setReturningAttributes(new String[] { this.ldapDnField,
                    this.ldapGroupField });

            final String ldapFilterExpression =
                    getGroupSearchExpression(groupName);

            results = ctx.search("", ldapFilterExpression, controls);

            if (results.hasMore()) {

                final SearchResult searchResultGroup = results.next();

                final String nestedGroupName =
                        searchResultGroup.getAttributes()
                                .get(this.ldapGroupField).get().toString();

                final String groupDn =
                        searchResultGroup.getAttributes().get(this.ldapDnField)
                                .get().toString();

                if (formatted) {
                    nestedGroupList.add(groupDn);
                } else {
                    nestedGroupList.add(nestedGroupName);
                }

                collectedGroups.add(groupDn);

                collectGroupHierarchy(ctx, nestedGroupList, collectedGroups,
                        groupDn, 1, formatted);
            }

            closeResources(results, null);
            results = null;

        } catch (NamingException e) {

            nestedGroupList.add("ERROR [" + e.getClass().getName() + "] "
                    + e.getMessage());

        } finally {
            closeResources(results, ctx);
        }

        return nestedGroupList;
    }

    /**
     * Recursively collects nested groups.
     *
     * @param ldapContext
     * @param nestedGroupList
     * @param collectedGroups
     * @param groupDN
     * @param iLevel
     * @param formatted
     *            When {@code true} groups formatted, i.e. the DN name and group
     *            name are indented (with leading space) according to nesting
     *            level. When {@code false} a flat list of nested groups is
     *            returned.
     * @throws NamingException
     */
    private void collectGroupHierarchy(final InitialLdapContext ldapContext,
            final List<String> nestedGroupList,
            final Set<String> collectedGroups, final String groupDN,
            final int iLevel, final boolean formatted) throws NamingException {

        final String ldapFilterPattern =
                getLdapConfigValue(Key.LDAP_SCHEMA_NESTED_GROUP_SEARCH);

        final String ldapFilterExpression =
                MessageFormat.format(ldapFilterPattern, groupDN);

        final SearchControls searchControls = new SearchControls();

        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setCountLimit(0);

        searchControls.setReturningAttributes(new String[] { this.ldapDnField,
                this.ldapGroupField });

        /*
         * Paging the results.
         */
        final LdapSearchResultPager ldapPager =
                new LdapSearchResultPager(ldapContext, searchControls,
                        ldapFilterExpression, getPageSize(ldapContext));

        boolean hasNextPage = true;

        final List<String> nestedGroups = new ArrayList<>();

        while (hasNextPage) {

            final NamingEnumeration<SearchResult> results =
                    ldapPager.nextPage();

            try {

                while (results.hasMoreElements()) {

                    final SearchResult searchResult = results.next();

                    final String nestedGroupName =
                            searchResult.getAttributes()
                                    .get(this.ldapGroupField).get().toString();

                    final String nestedGroupDn =
                            searchResult.getAttributes().get(this.ldapDnField)
                                    .get().toString();

                    if (formatted) {

                        String nestedName =
                                String.format("%s%s",
                                        StringUtils.repeat("    ", iLevel),
                                        nestedGroupDn);

                        if (collectedGroups.contains(nestedGroupDn)) {
                            nestedName += " [...]";
                        } else {
                            nestedGroups.add(nestedGroupDn);
                        }

                        nestedGroupList.add(nestedName);

                    } else {

                        String nestedName = nestedGroupName;

                        if (!collectedGroups.contains(nestedGroupDn)) {
                            nestedGroups.add(nestedGroupDn);
                            nestedGroupList.add(nestedName);
                        }
                    }

                }

            } finally {
                closeResources(results, null);
            }

            hasNextPage = ldapPager.hasNextPage();
        }

        for (final String nestedGroupDn : nestedGroups) {

            collectedGroups.add(nestedGroupDn);

            // recurse.
            collectGroupHierarchy(ldapContext, nestedGroupList,
                    collectedGroups, nestedGroupDn, iLevel + 1, formatted);
        }

    }
}
