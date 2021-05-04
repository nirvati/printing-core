/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.savapage.core.SpException;

/**
 * Provides information about the host system.
 *
 * @author Rijk Ravestein
 *
 */
public final class SystemInfo {

    /**
     * OS Commands.
     */
    public enum Command {
        /**
         * Part of Poppler.
         */
        PDFFONTS("pdffonts"),
        /**
         * Part of ImageMagick.
         */
        CONVERT("convert"),

        /**
         * Part of CUPS.
         */
        CUPSFILTER("cupsfilter"),

        /**
         * Part of LibreOffice.
         */
        LIBREOFFICE("libreoffice"),

        /**
         * Part of fontconfig (generic font configuration library).
         */
        FC_MATCH("fc-match"),

        /**
         * Part of Poppler.
         *
         * @deprecated Use {@link SystemInfo.Command#PDFTOCAIRO} instead (Mantis
         *             #1079).
         */
        PDFTOPPM("pdftoppm"),

        /**
         * Part of Poppler.
         */
        PDFTOCAIRO("pdftocairo"),
        /**
         * Part of Ghostscript.
         */
        GS("gs"),
        /**
         * Part of Ghostscript.
         */
        PS2PDF("ps2pdf"),
        /**
         * .
         */
        QPDF("qpdf"),
        /**
         * Part of librsvg2-bin package.
         */
        RSVG_CONVERT("rsvg-convert"),
        /**
         * .
         */
        ULIMIT("ulimit"),
        /**
         * .
         */
        SYSCTL("/sbin/sysctl"),
        /**
         * .
         */
        WHICH("which"),
        /**
         * .
         */
        XPSTOPDF("xpstopdf");

        /** */
        private final String cmd;

        /**
         * @param command
         *            OS Command.
         */
        Command(final String command) {
            this.cmd = command;
        }

        /**
         *
         * @return OS command.
         */
        public String cmd() {
            return this.cmd;
        }

        /**
         * Create command line by concatenating argument to command.
         *
         * @param argument
         *            Command line argument.
         * @return OS command with argument.
         */
        public String cmdLine(final String argument) {
            return String.format("%s %s", this.cmd, argument);
        }

        /**
         * Create command line by concatenating arguments to command.
         *
         * @param args
         *            Arguments.
         * @return OS command with arguments.
         */
        public String cmdLineExt(final String... args) {
            final StringBuilder line = new StringBuilder();
            line.append(this.cmd);
            for (final String arg : args) {
                line.append(" ").append(arg);
            }
            return line.toString();
        }
    }

    /**
     * {@link Command#GS} CLI options.
     *
     */
    public enum ArgumentGS {

        /** */
        EMBED_ALL_FONTS("-dEmbedAllFonts=true"),
        /** */
        STDOUT_TO_STDOUT("-sstdout=%stdout");

        /** */
        private final String arg;

        /**
         * @param argument
         *            CLI argument.
         */
        ArgumentGS(final String argument) {
            this.arg = argument;
        }

        /**
         * @return The CLI argument.
         */
        public String getArg() {
            return this.arg;
        }
    }

    /** */
    public enum SysctlEnum {
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

        /**
         * Not present in Ubuntu 18.04.
         */
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
        SysctlEnum(final String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }

    /**
     * Registry of one-time lazy captured {@link Command} versions (not
     * refreshed).
     */
    public static final class CommandVersionRegistry {

        /** */
        private final String pdfToCairo;

        /** */
        private CommandVersionRegistry() {
            pdfToCairo = SystemInfo.getPdfToCairoVersion();
        }

        /**
         *
         * @return The {@link Command#PDFTOCAIRO} version.
         */
        public String getPdfToCairo() {
            return pdfToCairo;
        }

    }

    /** */
    private static class VersionRegistryHolder {
        /**
         * The singleton.
         */
        private static final CommandVersionRegistry INSTANCE =
                new SystemInfo.CommandVersionRegistry();
    }

    /**
     * Utility class.
     */
    private SystemInfo() {
    }

