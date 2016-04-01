/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.core.community;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.VersionInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.iharder.Base64;

/**
 * Runtime Membership status information.
 *
 * @author Datraverse B.V.
 *
 */
public final class MemberCard {

    /**
     *
     */
    private static DaoContext getDaoContext() {
        return ServiceContext.getDaoContext();
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link MemberCard#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final MemberCard INSTANCE = new MemberCard();
    }

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MemberCard.class);

    /**
     *
     */
    public enum Stat {

        /**
         * No Member Card file present, 5 users or less.
         */
        VISITOR_EDITION,

        /**
         * No Member Card file present: visitor.
         */
        VISITOR,

        /**
         * Not registered, the visiting period expired.
         */
        VISITOR_EXPIRED,

        /**
         * Valid Member Card, all conditions are met.
         */
        VALID,

        /**
         * Oops, wrong community.
         */
        WRONG_COMMUNITY,

        /**
         * Member Card is for wrong module.
         */
        WRONG_MODULE,

        /**
         * Member Card is for wrong version.
         */
        WRONG_VERSION,

        /**
         * Member Card is for wrong version, but within grace period.
         */
        WRONG_VERSION_WITH_GRACE,

        /**
         * Member Card expired.
         */
        EXPIRED,

        /**
         * Membership user limit exceeded.
         */
        EXCEEDED,

    };

    /**
     *
     */
    private enum MembershipUpgInsurance {
        /**
         * No upgrade insurance.
         */
        NONE,

        /**
         * Membership upgrade insurance is active.
         */
        ACTIVE,

        /**
         * Membership insurance expired.
         */
        EXPIRED
    };

    /**
     *
     */
    private final MembershipModule myLicModule = new MembershipModule();

    /**
     *
     */
    private static final String MEMBERCARD_FILE_BASENAME =
            "savapage.membercard";

    /**
     *
     */
    private static final int VISITOR_PERIOD_DAYS = 40;

    /**
     *
     */
    private static final int DAYS_WARN_BEFORE_UPG_EXPIRE = 30;

    /**
     * If a membership started within 90 days of a major release then user is
     * eligible to run this new version.
     */
    private static final int DAYS_UPG_GRACE = 90;

    /**
     *
     */
    private static final String ALLOW_UPDATES_WITHIN_NEXT_VERSION =
            "ALLOW_UPDATES_WITHIN_NEXT_VERSION";

    /**
     *
     */
    @SuppressWarnings("unused")
    private static final String ALLOW_UPDATES_WITHIN_SAME_VERSION =
            "ALLOW_UPDATES_WITHIN_SAME_VERSION";

    /**
     *
     */
    private static final String BASE64_PUBLIC_KEY_MEMBERCARD =
            ""
                    + "H4sIAAAAAAAAAFvzloG1uIhBJCuxLFGvODW5tCizpFLPO7UyKLVg70//zR2zljozMbD4MHAm5qTn"
                    + "AyUzcksYhHxAyvVzEvPS9YNLijLz0q2jGdhT85LzU1JTShiYop18GNjS8otyE0sKGeoYGH0YWEoq"
                    + "C1JLGKQhGmH26EPsUQkBSlpXFJQwMLsEO5YWgQxY81n8BxtHyAMmBoaKAgYGxj0GTYw7gFiHjV2r"
                    + "zeOcBQsjkCPP1NjI8Lc+uFG2VEgz6L7XHL03T55/E9weZPPeheGwnH3DtkC1TNdYB6XA35G2vRG/"
                    + "9h/9usvg2+nZoTnXG60bZE3yP6UlbM+eGbB0yfz5L1iqBZQO+e9eef3f9mPSP6zDnx9bsUyUn+V3"
                    + "87fLR+UOM5mGCEeJTTT6Vvp5nXbida33n5SYJede9GA8ziTKMD0hoF9UmfvMpkk7mxa9buH+ECHz"
                    + "FeS07w8XtF6bbXvv9J7VMWY7wndWTlm/+5fVq6af4T7ctuxN6YGR4X27rkT6PytkF2hs2OIpVqj8"
                    + "wkdDTHj7eU6jnhPLHtqIVXWH1PRqPFi8Tk5787LSiXmL67l/KZomffydVMWoYn1mycd9KwIndK5o"
                    + "vv8w6ul8tkndaXEN1aGqKYw+1v/Oe2oxN7YygNzR7Mq4rUZo3bE9M2fK7GxM4dl9xIS9btXqn0rZ"
                    + "Cwq63t2unqymNCVWstMpetbp91e8tPlmR225npl+uNzSTf/Yd5lS+Yn1jMssz365u/YW25ezuz/0"
                    + "/1/XfKjozO3M3ttrjq4TFlht48qpdnpd8aH5QeUfm/OWH517WOlq1A4uLQ7BUw8/MJneLmFgjdAz"
                    + "NbCsK2KQxJbQwAmAAQqEgDFexMAHVgdKYXqueaW5yJLAdMIWEOrk4+kMALl/vou9AgAA";

    /**
     * As an extra security measure we use this value to check the MD5 of the
     * BASE64_PUBLIC_KEY_MEMBERCARD.
     */
    public static final String MD5_PUBLIC_KEY_MEMBERCARD =
            "5958cd46c39f140056190cd4ac2323e4";

    /**
     *
     */
    private java.security.PublicKey thePublicMemberCardKey = null;

    /**
     * The raw Member Card properties as read from file.
     */
    private Properties myMemberCardProps = new Properties();

    /**
     * Indication if application is registered. If there was a readable (valid
     * format) Member Card file this attribute will be {@code true}.
     */
    private boolean myHasMemberCardFile = false;

    /**
     * The number of member participants.
     */
    private long memberParticipants = 0;

    /**
     *
     */
    private final MemberCardManager myLicManager = new MemberCardManager();

    /**
     *
     */
    private MembershipUpgInsurance myLicUpgInsurance =
            MembershipUpgInsurance.NONE;

    /**
     *
     */
    private Stat myMembershipStat = Stat.VALID;

    /**
     *
     */
    private Date myCardExpDate = null;

    /**
     *
     */
    @SuppressWarnings("unused")
    private boolean myCardExpiring = false;

    /**
     *
     */
    @SuppressWarnings("unused")
    private Date myCardSupportExpDate = null;

    /**
     *
     */
    private Date myCardIssueDate = null;

    /**
     *
     */
    private Date myCardUpgExpDate = null;

    /**
     *
     */
    @SuppressWarnings("unused")
    private boolean myCardUpgExpiring = false;

    /**
     * User friendly formatted membership expiration string.
     */
    private String myMembershipExpDateString = null;

    /**
     * Days to go before the membership expires.
     */
    private Long myMembershipDaysTillExpiry = null;

    /**
     *
     */
    private MemberCard() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static MemberCard instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * The max number of users in the Visitor edition.
     * <p>
     * SavaPage will be fully functional if user count remains below this
     * number.
     * </p>
     *
     */
    private static long getVisitorEditionUsers() {
        return 5;
    }

    /**
     *
     * @return
     */
    public boolean hasMemberCardFile() {
        return myHasMemberCardFile;
    }

    /**
     * Get the days till expiration.
     *
     * @return {@code null} if the membership does not have an expiration date.
     */
    public Long getDaysTillExpiry() {
        return myMembershipDaysTillExpiry;
    }

    /**
     * Get the expiration date.
     *
     * @return {@code null} if the membership does not have an expiration date.
     */
    public Date getExpirationDate() {
        return myCardExpDate;
    }

    /**
     * Get the expiration date as formatted string.
     *
     * @return {@code null} if the membership does not have an expiration date.
     */
    public String getExpirationString() {
        return myMembershipExpDateString;
    }

    /**
     *
     * @return
     */
    public static File getMemberCardFile() {
        return new File(ConfigManager.getServerHome() + "/"
                + MEMBERCARD_FILE_BASENAME);
    }

    /**
     *
     * @param memberCardFile
     * @return
     */
    public boolean isMemberCardFormatValid(final File memberCardFile) {

        if (memberCardFile.exists()) {

            InputStream istrMemberCard = null;

            try {
                istrMemberCard = new FileInputStream(memberCardFile);

                final boolean bZipped = true;
                final Properties memberCardProperties = new Properties();

                return myLicManager.checkMemberCardFormat(
                        getMemberCardPublicKey(), istrMemberCard, bZipped,
                        memberCardProperties);

            } catch (FileNotFoundException e) {
                //
            } catch (MemberCardException e) {
                //
            } finally {
                if (istrMemberCard != null) {
                    try {
                        istrMemberCard.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        }
        return false;
    }

    /**
     * Reads the start date of the visitor period.
     *
     * @return {@code null} when there is no visitor period active.
     */
    private Date readVisitorPeriodStart() {

        Date visitorPeriodStart = null;

        final ConfigManager cm = ConfigManager.instance();

        final String strInstalled =
                cm.getConfigValue(Key.COMMUNITY_VISITOR_START_DATE);

        if (StringUtils.isNotBlank(strInstalled)) {
            if (!strInstalled.equals(cm.createInitialVisitorStartDate())) {
                visitorPeriodStart =
                        cm.cipher().decryptVisitorStartDate(strInstalled);
            }
        }
        return visitorPeriodStart;
    }

    /**
     * Writes the start date of the visitor period.
     *
     * @param start
     *            Start date of the visitor period or {@code null} when there no
     *            visitor period active.
     */
    private void writeVisitorPeriodStart(final Date start) {

        ConfigManager cm = ConfigManager.instance();

        if (start == null) {
            cm.updateConfigKey(Key.COMMUNITY_VISITOR_START_DATE,
                    cm.createInitialVisitorStartDate(), Entity.ACTOR_SYSTEM);
        } else {
            cm.updateConfigKey(Key.COMMUNITY_VISITOR_START_DATE, cm.cipher()
                    .encryptVisitorStartDate(start), Entity.ACTOR_SYSTEM);
        }
    }

    /**
     * Clears the visitor period.
     */
    private void clearVisitorPeriod() {
        writeVisitorPeriodStart(null);
    }

    /**
     * Resets the visitor period (starting a new period NOW).
     */
    private Date resetVisitorPeriod() {
        final Date now = new Date();
        writeVisitorPeriodStart(now);
        return now;
    }

    /**
     * Initializes the Membership information. Checks the Member Card file
     * properties (if present), interprets the membership state and resets or
     * clears the start of the visitor/grace period.
     * <p>
     * Special care is taken for <i>membership state transitions</i> and the use
     * of {@link #resetVisitorPeriod()} and {@link #clearVisitorPeriod()}.
     * <p>
     * <p>
     * <ul>
     * <li>When NO Member Card file is present, the start of the VISITOR period
     * is reset when not already set AND no users are present. This makes sure
     * the visitor period is set at first installation.</li>
     * <li>When a Member Card file is present the visitor period is used for a
     * GRACE period when user upgraded to a more recent major version. The
     * visitor period is reset each time the membership is valid (again).</li>
     * </ul>
     * </p>
     * <p>
     * Saving and restoring the database needs special attention, since we do
     * NOT want to import the exported visitor start of a random installation.
     * See {@link DbTools#exportDb()}, {@link DbTools#exportDb(File)} and
     * {@link DbTools#importDb(File)}.
     * </p>
     *
     * @see #check()
     */
    public synchronized void init() {

        final File memberCardFile = getMemberCardFile();

        final long userCount = getDaoContext().getUserDao().countActiveUsers();

        myHasMemberCardFile = false;

        if (memberCardFile.exists()) {

            InputStream istrMemberCard = null;

            try {
                istrMemberCard = new FileInputStream(memberCardFile);

                final boolean bZipped = true;
                myMemberCardProps =
                        myLicManager.readMemberCardProps(
                                getMemberCardPublicKey(), istrMemberCard,
                                bZipped);

                myHasMemberCardFile = true;

                /*
                 * CHECK
                 */
                check(userCount);

            } catch (FileNotFoundException e) {
                throw new SpException(e.getMessage(), e);
            } catch (MemberCardException e) {
                throw new SpException(e.getMessage(), e);
            } finally {
                if (istrMemberCard != null) {
                    try {
                        istrMemberCard.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        } else {
            /*
             * No Member Card file
             */
            Date visitorPeriodStart = readVisitorPeriodStart();

            if (visitorPeriodStart == null) {
                if (userCount == 0) {
                    /*
                     * This makes sure the visiting period is set at first
                     * installation:
                     *
                     * Start visiting period NOW.
                     */
                    visitorPeriodStart = resetVisitorPeriod();
                } else {
                    /*
                     * This happens when a valid Member Card file is removed
                     * from the file system:
                     *
                     * Force visitor period to expired.
                     */
                    visitorPeriodStart =
                            DateUtils.addDays(new Date(),
                                    -VISITOR_PERIOD_DAYS - 1);
                    writeVisitorPeriodStart(visitorPeriodStart);
                }
            }

            this.memberParticipants = getVisitorEditionUsers();

            if (userCount > this.memberParticipants) {

                final Date now = new Date();

                myCardExpDate =
                        DateUtils.addDays(visitorPeriodStart,
                                VISITOR_PERIOD_DAYS);
                myMembershipExpDateString =
                        DateFormat.getDateInstance().format(myCardExpDate);

                myMembershipDaysTillExpiry =
                        (myCardExpDate.getTime() - now.getTime())
                                / DateUtils.MILLIS_PER_DAY;

                if (myCardExpDate.before(now)) {
                    myMembershipStat = Stat.VISITOR_EXPIRED;
                } else {
                    myMembershipStat = Stat.VISITOR;
                }

            } else {
                /*
                 * 5-user visitor version.
                 *
                 * Do NOT clearVisitorPeriod() when NO Member Card file is
                 * present.
                 */
                myMembershipStat = Stat.VISITOR_EDITION;
            }
        }
        SpInfo.instance().logCommunityNotice();
    }

    /**
     * Returns the days remaining in the visiting or grace period.
     *
     * @param refDate
     *            The reference date.
     * @return {@code null} when NO visiting period is active.
     */
    public Long getDaysLeftInVisitorPeriod(final Date refDate) {

        final Date visitorPeriodStart = readVisitorPeriodStart();

        if (visitorPeriodStart == null) {
            return null;
        }

        final Date end =
                DateUtils.addDays(visitorPeriodStart, VISITOR_PERIOD_DAYS);

        return (end.getTime() - refDate.getTime()) / DateUtils.MILLIS_PER_DAY;
    }

    /**
     *
     * @return
     */
    public String getProduct() {
        return myMemberCardProps.getProperty(
                MemberCardManager.CARD_PROP_COMMUNITY, "");
    }

    /**
     * Checks the Membership properties, interprets the membership state and
     * resets or clears the start of the visitor/grace period.
     * <p>
     * See {@link #init()} for more detailed description of initialization and
     * checking in general.
     * </p>
     */
    private void check(final long userCount) {

        final Date now = new Date();

        /*
         * Force NullPointerException when used to early.
         */
        myMembershipStat = null; //

        /*
         * ====================================================================
         * Get basic properties
         * ====================================================================
         */
        final String orgUsers =
                myMemberCardProps
                        .getProperty(MembershipModule.CARD_MEMBER_PARTICIPANTS);

        if (orgUsers == null) {
            this.memberParticipants = 0L;
        } else {
            this.memberParticipants = Long.parseLong(orgUsers);
        }

        myCardIssueDate =
                getMembershipPropDate(MemberCardManager.CARD_PROP_ISSUED_DATE);

        /*
         * Upgrade assurance
         */
        myCardUpgExpDate = getMembershipPropDate("updates-expiry-date");
        myCardUpgExpiring = false;

        if (myCardUpgExpDate == null) {

            myLicUpgInsurance = MembershipUpgInsurance.NONE;

        } else {

            if (myCardUpgExpDate.after(now)) {

                myLicUpgInsurance = MembershipUpgInsurance.ACTIVE;

                myCardUpgExpiring =
                        myCardUpgExpDate.before(DateUtils.addDays(now,
                                DAYS_WARN_BEFORE_UPG_EXPIRE));

            } else {
                myLicUpgInsurance = MembershipUpgInsurance.EXPIRED;
            }
        }

        /*
         * Support contract
         */
        myCardSupportExpDate = getMembershipPropDate("support-expiry-date");
        // TODO - just like upgrade assurance

        /*
         * Expiry
         */
        myCardExpDate =
                getMembershipPropDate(MemberCardManager.CARD_PROP_EXPIRY_DATE);

        myCardExpiring = false;

        if (myCardExpDate == null) {

            myMembershipDaysTillExpiry = null;
            myMembershipExpDateString = "";

        } else {

            myMembershipExpDateString =
                    DateFormat.getDateInstance().format(myCardExpDate);

            myMembershipDaysTillExpiry =
                    (myCardExpDate.getTime() - now.getTime())
                            / DateUtils.MILLIS_PER_DAY;

            if (now.before(myCardExpDate)) {

                myCardExpiring =
                        myCardExpDate.before(DateUtils.addDays(now,
                                DAYS_WARN_BEFORE_UPG_EXPIRE));

            } else {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(CommunityDictEnum.MEMBERSHIP.getWord()
                            + " expired since [" + myMembershipExpDateString
                            + "]");
                }
            }
        }

        /*
         * ====================================================================
         * Evaluate the status
         * ====================================================================
         */
        String key = null;
        String value = null;

        myMembershipStat = Stat.VALID; // be optimistic :-)

        Date visitorPeriodStart = readVisitorPeriodStart();

        /*
         * Product
         */
        value = getProduct();

        if (!value.equals(myLicModule.getProduct())) {

            myMembershipStat = Stat.WRONG_COMMUNITY;

            LOGGER.error("Membership is for product " + "[" + value
                    + "], module belongs to product ["
                    + myLicModule.getProduct() + "]");
            return;
        }

        /*
         * Module
         */
        key = MemberCardManager.CARD_PROP_MEMBERSHIP_MODULES;
        value = myMemberCardProps.getProperty(key, "");

        StringTokenizer st =
                new StringTokenizer(value,
                        MemberCardManager.CARD_PROP_PROCUCT_DELIMITER);

        boolean bModFound = false;

        while (st.hasMoreTokens()) {

            if (st.nextToken().equals(myLicModule.getModule())) {
                bModFound = true;
                break;
            }
        }

        if (!bModFound) {

            myMembershipStat = Stat.WRONG_MODULE;

            LOGGER.error(CommunityDictEnum.MEMBERSHIP.getWord()
                    + " is for module(s) " + "[" + value
                    + "], this is module [" + myLicModule.getModule() + "]");
            return;
        }

        /*
         * Version
         */
        value = getMembershipVersionMajor();

        final int deltaVersion =
                Integer.parseInt(myLicModule.getVersionMajor())
                        - Integer.parseInt(value);

        if (deltaVersion < 0) {
            /*
             * DOWNGRADE
             */
            myMembershipStat = Stat.WRONG_VERSION;
        }

        if (deltaVersion > 0) {

            myMembershipStat = Stat.WRONG_VERSION;

            if (myLicUpgInsurance == MembershipUpgInsurance.ACTIVE) {

                myMembershipStat = Stat.VALID;

            } else if (myLicUpgInsurance == MembershipUpgInsurance.NONE) {

                if (VersionInfo.getBuildDate().before(
                        DateUtils.addDays(myCardIssueDate, DAYS_UPG_GRACE))) {
                    myMembershipStat = Stat.VALID;
                }

            } else if (myLicUpgInsurance == MembershipUpgInsurance.EXPIRED) {

                final String policy =
                        myMemberCardProps.getProperty("updates-expiry-policy",
                                ALLOW_UPDATES_WITHIN_NEXT_VERSION);

                if (policy.equals(ALLOW_UPDATES_WITHIN_NEXT_VERSION)
                        && deltaVersion == 1) {
                    myMembershipStat = Stat.VALID;
                }
            }
        }

        if (!myMembershipStat.equals(Stat.VALID)) {

            LOGGER.error(CommunityDictEnum.MEMBERSHIP.getWord()
                    + " is for version " + "[" + value
                    + "], module has version [" + myLicModule.getVersionMajor()
                    + "]");

            if (visitorPeriodStart == null) {
                /*
                 * Start visitor period NOW.
                 */
                visitorPeriodStart = resetVisitorPeriod();
            }

            final String msg =
                    CommunityDictEnum.MEMBERSHIP.getWord() + " is for version "
                            + "[" + value + "], module has version ["
                            + myLicModule.getVersionMajor() + "]";

            if (DateUtils.addDays(visitorPeriodStart, DAYS_UPG_GRACE)
                    .after(now)) {
                myMembershipStat = Stat.WRONG_VERSION_WITH_GRACE;
                LOGGER.error(msg + " [" + getDaysLeftInVisitorPeriod(now)
                        + "] days remaining to upgrade "
                        + CommunityDictEnum.MEMBERSHIP.getWord() + ".");
            } else {
                LOGGER.error(msg);
            }
            return;
        }

        /*
         * Expiry
         */
        if (myCardExpDate != null) {
            if (now.after(myCardExpDate)) {
                myMembershipStat = Stat.EXPIRED;
                return;
            }
        }

        /*
         * Users
         */
        if (getMemberParticipants() < userCount) {
            final String msg =
                    CommunityDictEnum.MEMBERSHIP.getWord()
                            + " exceeded. Valid for ["
                            + getMemberParticipants() + "] users, ["
                            + userCount + "] users found.";

            myMembershipStat = Stat.EXCEEDED;
            LOGGER.error(msg);
            return;
        }

        /*
         *
         */
        clearVisitorPeriod();
    }

    /**
     * Is membership desirable because of Member Card status?
     *
     * @return {@code true} when desirable.
     */
    public boolean isMembershipDesirable() {
        switch (myMembershipStat) {
        case VISITOR_EXPIRED:
        case WRONG_COMMUNITY:
        case WRONG_MODULE:
        case WRONG_VERSION:
        case EXPIRED:
        case EXCEEDED:
            return true;
        default:
            return false;
        }
    }

    /**
     *
     * @return The number of (visitor) Member participants.
     */
    public long getMemberParticipants() {
        return this.memberParticipants;
    }

    /**
     * Gets the date the membership was issued.
     *
     * @return {@code null} when no membership present.
     */
    public Date getMembershipIssueDate() {
        return getMembershipPropDate(MemberCardManager.CARD_PROP_ISSUED_DATE);
    }

    /**
     *
     *
     * @return {@code true} when no member card is present.
     */
    public boolean isVisitorCard() {
        return getMembershipPropBoolean(MemberCardManager.CARD_PROP_VISITOR,
                Boolean.TRUE);
    }

    /**
     *
     * @return
     */
    public String getMembershipIssuer() {
        if (hasMemberCardFile()) {
            return myMemberCardProps
                    .getProperty(MemberCardManager.CARD_PROP_ISSUED_BY);
        }
        return "";
    }

    /**
     * Gets the name of the membership organization.
     *
     * @return The membership organization or, when no Member Card is present,
     *         an empty string.
     */
    public String getMemberOrganisation() {
        if (hasMemberCardFile()) {
            return myMemberCardProps
                    .getProperty(MemberCardManager.CARD_PROP_MEMBER_NAME);
        }
        return "";
    }

    /**
     *
     * @return
     */
    public String getMembershipVersionMajor() {
        if (hasMemberCardFile()) {
            return myMemberCardProps
                    .getProperty(MembershipModule.CARD_PROP_MEMBERCARD_VERSION_MAJOR);
        }
        return "";
    }

    /**
     *
     * @return
     */
    public String getMembershipVersion() {

        if (!hasMemberCardFile()) {
            return "";
        }
        return String
                .format("%s.%s.%s",
                        myMemberCardProps
                                .getProperty(MembershipModule.CARD_PROP_MEMBERCARD_VERSION_MAJOR),
                        myMemberCardProps
                                .getProperty(MembershipModule.CARD_PROP_MEMBERCARD_VERSION_MINOR),
                        myMemberCardProps
                                .getProperty(MembershipModule.CARD_PROP_MEMBERCARD_VERSION_REVISION));

    }

    /**
     *
     * @return The {@link Stat}.
     */
    public Stat getStatus() {
        return myMembershipStat;
    }

    /**
     * Return a localized message string.
     *
     * @param locale
     *            The {@link Locale}.
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final Locale locale, final String key,
            final String... args) {
        return Messages.getMessage(getClass(), locale, key, args);
    }

    /**
     * @param locale
     *            The {@link Locale} for the text.
     * @return The short status text to be displayed to end users.
     */
    public String getStatusUserText(final Locale locale) {

        final String txtStatus;

        switch (this.getStatus()) {

        case WRONG_MODULE:
        case WRONG_COMMUNITY:
        case WRONG_VERSION:
        case WRONG_VERSION_WITH_GRACE:
            txtStatus =
                    localize(locale, "membership-status-invalid",
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case VALID:

            if (this.isVisitorCard()) {
                txtStatus = CommunityDictEnum.VISITOR.getWord();
            } else {
                txtStatus = CommunityDictEnum.FELLOW.getWord();
            }

            break;

        case EXCEEDED:
            txtStatus =
                    localize(locale, "membership-status-exceeded",
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case EXPIRED:
            txtStatus =
                    localize(locale, "membership-status-expired",
                            CommunityDictEnum.MEMBERSHIP.getWord());
            break;

        case VISITOR_EDITION:
            txtStatus = CommunityDictEnum.VISITING_GUEST.getWord();
            break;

        case VISITOR:
            txtStatus = CommunityDictEnum.VISITOR.getWord();
            break;

        case VISITOR_EXPIRED:
            txtStatus =
                    localize(locale, "membership-status-expired",
                            CommunityDictEnum.VISITOR.getWord());
            break;

        default:
            throw new SpException("Enum [" + this.getStatus()
                    + "] not handled.");
        }

        return txtStatus;
    }

    /**
     *
     * @return The public key used for verifying the Member Card.
     * @throws {@link MemberCardException}
     */
    private java.security.PublicKey getMemberCardPublicKey()
            throws MemberCardException {

        if (null == thePublicMemberCardKey) {

            byte[] decoded;

            try {
                decoded =
                        Base64.decode(BASE64_PUBLIC_KEY_MEMBERCARD, Base64.GZIP
                                | Base64.DO_BREAK_LINES);
            } catch (IOException ex) {
                throw new MemberCardException(ex.getMessage(), ex);
            }

            final InputStream istr = new java.io.ByteArrayInputStream(decoded);
            if (false == myLicManager.getMD5(istr).equals(
                    MD5_PUBLIC_KEY_MEMBERCARD)) {
                throw new MemberCardException("public key is tampered with.");
            }

            final InputStream istrPublicKey =
                    new java.io.ByteArrayInputStream(decoded);
            thePublicMemberCardKey =
                    myLicManager.getPublicKeyMemberCard(istrPublicKey);

        }
        return thePublicMemberCardKey;
    }

    /**
     * Gets a membership property and converts it to a Date.
     *
     * @param key
     *            The key of the membership property
     * @return The date, or null when the property is absent, or empty.
     */
    private Date getMembershipPropDate(final String key) {
        Date date = null;
        String strDate = myMemberCardProps.getProperty(key, "");
        if (strDate.length() != 0) {
            try {
                SimpleDateFormat formatter =
                        new SimpleDateFormat(
                                MemberCardManager.CARD_PROP_DATE_FORMAT);
                date = formatter.parse(strDate);
            } catch (ParseException ex) {
                throw new SpException(
                        CommunityDictEnum.MEMBERSHIP.getWord()
                                + " expiry date [" + strDate + "] : "
                                + ex.getMessage(), ex);
            }
        }
        return date;
    }

    /**
     * Gets a membership property and converts it to a boolean.
     *
     * @param key
     *            The key of the membership property
     * @param defaultValue
     *            The default value.
     * @return The boolean value.
     */
    private boolean getMembershipPropBoolean(final String key,
            final Boolean defaultValue) {
        return Boolean.valueOf(
                myMemberCardProps.getProperty(key, defaultValue.toString()))
                .booleanValue();
    }

    /**
     * @return The Membership notice string.
     */
    public String getCommunityNotice() {
        return getCommunityNotice("");
    }

    /**
     * @param linePfx
     *            The prefix for each line.
     * @return The Membership notice string.
     */
    public String getCommunityNotice(final String linePfx) {

        final long userCount = getDaoContext().getUserDao().countActiveUsers();

        final StringBuilder ret = new StringBuilder(128);

        if (hasMemberCardFile()) {

            final String users =
                    String.format("%s [%s] %s [%d]",
                            CommunityDictEnum.PARTICIPANTS.getWord(),
                            this.memberParticipants,
                            CommunityDictEnum.USERS.getWord(), userCount);

            ret.append(linePfx).append(CommunityDictEnum.SAVAPAGE.getWord())
                    .append(" ").append(CommunityDictEnum.COMMUNITY.getWord())
                    .append(" ");

            if (isVisitorCard()) {
                ret.append(CommunityDictEnum.VISITOR.getWord());
            } else {
                ret.append(CommunityDictEnum.FELLOW.getWord());
            }

            ret.append(" [")
                    .append(StringUtils.defaultString(getMemberOrganisation(),
                            "-")).append("]");

            ret.append("\n").append(linePfx)
                    .append(CommunityDictEnum.MEMBERSHIP.getWord())
                    .append(" issued by [").append(getMembershipIssuer())
                    .append("]");

            ret.append("\n").append(linePfx);

            switch (myMembershipStat) {

            case WRONG_COMMUNITY:
                ret.append(CommunityDictEnum.MEMBERSHIP.getWord()).append(
                        " is not valid for this product.");
                break;

            case WRONG_VERSION:
            case WRONG_VERSION_WITH_GRACE:
                ret.append(CommunityDictEnum.MEMBERSHIP.getWord()).append(
                        " is not valid for the installed version.");
                break;

            case WRONG_MODULE:
                ret.append(CommunityDictEnum.MEMBERSHIP.getWord()).append(
                        " is not valid for this module.");
                break;

            case EXPIRED:
                ret.append(CommunityDictEnum.MEMBERSHIP.getWord()).append(
                        " expired on [" + myMembershipExpDateString + "]");
                break;

            case EXCEEDED:
                ret.append(CommunityDictEnum.MEMBERSHIP.getWord())
                        .append(" user limit exceeded.\n").append(linePfx)
                        .append(users);
                break;

            case VALID:
                ret.append(users);

                if (getExpirationDate() != null) {
                    ret.append("\n").append(linePfx);

                    if (isVisitorCard()) {
                        ret.append("Visit");
                    } else {
                        ret.append(CommunityDictEnum.MEMBERSHIP.getWord());
                    }
                    ret.append(" expires on [").append(getExpirationString())
                            .append("] [").append(getDaysTillExpiry())
                            .append("] days remaining.");
                }
                break;

            default:
                throw new SpException(String.format("%s status unknown",
                        CommunityDictEnum.MEMBERSHIP.getWord()));
            }

        } else {

            ret.append(linePfx);

            switch (myMembershipStat) {

            case VISITOR_EDITION:
                ret.append(CommunityDictEnum.VISITING_GUEST.getWord()).append(
                        ".");
                break;

            case VISITOR:
                ret.append(CommunityDictEnum.VISITOR.getWord())
                        .append(" period. Expires on [")
                        .append(myMembershipExpDateString).append("] [")
                        .append(myMembershipDaysTillExpiry)
                        .append("] days remaining.");

                ret.append("\n").append(linePfx)
                        .append("Organization participants [")
                        .append(this.memberParticipants)
                        .append("] Application users [").append(userCount)
                        .append("]");
                break;

            case VISITOR_EXPIRED:
                ret.append(CommunityDictEnum.VISITOR.getWord())
                        .append(" period expired on [")
                        .append(myMembershipExpDateString).append("]");
                break;

            default:
                ret.append(myMembershipStat.toString());
                break;
            }

        }

        return ret.toString();
    }

    /**
     * Validates if the content is valid using the provided signature.
     *
     * @param strContent
     * @param strSignature
     * @throws Exception
     * @throws MemberCardException
     *             If content is NOT valid.
     */
    public void validateContent(final String strContent,
            final String strSignature) throws MemberCardException, Exception {

        if (!myLicManager.isContentValid(getMemberCardPublicKey(), strContent,
                strSignature)) {
            throw new SpException("invalid content");
        }
    }

}
