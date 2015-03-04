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
package org.savapage.core.system;

import org.savapage.core.SpException;

/**
 * Provides information about the host system.
 *
 * @author Datraverse B.V.
 *
 */
public final class SystemInfo {

    /**
     *
     * @author Datraverse B.V.
     *
     */
    public static enum SysctlEnum {

        /**
         * .
         */
        NET_CORE_RMEM_MAX("net.core.rmem_max"),
        /**
         * .
         */
        NET_CORE_WMEM_MAX("net.core.wmem_max"),

        /**
         * .
         */
        NET_CORE_SOMAXCONN("net.core.somaxconn"),

        /**
         * .
         */
        NET_CORE_NETDEV_MAX_BACKLOG("net.core.netdev_max_backlog"),

        /**
         * .
         */
        NET_IPV4_TCP_RMEM("net.ipv4.tcp_rmem"),

        /**
         * .
         */
        NET_IPV4_TCP_WMEM("net.ipv4.tcp_wmem"),

        /**
         * .
         */
        NET_IPV4_TCP_MAX_SYN_BACKLOG("net.ipv4.tcp_max_syn_backlog"),

        /**
         * .
         */
        NET_IPV4_TCP_SYNCOOKIES("net.ipv4.tcp_syncookies"),

        /**
         * .
         */
        NET_IPV4_IP_LOCAL_PORT_RANGE("net.ipv4.ip_local_port_range"),

        /**
         * .
         */
        NET_IPV4_TCP_TW_RECYCLE("net.ipv4.tcp_tw_recycle"),

        /**
         * .
         */
        NET_IPV4_TCP_TW_REUSE("net.ipv4.tcp_tw_reuse"),

        /**
         * .
         */
        NET_IPV4_TCP_AVAILABLE_CONGESTION_CONTROL(
                "net.ipv4.tcp_available_congestion_control"),

        /**
         * .
         */
        NET_IPV4_TCP_CONGESTION_CONTROL("net.ipv4.tcp_congestion_control");

        /**
         * .
         */
        private final String key;

        /**
         *
         * @param key
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
     * <a
     * href="http://poppler.freedesktop.org">http://poppler.freedesktop.org</a>
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
            return exec.getStandardErrorFromCommand().toString();

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
            return exec.getStandardOutputFromCommand().toString();
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
            return exec.getStandardOutputFromCommand().toString();
        } catch (Exception e) {
            throw new SpException(e);
        }
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
            return exec.getStandardOutputFromCommand().toString();
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
            return exec.getStandardOutputFromCommand().toString();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

}
