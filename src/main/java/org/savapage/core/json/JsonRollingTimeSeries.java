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
package org.savapage.core.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.jpa.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A statistical rolling time series for a maximum number of intervals.
 *
 * <p>
 * See <a
 * href="http://en.wikipedia.org/wiki/Time_series">http://en.wikipedia.org
 * /wiki/Time_series</a>
 * </p>
 *
 * @author Datraverse B.V.
 *
 * @param <T>
 *            Type should be {@link Integer}, {@link Long} or {@link Double}.
 */
public class JsonRollingTimeSeries<T> extends JsonAbstractBase {

    private final static String PROP_TIME = "time";
    private final static String PROP_SERIES = "series";

    @JsonIgnore
    private final TimeSeriesInterval interval;

    @JsonIgnore
    private final T zeroValue;

    @JsonIgnore
    private final int maxIntervals;

    @JsonProperty(PROP_TIME)
    private Long lastTime;

    @JsonProperty(PROP_SERIES)
    private ArrayList<T> data;

    /**
     * Creates a rolling time series instance.
     *
     * @param interval
     *            The type of {@link TimeSeriesInterval}.
     * @param maxPoints
     *            The maximum number of intervals in the time series.
     * @param zeroValue
     *            The value representing the zero value.
     */
    public JsonRollingTimeSeries(final TimeSeriesInterval interval,
            int maxIntervals, T zeroValue) {

        if (!(zeroValue instanceof Integer || zeroValue instanceof Long || zeroValue instanceof Double)) {
            throw new SpException("observation of type ["
                    + zeroValue.getClass().getSimpleName()
                    + "] is not supported");
        }
        this.zeroValue = zeroValue;
        this.interval = interval;
        this.maxIntervals = maxIntervals;
    }

    /**
     *
     */
    public void clear() {
        lastTime = null;
        if (data != null) {
            data.clear();
            data = null;
        }
    }

    /**
     * Initializes this object from JSON string, see {@link #init(String)} and
     * adds zeroValue observation at observationTime to interpolate zeroValues
     * between the last observation and observationTime.
     * <p>
     * NOTE: if JSON string is blank, than the object is cleared, see
     * {@link #clear()}.
     * </p>
     *
     * @param observationTime
     * @param json
     *            The json string.
     * @throws IOException
     */
    public void init(final Date observationTime, final String json)
            throws IOException {
        if (StringUtils.isBlank(json)) {
            clear();
        } else {
            init(json);
        }
        addDataPoint(observationTime, zeroValue);
    }

