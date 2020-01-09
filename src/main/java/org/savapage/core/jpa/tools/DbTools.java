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
package org.savapage.core.jpa.tools;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.VersionInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.job.DbBackupJob;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.AccountAttr;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.jpa.AppLog;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.jpa.CostChange;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.IppQueueAttr;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.PosItem;
import org.savapage.core.jpa.PosPurchase;
import org.savapage.core.jpa.PosPurchaseItem;
import org.savapage.core.jpa.PrintIn;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.jpa.Sequence;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAccount;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.jpa.UserCard;
import org.savapage.core.jpa.UserEmail;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupAccount;
import org.savapage.core.jpa.UserGroupAttr;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.jpa.UserNumber;
import org.savapage.core.jpa.schema.AccountAttrV01;
import org.savapage.core.jpa.schema.AccountTrxV01;
import org.savapage.core.jpa.schema.AccountV01;
import org.savapage.core.jpa.schema.AccountVoucherV01;
import org.savapage.core.jpa.schema.AppLogV01;
import org.savapage.core.jpa.schema.ConfigPropertyV01;
import org.savapage.core.jpa.schema.CostChangeV01;
import org.savapage.core.jpa.schema.DeviceAttrV01;
import org.savapage.core.jpa.schema.DeviceV01;
import org.savapage.core.jpa.schema.DocInOutV01;
import org.savapage.core.jpa.schema.DocInV01;
import org.savapage.core.jpa.schema.DocLogV01;
import org.savapage.core.jpa.schema.DocOutV01;
import org.savapage.core.jpa.schema.IppQueueAttrV01;
import org.savapage.core.jpa.schema.IppQueueV01;
import org.savapage.core.jpa.schema.PdfOutV01;
import org.savapage.core.jpa.schema.PosItemV01;
import org.savapage.core.jpa.schema.PosPurchaseItemV01;
import org.savapage.core.jpa.schema.PosPurchaseV01;
import org.savapage.core.jpa.schema.PrintInV01;
import org.savapage.core.jpa.schema.PrintOutV01;
import org.savapage.core.jpa.schema.PrinterAttrV01;
import org.savapage.core.jpa.schema.PrinterGroupMemberV01;
import org.savapage.core.jpa.schema.PrinterGroupV01;
import org.savapage.core.jpa.schema.PrinterV01;
import org.savapage.core.jpa.schema.UserAccountV01;
import org.savapage.core.jpa.schema.UserAttrV01;
import org.savapage.core.jpa.schema.UserCardV01;
import org.savapage.core.jpa.schema.UserEmailV01;
import org.savapage.core.jpa.schema.UserGroupAccountV01;
import org.savapage.core.jpa.schema.UserGroupAttrV01;
import org.savapage.core.jpa.schema.UserGroupMemberV01;
import org.savapage.core.jpa.schema.UserGroupV01;
import org.savapage.core.jpa.schema.UserNumberV01;
import org.savapage.core.jpa.schema.UserV01;
import org.savapage.core.jpa.xml.XAccountAttrV01;
import org.savapage.core.jpa.xml.XAccountTrxV01;
import org.savapage.core.jpa.xml.XAccountV01;
import org.savapage.core.jpa.xml.XAccountVoucherV01;
import org.savapage.core.jpa.xml.XAppLogV01;
import org.savapage.core.jpa.xml.XConfigPropertyV01;
import org.savapage.core.jpa.xml.XCostChangeV01;
import org.savapage.core.jpa.xml.XDeviceAttrV01;
import org.savapage.core.jpa.xml.XDeviceV01;
import org.savapage.core.jpa.xml.XDocInOutV01;
import org.savapage.core.jpa.xml.XDocInV01;
import org.savapage.core.jpa.xml.XDocLogV01;
import org.savapage.core.jpa.xml.XDocOutV01;
import org.savapage.core.jpa.xml.XEntityVersion;
import org.savapage.core.jpa.xml.XIppQueueAttrV01;
import org.savapage.core.jpa.xml.XIppQueueV01;
import org.savapage.core.jpa.xml.XPdfOutV01;
import org.savapage.core.jpa.xml.XPosItemV01;
import org.savapage.core.jpa.xml.XPosPurchaseItemV01;
import org.savapage.core.jpa.xml.XPosPurchaseV01;
import org.savapage.core.jpa.xml.XPrintInV01;
import org.savapage.core.jpa.xml.XPrintOutV01;
import org.savapage.core.jpa.xml.XPrinterAttrV01;
import org.savapage.core.jpa.xml.XPrinterGroupMemberV01;
import org.savapage.core.jpa.xml.XPrinterGroupV01;
import org.savapage.core.jpa.xml.XPrinterV01;
import org.savapage.core.jpa.xml.XSequenceV01;
import org.savapage.core.jpa.xml.XUserAccountV01;
import org.savapage.core.jpa.xml.XUserAttrV01;
import org.savapage.core.jpa.xml.XUserCardV01;
import org.savapage.core.jpa.xml.XUserEmailV01;
import org.savapage.core.jpa.xml.XUserGroupAccountV01;
import org.savapage.core.jpa.xml.XUserGroupAttrV01;
import org.savapage.core.jpa.xml.XUserGroupMemberV01;
import org.savapage.core.jpa.xml.XUserGroupV01;
import org.savapage.core.jpa.xml.XUserNumberV01;
import org.savapage.core.jpa.xml.XUserV01;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceEntryPoint;
import org.savapage.core.util.XmlParseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DbTools implements ServiceEntryPoint {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DbTools.class);

    /**
     *
     */
    private static final String JPQL_ENTITY_ALIAS = "T";

    /**
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced a new
     * constant should be added (search for DB_SCHEMA_DEPENDENT to find other
     * critical update places).
     */
    private static final String SCHEMA_V01 = "1";

    /**
     * {@link SimpleDateFormat} pattern to get an XML date from a Java Date
     * object.
     * <p>
     * By default Hibernate uses
     * {@link JdbcTimestampTypeDescriptor#TIMESTAMP_FORMAT} to format a Date to
     * string format. See
     * <a href="https://hibernate.onjira.com/browse/HHH-1859"> https://hibernate
     * .onjira.com/browse/HHH-1859</a>
     * </p>
     */
    private static final String XML_DATEFORMAT_PATTERN =
            "yyyy-MM-dd'T'HH:mm:ss.S";

    /**
     * Table Info for DB health check.
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced this
     * implementation should be updated (search for DB_SCHEMA_DEPENDENT to find
     * other critical update places).
     * </p>
     */
    private enum HealthCheckEntityEnum {
        /** */
        ACCOUNT(Account.TABLE_NAME, Account.class),
        /** */
        ACCOUNT_ATTR(AccountAttr.TABLE_NAME, AccountAttr.class),
        /** */
        ACCOUNT_TRX(AccountTrx.TABLE_NAME, AccountTrx.class),
        /** */
        ACCOUNT_VOUCHER(AccountVoucher.TABLE_NAME, AccountVoucher.class),
        /** */
        APP_LOG(AppLog.TABLE_NAME, AppLog.class),
        /** */
        CONFIG_PROPERTY(ConfigProperty.TABLE_NAME, ConfigProperty.class),
        /** */
        COST_CHANGE(CostChange.TABLE_NAME, CostChange.class),
        /** */
        DEVICE(Device.TABLE_NAME, Device.class),
        /** */
        DEVICE_ATTR(DeviceAttr.TABLE_NAME, DeviceAttr.class),
        /** */
        DOC_IN(DocIn.TABLE_NAME, DocIn.class),
        /** */
        DOC_IN_OUT(DocInOut.TABLE_NAME, DocInOut.class),
        /** */
        DOC_LOG(DocLog.TABLE_NAME, DocLog.class),
        /** */
        DOC_OUT(DocOut.TABLE_NAME, DocOut.class),
        /** */
        IPP_QUEUE(IppQueue.TABLE_NAME, IppQueue.class),
        /** */
        IPP_QUEUE_ATTR(IppQueueAttr.TABLE_NAME, IppQueueAttr.class),
        /** */
        PDF_OUT(PdfOut.TABLE_NAME, PdfOut.class),
        /** */
        POS_ITEM(PosItem.TABLE_NAME, PosItem.class),
        /** */
        POS_PURCHASE(PosPurchase.TABLE_NAME, PosPurchase.class),
        /** */
        POS_PURCHASE_ITEM(PosPurchaseItem.TABLE_NAME, PosPurchaseItem.class),
        /** */
        PRINTER(Printer.TABLE_NAME, Printer.class),
        /** */
        PRINTER_ATTR(PrinterAttr.TABLE_NAME, PrinterAttr.class),
        /** */
        PRINTER_GROUP(PrinterGroup.TABLE_NAME, PrinterGroup.class),
        /** */
        PRINTER_GROUP_MEMBER(PrinterGroupMember.TABLE_NAME,
                PrinterGroupMember.class),
        /** */
        PRINT_IN(PrintIn.TABLE_NAME, PrintIn.class),
        /** */
        PRINT_OUT(PrintOut.TABLE_NAME, PrintOut.class),
        //
        // Sequence skip intended.
        //
        /** */
        USER(User.TABLE_NAME, User.class),
        /** */
        USER_ACCOUNT(UserAccount.TABLE_NAME, UserAccount.class),
        /** */
        USER_ATTR(UserAttr.TABLE_NAME, UserAttr.class),
        /** */
        USER_CARD(UserCard.TABLE_NAME, UserCard.class),
        /** */
        USER_EMAIL(UserEmail.TABLE_NAME, UserEmail.class),
        /** */
        USER_GROUP(UserGroup.TABLE_NAME, UserGroup.class),
        /** */
        USER_GROUP_ACCOUNT(UserGroupAccount.TABLE_NAME, UserGroupAccount.class),
        /** */
        USER_GROUP_ATTR(UserGroupAttr.TABLE_NAME, UserGroupAttr.class),
        /** */
        USER_GROUP_MEMBER(UserGroupMember.TABLE_NAME, UserGroupMember.class),
        /** */
        USER_NUMBER(UserNumber.TABLE_NAME, UserNumber.class);

        /**
         * Database table name.
         */
        private final String tableName;

        /**
         * Name of {@link Entity} primary key sequence ID attribute.
         */
        private final String attrId;

        /**
         * Database table {@link Entity} class.
         */
        private final Class<? extends Entity> entClazz;

        /**
         *
         * @param table
         *            Table name.
         * @param clazz
         *            Entity class.
         */
        HealthCheckEntityEnum(final String table,
                final Class<? extends Entity> clazz) {
            this.tableName = table;
            this.entClazz = clazz;
            this.attrId = Entity.ATTR_ID;
        }

        /**
         * @return Look-up with DB table name as key.
         */
        public static Map<String, HealthCheckEntityEnum> createLookup() {
            final Map<String, HealthCheckEntityEnum> map = new HashMap<>();
            for (final HealthCheckEntityEnum value : HealthCheckEntityEnum
                    .values()) {
                map.put(value.tableName, value);
            }
            return map;
        }
    }

    /**
     * <b>*** HISTORY - DO NOT EDIT ***</b>
     * <p>
     * List of @Entity annotated classes which are part of the
     * {@link #SCHEMA_V01} database schema.
     * </p>
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced a new
     * constant should be added (search for DB_SCHEMA_DEPENDENT to find other
     * critical update places).
     * </p>
     * <p>
     * IMPORTANT: the Sequence class must NOT be part of this list, since it is
     * generated in the schema.
     * </p>
     */
    private static final Class<?>[] SCHEMA_ENTITIES_V01 = {
            // since V01
            AppLogV01.class,
            //
            ConfigPropertyV01.class,
            // since V01
            UserV01.class,
            // since V01
            UserAttrV01.class,
            // since V01
            IppQueueV01.class,
            // since V01
            IppQueueAttrV01.class,
            // since V01
            PrinterV01.class,
            // since V01
            PrinterAttrV01.class,
            // since V01
            DocLogV01.class,
            // since V01
            DocInV01.class,
            // since V01
            PrintInV01.class,
            // since V01
            DocOutV01.class,
            // since V01
            PrintOutV01.class,
            // since V01
            PdfOutV01.class,
            // since V01
            DocInOutV01.class,

            // --------------------
            // since V01.00
            AccountV01.class,
            // since V01.00
            AccountAttrV01.class,

            // since V01.02
            PosItemV01.class,
            // since V01.02
            PosPurchaseV01.class,
            // since V01.02
            PosPurchaseItemV01.class,

            // since V01.00
            AccountVoucherV01.class,

            // since V01.07
            CostChangeV01.class,

            // since V01.00
            AccountTrxV01.class,
            // since V01.00
            DeviceV01.class,
            // since V01.00
            DeviceAttrV01.class,
            // since V01.00
            PrinterGroupV01.class,
            // since V01.00
            PrinterGroupMemberV01.class,
            // since V01.00
            UserAccountV01.class,
            // since V01.00
            UserCardV01.class,
            // since V01.00
            UserNumberV01.class,
            // since V01.00
            UserEmailV01.class,
            // since V01.00
            UserGroupV01.class,
            // since V01.00
            UserGroupAttrV01.class,
            // since V01.00
            UserGroupMemberV01.class,
            // since V01.06
            UserGroupAccountV01.class //
    };

    /**
     * Custom ORDER BY clauses for XML schema entities. The array holds arrays
     * of 2 elements. Element 0 (zero) is the simple class name of the
     * {@link XEntityVersion} type, element 1 (one) is the ORDER BY clause.
     * (without the {@code ORDER BY} prefix).
     * <p>
     * REASON: Some entities need an ORDER BY clause, to ensure that on import
     * recursive foreign key constraints are satisfied.
     * </p>
     * <p>
     * CAUTION: This construct only works for one-level deep recursion.
     * </p>
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced, check if a
     * new array element should be added (search for DB_SCHEMA_DEPENDENT to find
     * other critical update places).
     * </p>
     *
     * @since 0.9.6
     * @see Mantis #369
     */
    private static final String[][] XML_SCHEMA_ENTITIES_EXPORT_ORDER_BY = {
            /*
             * AccountV01 has a foreign key that points to itself. Therefore the
             * rows that do NOT have this relation (NULL value for secondary
             * key) should be exported first.
             */
            { XAccountV01.class.getSimpleName(),
                    "COALESCE(" + JPQL_ENTITY_ALIAS + ".parent, 0)" },
            /*
             * DeviceV01 has a foreign key that points to itself. Therefore the
             * rows that do NOT have this relation (NULL value for secondary
             * key) should be exported first.
             */
            { XDeviceV01.class.getSimpleName(),
                    "COALESCE(" + JPQL_ENTITY_ALIAS + ".cardReader, 0)" },

    };

    /**
     * <b>*** HISTORY - DO NOT EDIT ***</b>
     * <p>
     * Sequence entity class used for the database export and import for the
     * {@link #SCHEMA_V01} database schema.
     * </p>
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced a new
     * constant should be added (search for DB_SCHEMA_DEPENDENT to find other
     * critical update places).
     * </p>
     */
    private static final Class<?> XML_SCHEMA_SEQUENCE_ENTITY_V01 =
            XSequenceV01.class;

    /**
     * <b>*** HISTORY - DO NOT EDIT ***</b>
     * <p>
     * List of XML @Entity annotated classes used for the database export and
     * import for the {@link #SCHEMA_V01} database schema.
     * </p>
     * <p>
     * IMPORTANT: The order of the classes in the list are <b>bottom-up</b> the
     * data-model, so foreign key constraints are satisfied.
     * </p>
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced a new
     * constant should be added (search for DB_SCHEMA_DEPENDENT to find other
     * critical update places).
     * </p>
     */
    private static final Class<?>[] XML_SCHEMA_ENTITIES_V01 = {
            // since V01
            XAppLogV01.class,
            // since V01
            XConfigPropertyV01.class,

            // since V01
            XUserV01.class,
            // since V01
            XUserAttrV01.class,

            // since V01
            XIppQueueV01.class,
            // since V01
            XIppQueueAttrV01.class,

            // since V01
            XPrinterV01.class,
            // since V01
            XPrinterAttrV01.class,

            // since V01
            XPrintInV01.class,
            // since V01
            XDocInV01.class,

            // since V01
            XPrintOutV01.class,
            // since V01
            XPdfOutV01.class,
            // since V01
            XDocOutV01.class,

            // since V01
            XDocInOutV01.class,

            // since V01
            XDocLogV01.class,

            // since V01.00
            XPrinterGroupV01.class,
            // since V01.00
            XPrinterGroupMemberV01.class,

            // since V01.00
            XAccountV01.class,
            // since V01.00
            XAccountAttrV01.class,

            // since V01.02
            XPosItemV01.class,
            // since V01.02
            XPosPurchaseV01.class,
            // since V01.02
            XPosPurchaseItemV01.class,

            // since V01.00
            XAccountVoucherV01.class,
            // since V01.07
            XCostChangeV01.class,

            // since V01.00
            XAccountTrxV01.class,

            // since V01.00
            XDeviceV01.class,
            // since V01.00
            XDeviceAttrV01.class,
            // since V01.00
            XUserAccountV01.class,
            // since V01.00
            XUserCardV01.class,
            // since V01.00
            XUserNumberV01.class,
            // since V01.00
            XUserEmailV01.class,
            // since V01.00
            XUserGroupV01.class,
            // since V01.00
            XUserGroupAttrV01.class,
            // since V01.00
            XUserGroupMemberV01.class,
            // since V01.06
            XUserGroupAccountV01.class,

            // --------------------
            // since V01
            XML_SCHEMA_SEQUENCE_ENTITY_V01 //
    };

    /**
     * Array with table names used for Derby table compression of the
     * <b>CURRENT</b> schema version.
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced this
     * implementation should be updated (search for DB_SCHEMA_DEPENDENT to find
     * other critical update places).
     * </p>
     */
    private static final String[] TABLES_FOR_DERBY_COMPRESSION = {
            //
            ConfigProperty.TABLE_NAME,
            // since V01
            DocLog.TABLE_NAME,
            // since V01
            DocIn.TABLE_NAME,
            // since V01
            PrintIn.TABLE_NAME,
            // since V01
            DocOut.TABLE_NAME,
            // since V01
            PrintOut.TABLE_NAME,
            // since V01
            PdfOut.TABLE_NAME,
            // since V01
            DocInOut.TABLE_NAME,
            // since V01
            User.TABLE_NAME,
            // since V01
            UserAttr.TABLE_NAME,
            // since V01
            AppLog.TABLE_NAME,
            // since V01
            IppQueue.TABLE_NAME,
            // since V01
            Printer.TABLE_NAME,
            // since V01
            PrinterAttr.TABLE_NAME,
            // since V01
            IppQueueAttr.TABLE_NAME,

            // since V01.00
            Account.TABLE_NAME,
            // since V01.00
            AccountAttr.TABLE_NAME,
            // since V01.00
            AccountTrx.TABLE_NAME,
            // since V01.00
            AccountVoucher.TABLE_NAME,
            // since V01.07
            CostChange.TABLE_NAME,
            // since V01.00
            Device.TABLE_NAME,
            // since V01.00
            DeviceAttr.TABLE_NAME,
            // since V01.00
            PrinterGroup.TABLE_NAME,
            // since V01.00
            PrinterGroupMember.TABLE_NAME,
            // since V01.00
            UserAccount.TABLE_NAME,
            // since V01.00
            UserCard.TABLE_NAME,
            // since V01.00
            UserNumber.TABLE_NAME,
            // since V01.00
            UserEmail.TABLE_NAME,
            // since V01.00
            UserGroup.TABLE_NAME,
            // since V01.00
            UserGroupAttr.TABLE_NAME,
            // since V01.00
            UserGroupMember.TABLE_NAME,
            // since V01.06
            UserGroupAccount.TABLE_NAME,

            // since V01.02
            PosItem.TABLE_NAME,
            // since V01.02
            PosPurchase.TABLE_NAME,
            // since V01.02
            PosPurchaseItemV01.TABLE_NAME

            /* */
    };

    //
    private static final String XML_ATTR_APP_VERSION_MAJOR =
            "app-version-major";
    private static final String XML_ATTR_APP_VERSION_MINOR =
            "app-version-minor";
    private static final String XML_ATTR_APP_VERSION_REVISION =
            "app-version-revision";
    private static final String XML_ATTR_APP_VERSION_BUILD =
            "app-version-build";
    private static final String XML_ATTR_APP_SCHEMA_VERSION =
            "app-schema-version";
    private static final String XML_ATTR_APP_SCHEMA_VERSION_MINOR =
            "app-schema-version-minor";
    //
    private static final String XML_ATTR_DB_PRODUCT_NAME = "db-product-name";
    private static final String XML_ATTR_DB_PRODUCT_VERSION =
            "db-product-version";
    private static final String XML_ATTR_DB_VERSION_MAJOR = "db-version-major";
    private static final String XML_ATTR_DB_VERSION_MINOR = "db-version-minor";
    //
    private static final String XML_ATTR_SCHEMA_VERSION = "schema-version";
    private static final String XML_ATTR_EXPORT_DATETIME = "export-datetime";

    /**
     *
     */
    private static final String DERBY_DB_SCHEMA_NAME = "APP";

    /**
     * Array with simple JPA persistable classes.
     * <p>
     * NOTE: primitive types like boolean, byte, short, char, int, long, float
     * and double and not used in our application. We use the equivalent wrapper
     * classes from package java.lang: Boolean, Byte, Short, Character, Integer,
     * Long, Float and Double.
     * <p>
     */
    private static final Class<?> SIMPLE_JPA_TYPES[] = {
            // In order of most likely
            String.class, Long.class, Boolean.class, Integer.class, Float.class,
            Double.class, java.util.Date.class, Short.class,
            java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class,
            //
            Byte.class, Character.class, java.math.BigInteger.class,
            java.math.BigDecimal.class, java.util.Calendar.class };

    /**
     *
     */
    private DbTools() {
    }

    /**
     * Returns the list of @Entity annotated classes (including the Sequence
     * class) which are part of a database schema version.
     *
     * <p>
     * IMPORTANT: the Sequence class is NOT be part of this list, since it is
     * generated in the schema.
     * </p>
     *
     * <p>
     * DB_SCHEMA_DEPENDENT: when a new schema version is introduced this
     * implementation should be updated (search for DB_SCHEMA_DEPENDENT to find
     * other critical update places).
     * </p>
     *
     * @param version
     *            The schema version.
     * @return The list, or RuntimeException when the version is not supported.
     */
    private static Class<?>[] getSchemaEntities(final String version) {

        if (version.equals(SCHEMA_V01)) {
            return SCHEMA_ENTITIES_V01;
        }

        throw new SpException(
                "schema version [" + version + "] is not supported");
    }

    /**
     *
     * @param version
     *            Schema version.
     * @return XML schema version entity classes.
     */
    private static Class<?>[] getXmlSchemaEntities(final String version) {

        if (version.equals(SCHEMA_V01)) {
            return XML_SCHEMA_ENTITIES_V01;
        }

        throw new SpException(
                "XML schema version [" + version + "] is not supported");
    }

    /**
     *
     * @param version
     *            Schema version.
     * @return XML schema version Sequence entity class.
     */
    private static Class<?> getXmlSchemaSequenceEntity(final String version) {

        if (version.equals(SCHEMA_V01)) {
            return XML_SCHEMA_SEQUENCE_ENTITY_V01;
        }

        throw new SpException("XML Sequence Entity version [" + version
                + "] is not supported");
    }

    /**
     * Generates the database drop and create scripts of the current database
     * schema, for a database type, and writes them to file.
     *
     * @param databaseType
     *            The {@link DatabaseTypeEnum}.
     * @param fileDropSql
     *            The file where the drop statements should be generated to.
     * @param fileCreateSql
     *            The file where the create statements should be generated to.
     */
    public static void generateDbSchema(final DatabaseTypeEnum databaseType,
            final File fileDropSql, final File fileCreateSql) {
        exportDbSchema(getAppSchemaVersion(), fileDropSql, fileCreateSql, null);
    }

    /**
     * Gets the (hard-coded) major schema version of this <b>application</b>.
     *
     * @return The schema version.
     */
    public static String getAppSchemaVersion() {
        return VersionInfo.DB_SCHEMA_VERSION_MAJOR;
    }

    /**
     * Gets the (hard-coded) minor schema version of this <b>application</b>.
     *
     * @return The schema version.
     */
    public static String getAppSchemaVersionMinor() {
        return VersionInfo.DB_SCHEMA_VERSION_MINOR;
    }

    /**
     * Gets the major schema version from the <b>current database</b>.
     *
     * @return The major schema version.
     */
    public static String getDbSchemaVersion() {
        return ConfigManager.instance().getConfigValue(Key.SYS_SCHEMA_VERSION);
    }

    /**
     * Initializes the current database, erasing and initializing all data
     * according to the application schema version.
     * <p>
     * <b>IMPORTANT: this method implements its own commit scope.</b>
     * </p>
     * <p>
     * A default {@link IppQueue} is created, and some {@link ConfigProperty}
     * objects are written to the database:
     * </p>
     * <ul>
     * <li>{@link IConfigProp.Key#SYS_SCHEMA_VERSION}</li>
     * <li>{@link IConfigProp.Key#SYS_SCHEMA_VERSION_MINOR}</li>
     * <li>{@link IConfigProp.Key#SYS_SETUP_COMPLETED}</li>
     * <li>{@link IConfigProp.Key#COMMUNITY_VISITOR_START_DATE}</li>
     * </ul>
     *
     * @param listener
     *            The {@link DbProcessListener}.
     * @param createNew
     *            If {@code true} a new database is created, if {@code false} an
     *            existing database initialized.
     */
    public static void initDb(final DbProcessListener listener,
            final boolean createNew) {

        initDb(getAppSchemaVersion(), listener, createNew);

        ConfigManager cm = ConfigManager.instance();

        final EntityManager em = cm.createEntityManager();

        em.getTransaction().begin();
        boolean committed = false;

        try {
            ConfigProperty prop = null;

            final String actor = Entity.ACTOR_INSTALL;
            final Date now = new Date();

            //
            prop = new ConfigProperty();
            prop.setPropertyName(
                    cm.getConfigKey(IConfigProp.Key.SYS_SCHEMA_VERSION));

            prop.setValue(getAppSchemaVersion());
            prop.setCreatedBy(actor);
            prop.setModifiedBy(actor);
            prop.setCreatedDate(now);
            prop.setModifiedDate(now);
            em.persist(prop);

            //
            prop = new ConfigProperty();
            prop.setPropertyName(
                    cm.getConfigKey(IConfigProp.Key.SYS_SCHEMA_VERSION_MINOR));

            prop.setValue(getAppSchemaVersionMinor());
            prop.setCreatedBy(actor);
            prop.setModifiedBy(actor);
            prop.setCreatedDate(now);
            prop.setModifiedDate(now);
            em.persist(prop);

            //
            prop = new ConfigProperty();
            prop.setPropertyName(
                    cm.getConfigKey(IConfigProp.Key.SYS_SETUP_COMPLETED));
            prop.setValue(IConfigProp.V_NO);
            prop.setCreatedBy(actor);
            prop.setModifiedBy(actor);
            prop.setCreatedDate(now);
            prop.setModifiedDate(now);
            em.persist(prop);

            /*
             * TODO: content is not meaningful yet.
             */
            prop = new ConfigProperty();
            prop.setPropertyName(cm.getConfigKey(
                    IConfigProp.Key.COMMUNITY_VISITOR_START_DATE));
            prop.setValue(cm.createInitialVisitorStartDate());

            prop.setCreatedBy(actor);
            prop.setModifiedBy(actor);
            prop.setCreatedDate(now);
            prop.setModifiedDate(now);
            em.persist(prop);

            /*
             * Create the default IppQueue.
             */
            IppQueue queue = new IppQueue();
            queue.setCreatedBy(actor);
            queue.setCreatedDate(now);
            queue.setUrlPath(ReservedIppQueueEnum.IPP_PRINT.getUrlPath());
            queue.setIpAllowed("");
            queue.setTrusted(true);

            em.persist(queue);

            /*
             *
             */
            em.getTransaction().commit();
            committed = true;

            listener.onLogEvent("Initialization finished.");

        } finally {

            if (!committed) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /**
     * Initializes a database, erasing all data according to the supplied schema
     * version.
     *
     * @param schemaVersion
     *            The schema version.
     * @param listener
     *            The {@link DbProcessListener}.
     * @param createNew
     *            If {@code true} a new database is created, if {@code false} an
     *            existing database initialized.
     */
    private static void initDb(final String schemaVersion,
            final DbProcessListener listener, final boolean createNew) {

        listener.onLogEvent("Initializing database version ["
                + getAppSchemaVersion() + "] ...");

        exportDbSchema(schemaVersion, null, null, createNew);
    }

    /**
     * Generates database using a schema version, and optionally exports it to a
     * file and/or applies it to the configured database.
     * <p>
     * If BOTH input files are {@code null}, then the exported schema is applied
     * to the current database, so the database is initialized according to a
     * schema version and all data is cleared.
     * </p>
     *
     * @param schemaVersion
     *            The database schema version.
     * @param fileCreateSql
     *            If null, create file is NOT created.
     * @param fileDropSql
     *            If null, drop file is NOT created.
     * @param createNew
     *            If {@code true} a new database is created, if {@code false} an
     *            existing database initialized.
     */
    private static void exportDbSchema(final String schemaVersion,
            final File fileDropSql, final File fileCreateSql,
            final Boolean createNew) {

        final boolean applyToDatabase =
                (fileDropSql == null && fileCreateSql == null);

        final ConfigManager cm = ConfigManager.instance();

        final Map<String, Object> settingsMap = new HashMap<>();

        settingsMap.put(DbConfig.HIBERNATE_DIALECT,
                cm.getHibernateInfo().getDialect());
        settingsMap.put(DbConfig.HIBERNATE_DRIVER,
                cm.getJdbcInfo().getDriver());

        if (applyToDatabase) {

            final String jdbcUrl = cm.getJdbcInfo().getUrl();

            /*
             * NOTE: the ";create=true" at the end ensures that a new database
             * is created when it does NOT exist.
             *
             * IMPORTANT: When the database directory (we are using embedded
             * Derby) described in the URL exists and is empty, errors occur. A
             * new database is created only when the directory does NOT exist.
             */
            final boolean isDerby =
                    StringUtils.contains(jdbcUrl.toLowerCase(), "derby");

            final String url;

            if (isDerby) {
                url = jdbcUrl.replace(
                        "${" + ConfigManager.SYS_PROP_SERVER_HOME + "}",
                        ConfigManager.getServerHome()) + ";create=true";
            } else {
                url = jdbcUrl;
                /*
                 * Mantis #879, #968: do NOT use DbConfig.JPA_JDBC_USER and
                 * DbConfig.JPA_JDBC_PASSWORD keys from theEmf.getProperties().
                 */
                settingsMap.put(Environment.USER,
                        ConfigManager.getExternalDbUser());
                settingsMap.put(Environment.PASS,
                        ConfigManager.getExternalDbPassword());
            }

            settingsMap.put(Environment.URL, url);
        }

        final MetadataSources metadata =
                new MetadataSources(new StandardServiceRegistryBuilder()
                        .applySettings(settingsMap).build());

        /*
         * Use the @Entity classes of the supplied schema version.
         */
        for (Class<?> c : getSchemaEntities(schemaVersion)) {
            metadata.addAnnotatedClass(c);
        }

        final SchemaExport schema = new SchemaExport();

        if (applyToDatabase) {

            if (createNew) {
                // Execute just create statements (create of new database).
                schema.createOnly(EnumSet.of(TargetType.DATABASE),
                        metadata.buildMetadata());
            } else {
                // Execute drop + create statements (clean of current database).
                schema.create(EnumSet.of(TargetType.DATABASE),
                        metadata.buildMetadata());
            }

        } else {

            schema.setFormat(true);
            schema.setDelimiter(";");

            if (fileDropSql != null) {
                schema.setOutputFile(fileDropSql.getAbsolutePath());
                // only the drop statements
                schema.execute(EnumSet.of(TargetType.SCRIPT), Action.DROP,
                        metadata.buildMetadata());
            }
            if (fileCreateSql != null) {
                schema.setOutputFile(fileCreateSql.getAbsolutePath());
                // only the create statements
                schema.execute(EnumSet.of(TargetType.SCRIPT), Action.CREATE,
                        metadata.buildMetadata());
            }
        }
    }

    /**
     * Checks Database health.
     *
     * @param info
     *            Information log.
     * @param err
     *            Error log.
     * @param fix
     *            If {@code true} then fix any inconsistency.
     */
    private static void checkSequences(final StringBuilder info,
            final StringBuilder err, final boolean fix) {
        final Map<String, HealthCheckEntityEnum> lookup =
                HealthCheckEntityEnum.createLookup();

        final EntityManager em = DaoContextImpl.peekEntityManager();

        final Query queryList = em.createQuery(String
                .format("SELECT S FROM %s S", Sequence.class.getSimpleName()));
        @SuppressWarnings("unchecked")
        final List<Sequence> seqList = queryList.getResultList();

        boolean dbTrx = false;
        if (fix && !em.getTransaction().isActive()) {
            em.getTransaction().begin();
            dbTrx = true;
        }

        if (LOGGER.isDebugEnabled()) {
            for (final Sequence seq : seqList) {
                LOGGER.debug("Sequence {} : {}", seq.getName(),
                        seq.getValue().longValue());
            }
        }

        try {
            for (final Sequence seq : seqList) {

                final String tableName = seq.getName();
                final HealthCheckEntityEnum entEnum = lookup.get(tableName);

                if (entEnum == null) {
                    throw new IllegalStateException(tableName + " is missing.");
                }

                final long lastSequenceId = seq.getValue().longValue();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sequence {}|{} : last {}",
                            entEnum.entClazz.getSimpleName(), entEnum.tableName,
                            lastSequenceId);
                }

                final Query queryMax = em.createQuery(String.format(
                        "SELECT MAX(S.%s) FROM %s S", entEnum.attrId,
                        entEnum.entClazz.getSimpleName()));
                final Long maxSeqID = (Long) queryMax.getSingleResult();

                final long maxIdTable;
                if (maxSeqID == null) {
                    maxIdTable = 0;
                } else {
                    maxIdTable = maxSeqID.longValue();
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("         {}|{} :  max {}",
                            entEnum.entClazz.getSimpleName(), entEnum.tableName,
                            maxIdTable);
                }

                if (maxSeqID != null && maxIdTable > lastSequenceId) {
                    if (fix) {
                        seq.setValue(Long.valueOf(maxIdTable));
                        em.merge(seq);
                        if (info.length() > 0) {
                            info.append("\n");
                        }
                        info.append(String.format("FIXED [%s]: id %d > %d",
                                entEnum.tableName, maxIdTable, lastSequenceId));
                    } else {
                        if (err.length() > 0) {
                            info.append("\n");
                        }
                        err.append(String.format("ERROR [%s]: id %d > %d",
                                entEnum.tableName, maxIdTable, lastSequenceId));
                    }

                } else {
                    if (info.length() > 0) {
                        info.append("\n");
                    }
                    info.append(String.format("OK [%s] id %d <= %d",
                            entEnum.tableName, maxIdTable, lastSequenceId));
                }
            }
        } finally {
            if (dbTrx) {
                em.getTransaction().commit();
            }
        }
    }

    /**
     * Checks Database health.
     */
    public static void checkSequences() {

        final StringBuilder info = new StringBuilder();
        final StringBuilder err = new StringBuilder();

        checkSequences(info, err, false);

        if (err.length() == 0) {
            SpInfo.instance().log(
                    String.format("Database [%s] OK.", Sequence.TABLE_NAME));
        } else {
            LOGGER.error("{}\n", err.toString());
            throw new IllegalStateException(String
                    .format("Database [%s] MISMATCH.", Sequence.TABLE_NAME));
        }
    }

    /**
     * Checks Database health.
     *
     * @param strInfo
     *            Information log.
     * @param strErr
     *            Error log.
     * @param fix
     *            If {@code true} then fix any inconsistency.
     */
    public static void checkSequences(final PrintStream strInfo,
            final PrintStream strErr, final boolean fix) {

        final StringBuilder info = new StringBuilder();
        final StringBuilder err = new StringBuilder();

        checkSequences(info, err, fix);

        strInfo.println(info.toString());
        strErr.println(err.toString());
    }

    /**
     * Exports the database data to zipped XML file in the standard backup
     * directory. File name format:
     * <p>
     * {@code savapage-export-yyyy-MM-dd'T'HH-mm-ss.zip}
     * </p>
     * <p>
     * Also see {@link #importDb(File)}.
     * </p>
     *
     * @param em
     *            The {@link EntityManager}.
     * @param queryMaxResults
     *            The maximum number of rows in each query result set.
     * @return The exported file.
     * @throws IOException
     *             When IO error.
     */
    public static File exportDb(final EntityManager em,
            final int queryMaxResults) throws IOException {
        return exportDb(em, queryMaxResults,
                new File(ConfigManager.getDbBackupHome()));
    }

    /**
     * Deletes {@code savapage*.zip} files in the standard backup directory
     * older than {@code backupDaysToKeep}.
     *
     * @param now
     *            Current date/time.
     * @param backupDaysToKeep
     *            Number of days to keep the backup.
     * @return The number of files deleted.
     * @throws IOException
     *             When IO error.
     */
    public static int cleanBackupDirectory(final Date now,
            final int backupDaysToKeep) throws IOException {

        /*
         * Go back in time and truncate.
         */
        final long msecBackInTime =
                DateUtils.truncate(DateUtils.addDays(now, -backupDaysToKeep),
                        Calendar.DAY_OF_MONTH).getTime();

        int nFilesDeleted = 0;

        final Path backupPath = FileSystems.getDefault()
                .getPath(ConfigManager.getDbBackupHome());

        try (DirectoryStream<Path> ds =
                Files.newDirectoryStream(backupPath, "savapage*.zip");) {
            /*
             * Iterate over the paths in the directory and print filenames
             */
            for (Path p : ds) {
                /*
                 * Check if file or directory
                 */
                BasicFileAttributes attrs =
                        Files.readAttributes(p, BasicFileAttributes.class);

                if (attrs.isRegularFile()
                        && attrs.creationTime().toMillis() < msecBackInTime) {

                    Files.delete(p);

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("deleted [" + p + "]");
                    }

                    nFilesDeleted++;
                }
            }
        }

        return nFilesDeleted;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    /**
     *
     * @param em
     * @param queryMaxResults
     *            The maximum number of rows in each query result set.
     * @param schemaVersion
     * @param dateExport
     * @return
     * @throws IOException
     */
    public static File exportDbBeforeUpg(final EntityManager em,
            final int queryMaxResults, final long schemaVersion,
            final Date dateExport) throws IOException {

        return exportDb(em, queryMaxResults,
                new File(ConfigManager.getDbBackupHome() + "/"
                        + createSchemaUpgBackupDbFileName(schemaVersion,
                                dateExport)));
    }

    /**
     * Creates the default basename for a db export file. File name format:
     * <p>
     * {@code savapage-export-yyyy-MM-dd'T'HH-mm-ss.zip}
     * </p>
     *
     * @param dateExport
     *            The date used for the formatted datetime in the name.
     *
     * @return The basename of the (zip) file.
     */
    private static String createExportDbFileName(final Date dateExport) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyyy-MM-dd'T'HH-mm-ss");
        return "savapage-export-" + dateFormat.format(dateExport).toString()
                + ".zip";
    }

    /**
     * Creates the default basename for the db export file in a database upgrade
     * scenario, just before the upgrade is performed. File name format:
     * <p>
     * {@code savapage-schema-X-upgrade-backup-yyyy-MM-dd'T'HH-mm-ss.zip}
     * </p>
     *
     * @param schemaVersion
     *            The schema version of the current database to be upgraded.
     *
     * @param dateExport
     *            The date used for the formatted datetime in the name.
     *
     * @return The basename of the (zip) file.
     */
    private static String createSchemaUpgBackupDbFileName(
            final long schemaVersion, final Date dateExport) {
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyyy-MM-dd'T'HH-mm-ss");
        return "savapage-schema-" + schemaVersion + "-upgrade-backup-"
                + dateFormat.format(dateExport).toString() + ".zip";
    }

    /**
     * Exports the database content to named zipped XML file, or to a directory.
     * Also see {@link #importDb(File)}.
     * <p>
     * The returned file can be used with importDb() to restore a database. This
     * method uses a writeLock() of a special R/W lock object managed by the
     * {@link ConfigManager}.
     * </p>
     * <p>
     * NOTE: {@link IConfigProp.Key#SYS_BACKUP_LAST_RUN_TIME} is NOT set in the
     * database at this point: this done in
     * {@link DbBackupJob#execute(org.quartz.JobExecutionContext)}. So, when
     * doing a {@code --db-export} from the command line this key is NOT set in
     * the database.
     * </p>
     *
     * @param em
     *            The {@link EntityManager}.
     * @param queryMaxResults
     *            The maximum number of rows in each query result set.
     * @param fileExport
     *            The output file. When the file is a directory a default
     *            filename in this directory is constructed.
     * @return The exported file.
     * @throws IOException
     *             When file i/o errors.
     */
    public static File exportDb(final EntityManager em,
            final int queryMaxResults, final File fileExport)
            throws IOException {

        final ConfigManager cm = ConfigManager.instance();

        final Date dateExport = new Date();

        String exportFile;

        if (fileExport.isDirectory()) {
            exportFile = fileExport + "/" + createExportDbFileName(dateExport);
        } else {
            exportFile = fileExport.getAbsolutePath();
        }

        XMLStreamWriter writer = null;

        try {

            final SimpleDateFormat dateFormat = new SimpleDateFormat();
            dateFormat.applyPattern("yyyy-MM-dd'T'HH-mm-ss");

            /*
             * Zipped XML output
             */
            FileOutputStream fout = new FileOutputStream(exportFile);
            ZipOutputStream zout = new ZipOutputStream(fout);
            final int level = 9; // highest level
            zout.setLevel(level);

            ZipEntry ze = new ZipEntry(
                    FilenameUtils.getBaseName(exportFile) + ".xml");
            zout.putNextEntry(ze);

            final XMLOutputFactory factory = XMLOutputFactory.newInstance();

            writer = factory.createXMLStreamWriter(zout, "UTF-8");

            /*
             * XML document
             */
            writer.writeStartDocument("UTF-8", "1.0");

            /*
             * Root element
             */
            writer.writeStartElement("data");

            dateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
            final String exportTime = dateFormat.format(dateExport).toString();

            writer.writeAttribute(XML_ATTR_APP_VERSION_MAJOR,
                    VersionInfo.VERSION_A_MAJOR);
            writer.writeAttribute(XML_ATTR_APP_VERSION_MINOR,
                    VersionInfo.VERSION_B_MINOR);
            writer.writeAttribute(XML_ATTR_APP_VERSION_REVISION,
                    VersionInfo.VERSION_C_REVISION);
            writer.writeAttribute(XML_ATTR_APP_VERSION_BUILD,
                    VersionInfo.VERSION_E_BUILD);
            writer.writeAttribute(XML_ATTR_APP_SCHEMA_VERSION,
                    VersionInfo.DB_SCHEMA_VERSION_MAJOR);
            writer.writeAttribute(XML_ATTR_APP_SCHEMA_VERSION_MINOR,
                    VersionInfo.DB_SCHEMA_VERSION_MINOR);

            /*
             * IMPORTANT: this attribute is used in the restore (import) for
             * getting the right schema entities.
             *
             * We need the schema version from the database, because may be this
             * is a backup-before-upgrade.
             */
            writer.writeAttribute(XML_ATTR_SCHEMA_VERSION,
                    getDbSchemaVersion());

            writer.writeAttribute(XML_ATTR_EXPORT_DATETIME, exportTime);

            //
            final DbVersionInfo dbVersionInfo = cm.getDbVersionInfo();

            writer.writeAttribute(XML_ATTR_DB_PRODUCT_NAME,
                    dbVersionInfo.getProdName());
            writer.writeAttribute(XML_ATTR_DB_PRODUCT_VERSION,
                    dbVersionInfo.getProdVersion());

            writer.writeAttribute(XML_ATTR_DB_VERSION_MAJOR,
                    String.valueOf(dbVersionInfo.getMajorVersion()));
            writer.writeAttribute(XML_ATTR_DB_VERSION_MINOR,
                    String.valueOf(dbVersionInfo.getMinorVersion()));

            /*
             * Use the XML @Entity classes of the schema version of the current
             * Database. IMPORTANT: this might NOT be equal to the schema
             * version of the application, since e.g. we want to backup the
             * database before an upgrade.
             */
            for (Class<?> objClass : getXmlSchemaEntities(
                    getDbSchemaVersion())) {
                exportDbTable(em, queryMaxResults, writer, objClass);
            }

            writer.writeEndElement(); // </data>
            writer.writeEndDocument();

            writer.flush();
            writer.close();

            zout.closeEntry();
            zout.flush();
            zout.close();

            writer = null;

        } catch (XMLStreamException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (XMLStreamException e) {
                    LOGGER.error("Close failed: " + e.getMessage());
                }
            }
        }
        return new File(exportFile);
    }

    /**
     *
     * @param em
     * @param entityClassNameFull
     *            The full entity class name.
     * @param entityClassNameSimple
     *            The simple entity class name.
     * @return The {@link Query} object.
     */
    private static Query createDbTableQueryForExport(final EntityManager em,
            final String entityClassNameFull,
            final String entityClassNameSimple) {
        /*
         * Compose statement.
         */
        final StringBuilder jpql = new StringBuilder(128);

        jpql.append("SELECT ").append(JPQL_ENTITY_ALIAS).append(" FROM ")
                .append(entityClassNameFull).append(" ")
                .append(JPQL_ENTITY_ALIAS);

        /*
         * Some entities need an ORDER BY clause, to ensure that on import
         * recursive foreign key constraints are satisfied.
         */
        for (final String[] props : XML_SCHEMA_ENTITIES_EXPORT_ORDER_BY) {
            if (props[0].equals(entityClassNameSimple)) {
                jpql.append(" ORDER BY ").append(props[1]);
                break;
            }
        }

        /*
         * Execute statement.
         */
        final Query query = em.createQuery(jpql.toString());

        return query;
    }

    /**
     * Exports a database table.
     *
     * @param em
     *            The JPA EntityManager.
     * @param queryMaxResults
     *            The maximum number of rows in each query result set.
     * @param writer
     *            The {@link XMLStreamWriter}.
     * @param objClass
     *            The class of the JPA entity to export.
     */
    private static void exportDbTable(final EntityManager em,
            final int queryMaxResults, final XMLStreamWriter writer,
            final Class<?> objClass) {

        final SimpleDateFormat xmlDateFormat =
                new SimpleDateFormat(XML_DATEFORMAT_PATTERN);

        final String entityClassNameFull = objClass.getName();

        final String entityClassNameSimple = objClass.getSimpleName();

        try {

            final BeanInfo binfo =
                    java.beans.Introspector.getBeanInfo(objClass);

            writer.writeStartElement("entity");
            writer.writeAttribute("name", entityClassNameSimple);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Creating export query for ["
                        + entityClassNameSimple + "]");
            }

            /*
             * Create Query.
             */
            final Query query = createDbTableQueryForExport(em,
                    entityClassNameFull, entityClassNameSimple);

            int startPositionWlk = 0;
            int resultListSizeWlk = queryMaxResults;

            /*
             * Process result chunks.
             */
            while (resultListSizeWlk == queryMaxResults) {

                /*
                 * Get chunk.
                 */
                query.setFirstResult(startPositionWlk);
                query.setMaxResults(queryMaxResults);

                @SuppressWarnings("unchecked")
                final List<org.savapage.core.jpa.xml.XEntityVersion> list =
                        query.getResultList();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Exporting [%s] rows %d - %d",
                            entityClassNameSimple, startPositionWlk + 1,
                            startPositionWlk + list.size()));
                }

                /*
                 * Process rows.
                 */
                for (final XEntityVersion obj : list) {

                    writer.writeStartElement(obj.xmlName());

                    for (final PropertyDescriptor descr : binfo
                            .getPropertyDescriptors()) {

                        final String propName = descr.getName();

                        if (propName.equals("class")) {
                            continue;
                        }

                        final String propVal =
                                BeanUtils.getProperty(obj, propName);

                        if (propVal == null) {
                            continue;
                        }

                        final Class<?> propClass = descr.getPropertyType();

                        final String text;

                        if (propClass.equals(java.util.Date.class)) {

                            text = xmlDateFormat
                                    .format(descr.getReadMethod().invoke(obj));

                        } else {

                            boolean isSimpleType = false;

                            for (final Class<?> cls : SIMPLE_JPA_TYPES) {
                                if (propClass.equals(cls)) {
                                    isSimpleType = true;
                                    break;
                                }
                            }

                            if (isSimpleType) {
                                text = XmlParseHelper
                                        .removeIllegalChars(propVal);
                            } else {
                                text = null;
                            }
                        }

                        if (text != null) {
                            writer.writeStartElement(propName);
                            writer.writeCharacters(text);
                            writer.writeEndElement();
                        }
                    }
                    writer.writeEndElement(); // </>
                }

                writer.flush(); // !!

                resultListSizeWlk = list.size();
                startPositionWlk += resultListSizeWlk;
            }

            writer.writeEndElement(); // </entity>

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * Imports the XML data into the current database according to the schema
     * version as specified in the XML root element. Also see
     * {@link #exportDb()} .
     * <p>
     * <b>IMPORTANT: this method implements its own commit scope.</b>
     * </p>
     * <p>
     * With this method we can create and populate the current database from an
     * export of an older schemaVersion. And when the application is started the
     * regular upgrade process will upgrade the current database to its own
     * application schema version.
     * </p>
     *
     * <p>
     * The database cannot be in-use when performing a restore, so the
     * application server needs to be stopped first.
     * </p>
     *
     * <p>
     * The value of {@link Key#COMMUNITY_VISITOR_START_DATE} is saved before the
     * import and restore after.
     * </p>
     *
     * @param exportedFile
     *            The exported XML file to import.
     * @param listener
     *            The {@link DbProcessListener}.
     */
    public static void importDb(final File exportedFile,
            final DbProcessListener listener) {

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Save the start of any visiting period.
         */
        final String savedVisitorStartDate =
                cm.readDbConfigKey(Key.COMMUNITY_VISITOR_START_DATE);

        /**
         * The BeanUtils.populate() method needs this converter to convert the
         * XML timestamp format (we used at the import) back to a Date object
         * again.
         */
        class DateConverter implements org.apache.commons.beanutils.Converter {

            private final SimpleDateFormat xmlDateFormat =
                    new SimpleDateFormat(XML_DATEFORMAT_PATTERN);

            @SuppressWarnings("rawtypes")
            @Override
            public Object convert(final Class arg0, final Object arg1) {

                Date date = null;

                if (arg0.equals(Date.class)) {
                    try {
                        date = xmlDateFormat.parse(arg1.toString());
                    } catch (ParseException e) {
                        throw new SpException("[" + arg1.toString()
                                + "] should be formatted as ["
                                + xmlDateFormat.toPattern() + "]", e);
                    }
                }
                return date;
            }
        }

        ConvertUtils.register(new DateConverter(), Date.class);

        /*
         * Zipped inputstream + dom4j document
         */
        FileInputStream fin = null;
        ZipInputStream zin = null;

        DaoBatchCommitter batchCommitter = null;

        try {
            fin = new FileInputStream(exportedFile);
            zin = new ZipInputStream(fin);

            final ZipEntry ze = zin.getNextEntry();

            if (ze == null) {
                // Empty zip file
                zin.close();
                return;
            }

            final XMLInputFactory factory = XMLInputFactory.newInstance();
            final XMLStreamReader reader = factory.createXMLStreamReader(zin);

            if (reader.getEventType() != XMLStreamReader.START_DOCUMENT) {
                throw new IllegalStateException("No XML Document.");
            }

            if (!reader.hasNext()) {
                throw new IllegalStateException("XML document is empty.");
            }

            /*
             * Read root element, and schema version.
             */
            int readerPosition = reader.next();

            if (readerPosition != XMLStreamReader.START_ELEMENT) {
                throw new IllegalStateException("XML document is empty.");
            }

            final String xmlSchemaVersion =
                    reader.getAttributeValue("", XML_ATTR_SCHEMA_VERSION);

            if (StringUtils.isBlank(xmlSchemaVersion)) {
                throw new IllegalStateException(
                        "Unknown database schema version.");
            }

            /*
             * IMPORTANT: close the context, i.e. the EntityManager, before
             * initializing the database. If NOT closed the initDb() hangs ...
             */
            ServiceContext.getDaoContext().close();

            /*
             * Initialize the database with the schema version of the import
             * file.
             */
            initDb(xmlSchemaVersion, listener, false);

            listener.onLogEvent("Importing ...");

            batchCommitter = ServiceContext.getDaoContext()
                    .createBatchCommitter(ConfigManager.getDaoBatchChunkSize());

            batchCommitter.open();

            /*
             * Read entities.
             */
            readerPosition = reader.next();

            while (readerPosition == XMLStreamReader.START_ELEMENT) {
                readerPosition = importDbEntityFromXml(reader, batchCommitter,
                        listener, getXmlSchemaSequenceEntity(xmlSchemaVersion));
                batchCommitter.commit();
            }

            reader.close();

        } catch (Exception e) {
            if (batchCommitter != null) {
                batchCommitter.rollback();
            }
            throw new SpException(e);

        } finally {

            if (batchCommitter != null) {
                batchCommitter.close();
            }

            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            if (fin != null) {
                /*
                 * Note: when FileInputStream runs out of scope, the OS file
                 * handle is actually closed after a garbage collect. In some
                 * environments a garbage collect only takes place after a
                 * certain number of open file handles is exceeded.
                 *
                 * You can check this with the Linux 'lsof' command.
                 */
                try {
                    fin.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            /*
             * Restore the saved start of any visiting period.
             */
            final DaoContext daoContext = ServiceContext.getDaoContext();
            final String actor = ServiceContext.getActor();

            try {
                daoContext.beginTransaction();
                cm.saveDbConfigKey(Key.COMMUNITY_VISITOR_START_DATE,
                        savedVisitorStartDate, actor);
                daoContext.commit();
            } finally {
                daoContext.rollback();
            }
        }
        listener.onLogEvent("Import finished.");
    }

    /**
     * Imports the XML data of {@link XEntityVersion} into the current database.
     *
     * @param reader
     *            The {@link XMLStreamReader}.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @param listener
     *            The {@link DbProcessListener}.
     * @param schemaSequenceEntityClass
     *            XML Entity Sequence class.
     * @return The current {@link XMLStreamReader#getEventType()} of the reader.
     * @throws Exception
     *             When an error occurs.
     */
    private static int importDbEntityFromXml(final XMLStreamReader reader,
            final DaoBatchCommitter batchCommitter,
            final DbProcessListener listener,
            final Class<?> schemaSequenceEntityClass) throws Exception {

        final String entityClassName = reader.getAttributeValue("", "name");
        final Class<?> entityClass = getEntityClassFromXmlAttr(entityClassName);

        final boolean isSequenceEntity = entityClass.getSimpleName()
                .equals(schemaSequenceEntityClass.getSimpleName());

        int count = 0;

        int readerPosition = reader.next();

        while (readerPosition == XMLStreamReader.START_ELEMENT) {
            readerPosition = importDbEntityRowFromXml(reader, entityClass,
                    isSequenceEntity);
            count++;
            batchCommitter.increment();
        }

        if (count > 0) {
            listener.onLogEvent(String.format("%20s : %d",
                    ((XEntityVersion) entityClass.newInstance()).xmlName(),
                    count));
        }

        return reader.next();
    }

    /**
     * Imports the XML data of an {@link XEntityVersion} row into the current
     * database.
     *
     * @param reader
     *            The {@link XMLStreamReader}.
     * @param entityClass
     *            The class of type {@link XEntityVersion}.
     * @param isSequenceEntityClass
     *            If {@code true}, entityClass represents a Sequence.
     * @return The current {@link XMLStreamReader#getEventType()} of the reader.
     * @throws Exception
     *             When an error occurs.
     */
    private static int importDbEntityRowFromXml(final XMLStreamReader reader,
            final Class<?> entityClass, final boolean isSequenceEntityClass)
            throws Exception {

        final Map<String, String> properties = new HashMap<String, String>();
        final StringBuilder value = new StringBuilder();

        int readerPosition = reader.next();

        while (readerPosition == XMLStreamReader.START_ELEMENT) {

            final String propName = reader.getLocalName();

            value.setLength(0);
            readerPosition = importDbEntityRowColumnFromXml(reader, value);

            final String propText = value.toString();

            final PropertyDescriptor desc =
                    new PropertyDescriptor(propName, entityClass);

            final Class<?> propClass = desc.getPropertyType();

            for (final Class<?> clsWlk : SIMPLE_JPA_TYPES) {
                if (propClass.equals(clsWlk)) {
                    properties.put(propName, propText);
                    break;
                }
            }
        }

        final EntityManager em = DaoContextImpl.peekEntityManager();
        final Entity objEntity = (Entity) entityClass.newInstance();

        if (!properties.isEmpty()) {
            BeanUtils.populate(objEntity, properties);
        }

        if (isSequenceEntityClass) {
            em.merge(objEntity);
        } else {
            em.persist(objEntity);
        }

        return reader.next();
    }

    /**
     * Imports (appends) the characters of the current XML element to the
     * "result" {@link StringBuilder}.
     *
     * @param reader
     *            The {@link XMLStreamReader}.
     * @param result
     *            {@link StringBuilder} to append the characters to.
     * @return The current {@link XMLStreamReader#getEventType()} of the reader.
     * @throws XMLStreamException
     *             When XML stream error.
     */
    private static int importDbEntityRowColumnFromXml(
            final XMLStreamReader reader, final StringBuilder result)
            throws XMLStreamException {

        int readerPosition = reader.next();

        while (readerPosition == XMLStreamReader.CHARACTERS) {
            result.append(reader.getText());
            readerPosition = reader.next();
        }

        return reader.next();
    }

    /**
     * Gets the class of type XEntityVersion from the class name found in the
     * database backup XML file.
     * <p>
     * For backwards compatibility, any package prefix is ignored, since we use
     * the fixed package of {@link Entity}.
     * </p>
     *
     * @see Mantis #368
     * @since 0.9.6
     * @param className
     *            The class name from the XML attribute.
     * @return The class of type {@link XEntityVersion}.
     * @throws ClassNotFoundException
     *             When no class could be found.
     */
    public static Class<?> getEntityClassFromXmlAttr(final String className)
            throws ClassNotFoundException {

        final String packageName = XEntityVersion.class.getPackage().getName();

        final int iStartSimpleName =
                className.lastIndexOf('.', className.length());
        final String fullClassName;

        if (iStartSimpleName < 0) {
            fullClassName = packageName + '.' + className;
        } else {
            fullClassName = packageName + '.'
                    + className.substring(iStartSimpleName + 1);
        }

        return Class.forName(fullClassName);
    }

    /**
     * Optimizes the Derby DB by compressing the tables. This reclaims unused,
     * allocated space in tables and its indexes.
     * <p>
     * <b>IMPORTANT: this method needs its own {@link EntityManager}.</b>
     * </p>
     * <p>
     * Look <a href=
     * "http://db.apache.org/derby/docs/10.1/ref/rrefaltertablecompress.html" >
     * here</a> for more detail.
     * </p>
     */
    public static void optimizeDbInternal() {

        if (!ConfigManager.isDbInternal()) {
            LOGGER.warn("The application is not configured to run the "
                    + "internal database. Cannot perform optimization.");
            return;
        }

        final EntityManager em = ConfigManager.instance().createEntityManager();

        try {
            Session session = em.unwrap(Session.class);

            session.doWork(new Work() {

                @Override
                public void execute(final Connection conn) throws SQLException {

                    CallableStatement cs = null;
                    try {
                        cs = conn.prepareCall(
                                "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE('"
                                        + DERBY_DB_SCHEMA_NAME + "', ?, 1)");
                    } catch (SQLException se) {
                        LOGGER.error("Unable to optimize internal database. "
                                + se.getMessage(), se);
                        throw se;
                    }

                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(
                                "Starting optimization of internal database.");
                    }

                    try {

                        for (String tableName : TABLES_FOR_DERBY_COMPRESSION) {

                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info(
                                        "Compressing internal database table: "
                                                + tableName);
                            }

                            try {
                                /*
                                 * Start transaction
                                 */
                                EntityTransaction entr = em.getTransaction();
                                entr.begin();

                                /*
                                 * Set parameter + execute
                                 */
                                cs.setString(1,
                                        tableName.toUpperCase(Locale.US));
                                cs.execute();

                                /*
                                 * Commit
                                 */
                                entr.commit();

                            } catch (SQLException se) {
                                LOGGER.error(
                                        "Error occurred compressing table: "
                                                + tableName + ". "
                                                + se.getMessage(),
                                        se);
                            }
                        }

                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Completed optimization of internal "
                                    + "database.");
                        }

                    } finally {
                        try {
                            cs.close();
                        } catch (SQLException e) {
                            LOGGER.error("Error closing CallableStatement: "
                                    + e.getMessage(), e);
                        }
                    }
                }
            });

        } finally {
            em.close();
        }
    }

}
