/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2021 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.savapage.common.SystemPropertyEnum;
import org.savapage.core.SpException;
import org.savapage.core.config.ServerPathEnum;
import org.savapage.core.template.feed.AdminFeedTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * I18n statistics and diagnostics.
 *
 * @author Rijk Ravestein
 *
 */
public final class I18nStats {

    /**
     * XML statistics.
     */
    public static class XMLStats {

        /**
         * Number of XML {@code <entry>} elements.
         */
        private int entries;

        public XMLStats() {
            this.entries = 0;
        }

        public int getEntries() {
            return entries;
        }

        public void setEntries(int entries) {
            this.entries = entries;
        }
    }

    /**
     * Statistics of i18n language.
     */
    public static class XMLFiles {

        /**
         * Statistics (value) of XML files present (key).
         */
        private final TreeMap<String, XMLStats> filesPresent;

        /**
         * Statistics (value) of XML files to needs to be (fully) translated
         * (key).
         */
        private final TreeMap<String, XMLStats> filesToDo;

        /**
         * {@code <entry>} elements to be translated (value) of "translated" XML
         * files (key).
         */
        private final TreeMap<String, TreeSet<String>> keysToDo;

        /**
         * Obsolete {@code <entry>} elements (value) of "translated" XML files
         * (key).
         */
        private final TreeMap<String, TreeSet<String>> keysObsolete;

        /**
         * Statistics (sum) of all XML files present.
         */
        private final XMLStats totalPresent;

        /**
         * Statistics (sum) of entries to be translated of all XML files present
         * or absent.
         */
        private final XMLStats totalToDo;

        /**
         * Statistics (sum) of obsolete entries of all XML files present.
         */
        private final XMLStats totalObsolete;

        /**
         * Percentage translated.
         */
        private int percentage;

        /**
         * Constructor.
         */
        public XMLFiles() {
            this.filesPresent = new TreeMap<>();
            this.filesToDo = new TreeMap<>();

            this.keysToDo = new TreeMap<>();
            this.keysObsolete = new TreeMap<>();

            this.totalToDo = new XMLStats();
            this.totalObsolete = new XMLStats();
            this.totalPresent = new XMLStats();
        }

        public TreeMap<String, XMLStats> getFilesPresent() {
            return filesPresent;
        }

        public TreeMap<String, XMLStats> getFilesToDo() {
            return filesToDo;
        }

        public TreeMap<String, TreeSet<String>> getKeysToDo() {
            return keysToDo;
        }

        public TreeMap<String, TreeSet<String>> getKeysObsolete() {
            return keysObsolete;
        }

        public XMLStats getTotalPresent() {
            return totalPresent;
        }

        public XMLStats getTotalToDo() {
            return totalToDo;
        }

        public XMLStats getTotalObsolete() {
            return totalObsolete;
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(int percentage) {
            this.percentage = percentage;
        }
    }

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(I18nStats.class);

    private static final String LOCALE_LANG_DUTCH = "nl";
    private static final String LOCALE_LANG_POLISH = "pl";
    private static final String LOCALE_LANG_RUSSIAN = "ru";
    private static final String LOCALE_LANG_SPANISH = "es";

    private static final String LOCALE_CTRY_NL = "NL";
    private static final String LOCALE_CTRY_ES = "ES";
    private static final String LOCALE_CTRY_PL = "PL";
    private static final String LOCALE_CTRY_RU = "RU";

    private static final Locale LOCALE_SPANISH_ES =
            new Locale(LOCALE_LANG_SPANISH, LOCALE_CTRY_ES);
    private static final Locale LOCALE_POLISH_PL =
            new Locale(LOCALE_LANG_POLISH, LOCALE_CTRY_PL);
    private static final Locale LOCALE_RUSSIAN_RU =
            new Locale(LOCALE_LANG_RUSSIAN, LOCALE_CTRY_RU);
    private static final Locale LOCALE_DUTCH_NL =
            new Locale(LOCALE_LANG_DUTCH, LOCALE_CTRY_NL);

    /**
     * i18n jar file prefix.
     */
    private static final String I18N_JAR_PREFIX = "savapage-i18n-";

    /**
     * i18n JAR file extension.
     */
    private static final String I18N_JAR_EXT = "jar";