    /**
     * Initializes this object from JSON string. If the JSON string is invalid
     * (or empty) the object is cleared, see {@link #clear()}.
     *
     * @param json
     *            The json string.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void init(final String json) throws IOException {

        JsonNode rootNode = null;
        try {
            rootNode = getMapper().readValue(json, JsonNode.class);
        } catch (JsonProcessingException e) {
            /*
             * Be forgiving...
             */
            clear();
            return;
        }

        data = new ArrayList<>();
        lastTime = rootNode.path(PROP_TIME).longValue();

        Iterator<JsonNode> elements = rootNode.path(PROP_SERIES).elements();

        int nIntervals = 0;

        while (elements.hasNext() && nIntervals < maxIntervals) {

            JsonNode element = elements.next();

            Object point = null;

            if (zeroValue instanceof Integer) {
                point = Integer.valueOf(element.asInt());
            } else if (zeroValue instanceof Long) {
                point = Long.valueOf(element.asLong());
            } else if (zeroValue instanceof Double) {
                point = Double.valueOf(element.asDouble());
            } else {
                throw new SpException("Oops, missed type "
                        + zeroValue.getClass().getSimpleName());
            }
            data.add((T) point);
            nIntervals++;
        }
    }

    public Long getLastTime() {
        return lastTime;
    }

    public void setLastTime(Long lastTime) {
        this.lastTime = lastTime;
    }

    /**
     * The first element in the array is the total on {@link #lastTime}. Every
     * next item is one (1) {@link #interval} EARLIER.
     *
     * @return
     */
    public ArrayList<T> getData() {
        return data;
    }

    public void setData(ArrayList<T> data) {
        this.data = data;
    }

    @JsonIgnore
    public TimeSeriesInterval getInterval() {
        return interval;
    }

    @JsonIgnore
    public int getMaxIntervals() {
        return maxIntervals;
    }

    /**
     * Adds a number of intervals to a date.
     *
     * @param date
     * @param interval
     * @param nIntervals
     * @return
     */
    private static Date addInterval(TimeSeriesInterval interval, Date date,
            int nIntervals) {
        switch (interval) {
        case DAY:
            return DateUtils.addDays(date, nIntervals);
        case HOUR:
            return DateUtils.addHours(date, nIntervals);
        case MONTH:
            return DateUtils.addMonths(date, nIntervals);
        case WEEK:
            return DateUtils.addWeeks(date, nIntervals);
        default:
            throw new SpException("Oops, missed a TimeSeriesInterval");
        }
    }

    /**
     * Adds an observation to a time series key and updates the total in the
     * database.
     *
     * @param key
     * @param observationTime
     * @param observation
     * @throws IOException
     */
    public void addDataPoint(final IConfigProp.Key key,
            final Date observationTime, T observation) {

        final ConfigManager cm = ConfigManager.instance();
        final String json = cm.getConfigValue(key);

        try {

            if (StringUtils.isNotBlank(json)) {
                init(json);
            }

            addDataPoint(observationTime, observation);
            cm.updateConfigKey(key, stringify(), Entity.ACTOR_SYSTEM);

        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param observationTime
     * @param observation
     */
    public void addDataPoint(final Date observationTime, T observation) {

        if (data == null) {
            data = new ArrayList<>();
        }

        /*
         * The date of the interval to cumulate the observation on.
         */
        Date pointDate = null;

        if (interval == TimeSeriesInterval.WEEK) {

            final Calendar observationCal = Calendar.getInstance();

            observationCal.setTime(observationTime);

            /*
             * The week-of-the-year of the observation.
             */
            int observationWeekOfYear =
                    observationCal.get(Calendar.WEEK_OF_YEAR) - 1;

            /*
             * If observation month is DECEMBER and observationWeekOfYear is 0
             * (zero) then we have a SPECIAL situation: one of the last dates of
             * the year falling in the first week of next year.
             */
            if (observationCal.get(Calendar.MONTH) == (12 - 1)
                    && observationWeekOfYear == 0) {
                /*
                 * Initialize pointDate to the first day of the NEXT observation
                 * year (by adding a week to the observation time).
                 */
                pointDate =
                        DateUtils.truncate(
                                DateUtils.addWeeks(observationTime, 1),
                                Calendar.YEAR);

            } else {
                /*
                 * Initialize pointDate to the first day of the observation
                 * year.
                 */
                pointDate = DateUtils.truncate(observationTime, Calendar.YEAR);
            }

            /*
             * Find the first day of the observation week by stepping BACK one
             * (1) day at a time, till we are at the first day of the
             * observation week.
             */
            observationCal.setTime(pointDate);

            while (observationCal.get(Calendar.DAY_OF_WEEK) != observationCal
                    .getFirstDayOfWeek()) {
                observationCal.setTime(DateUtils.addDays(
                        observationCal.getTime(), -1));
            }
            /*
             * Set the point date by adding the number of weeks to the
             * first-day-of-the-week of the observation time.
             */
            pointDate =
                    DateUtils.addWeeks(observationCal.getTime(),
                            observationWeekOfYear);

        } else {

            int field;

            switch (interval) {
            case DAY:
                field = Calendar.DAY_OF_MONTH;
                break;
            case HOUR:
                field = Calendar.HOUR_OF_DAY;
                break;
            case MONTH:
                field = Calendar.MONTH;
                break;
            case WEEK:
                // no break intended
            default:
                throw new SpException("Oops, missed a TimeSeriesInterval");
            }

            pointDate = DateUtils.truncate(observationTime, field);
        }

        /*
         * We got the point date, now add on NEW or EXISTING point date.
         */
        if (lastTime == null) {

            data.add(observation);
            lastTime = pointDate.getTime();

        } else if (pointDate.getTime() == lastTime) {

            /*
             * ADD observation on existing point.
             */
            Object point = data.get(0);

            if (point instanceof Integer) {
                Integer total = (Integer) point;
                total += (Integer) observation;
                point = total;
            } else if (point instanceof Long) {
                Long total = (Long) point;
                total += (Long) observation;
                point = total;
            } else if (point instanceof Double) {
                Double total = (Double) point;
                total += (Double) observation;
                point = total;
            }
            data.set(0, ((T) point));

        } else {

            /*
             * NEW observation point.
             */

            /*
             * Calculate the number of intervals from last observation.
             */
            final Date lastDate = new Date(lastTime);
            int zeroIntervals = 0;

            while (zeroIntervals < maxIntervals
                    && !DateUtils.isSameInstant(pointDate,
                            addInterval(interval, lastDate, zeroIntervals + 1))) {
                zeroIntervals++;
            }

            if (zeroIntervals < maxIntervals) {
                for (int i = 0; i < zeroIntervals; i++) {
                    data.add(0, zeroValue);
                }
            } else {
                data.clear();
            }

            lastTime = pointDate.getTime();
            data.add(0, observation);

            while (data.size() > maxIntervals) {
                data.remove(data.size() - 1);
            }
        }
    }
}
