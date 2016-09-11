/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.cli.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.savapage.core.SpException;
import org.savapage.core.cli.AbstractApp;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.JsonRpcConfig;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethod;
import org.savapage.core.json.rpc.JsonRpcResponseParser;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.util.InetUtils;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Command Line Interface (CLI) for Public JSON-RPC API.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractAppApi extends AbstractApp {

    /**
     * Is the App running in batch mode?
     */
    private boolean isBatchMode = false;

    /**
     *
     */
    private Properties serverProps = null;

    /**
     * The command line parameters.
     */
    private CommandLine commandLine;

    /**
     *
     */
    protected static final String CLI_BATCH_SWITCH = "batch";

    /**
     *
     */
    protected static final String CLI_BATCH_SWITCH_CONTINUE = "continue";

    /**
     * The batch option for charset. See:
     * <a href="http://www.iana.org/assignments/character-sets/">www.iana.org
     * </a>
     */
    protected static final String CLI_BATCH_OPTION_CHARSET = "charset";

    /**
    *
    */
    protected static final String CLI_BATCH_OPTION_INPUT = "input";

    /**
     *
     */
    protected static final String CLI_OPTION_LOCALE = "locale";

    /**
     *
     */
    private Options cliOptions;

    /**
     * Option names in a list, used to ordinal sort the options in usage text.
     */
    private final List<String> optionNames = new ArrayList<>();;

    /**
    *
    */
    protected static final String ARG_TEXT = "text";

    /**
     *
     */
    protected static final String ARG_DECIMAL = "decimal";

    /**
    *
    */
    protected static final String ARG_NUMBER = "number";

    /**
     *
     */
    protected static final String ARG_LIST = "list";

    /**
     *
     */
    protected static final String ARG_CARD_FORMAT = "HEX|DEC";
    /**
     *
     */
    protected static final String ARG_CARD_FIRST_BYTE = "LSB|MSB";

    /**
     *
     */
    protected static final String CLI_SWITCH_PFX_KEEP = "keep-";
    /**
     *
     */
    protected static final String CLI_SWITCH_PFX_REMOVE = "remove-";

    /**
     *
     */
    protected AbstractAppApi() {
        try {
            serverProps = ConfigManager.loadServerProperties();
        } catch (Exception e) {
            throw new SpException(e);
        }
    }

    /**
     * Gets the runtime {@link CommandLine}.
     *
     * @return The {@link CommandLine}.
     */
    protected final CommandLine getCommandLine() {
        return this.commandLine;
    }

    /**
     * Gets the JSON-RPC method name.
     *
     * @return the JSON-RPC method name.
     */
    protected abstract String getMethodName();

    /**
     *
     * @return The short description of the CLI method.
     */
    protected abstract String getShortDecription();

    /**
     *
     * @return The long description of the CLI method.
     */
    protected String getLongDecription() {
        return null;
    }

    /**
     *
     * @return
     */
    protected abstract Object[][] getOptionDictionary();

    /**
     *
     * @param cmd
     * @return
     */
    protected abstract AbstractJsonRpcMethodParms
            createMethodParms(CommandLine cmd);

    /**
     *
     * @param error
     */
    protected abstract void onErrorResponse(JsonRpcError error);

    /**
     * Notifies a {@link JsonRpcResult}. When {@code true} is returned another
     * method request is issued. I.e. {@link #createMethodParms(CommandLine)} is
     * called again: this mechanism can be used for list paging.
     *
     * @param result
     *            {@code true} if another method (paging) request is needed,
     *            {@code false} if this is the last method request.
     * @return
     */
    protected abstract boolean onResultResponse(JsonRpcResult result);

    /**
     *
     * @param cmd
     * @return
     */
    protected abstract boolean isValidCliInput(CommandLine cmd);

    /**
     * Checks if a batch options are applicable.
     *
     * @return {@code true} is batch options are applicable.
     */
    protected abstract boolean hasBatchOptions();

    /**
     * Checks if a Locale option is applicable.
     *
     * @return {@code true} is Locale option is applicable.
     */
    protected abstract boolean hasLocaleOption();

    /**
     * Checks if a locale option present on the {@link CommandLine} is valid.
     *
     * @param cmd
     *            The {@link CommandLine}.
     * @return {@code true} is locale is absent or present and valid.
     */
    private boolean isValidLocale(final CommandLine cmd) {
        boolean isValid = true;
        if (hasLocaleOption() && cmd.hasOption(CLI_OPTION_LOCALE)) {
            isValid = isValidLocaleLanguageTag(
                    cmd.getOptionValue(CLI_OPTION_LOCALE));
        }
        return isValid;
    }

    /**
     *
     * @return
     */
    protected final Options getCliOptions() {

        if (cliOptions == null) {
            try {
                cliOptions = createCliOptions();
            } catch (Exception e) {
                throw new SpException(e.getMessage(), e);
            }
        }
        return cliOptions;
    }

    /**
     *
     * @return
     */
    protected List<String> getOptionNames() {
        return optionNames;
    }

    /**
     * Add option names to {@link Option}.
     *
     * @param options
     *            The {@link Option}.
     * @param optionNames
     *            The names.
     */
    protected static final void addHelpOptions(final Options options,
            final List<String> optionNames) {
        optionNames.add(CLI_SWITCH_HELP_LONG);
        options.addOption(CLI_SWITCH_HELP, CLI_SWITCH_HELP_LONG, false,
                "Displays this help text.");
    }

    /**
     *
     * @param options
     * @param optionNames
     */
    protected static final void addBatchOptions(final Options options,
            final List<String> optionNames) {

        String opt;

        //
        opt = CLI_BATCH_SWITCH;
        optionNames.add(opt);
        options.addOption(opt, null, false,
                "Enables batch mode: executing from CSV or TSV input.");

        //
        opt = CLI_BATCH_SWITCH_CONTINUE;
        optionNames.add(opt);
        options.addOption(opt, null, false,
                "Continues batch processing after a batch line execution error.");

        //
        opt = CLI_BATCH_OPTION_INPUT;
        optionNames.add(opt);
        options.addOption(opt, null, true,
                "Batch input file: " + "optional with stdin as default.");
        //
        opt = CLI_BATCH_OPTION_CHARSET;
        optionNames.add(opt);
        options.addOption(opt, null, true, "IANA Charset Name of batch input "
                + "character encoding [default: utf-8].");
    }

    /**
     *
     * @return
     */
    protected final String getHostLocale() {
        return ConfigManager.getServerHostLocale().toLanguageTag();
    }

    /**
     *
     * @param languageTag
     * @return
     */
    protected final boolean isValidLocaleLanguageTag(final String languageTag) {
        /*
         * Since we want an exact match we round-trip to check validity.
         */
        return Locale.forLanguageTag(languageTag).toLanguageTag()
                .equals(languageTag);
    }

    /**
     *
     * @param options
     * @param optionNames
     */
    protected final void addLocaleOption(final Options options,
            final List<String> optionNames) {
        String opt;
        opt = CLI_OPTION_LOCALE;
        optionNames.add(opt);
        options.addOption(opt, null, true,
                "The IETF BCP 47 Locale used for numeric values."
                        + " Example values are:"
                        + " en, en-GB, en-US, nl, nl-NL, nl-BE."
                        + " [defaults to system default " + getHostLocale()
                        + "].");
    }

    /**
     *
     * @param cmd
     */
    protected final String getLocaleOption(final CommandLine cmd) {
        String locale = cmd.getOptionValue(CLI_OPTION_LOCALE);
        if (locale == null) {
            locale = getHostLocale();
        }
        return locale;
    }

    /**
     *
     * @return
     */
    public static String getApiDescriptHeader() {
        final String eol = System.lineSeparator();
        final String name = CommunityDictEnum.SAVAPAGE.getWord()
                + " Command Line Interface";
        return StringUtils.repeat("_", name.length() + 1) + eol + name + eol
                + eol;
    }

    /**
     *
     * @param camelCase
     * @return
     */
    protected static String camelCaseToTrainCase(final String camelCase) {
        return camelCase.replaceAll("(.)(\\p{Upper})", "$1-$2").toLowerCase();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    protected int getServerSslPort() throws IOException {
        return Integer.valueOf(ConfigManager.getServerSslPort(serverProps));
    }

    /**
     *
     * @return
     * @throws IOException
     */
    protected final URL getSecureServerUrl() throws IOException {
        /*
         * We need the server hostname in the URL, since it must match the name
         * in the (self-signed) SSL certificate.
         */
        return new URL("https://" + InetUtils.getServerHostName() + ":"
                + ConfigManager.getServerSslPort(serverProps) + "/jsonrpc");
    }

    /**
     *
     * @param api
     * @param cmd
     * @return
     * @throws Exception
     */
    private int handleBatchRequest(final String[] args) throws Exception {

        final CommandLineParser parser = new DefaultParser();
        final Options options = new Options();
        final List<String> optionNameList = new ArrayList<>();

        addHelpOptions(options, optionNameList);
        addBatchOptions(options, optionNameList);

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            getDisplayStream().println(e.getMessage());
            return EXIT_CODE_PARMS_PARSE_ERROR;
        }

        if (cmd.hasOption(CLI_SWITCH_HELP)
                || cmd.hasOption(CLI_SWITCH_HELP_LONG)) {
            showUsage();
            return EXIT_CODE_OK;
        }

        /*
         *
         */
        final String fileName =
                cmd.getOptionValue(AbstractAppApi.CLI_BATCH_OPTION_INPUT);

        File file = null;
        if (fileName != null) {
            file = new File(fileName);
        }

        /*
         *
         */
        final String charset = cmd.getOptionValue(
                AbstractAppApi.CLI_BATCH_OPTION_CHARSET, "utf-8");

        /*
         *
         */
        Reader reader = null;
        char separator = ',';

        if (file == null) {
            reader = new InputStreamReader(System.in);
        } else {
            if (FilenameUtils.getExtension(file.getName().toLowerCase())
                    .equals("tsv")) {
                separator = '\t';
            }
            reader = new InputStreamReader(new FileInputStream(file), charset);
        }

        return runApiBatch(reader, separator,
                cmd.hasOption(CLI_BATCH_SWITCH_CONTINUE));
    }

    /**
     * Convenience wrapper.
     *
     * @param args
     * @return
     * @throws Exception
     */
    final protected int runApi(final String[] args) throws Exception {

        isBatchMode = false;

        if (hasBatchOptions()) {
            for (String arg : args) {
                if (arg.length() > 0
                        && arg.substring(1).equals(CLI_BATCH_SWITCH)) {
                    isBatchMode = true;
                    break;
                }
            }
        }

        if (isBatchMode) {
            return handleBatchRequest(args);
        } else {
            return run(args);
        }

    }

    /**
     * Gets the command line method name <i>without</i> the {@code --} prefix.
     *
     * @return
     */
    protected String getCliMethodName() {
        return camelCaseToTrainCase(getMethodName());
    }

    /**
     * Tells whether a specific option represents a switch.
     *
     * @param optionName
     *            The option name which is specific for the implementing class,
     *            i.e. common switches (like "h" or "help") are not offered as
     *            parameter.
     * @return {@code true} when this is a switch.
     */
    protected abstract boolean isSwitchOption(final String optionName);

    /**
     * Tells whether a common option (like "h" or "help") represents a switch.
     *
     * @param optionName
     *            The option name which is common to all implementing classes.
     * @return {@code true} when this is a switch.
     */
    protected final boolean isCommonSwitchOption(final String optionName) {
        return CLI_SWITCH_HELP.equals(optionName)
                || CLI_SWITCH_HELP_LONG.equals(optionName);
    }

    /**
     *
     * <p>
     * See
     * <a href="http://opencsv.sourceforge.net/">http://opencsv.sourceforge.net
     * /</a>
     * </p>
     *
     * @param reader
     *            The reader with the
     * @param separator
     *            The field separator: {@code '\t'} for TSV file, and
     *            {@code ','} for CSV file.
     * @param continueOnError
     *            If {@code true}, batch processing continues after a batch line
     *            execution error.
     * @return
     * @throws Exception
     */
    protected final int runApiBatch(final Reader reader, final char separator,
            final boolean continueOnError) throws Exception {

        int exitCode = EXIT_CODE_OK;

        int nErrors = 0;

        CSVReader csvReader = null;

        try {
            csvReader = new CSVReader(reader, separator);

            int nLine = 0;

            /*
             * Read the header with the options.
             */
            String[] optionsRaw = csvReader.readNext();
            if (optionsRaw == null) {
                return EXIT_CODE_PARMS_PARSE_ERROR;
            }
            nLine++;

            // Correct common formatting artifacts.
            String[] options = new String[optionsRaw.length];

            for (int i = 0; i < optionsRaw.length; i++) {
                options[i] = optionsRaw[i].toLowerCase().trim();
            }

            /*
             * Read the data rows.
             */
            final List<String> args = new ArrayList<>();

            // Initial row.
            String[] fields = csvReader.readNext();

            while (fields != null
                    && (continueOnError || exitCode == EXIT_CODE_OK)) {

                nLine++;

                args.clear();

                String command = "--" + getCliMethodName();

                for (int i = 0; i < fields.length; i++) {

                    final String optionName = options[i];
                    final String cliOption = "--" + optionName;

                    final String rawValue = fields[i].trim();

                    if (StringUtils.isNotBlank(rawValue)) {

                        if (isSwitchOption(optionName)) {

                            args.add(cliOption);

                            command += " " + cliOption;

                        } else {

                            final String value =
                                    String.format("\"%s\"", rawValue);
                            args.add(cliOption);
                            args.add(value);

                            command += " " + cliOption + " " + value;
                        }
                    }
                }

                exitCode = run(args.toArray(new String[args.size()]));

                if (exitCode != EXIT_CODE_OK) {

                    nErrors++;

                    getErrorDisplayStream().println(
                            "Error on line [" + nLine + "] : " + command);
                    getErrorDisplayStream().flush();
                }

                // Next row.
                if (continueOnError || exitCode == EXIT_CODE_OK) {
                    fields = csvReader.readNext();
                }

            }
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }

        if (continueOnError && nErrors > 0) {
            getErrorDisplayStream().println("Total errors [" + nErrors + "]");
            exitCode = EXIT_CODE_ERROR_AFTER_BATCH_CONTINUE;
        }

        return exitCode;
    }

    /**
     * Sends the JSON-RPC request.
     * <p>
     * Note: The ApiKey is hard-coded. Its value is the
     * {@link JsonRpcConfig#API_INTERNAL_ID} encrypted with the SavaPage private
     * key.
     * </p>
     *
     * @param request
     * @return
     * @throws Exception
     */
    protected final String send(final JsonRpcMethod request) throws Exception {

        request.getParams()
                .setApiKey("302c02145da35d18d85dcc724aabcb237b63092802e7ec"
                        + "cb0214622a0d0eb5658c15edeea6d75f4ece349110490f");

        final String jsonIn = request.stringifyPrettyPrinted();

        /*
         * Trust self-signed SSL certificates (this is the default SSL after
         * installation).
         */
        SSLContextBuilder builder = new SSLContextBuilder();

        builder.loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE);

        /*
         * The NoopHostnameVerifier instance is used to turn hostname
         * verification off. As a result SSLPeerUnverifiedException: Host name
         * 'XXX' does not match the certificate subject provided by the peer
         * (CN=YYY).
         */
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                builder.build(), NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient =
                HttpClients.custom().setSSLSocketFactory(sslsf).build();

        /*
         *
         */
        final HttpPost httpPost = new HttpPost(getSecureServerUrl().toURI());

        /*
         * Our own signature :-)
         */
        httpPost.setHeader(HttpHeaders.USER_AGENT,
                ConfigManager.getAppNameVersion());

        /*
         *
         */
        HttpEntity entity = null;
        ContentType contentType =
                ContentType.create(JsonRpcConfig.INTERNET_MEDIA_TYPE);

        entity = new StringEntity(jsonIn, contentType);

        httpPost.setEntity(entity);

        /*
         *
         */
        final CloseableHttpResponse response = httpClient.execute(httpPost);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {

            bos = new ByteArrayOutputStream();
            response.getEntity().writeTo(bos);

        } finally {
            /*
             * Mantis #487: release the connection.
             */
            httpPost.reset();
            //
            response.close();
            httpClient.close();
        }

        return bos.toString();
    }

    @Override
    protected final Options createCliOptions() throws Exception {

        final Options options = new Options();

        final List<String> optionNameList = getOptionNames();

        /*
         *
         */
        for (Object[] option : getOptionDictionary()) {

            optionNameList.add(option[1].toString());

            Boolean required = false;
            if (option.length > 3) {
                required = (Boolean) option[3];
            }
            String descript;

            if (required) {
                descript = "[required] ";
            } else {
                descript = "[optional] ";
            }
            descript += option[2].toString();

            /*
             * IMPORTANT: do NOT set required, since this will throw a parse
             * exception, even if we just want --help
             */
            final boolean hasArg = (option[0] != null);

            final Option.Builder optionBuilder = Option.builder();

            optionBuilder.hasArg(hasArg);

            if (hasArg) {
                optionBuilder.argName(option[0].toString());
            }

            optionBuilder.longOpt(option[1].toString());
            optionBuilder.desc(descript);

            options.addOption(optionBuilder.build());
        }

        addHelpOptions(options, optionNameList);

        if (hasBatchOptions()) {
            addBatchOptions(options, optionNameList);
        }

        if (hasLocaleOption()) {
            addLocaleOption(options, optionNameList);
        }

        return options;
    }

    /**
     *
     */
    private void showUsage() {
        usage(getUsageCmdLineSyntax(), getUsageDescript(), getCliOptions(),
                getOptionNames());
    }

    /**
     * @return The usage string.
     */
    private String getUsageCmdLineSyntax() {
        return "--" + getCliMethodName() + " [OPTION]...";
    }

    /**
     *
     * @return The API version.
     */
    protected abstract String getApiVersion();

    /**
     * @return The full usage text.
     */
    private String getUsageDescript() {

        final String eol = System.lineSeparator();

        final StringBuilder txt = new StringBuilder()
                .append(getApiDescriptHeader()).append("Method  : ")
                .append(getMethodName()).append(eol).append("Version : ")
                .append(getApiVersion()).append(eol).append(eol)
                .append(getShortDecription());

        if (getLongDecription() != null) {
            txt.append(eol).append(eol).append(getLongDecription());
        }

        return txt.toString();
    }

    @Override
    protected final int run(final String[] args) throws Exception {

        // ......................................................
        // Parse parameters from CLI
        // ......................................................
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        int exitCode = EXIT_CODE_OK;

        try {
            cmd = parser.parse(getCliOptions(), args);

        } catch (org.apache.commons.cli.ParseException e) {
            getDisplayStream().println(e.getMessage());
            exitCode = EXIT_CODE_PARMS_PARSE_ERROR;
        }

        if (exitCode == EXIT_CODE_OK && (cmd.hasOption(CLI_SWITCH_HELP)
                || cmd.hasOption(CLI_SWITCH_HELP_LONG))) {
            showUsage();
            return EXIT_CODE_OK;
        }

        if (exitCode == EXIT_CODE_OK && args == null) {
            exitCode = EXIT_CODE_MISSING_PARMS;
        }

        /*
         * Check input: all required options should be present.
         */
        if (exitCode == EXIT_CODE_OK
                && (!isValidCliInput(cmd) || !isValidLocale(cmd))) {
            exitCode = EXIT_CODE_MISSING_PARMS;
        }

        /*
         *
         */
        if (exitCode != EXIT_CODE_OK) {
            if (!this.isBatchMode) {
                showUsage();
            }
            return exitCode;
        }

        /*
         *
         */
        this.commandLine = cmd;

        JsonRpcMethod request = new JsonRpcMethod();
        request.setMethod(getMethodName());

        boolean performMethod = true;

        /*
         * NOTE: We need a while loop for paging (chunks).
         */
        while (performMethod && exitCode == EXIT_CODE_OK) {

            request.setId(String.valueOf(System.currentTimeMillis()));
            request.setParams(createMethodParms(cmd));

            final String jsonOut = send(request);

            JsonRpcResponseParser rpcResponseParser =
                    new JsonRpcResponseParser(jsonOut);

            if (rpcResponseParser.isErrorResponse()) {

                exitCode = EXIT_CODE_ERROR;

                onErrorResponse(rpcResponseParser.getError());

            } else if (rpcResponseParser.isResultResponse()) {

                performMethod = onResultResponse(rpcResponseParser.getResult());

            } else {
                throw new SpException("No valid response received.");
            }
        }
        return exitCode;
    }

    /**
     * Gets the value of an option switch.
     *
     * @param cmd
     *            The {@link CommandLine}.
     * @param optionSwitch
     *            The name of the option switch.
     * @return {@code true} when the option switch is found or {@code false}
     *         when not.
     */
    protected final boolean getSwitchValue(final CommandLine cmd,
            final String optionSwitch) {
        return getSwitchValue(cmd, optionSwitch, true);
    }

    /**
     * Gets the value of an option switch.
     *
     * @param cmd
     *            The {@link CommandLine}.
     * @param optionSwitch
     *            The name of the option switch.
     * @param valueOn
     *            The return value when the option switch is ON.
     * @return valueOn when the option switch is found or !valueOn when not.
     */
    protected final boolean getSwitchValue(final CommandLine cmd,
            final String optionSwitch, final boolean valueOn) {

        if (cmd.hasOption(optionSwitch)) {
            return valueOn;
        }
        return !valueOn;
    }

}
