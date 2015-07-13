/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.config;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.dao.ConfigPropertyDao;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.jpa.Entity;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public final class ConfigPropImpl implements IConfigProp {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfigPropImpl.class);

    /**
     *
     */
    private final Map<Key, String> myPropNameByKey = new HashMap<>();
    /**
     *
     */
    private final Map<String, Prop> myPropByName = new HashMap<>();
    /**
     *
     */
    private final Map<String, ConfigProperty> myDbCache =
            new ConcurrentHashMap<>();

    /**
     *
     */
    private final Map<LdapType, Map<Key, LdapProp>> myLdapDefaults =
            new HashMap<>();

    /**
     * Indicator to check whether the application is runnable or not.
     */
    private boolean myIsRunnable = false;

    /**
     *
     */
    private LdapProp[] myLdapProps = null;

    /**
     * <p>
     * NOTE: My OpenLDAP test differs from Standard Unix / Open Directory
     * </p>
     */
    @Override
    public LdapProp[] getLdapProps() {

        return new LdapProp[] {

                /*
                 * OpenLDAP
                 */
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD, "member"),
                //
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_GROUP_NAME_FIELD, "cn"),

                //
                new LdapProp(LdapType.OPEN_LDAP, Key.LDAP_SCHEMA_GROUP_SEARCH,
                        "(&(cn={0})(objectClass=groupOfNames))"),

                new LdapProp(LdapType.OPEN_LDAP, Key.LDAP_SCHEMA_POSIX_GROUPS,
                        V_NO),

                //
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_USER_DEPARTMENT_FIELD,
                        "departmentNumber"),
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_USER_EMAIL_FIELD, "mail"),
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD, "cn"),
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_USER_NAME_FIELD, "uid"),
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_USER_NAME_SEARCH, "(uid={0})"),
                // not set
                new LdapProp(LdapType.OPEN_LDAP,
                        Key.LDAP_SCHEMA_USER_OFFICE_FIELD, null),

                /*
                 * Apple Open Directory
                 */
                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD, "memberUid"),

                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_GROUP_NAME_FIELD, "cn"),

                new LdapProp(LdapType.OPEN_DIR, Key.LDAP_SCHEMA_GROUP_SEARCH,
                        "(memberUid={0})"),

                new LdapProp(LdapType.OPEN_DIR, Key.LDAP_SCHEMA_POSIX_GROUPS,
                        V_YES),

                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_USER_DEPARTMENT_FIELD,
                        "departmentNumber"),
                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_USER_EMAIL_FIELD, "mail"),
                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD, "cn"),
                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_USER_NAME_FIELD, "uid"),
                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_USER_NAME_SEARCH, "(uid={0})"),
                // not set
                new LdapProp(LdapType.OPEN_DIR,
                        Key.LDAP_SCHEMA_USER_OFFICE_FIELD, null),

                /*
                 * Novell eDirectory
                 */
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD,
                        "member"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_GROUP_NAME_FIELD,
                        "cn"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_GROUP_SEARCH,
                        "(&(member={0})(objectClass=groupOfNames))"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_POSIX_GROUPS, V_NO),
                new LdapProp(LdapType.EDIR,
                        Key.LDAP_SCHEMA_USER_DEPARTMENT_FIELD, "OU"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_USER_EMAIL_FIELD,
                        "mail"),
                new LdapProp(LdapType.EDIR,
                        Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD, "fullName"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_USER_NAME_FIELD,
                        "cn"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_USER_NAME_SEARCH,
                        "(&(cn={0})(objectClass=person))"),
                new LdapProp(LdapType.EDIR, Key.LDAP_SCHEMA_USER_OFFICE_FIELD,
                        "l"),

                /*
                 * Microsoft Active Directory
                 */
                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_GROUP_MEMBER_FIELD,
                        "member"),
                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_GROUP_NAME_FIELD,
                        "sAMAccountName"),

                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_GROUP_SEARCH,
                        "(&(sAMAccountName={0})(objectCategory=group))"),

                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_POSIX_GROUPS, V_NO),
                new LdapProp(LdapType.ACTD,
                        Key.LDAP_SCHEMA_USER_DEPARTMENT_FIELD, "department"),
                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_USER_EMAIL_FIELD,
                        "mail"),
                new LdapProp(LdapType.ACTD,
                        Key.LDAP_SCHEMA_USER_FULL_NAME_FIELD, "displayName"),
                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_USER_NAME_FIELD,
                        "sAMAccountName"),
                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_USER_NAME_SEARCH,
                        "(&(sAMAccountName={0})(objectCategory=person)"
                                + "(objectClass=user)"
                                + "(sAMAccountType=805306368){1})"),
                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_USER_OFFICE_FIELD,
                        "physicalDeliveryOfficeName"),

                // Active Directory Only.

                new LdapProp(LdapType.ACTD, Key.LDAP_ALLOW_DISABLED_USERS, V_NO),
                new LdapProp(LdapType.ACTD,
                        Key.LDAP_FILTER_DISABLED_USERS_LOCALLY, V_YES),

                new LdapProp(LdapType.ACTD,
                        Key.LDAP_SCHEMA_USER_NAME_GROUP_SEARCH,
                        "(&(memberOf={0})(objectCategory=person)"
                                + "(objectClass=user)"
                                + "(sAMAccountType=805306368){1})"),

                new LdapProp(LdapType.ACTD,
                        Key.LDAP_SCHEMA_NESTED_GROUP_SEARCH,
                        "(&(memberOf={0})(objectCategory=group))"),

                new LdapProp(LdapType.ACTD, Key.LDAP_SCHEMA_DN_FIELD,
                        "distinguishedName"),

        //
        };
    }

    @Override
    public void updateValue(final Key key, final String value,
            final String actor) {

        final String name = myPropNameByKey.get(key);

        ConfigProperty prop = myDbCache.get(name);

        if (prop == null) {
            throw new SpException("ConfigProperty [" + name + "] not found");
        }

        prop.setValue(value);
        prop.setModifiedBy(actor);
        prop.setModifiedDate(new Date());

        ServiceContext.getDaoContext().getConfigPropertyDao().update(prop);

        /*
         * Update the value in the dictionary, so a calcRunnable will work with
         * the right value.
         */
        myPropByName.get(name).setValue(value);
    }

    /**
     * Saves (updates or lazy inserts) the string value of a configuration key
     * in the <b>database</b> and updates the internal cache.
     * <p>
     * NOTE: the key NEED NOT exist in the cache.
     * </p>
     *
     * @param key
     *            The key as enum.
     * @param value
     *            The string value.
     * @param actor
     *            The actor.
     */
    public void saveDbValue(final Key key, final String value,
            final String actor) {

        final ConfigPropertyDao dao =
                ServiceContext.getDaoContext().getConfigPropertyDao();

        final String name = myPropNameByKey.get(key);

        ConfigProperty prop = dao.findByName(name);

        if (prop == null) {

            prop = new ConfigProperty();

            prop.setValue(value);
            prop.setPropertyName(name);
            prop.setCreatedBy(actor);

            /*
             * Fill standard attributes
             */
            final Date now = new Date();
            prop.setModifiedBy(prop.getCreatedBy());
            prop.setCreatedDate(now);
            prop.setModifiedDate(now);

            /*
             * Insert
             */
            dao.create(prop);

            myDbCache.put(name, prop);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ad-hoc: Insert Db -> Dict: " + name + " ["
                        + value + "]");
            }

        } else {

            prop.setValue(value);
            prop.setModifiedBy(actor);
            prop.setModifiedDate(new Date());

            /*
             * Update
             */
            dao.update(prop);
        }

        /*
         * Update the value in the dictionary, so a calcRunnable will work with
         * the right value.
         */
        myPropByName.get(name).setValue(value);

    }

    @Override
    public void saveValue(final Key key, final String value) {

        final ConfigPropertyDao dao =
                ServiceContext.getDaoContext().getConfigPropertyDao();

        final String name = myPropNameByKey.get(key);

        ConfigProperty prop = myDbCache.get(name);

        if (prop == null) {

            prop = new ConfigProperty();

            prop.setValue(value);
            prop.setPropertyName(name);
            prop.setCreatedBy(Entity.ACTOR_SYSTEM);

            /*
             * Fill standard attributes
             */
            Date now = new Date();
            prop.setModifiedBy(prop.getCreatedBy());
            prop.setCreatedDate(now);
            prop.setModifiedDate(now);

            /*
             * Insert
             */
            dao.create(prop);

            myDbCache.put(name, prop);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ad-hoc: Dict -> Db: " + name + " [" + value + "]");
            }

        } else {

            prop.setValue(value);
            prop.setModifiedBy(Entity.ACTOR_SYSTEM);
            prop.setModifiedDate(new Date());

            /*
             * Update
             */
            dao.update(prop);
        }

        /*
         * Update the value in the dictionary, so a calcRunnable will work with
         * the right value.
         */
        myPropByName.get(name).setValue(value);

    }

    /**
     *
     * @param key
     * @param value
     */
    private void saveValue(final Key key, final boolean value) {
        if (value) {
            saveValue(key, V_YES);
        } else {
            saveValue(key, V_NO);
        }
    }

    @Override
    public Set<String> getSet(final Key key) {
        /**
         * Retrieves the value from the cached DB values. If the key is not
         * present in the cache an empty {@link Set} is returned.
         */
        final Set<String> values = new HashSet<>();
        final String commaList = getString(key);
        if (StringUtils.isNotBlank(commaList)) {
            for (String displayName : StringUtils.split(commaList, ',')) {
                values.add(displayName.trim());
            }
        }
        return values;
    }

    @Override
    public String getString(final Key key) {
        /**
         * Retrieves the value from the cached DB values. If the key is not
         * present in the cache an empty string is returned.
         */
        ConfigProperty configProp = myDbCache.get(myPropNameByKey.get(key));
        if (configProp == null) {
            return "";
        }
        return configProp.getValue();
    }

    @Override
    public double getDouble(final Key key) {
        return Double.parseDouble(getString(key));
    }

    @Override
    public BigDecimal getBigDecimal(final Key key) {
        return new BigDecimal(getString(key));
    }

    @Override
    public long getLong(final Key key) {
        return Long.parseLong(getString(key));
    }

    @Override
    public int getInt(final Key key) {
        return Integer.parseInt(getString(key));
    }

    @Override
    public boolean getBoolean(final Key key) {
        return getString(key).equals(V_YES);
    }

    @Override
    public void init(final Properties defaultProps) {

        myIsRunnable = false;

        initDictionaries(defaultProps);
    }

    @Override
    public void initRunnable() {
        initDbCache();
        calcRunnable();
    }

    @Override
    public String getKey(final Key key) {
        return myPropNameByKey.get(key);
    }

    @Override
    public Key getKey(final String key) {
        final Prop prop = myPropByName.get(key);
        if (prop == null) {
            return null;
        }
        return prop.getKey();
    }

    /**
     * Initializes the dictionary with configuration properties.
     * <p>
     * Note: NO database access is needed for this action.
     * </p>
     *
     * @param defaultProps
     *            Properties with defaults that override the defaults from the
     *            dictionary.
     */
    private void initDictionaries(final Properties defaultProps) {

        myPropNameByKey.clear();
        myPropByName.clear();

        /*
         * Traverse the array of property definitions and apply the supplied
         * (external) default (if present).
         */
        for (final Key theKey : Key.values()) {

            final Prop prop = theKey.getProperty();

            myPropNameByKey.put(prop.getKey(), prop.getName());
            myPropByName.put(prop.getName(), prop);

            final String value = defaultProps.getProperty(prop.getName());

            if (value != null) {
                prop.setValue(value);
            }
        }

        /**
         * Lazy initialize LDAP default properties and lookup cache.
         */
        if (myLdapProps == null) {

            myLdapProps = getLdapProps();

            for (final LdapProp prop : myLdapProps) {

                if (!myLdapDefaults.containsKey(prop.getLdapType())) {
                    myLdapDefaults.put(prop.getLdapType(),
                            new HashMap<Key, LdapProp>());
                }
                myLdapDefaults.get(prop.getLdapType()).put(prop.getKey(), prop);
            }
        }

    }

    /**
     * Reads the properties from the database and puts them in a cache for easy
     * access.
     * <p>
     * Properties in the database which are NOT in the dictionary are NOT put in
     * the cache (a warning message is logged).
     * </p>
     */
    private void initDbCache() {

        myDbCache.clear();

        final ConfigPropertyDao dao =
                ServiceContext.getDaoContext().getConfigPropertyDao();

        final ConfigPropertyDao.ListFilter filter =
                new ConfigPropertyDao.ListFilter();

        final List<ConfigProperty> propsDb =
                dao.getListChunk(filter, null, null,
                        ConfigPropertyDao.Field.NAME, true);

        for (ConfigProperty propDb : propsDb) {

            final String name = propDb.getPropertyName();
            final String value = propDb.getValue();

            final Prop propDict = myPropByName.get(name);

            if (propDict == null) {
                /*
                 * REMOVE property that is NOT present in dictionary
                 */
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("ConfigProperty [" + name
                            + "] REMOVED from Database, because "
                            + "NOT found in dictionary.");
                }

                dao.delete(propDb);

            } else {
                /*
                 * Add to cache
                 */
                myDbCache.put(name, propDb);
                /*
                 * Update dictionary
                 */
                propDict.setValue(value);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Db: ConfigProperty [" + name + "] [" + value
                            + "]");
                }
            }
        }
    }

    /**
     * Checks if the application is runnable by configuration standards.
     *
     * <p>
     * The method traverses the config property dictionary. Every <b>valid</b>
     * property which is NOT in the database, is ad-hoc inserted in the
     * database.
     * </p>
     * <p>
     * As a result the {@linkplain #myIsRunnable} instance variable is set, and
     * the {@linkplain IConfigProp.Key#SYS_SETUP_COMPLETED} is set to
     * {@code true} if the application is runnable.
     * </p>
     *
     * @return <code>true</code> is all configurations properties are valid.
     *         <code>false</code> when at least one property has an invalid
     *         value.
     */
    @Override
    public synchronized boolean calcRunnable() {

        boolean isValid = true;

        for (final Key theKey : Key.values()) {

            final Prop prop = theKey.getProperty();

            ValidationResult res = prop.validate();

            if (!res.isValid()) {
                isValid = false;
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("" + prop.getName() + " [" + res.getMessage()
                            + "] is NOT valid");
                }
            }

            /*
             * IMPORTANT: even if the value is invalid, we add it to the
             * database and cache.
             */
            final String msg =
                    prop.getName() + " [" + prop.valueAsString() + "]";

            if (myDbCache.containsKey(prop.getName())) {

                final String valueDict = prop.valueAsString();
                final String valueDb = myDbCache.get(prop.getName()).getValue();

                if (LOGGER.isDebugEnabled()) {
                    if (valueDb != null && valueDb.equals(valueDict)) {
                        LOGGER.debug("Dict == Db: " + msg);
                    } else {
                        LOGGER.debug("Dict <> Db: " + msg);
                    }
                }

            } else {
                /*
                 * Insert dictionary item into database
                 */
                ConfigProperty configProp = new ConfigProperty();
                configProp.setPropertyName(prop.getName());
                configProp.setValue(prop.valueAsString());
                configProp.setCreatedBy(ConfigProperty.ACTOR_SYSTEM);

                final ConfigPropertyDao dao =
                        ServiceContext.getDaoContext().getConfigPropertyDao();

                myDbCache.put(prop.getName(), dao.findByNameInsert(configProp));

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Dict -> Db: " + msg);
                }
            }
        }

        if (isValid != myIsRunnable) {
            saveValue(Key.SYS_SETUP_COMPLETED, isValid);
        }

        myIsRunnable = isValid;

        return myIsRunnable;
    }

    @Override
    public boolean isRunnable() {
        return myIsRunnable;
    }

    @Override
    public ValidationResult validate(final Key key, final String value) {
        return Prop.validate(myPropByName.get(getKey(key)), value);
    }

    @Override
    public String getString(final LdapType ldapType, final Key key) {
        String value = getString(key);
        if (value == null || value.trim().isEmpty()) {
            final LdapProp prop = myLdapDefaults.get(ldapType).get(key);
            if (prop != null) {
                value = prop.getValue();
            }
        }
        return value;
    }

    @Override
    public Boolean getBoolean(final LdapType ldapType, final Key key) {

        final String value = getString(ldapType, key);
        final Boolean boolValue;

        if (value == null) {
            boolValue = null;
        } else {
            boolValue = value.equals(V_YES);
        }

        return boolValue;
    }

    @Override
    public Prop getProp(final Key key) {
        return getProp(myPropNameByKey.get(key));
    }

    @Override
    public Prop getProp(final String name) {
        return myPropByName.get(name);
    }

    @Override
    public boolean isMultiLine(final Key key) {
        return getProp(key).isMultiLine();
    }

    @Override
    public boolean isBigDecimal(final Key key) {
        return getProp(key).isBigDecimal();
    }

}