    /**
     * i18n JAR file suffix.
     */
    private static final String I18N_JAR_SUFFIX = "." + I18N_JAR_EXT;

    /**
     * i18n XML file path prefix.
     */
    private static final String I18N_XML_PATH_PREFIX = "org/savapage/";

    /**
     * i18n XML file extension.
     */
    private static final String I18N_XML_EXT = "xml";
    /**
     * i18n XML file suffix.
     */
    private static final String I18N_XML_SUFFIX = "." + I18N_XML_EXT;

    /**
     * i18n properties XML file suffix.
     */
    private static final String I18N_PROPERTIES_XML_SUFFIX = ".properties.xml";

    /**
     * Prefix of XML translation entry.
     */
    private static final String I18N_XML_ENTRY_PREFIX = "<entry";

    private static final String I18N_XML_ENTRY_KEY_PREFIX = "key=\"";

    /**
     * Key of XML translation entry for translator.
     */
    private static final String I18N_XML_ENTRY_TRANSLATOR =
            I18N_XML_ENTRY_KEY_PREFIX + "_translator";

    /** */
    private static final String FILE_SEPARATOR =
            SystemPropertyEnum.FILE_SEPARATOR.getValue();

    /**
     * i18n files that do not need a translation.
     */
    private static final Set<String> I18N_XML_FILE_EXCEPTIONS = new HashSet<>();

    /**
     * {@link Locale} map of supported i18n translations with Locale (key) and
     * XML suffix string.
     */
    private static final Map<Locale, String> I18N_SUPPORTED_MAP =
            new HashMap<>();

    /**
     * Hundred percent.
     */
    private static final int PERC_100 = 100;

    static {
        I18N_XML_FILE_EXCEPTIONS.add(AdminFeedTemplate.class.getName()
                .replace('.', File.separatorChar).concat(I18N_XML_SUFFIX));

        I18N_SUPPORTED_MAP.put(Locale.GERMANY, Locale.GERMANY.getLanguage());
        I18N_SUPPORTED_MAP.put(Locale.US, Locale.US.getLanguage());
        I18N_SUPPORTED_MAP.put(Locale.FRANCE, Locale.FRANCE.getLanguage());
        I18N_SUPPORTED_MAP.put(LOCALE_SPANISH_ES, LOCALE_LANG_SPANISH);
        I18N_SUPPORTED_MAP.put(LOCALE_POLISH_PL, LOCALE_LANG_POLISH);
        I18N_SUPPORTED_MAP.put(LOCALE_RUSSIAN_RU, LOCALE_LANG_RUSSIAN);
        I18N_SUPPORTED_MAP.put(LOCALE_DUTCH_NL, LOCALE_LANG_DUTCH);
    }

    /**
     * Utility class.
     */
    private I18nStats() {
    }

    /**
     * @return The {@link Locale} set of supported i18n translations.
     */
    public static Set<Locale> getI18nSupported() {
        return I18N_SUPPORTED_MAP.keySet();
    }

    /**
     * @return Reference locale.
     */
    private static Locale getReferenceLocale() {
        return Locale.US;
    }

    /**
     * @param locale
     *            wrapper for language.
     * @return {@code true} if locale represents reference language
     */
    private static boolean isReferenceLocale(final Locale locale) {
        // return
        // getReferenceLocale().getLanguage().equals(locale.getLanguage());
        return getReferenceLocale().equals(locale);
    }

    /**
     * @return List of {@code "savapage-i18n-*.jar"} files from
     *         {@link ServerPathEnum#LIB_WEB} and
     *         {@link ServerPathEnum#EXT_LIB}.
     */
    public static List<File> getI18nJarsFromServerPath() {

        final List<File> filesList = new ArrayList<File>();

        final String[] extensions = { I18N_JAR_EXT };

        final Collection<File> jarFiles = FileUtils.listFiles(
                ServerPathEnum.LIB_WEB.getPathAbsolute(), extensions, false);

        jarFiles.addAll(FileUtils.listFiles(
                ServerPathEnum.EXT_LIB.getPathAbsolute(), extensions, false));

        for (final File file : jarFiles) {

            if (!file.isDirectory()
                    && file.getName().startsWith(I18N_JAR_PREFIX)
                    && file.getName().endsWith(I18N_JAR_SUFFIX)) {
                filesList.add(file);
            }
        }
        return filesList;
    }

