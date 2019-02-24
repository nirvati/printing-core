/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides information about the host system.
 *
 * @author Rijk Ravestein
 *
 */
public final class SystemInfo {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SystemInfo.class);

    /** */
    public static enum SysctlEnum {
        /** */
        NET_CORE_RMEM_MAX("net.core.rmem_max"),
        /** */
        NET_CORE_WMEM_MAX("net.core.wmem_max"),
        /** */
        NET_CORE_SOMAXCONN("net.core.somaxconn"),
        /** */
        NET_CORE_NETDEV_MAX_BACKLOG("net.core.netdev_max_backlog"),
        /** */
        NET_IPV4_TCP_RMEM("net.ipv4.tcp_rmem"),
        /** */
        NET_IPV4_TCP_WMEM("net.ipv4.tcp_wmem"),
        /** */
        NET_IPV4_TCP_MAX_SYN_BACKLOG("net.ipv4.tcp_max_syn_backlog"),
        /** */
        NET_IPV4_TCP_SYNCOOKIES("net.ipv4.tcp_syncookies"),
        /** */
        NET_IPV4_IP_LOCAL_PORT_RANGE("net.ipv4.ip_local_port_range"),
        /** */
        NET_IPV4_TCP_TW_RECYCLE("net.ipv4.tcp_tw_recycle"),
        /** */
        NET_IPV4_TCP_TW_REUSE("net.ipv4.tcp_tw_reuse"),
        /** */
        NET_IPV4_TCP_AVAILABLE_CONGESTION_CONTROL(
                "net.ipv4.tcp_available_congestion_control"),
        /** */
        NET_IPV4_TCP_CONGESTION_CONTROL("net.ipv4.tcp_congestion_control");

        /** */
        private final String key;

        /**
         * @param key
         *            Key.
         */
        private SysctlEnum(final String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }

    private SystemInfo() {
    }

    /**
     * Retrieves the Poppler {@code pdftoppm} version from the system.
     * <p>
     * <a href=
     * "http://poppler.freedesktop.org">http://poppler.freedesktop.org</a>
     * </p>
     *
     * @return The version string(s) or {@code null} when not installed.
     */
    public static String getPdfToPpmVersion() {

        final String cmd = "pdftoppm -v";

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();

            /*
             * pdftoppm version 0.12.4 gives rc == 99
             *
             * pdftoppm version 0.18.4 gives rc == 0
             */
            if (rc != 0 && rc != 99) {
                return null;
            }

            /*
             * Note: version is echoed on stderr.
             */
            return exec.getStandardError();

        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * Retrieves the ImageMagick version from the system.
     *
     * @return The version string(s) or {@code null} when ImageMagick is not
     *         installed.
     */
    public static String getImageMagickVersion() {

        final String cmd = "convert -version";

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();
            if (rc != 0) {
                return null;
            }
            return exec.getStandardOutput();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * Retrieves the Ghostscript version from the system.
     *
     * @return The version string(s) or {@code null} when the Ghostscript is not
     *         installed.
     */
    public static String getGhostscriptVersion() {

        String cmd = "gs -version";

        ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();
            if (rc != 0) {
                return null;
            }
            return exec.getStandardOutput();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /** */
    private static volatile Boolean cachedQPdfInstallIndication = null;

    /**
     * Finds out if {@code qpdf} is installed using indication from cache.
     *
     * @return {@code true} if installed.
     */
    public static boolean isQPdfInstalled() {

        if (cachedQPdfInstallIndication == null) {
            getQPdfVersion();
        }
        return cachedQPdfInstallIndication.booleanValue();
    }

    /**
     * Retrieves the qpdf version from the system (and sets installed achache
     * indication).
     *
     * @return The version string(s) or {@code null} when the qpdf is not
     *         installed.
     */
    public static String getQPdfVersion() {

        final String cmd = "qpdf --version";
        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        String version = null;

        try {
            if (exec.executeCommand() == 0) {
                version = exec.getStandardOutput();
            }
        } catch (Exception e) {
            throw new SpException(e);
        }

        cachedQPdfInstallIndication = Boolean.valueOf(version != null);

        return version;
    }

    /**
     * @return The output of the command: {@code ulimit -n}.
     */
    public static String getUlimitsNofile() {

        final String cmd = "ulimit -n";

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();
            if (rc != 0) {
                return null;
            }
            return exec.getStandardOutput();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * TODO: /sbin/sysctl
     *
     * @param sysctl
     *            The {@link SysctlEnum}.
     * @return The output of the command: {@code sysctl -n key}.
     */
    public static String getSysctl(final SysctlEnum sysctl) {

        final String cmd = String.format("/sbin/sysctl -n %s", sysctl.getKey());

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();
            if (rc != 0) {
                return null;
            }
            return exec.getStandardOutput();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * @return Uptime of the Java virtual machine in milliseconds.
     */
    public static long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    /**
     *
     * @return {@link OperatingSystemProps}.
     */
    public static OperatingSystemProps getOperatingSystemProps() {

        final OperatingSystemProps props = new OperatingSystemProps();

        final OperatingSystemMXBean osBean =
                ManagementFactory.getOperatingSystemMXBean();

        for (final Method method : osBean.getClass().getDeclaredMethods()) {

            method.setAccessible(true);

            if (method.getName().startsWith("get")
                    && Modifier.isPublic(method.getModifiers())) {

                try {
                    switch (method.getName()) {
                    case "getCommittedVirtualMemorySize":
                        props.setCommittedVirtualMemorySize(
                                Long.valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getTotalSwapSpaceSize":
                        props.setTotalSwapSpaceSize(
                                Long.valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getFreeSwapSpaceSize":
                        props.setFreeSwapSpaceSize(
                                Long.valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getProcessCpuTime":
                        props.setProcessCpuTime(
                                Long.valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getFreePhysicalMemorySize":
                        props.setFreePhysicalMemorySize(
                                Long.valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getTotalPhysicalMemorySize":
                        props.setTotalPhysicalMemorySize(
                                Long.valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getSystemCpuLoad":
                        props.setSystemCpuLoad(Double
                                .valueOf(method.invoke(osBean).toString()));
                        break;
                    case "getProcessCpuLoad":
                        props.setProcessCpuLoad(Double
                                .valueOf(method.invoke(osBean).toString()));
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    // no code intended
                }
            }
        }
        props.setFileDescriptorCount(getFileDescriptorCount(osBean));
        return props;
    }

    /**
     * @return (@link SystemFileDescriptorCount} from
     *         {@link OperatingSystemMXBean}.
     */
    private static SystemFileDescriptorCount
            getFileDescriptorCount(final OperatingSystemMXBean osBean) {

        final SystemFileDescriptorCount count = new SystemFileDescriptorCount();

        int nProp = 0;

        for (final Method method : osBean.getClass().getDeclaredMethods()) {

            method.setAccessible(true);

            if (method.getName().startsWith("get")
                    && Modifier.isPublic(method.getModifiers())) {

                try {
                    switch (method.getName()) {
                    case "getOpenFileDescriptorCount":
                        count.setOpenFileCount(
                                Long.valueOf(method.invoke(osBean).toString()));
                        nProp++;
                        break;
                    case "getMaxFileDescriptorCount":
                        count.setMaxFileCount(
                                Long.valueOf(method.invoke(osBean).toString()));
                        nProp++;
                        break;
                    default:
                        break;
                    }
                } catch (Exception e) {
                    // no code intended
                }
            }
            if (nProp == 2) {
                break;
            }
        }
        return count;
    }

    /**
     * @return (@link SystemFileDescriptorCount} from
     *         {@link OperatingSystemMXBean}.
     */
    public static SystemFileDescriptorCount getFileDescriptorCount() {
        return getFileDescriptorCount(
                ManagementFactory.getOperatingSystemMXBean());
    }

}