    /**
     * @return {@link SystemInfo.CommandVersionRegistry}.
     */
    public static CommandVersionRegistry getCommandVersionRegistry() {
        return VersionRegistryHolder.INSTANCE;
    }

    /**
     * Initialize.
     */
    public static void init() {
        getCommandVersionRegistry();
    }

    /**
     * Retrieves the Poppler {@link Command#PDFTOPPM} version from the system.
     * <p>
     * <a href=
     * "http://poppler.freedesktop.org">http://poppler.freedesktop.org</a>
     * </p>
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getPdfToPpmVersion() {

        final String cmd = Command.PDFTOPPM.cmdLine("-v");

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
     * Retrieves the Poppler {@link Command#PDFTOCAIRO} version from the system.
     * <p>
     * <a href=
     * "http://poppler.freedesktop.org">http://poppler.freedesktop.org</a>
     * </p>
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getPdfToCairoVersion() {

        final String cmd = Command.PDFTOCAIRO.cmdLine("-v");

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();

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
     * Retrieves the Poppler {@link Command#PDFFONTS} version from the system.
     * <p>
     * <a href=
     * "http://poppler.freedesktop.org">http://poppler.freedesktop.org</a>
     * </p>
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getPdfFontsVersion() {

        final String cmd = Command.PDFFONTS.cmdLine("-v");

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();

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
     * Retrieves the ImageMagick {@link Command#CONVERT} version from the
     * system.
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getImageMagickVersion() {

        final String cmd = Command.CONVERT.cmdLine("-version");

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
     * Retrieves the fontconfig {@link Command#FC_MATCH} version from the
     * system.
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getFontConfigVersion() {

        final String cmd = Command.FC_MATCH.cmdLine("--version");

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        try {
            int rc = exec.executeCommand();
            if (rc != 0) {
                return null;
            }
            return exec.getStandardError();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * Retrieves the Ghostscript {@link Command.GS} version from the system.
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getGhostscriptVersion() {

        final String cmd = Command.GS.cmdLine("-version");

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

    /** */
    private static volatile Boolean cachedRSvgConvertInstallIndication = null;

    /**
     * Finds out if {@link Command#QPDF} is installed using indication from
     * cache.
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
     * Retrieves the {@link Command#QPDF} version from the system (and sets
     * installed cache indication).
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getQPdfVersion() {

        final String cmd = Command.QPDF.cmdLine("--version");

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
     * Finds out if {@link Command#RSVG_CONVERT} is installed using indication
     * from cache.
     *
     * @return {@code true} if installed.
     */
    public static boolean isRSvgConvertInstalled() {

        if (cachedRSvgConvertInstallIndication == null) {
            getRSvgConvertVersion();
        }
        return cachedRSvgConvertInstallIndication.booleanValue();
    }

    /**
     * Retrieves the {@link Command.RSVG_CONVERT} version from the system (and
     * sets installed cache indication).
     *
     * @return The version string(s) or {@code null} if not installed.
     */
    public static String getRSvgConvertVersion() {

        final String cmd = Command.RSVG_CONVERT.cmdLine("--version");

        final ICommandExecutor exec = CommandExecutor.createSimple(cmd);

        String version = null;

        try {
            if (exec.executeCommand() == 0) {
                version = exec.getStandardOutput();
            }
        } catch (Exception e) {
            throw new SpException(e);
        }

        cachedRSvgConvertInstallIndication = Boolean.valueOf(version != null);

        return version;
    }

    /**
     * @return The output of the command: {@code ulimit -n}.
     */
    public static String getUlimitsNofile() {

        final String cmd = Command.ULIMIT.cmdLine("-n");

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
     * @param sysctl
     *            The {@link SysctlEnum}.
     * @return The output of the command: {@code sysctl -n key}, or {@code null}
     *         when value of key not found.
     */
    public static String getSysctl(final SysctlEnum sysctl) {

        final String cmd = Command.SYSCTL.cmdLineExt("-n", sysctl.getKey());

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
     * @return Start time of the Java virtual machine in milliseconds.
     */
    public static long getStarttime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
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
                } catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
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
