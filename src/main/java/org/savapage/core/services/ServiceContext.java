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
package org.savapage.core.services;

import java.util.Date;
import java.util.Locale;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.jpa.Entity;
import org.savapage.core.services.impl.ServiceFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The context for each {@link ServiceEntryPoint}.
 * <p>
 * The static {@link #open()} and {@link #close()} methods MUST be called and
 * the start and end of the {@link ServiceEntryPoint} scope.
 * </p>
 * <p>
 * Note: A {@link ThreadLocal} instance of this class is created upon
 * {@link #open()} and removed upon {@link #close()}.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public final class ServiceContext {

    private static int openCount = 0;

    /**
     * @return The number of open {@link ServiceContext} objects.
     */
    public static int getOpenCount() {
        synchronized (ServiceContext.class) {
            return openCount;
        }
    }

    private static void incrementOpenCount() {
        synchronized (ServiceContext.class) {
            openCount++;
        }
    }

    private static void decrementOpenCount() {
        synchronized (ServiceContext.class) {
            openCount--;
        }
    }

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServiceContext.class);

    /**
     *
     */
    private Locale locale;

    /**
     *
     */
    private String actor;

    /**
     *
     */
    private Date transactionDate;

    /**
     *
     */
    private boolean daoContextOpened = false;

    /**
     * The {@link ThreadLocal} instance of this class.
     */
    private static final ThreadLocal<ServiceContext> SERVICE_CONTEXT =
            new ThreadLocal<ServiceContext>() {

                @Override
                protected ServiceContext initialValue() {
                    incrementOpenCount();
                    LOGGER.trace("initialValue()");
                    return new ServiceContext();
                }

                @Override
                public void remove() {
                    LOGGER.trace("remove()");
                    super.remove();
                    decrementOpenCount();
                }
            };

    /**
     */
    private static class ServiceFactoryHolder {
        public static final ServiceFactory SERVICE_FACTORY =
                new ServiceFactoryImpl();
    }

    /**
     * NO public instantiation allowed.
     */
    private ServiceContext() {
    }

    /**
     * This method MUST be called at the <i>start</i> of a
     * {@link ServiceEntryPoint}.
     * <p>
     * This method is <i>idempotent</i>. It creates {@link ServiceContext} as
     * {@link ThreadLocal} object on first use.
     * </p>
     * <p>
     * Important: {@link #close()} MUST be called at the <i>end</i> of a
     * {@link ServiceEntryPoint} (finally block).
     * </p>
     */
    public static void open() {
        /*
         * Trigger ThreadLocal#initialValue()
         */
        SERVICE_CONTEXT.get();
        resetTransactionDate();
    }

    /**
     * This method MUST be called at the <i>end</i> of a
     * {@link ServiceEntryPoint}.
     * <p>
     * This method is <i>idempotent</i>. It removes any existing
     * {@link ThreadLocal} objects of class {@link ServiceContext} and
     * {@link DaoContextImpl}.
     * </p>
     */
    public static void close() {

        if (instance().daoContextOpened) {
            DaoContextImpl.instance().close();
            instance().daoContextOpened = false;
        }
        SERVICE_CONTEXT.remove();
    }

    /**
     *
     * @return {@link ServiceFactory}.
     */
    public static ServiceFactory getServiceFactory() {
        return ServiceFactoryHolder.SERVICE_FACTORY;
    }

    /**
     *
     * @return The singleton.
     */
    private static ServiceContext instance() {
        return SERVICE_CONTEXT.get();
    }

    /**
     * Lazy creates the {@link DaoContextImpl}.
     *
     * @return The {@link DaoContext}.
     */
    public static DaoContext getDaoContext() {
        instance().daoContextOpened = true;
        return DaoContextImpl.instance();
    }

    /**
     *
     * @return The Locale.
     */
    public static Locale getLocale() {
        if (instance().locale == null) {
            return Locale.getDefault();
        }
        return instance().locale;
    }

    /**
     *
     * @return The currency symbol.
     */
    public static String getAppCurrencySymbol() {
        return ConfigManager.getAppCurrencySymbol(getLocale());
    }

    /**
     *
     * @param locale
     *            The Locale.
     */
    public static void setLocale(final Locale locale) {
        instance().locale = locale;
    }

    /**
     * Gets the current Actor.
     *
     * @return The actor.
     */
    public static String getActor() {
        if (instance().actor == null) {
            return Entity.ACTOR_SYSTEM;
        }
        return instance().actor;
    }

    /**
     * Sets the current Actor.
     *
     * @param actor
     *            The actor.
     */
    public static void setActor(final String actor) {
        instance().actor = actor;
    }

    /**
     * Gets the date/time of the current transaction.
     *
     * @return The date.
     */
    public static Date getTransactionDate() {
        if (instance().transactionDate == null) {
            resetTransactionDate();
        }
        return instance().transactionDate;
    }

    /**
     * Sets the date/time of the current transaction.
     *
     * @param date
     *            The date.
     */
    public static void setTransactionDate(final Date date) {
        instance().transactionDate = date;
    }

    /**
     * Resets the date/time of the current transaction to <i>now</i>.
     */
    public static void resetTransactionDate() {
        instance().transactionDate = new Date();
    }

}