    /**
     * Collect entries from an i18n XML resource file.
     *
     * @param xmlFile
     *            Path of XML file.
     * @return Entries.
     * @throws IOException
     *             If read error.
     */
    private static Set<String> collectI18nXMLEntries(final String xmlFile)
            throws IOException {

        final Set<String> keys = new HashSet<>();

        try (InputStream is = I18nStats.class.getResourceAsStream(xmlFile);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr)) {

            String line;
            int nLine = 0;
            boolean splitEntry = false;

            while ((line = br.readLine()) != null) {

                nLine++;

                final String trimmedLine = line.trim();

                if (trimmedLine.equals(I18N_XML_ENTRY_PREFIX)) {
                    splitEntry = true;
                    continue;
                }

                final boolean processKey = splitEntry || (trimmedLine
                        .startsWith(I18N_XML_ENTRY_PREFIX)
                        && !trimmedLine.contains(I18N_XML_ENTRY_TRANSLATOR));

                if (processKey) {

                    final int iKeyProbe =
                            line.indexOf(I18N_XML_ENTRY_KEY_PREFIX, 0);

                    boolean syntaxError = true;

                    if (iKeyProbe > 0) {

                        final int iBegin =
                                iKeyProbe + I18N_XML_ENTRY_KEY_PREFIX.length();
                        final int iEnd = line.indexOf("\"", iBegin);

                        if (iEnd > 0) {
                            keys.add(line.substring(iBegin, iEnd));
                            syntaxError = false;
                        }
                    }
                    if (syntaxError) {
                        LOGGER.warn("Line {}: {}: {}\n", nLine, xmlFile, line);
                    }
                }

                splitEntry = false;
            }
        }
        return keys;
    }

