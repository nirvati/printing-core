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
package org.savapage.core.print.smartschool;

/**
 * Smartschool Account.
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolAccount {

    /**
     * The Smartschool Cluster Node.
     *
     * @author Rijk Ravestein
     *
     */
    public final static class Node {

        private String id;
        private boolean proxy;
        private String proxyEndpoint;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isProxy() {
            return proxy;
        }

        public void setProxy(boolean proxy) {
            this.proxy = proxy;
        }

        public String getProxyEndpoint() {
            return proxyEndpoint;
        }

        public void setProxyEndpoint(String proxyEndpoint) {
            this.proxyEndpoint = proxyEndpoint;
        }

    }

    /**
     * The Smartschool Configuration.
     *
     * @author Rijk Ravestein
     *
     */
    public final static class Config {

        /**
         * Name of the proxy printer, can be {@code null} or empty.
         */
        private String proxyPrinterName;

        /**
         * Name of the duplex proxy printer, can be {@code null} or empty.
         */
        private String proxyPrinterDuplexName;

        /**
         * Name of the grayscale proxy printer, can be {@code null} or empty.
         */
        private String proxyPrinterGrayscaleName;

        /**
         * Name of the grayscale duplex proxy printer, can be {@code null} or
         * empty.
         */
        private String proxyPrinterGrayscaleDuplexName;

        /**
         * {@code true} if costs are charged to individual students,
         * {@code false} if costs are charged to shared "Klas" accounts only.
         */
        private boolean chargeToStudents;

        /**
         * @return The default proxy printer (can be {@code null} or empty).
         */
        public String getProxyPrinterName() {
            return proxyPrinterName;
        }

        public void setProxyPrinterName(String proxyPrinterName) {
            this.proxyPrinterName = proxyPrinterName;
        }

        /**
         * @return The default proxy printer for duplex printing (can be
         *         {@code null} or empty).
         */
        public String getProxyPrinterDuplexName() {
            return proxyPrinterDuplexName;
        }

        public void setProxyPrinterDuplexName(String proxyPrinterDuplexName) {
            this.proxyPrinterDuplexName = proxyPrinterDuplexName;
        }

        /**
         * @return The proxy printer for grayscale printing (can be {@code null}
         *         or empty).
         */
        public String getProxyPrinterGrayscaleName() {
            return proxyPrinterGrayscaleName;
        }

        public void setProxyPrinterGrayscaleName(
                String proxyPrinterGrayscaleName) {
            this.proxyPrinterGrayscaleName = proxyPrinterGrayscaleName;
        }

        /**
         * @return The proxy printer for grayscale duplex printing (can be
         *         {@code null} or empty).
         */
        public String getProxyPrinterGrayscaleDuplexName() {
            return proxyPrinterGrayscaleDuplexName;
        }

        public void setProxyPrinterGrayscaleDuplexName(
                String proxyPrinterGrayscaleDuplexName) {
            this.proxyPrinterGrayscaleDuplexName =
                    proxyPrinterGrayscaleDuplexName;
        }

        /**
         * @return {@code true} if costs are charged to individual students,
         *         {@code false} if costs are charged to shared "Klas" accounts
         *         only.
         */
        public boolean isChargeToStudents() {
            return chargeToStudents;
        }

        public void setChargeToStudents(boolean chargeToStudents) {
            this.chargeToStudents = chargeToStudents;
        }

    }

    /**
     * The SOAP endpoint.
     */
    private String endpoint;

    /**
     * Password for the Smartschool Afdrukcentrum.
     */
    private char[] password;

    private Node node;
    private final Config config = new Config();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    /**
     *
     * @return {@code null} when NO Smartschool clustering.
     */
    public Node getNode() {
        return node;
    }

    /**
     *
     * @param node
     *            {@code null} when NO Smartschool clustering.
     */
    public void setNode(Node node) {
        this.node = node;
    }

    public Config getConfig() {
        return config;
    }

}