    /**
     * Gets XML files from an i18n JAR file.
     *
     * @param jarFile
     *            Path of JAR file.
     * @return Map with file path (key) and {@link JarEntry} (value) of all XML
     *         files in an i18n JAR file.
     * @throws URISyntaxException
     *             If URI syntax error.
     * @throws IOException
     *             If read error.
     */
    private static Map<String, JarEntry> getI18nXmlFiles(final File jarFile)
            throws URISyntaxException, IOException {

        final URI jarURI = new URI("file:".concat(jarFile.getPath()));
        final JarFile jar = new JarFile(jarURI.getPath());

        final Enumeration<JarEntry> entries = jar.entries();
        final Map<String, JarEntry> result = new TreeMap<>();

        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(I18N_XML_PATH_PREFIX)
                    && name.endsWith(I18N_XML_SUFFIX)) {
                result.put(name, entry);
            }
        }
        return result;
    }

    /**
     * @param path
     *            resource path.
     * @return {@code true} if resource is present.
     */
    private static boolean isResourcePresent(final String path) {
        return I18nStats.class.getClassLoader().getResource(path) != null;
    }

    /**
     * @param i18nFiles
     *            XML file collection.
     * @param xmlFileI18n
     *            XML file
     * @return Number of entries in XML file.
     */
    private static Set<String> appendStats(final XMLFiles i18nFiles,
            final String xmlFileI18n) {

        try {
            final Set<String> keys =
                    collectI18nXMLEntries(FILE_SEPARATOR.concat(xmlFileI18n));

            final XMLStats stats = new XMLStats();
            stats.setEntries(keys.size());

            i18nFiles.getTotalPresent().entries += stats.getEntries();
            i18nFiles.filesPresent.put(xmlFileI18n, stats);

            return keys;

        } catch (IOException e) {
            throw new SpException(e);
        }
    }

    /**
     * @param i18nXMLReference
     * @param xmlFileEntriesMap
     * @param i18nTag
     * @return {@link XMLFiles} statistics.
     */
    private static XMLFiles getI18nStats(final XMLFiles i18nXMLReference,
            final Map<String, Set<String>> xmlFileEntriesMap,
            final String i18nTag) {

        final TreeMap<String, XMLStats> xmlRefFilesMap =
                i18nXMLReference.getFilesPresent();

        final XMLFiles i18nFiles = new XMLFiles();

        for (final String xmlFile : xmlRefFilesMap.keySet()) {

            final String xmlFileI18n;

            if (xmlFile.endsWith(I18N_PROPERTIES_XML_SUFFIX)) {
                xmlFileI18n =
                        xmlFile.replace(I18N_PROPERTIES_XML_SUFFIX, i18nTag)
                                .concat(I18N_PROPERTIES_XML_SUFFIX);
            } else {
                xmlFileI18n = xmlFile.replace(I18N_XML_SUFFIX, i18nTag)
                        .concat(I18N_XML_SUFFIX);
            }

            final XMLStats statsRef = xmlRefFilesMap.get(xmlFile);

            if (isResourcePresent(xmlFileI18n)) {

                final Set<String> keysRef = xmlFileEntriesMap.get(xmlFile);
                final Set<String> keys = appendStats(i18nFiles, xmlFileI18n);

                final int entriesToDo = statsRef.getEntries() - keys.size();

                final TreeSet<String> keysToDo = new TreeSet<>();
                final TreeSet<String> keysObsolete = new TreeSet<>();

                for (final String ref : keysRef) {
                    if (!keys.contains(ref)) {
                        keysToDo.add(ref);
                    }
                }
                for (final String key : keys) {
                    if (!keysRef.contains(key)) {
                        keysObsolete.add(key);
                    }
                }

                if (!keysToDo.isEmpty()) {
                    i18nFiles.getKeysToDo().put(xmlFileI18n, keysToDo);
                }
                if (!keysObsolete.isEmpty()) {
                    i18nFiles.getKeysObsolete().put(xmlFileI18n, keysObsolete);
                }
                if (entriesToDo != 0) {

                    final XMLStats statsDiff = new XMLStats();
                    statsDiff.setEntries(entriesToDo);
                    i18nFiles.getFilesToDo().put(xmlFileI18n, statsDiff);

                    if (entriesToDo > 0) {
                        i18nFiles.totalToDo.entries += statsDiff.getEntries();
                    } else {
                        i18nFiles.totalObsolete.entries +=
                                statsDiff.getEntries();
                    }
                }
            } else {
                if (I18N_XML_FILE_EXCEPTIONS.contains(xmlFile)) {
                    i18nFiles.totalPresent.entries += statsRef.getEntries();
                } else {
                    i18nFiles.getFilesToDo().put(xmlFileI18n, statsRef);
                    i18nFiles.totalToDo.entries += statsRef.getEntries();
                }
            }

        }

        return i18nFiles;
    }

    /**
     * Gets i18n statistics.
     *
     * @return Map with i18n locale (key) and {@link XMLFiles} statistics
     *         (value).
     */
    public static Map<Locale, XMLFiles> getI18nStats() {

        // (1) Get reference i18n.

        // Probe
        final String probe = "core/i18n/NounEnum.xml";
        final URL probeURL = I18nStats.class.getClassLoader()
                .getResource(I18N_XML_PATH_PREFIX.concat(probe));

        if (probeURL == null) {
            throw new SpException(probe + " : not found");
        }

        // Navigate to root
        Path homePath = Paths.get(probeURL.getPath());
        final int dirDepth = 3;
        for (int i = 0; i < dirDepth; i++) {
            homePath = homePath.getParent();
            if (homePath == null) {
                throw new SpException("no parent");
            }
        }

        // Get all
        final String[] extensions = { I18N_XML_EXT };
        final Collection<File> refFiles =
                FileUtils.listFiles(homePath.toFile(), extensions, true);

        // Sort
        final TreeSet<String> xmlRefFiles = new TreeSet<>();
        for (final File file : refFiles) {
            final String path = file.getPath();
            xmlRefFiles.add(path.substring(path.indexOf(I18N_XML_PATH_PREFIX)));
        }

        // (2) Collect statistics for ...
        final Map<Locale, XMLFiles> localeMap = new HashMap<>();

        // i18n reference XML files (key) and their set of i18n key entries.
        final Map<String, Set<String>> xmlRefFileEntriesMap = new HashMap<>();

        final XMLFiles valueRef = getI18nStatsReference(localeMap, xmlRefFiles,
                xmlRefFileEntriesMap);

        // ... and all others
        for (final Entry<Locale, String> entry : I18N_SUPPORTED_MAP
                .entrySet()) {

            final Locale locale = entry.getKey();

            if (isReferenceLocale(locale)) {
                continue;
            }
            localeMap.put(locale, getI18nStatsOther(entry.getValue(), valueRef,
                    xmlRefFileEntriesMap));
        }

        return localeMap;
    }

    /**
     * Gets statistics for i18n JAR files.
     *
     * @param jarFiles
     *            List of i18n jar files.
     * @return Map with i18n locale (key) and {@link XMLFiles} statistics
     *         (value).
     * @throws IOException
     *             Read error.
     * @throws URISyntaxException
     *             URI error.
     */
    public static Map<Locale, XMLFiles> getI18nStats(final List<File> jarFiles)
            throws URISyntaxException, IOException {

        // (1) Get reference i18n.

        File jarFileRef = null;
        for (final File jarFile : jarFiles) {
            final Entry<Locale, String> entry = getLocaleEntryOfJar(jarFile);
            if (isReferenceLocale(entry.getKey())) {
                jarFileRef = jarFile;
            }
        }
        if (jarFileRef == null) {
            throw new SpException("i18n reference .jar not found.");
        }

        // Get all
        final Map<String, JarEntry> map = getI18nXmlFiles(jarFileRef);

        // Select/Sort
        final TreeSet<String> xmlRefFiles = new TreeSet<>();

        for (final Entry<String, JarEntry> entry : map.entrySet()) {
            final String path = entry.getValue().getName();
            xmlRefFiles.add(path.substring(path.indexOf(I18N_XML_PATH_PREFIX)));
        }

        // (2) Collect statistics for ...
        final Map<Locale, XMLFiles> localeMap = new HashMap<>();

        // i18n reference XML files (key) and their set of i18n key entries.
        final Map<String, Set<String>> xmlRefFileEntriesMap = new HashMap<>();

        final XMLFiles valueRef = getI18nStatsReference(localeMap, xmlRefFiles,
                xmlRefFileEntriesMap);

        // ... and all others
        for (final File jarFile : jarFiles) {

            if (jarFile.equals(jarFileRef)) {
                continue;
            }

            final Entry<Locale, String> entry = getLocaleEntryOfJar(jarFile);

            localeMap.put(entry.getKey(), getI18nStatsOther(entry.getValue(),
                    valueRef, xmlRefFileEntriesMap));
        }
        return localeMap;
    }

    /**
     * Creates the reference {@link XMLFiles} object from reference i18n XML
     * files, puts the reference on the locale Map and fills the Map of XML
     * reference File i18n key entries.
     *
     * @param localeMap
     *            Map of i18n translations (key) and their {@link XMLFiles}
     *            (value).
     * @param xmlRefFiles
     *            The reference i18n XML files.
     * @param xmlRefFileEntriesMap
     *            Map of i18n XML reference files (key) and their set of i18n
     *            key entries (value).
     * @return The reference {@link XMLFiles} object.
     */
    private static XMLFiles getI18nStatsReference(
            final Map<Locale, XMLFiles> localeMap,
            final TreeSet<String> xmlRefFiles,
            final Map<String, Set<String>> xmlRefFileEntriesMap) {

        final XMLFiles valueRef = new XMLFiles();

        for (final String xmlFile : xmlRefFiles) {
            xmlRefFileEntriesMap.put(xmlFile, appendStats(valueRef, xmlFile));
        }

        localeMap.put(getReferenceLocale(), valueRef);

        valueRef.setPercentage(PERC_100);

        return valueRef;
    }

    /**
     * Creates another {@link XMLFiles} object using reference i18n information.
     *
     * @param xmlID
     *            Unique i18n identifier of another i18n XML file.
     * @param valueRef
     *            Read-only reference {@link XMLFiles} object.
     * @param xmlRefFileEntriesMap
     *            Read-only Map of i18n XML reference files (key) and their set
     *            of i18n key entries (value).
     * @return Another {@link XMLFiles} object.
     */
    private static XMLFiles getI18nStatsOther(final String xmlID,
            final XMLFiles valueRef,
            final Map<String, Set<String>> xmlRefFileEntriesMap) {

        final XMLFiles value =
                getI18nStats(valueRef, xmlRefFileEntriesMap, "_".concat(xmlID));

        value.setPercentage(PERC_100 * value.getTotalPresent().getEntries()
                / valueRef.getTotalPresent().getEntries());

        return value;
    }

    /**
     * Gets the locale for an i18n JAR file.
     *
     * @param jar
     *            JAR file.
     * @return Entry from {@link #I18N_SUPPORTED_MAP}.
     */
    private static Entry<Locale, String> getLocaleEntryOfJar(final File jar) {

        for (final Entry<Locale, String> entry : I18N_SUPPORTED_MAP
                .entrySet()) {
            final String xmlID = entry.getValue();
            if (jar.getName().contains(I18N_JAR_PREFIX.concat(xmlID))) {
                return entry;
            }
        }
        LOGGER.error("{} : unknown locale.", jar.getPath());
        throw new SpException(jar.getName().concat(" : unknown locale"));
    }

    /**
     *
     * @param map
     *            Unsorted map.
     * @return Sorted by percentage (descending) + display language (ascending)
     */
    private static Map<Locale, Integer>
            getI18nPercentagesSorted(final Map<Locale, Integer> map) {

        final Map<String, Locale> mapSortedTmp = new TreeMap<>();

        for (final Entry<Locale, Integer> entry : map.entrySet()) {
            final Locale locale = entry.getKey();
            final String key = String.format("%03d%s",
                    PERC_100 - entry.getValue().intValue(),
                    locale.getDisplayLanguage(locale).toUpperCase());
            mapSortedTmp.put(key, entry.getKey());
        }

        final Map<Locale, Integer> mapSorted = new LinkedHashMap<>();

        for (final Entry<String, Locale> entry : mapSortedTmp.entrySet()) {
            final Locale key = entry.getValue();
            mapSorted.put(key, map.get(key));
        }

        return mapSorted;
    }

    /**
     * @return Percentage translated (value) for each supported language, sorted
     *         by percentage (descending) + display language (ascending).
     */
    public static Map<Locale, Integer> getI18nPercentages() {

        final Map<Locale, Integer> percMap = new HashMap<>();

        for (final Entry<Locale, I18nStats.XMLFiles> entryLang : I18nStats
                .getI18nStats().entrySet()) {

            percMap.put(entryLang.getKey(),
                    entryLang.getValue().getPercentage());
        }
        return getI18nPercentagesSorted(percMap);
    }

    /**
     * @param jars
     *            List of i18n JAR files.
     * @return Percentage translated (value) for each supported language, sorted
     *         by percentage (descending) + display language (ascending)
     */
    public static Map<Locale, Integer>
            getI18nPercentagesFromJars(final List<File> jars) {

        final Map<Locale, Integer> percMap = new HashMap<>();

        try {
            for (final Entry<Locale, I18nStats.XMLFiles> entryLang : I18nStats
                    .getI18nStats(jars).entrySet()) {

                percMap.put(entryLang.getKey(),
                        entryLang.getValue().getPercentage());
            }
        } catch (URISyntaxException | IOException e) {
            throw new SpException(e);
        }
        return getI18nPercentagesSorted(percMap);
    }

    /**
     * @return Percentage translated (value) for each supported language from
     *         server jar files.
     */
    public static Map<Locale, Integer> getI18nPercentagesFromJars() {
        return getI18nPercentagesFromJars(getI18nJarsFromServerPath());
    }

    /**
     * Percentage translated (value) for each supported language.
     */
    private static final Map<Locale, Integer> PERC_MAP_CACHE =
            new LinkedHashMap<>();

    /**
     * @param scanJarFiles
     *            If {@code true}, scan i18n .jar files from server home.
     * @return Percentage translated (value) for each supported language from
     *         cache.
     */
    public static Map<Locale, Integer>
            getI18nPercentagesCached(final boolean scanJarFiles) {

        synchronized (PERC_MAP_CACHE) {
            if (PERC_MAP_CACHE.isEmpty()) {
                final Map<Locale, Integer> perc;
                if (scanJarFiles) {
                    perc = getI18nPercentagesFromJars();
                } else {
                    perc = getI18nPercentages();
                }
                PERC_MAP_CACHE.putAll(perc);
            }
            return PERC_MAP_CACHE;
        }
    }
}
