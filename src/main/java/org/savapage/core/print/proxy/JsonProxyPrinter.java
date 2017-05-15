/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.print.proxy;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savapage.core.dto.IppCostRule;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.jpa.Printer;
import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Printer returned from the IPP operation.
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonProxyPrinter extends JsonAbstractBase {

    @JsonIgnore
    private URI deviceUri;

    @JsonIgnore
    private URI printerUri;

    /**
     * If this is a CUPS printer class, the number of printer members. If
     * {@code 0} (zero) this is <i>not</i> a printer class.
     */
    @JsonIgnore
    private int cupsClassMembers;

    /**
     * Corresponding Database Printer Object.
     */
    @JsonIgnore
    private Printer dbPrinter;

    /**
     * Custom cost rules for a printed media side.
     */
    @JsonIgnore
    private List<IppCostRule> customCostRulesMedia;

    /**
     * Custom cost rules for a printed copy.
     */
    @JsonIgnore
    private List<IppCostRule> customCostRulesCopy;

    /**
     *
     */
    public enum State {
        IDLE, BUSY, STOPPED
    };

    private String name;

    @JsonProperty("pcfilename")
    private String ppd;

    @JsonProperty("FileVersion")
    private String ppdVersion;

    private Boolean dfault;

    @JsonProperty("is-accepting-jobs")
    private Boolean acceptingJobs;

    private String state;

    @JsonProperty("state-change-time")
    private String stateChangeTime;

    @JsonProperty("state-reasons")
    private String stateReasons;

    private String location;
    private String info;

    @JsonProperty("color_device")
    private Boolean colorDevice;

    @JsonProperty("duplex_device")
    private Boolean duplexDevice;

    @JsonProperty("manualMediaSource")
    private Boolean manualMediaSource;

    @JsonProperty("autoMediaSource")
    private Boolean autoMediaSource;

    /**
     * {@code true} when printer supports both
     * {@link IppKeyword#SHEET_COLLATE_COLLATED} and
     * {@link IppKeyword#SHEET_COLLATE_UNCOLLATED}.
     */
    @JsonProperty("sheetCollate")
    private Boolean sheetCollate;

    @JsonProperty("modelname")
    private String modelName;

    private String manufacturer;

    private ArrayList<JsonProxyPrinterOptGroup> groups;

    /**
     * {@code true} when info was injected from a SavaPage PPD extension.
     */
    @JsonIgnore
    private boolean injectPpdExt = false;

    /**
     * {@code true} when print-scaling was injected from a SavaPage PPD
     * extension.
     */
    @JsonIgnore
    private boolean printScalingExt = false;

    /**
     * {@code true} when this is a job ticket printer.
     */
    @JsonIgnore
    private Boolean jobTicket = Boolean.FALSE;

    /**
     * Gets the corresponding Database Printer Object.
     *
     * @return The db printer object.
     */
    @JsonIgnore
    public final Printer getDbPrinter() {
        return dbPrinter;
    }

    /**
     * Sets the corresponding Database Printer Object.
     *
     * @param printer
     *            The db printer object.
     */
    @JsonIgnore
    public final void setDbPrinter(final Printer printer) {
        this.dbPrinter = printer;
    }

    /**
     *
     * @return Custom cost rules for a printed media side.
     */
    @JsonIgnore
    public List<IppCostRule> getCustomCostRulesMedia() {
        return customCostRulesMedia;
    }

    /**
     *
     * @param rules
     *            Custom cost rules for a printed media side.
     */
    @JsonIgnore
    public void setCustomCostRulesMedia(final List<IppCostRule> rules) {
        this.customCostRulesMedia = rules;
    }

    /**
     *
     * @return Custom cost rules for a printed copy.
     */
    @JsonIgnore
    public List<IppCostRule> getCustomCostRulesCopy() {
        return customCostRulesCopy;
    }

    /**
     *
     * @param rules
     *            Custom cost rules for a printed copy.
     */
    @JsonIgnore
    public void setCustomCostRulesCopy(final List<IppCostRule> rules) {
        this.customCostRulesCopy = rules;
    }

    /**
     *
     * @return The {@link State}.
     */
    public State state() {
        if (state.equals("3")) {
            return State.IDLE;
        } else if (state.equals("4")) {
            return State.BUSY;
        } else if (state.equals("5")) {
            return State.STOPPED;
        }
        return State.STOPPED;
    }

    /**
     *
     * @return The UPPERCASE name of the printer.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the printer.
     * <p>
     * IMPORTANT: the name MUST be UPPERCASE.
     * </p>
     *
     * @param name
     *            The UPPERCASE name.
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getPpd() {
        return ppd;
    }

    public void setPpd(String ppd) {
        this.ppd = ppd;
    }

    public String getPpdVersion() {
        return ppdVersion;
    }

    public void setPpdVersion(String ppdVersion) {
        this.ppdVersion = ppdVersion;
    }

    public Boolean getDfault() {
        return dfault;
    }

    public void setDfault(Boolean dfault) {
        this.dfault = dfault;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStateChangeTime() {
        return stateChangeTime;
    }

    public void setStateChangeTime(String stateChangeTime) {
        this.stateChangeTime = stateChangeTime;
    }

    public String getStateReasons() {
        return stateReasons;
    }

    public void setStateReasons(String stateReasons) {
        this.stateReasons = stateReasons;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Boolean getColorDevice() {
        return colorDevice;
    }

    public void setColorDevice(Boolean colorDevice) {
        this.colorDevice = colorDevice;
    }

    public Boolean getDuplexDevice() {
        return duplexDevice;
    }

    public void setDuplexDevice(Boolean duplexDevice) {
        this.duplexDevice = duplexDevice;
    }

    public Boolean getManualMediaSource() {
        return this.manualMediaSource;
    }

    public void setManualMediaSource(Boolean manualMediaSource) {
        this.manualMediaSource = manualMediaSource;
    }

    public Boolean getAutoMediaSource() {
        return autoMediaSource;
    }

    public void setAutoMediaSource(Boolean autoMediaSource) {
        this.autoMediaSource = autoMediaSource;
    }

    /**
     * @return {@code true} when printer supports both
     *         {@link IppKeyword#SHEET_COLLATE_COLLATED} and
     *         {@link IppKeyword#SHEET_COLLATE_UNCOLLATED}.
     */
    public Boolean getSheetCollate() {
        return sheetCollate;
    }

    /**
     * @param sheetCollate
     *            {@code true} when printer supports both
     *            {@link IppKeyword#SHEET_COLLATE_COLLATED} and
     *            {@link IppKeyword#SHEET_COLLATE_UNCOLLATED}.
     */
    public void setSheetCollate(Boolean sheetCollate) {
        this.sheetCollate = sheetCollate;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Boolean getAcceptingJobs() {
        return acceptingJobs;
    }

    public void setAcceptingJobs(Boolean acceptingJobs) {
        this.acceptingJobs = acceptingJobs;
    }

    public ArrayList<JsonProxyPrinterOptGroup> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<JsonProxyPrinterOptGroup> groups) {
        this.groups = groups;
    }

    /**
     * Flattens the printer options from groups and subgroups into one (1)
     * lookup.
     *
     * @return The lookup {@link Map} with
     *         {@link JsonProxyPrinterOpt#getKeyword()} as key.
     */
    public Map<String, JsonProxyPrinterOpt> getOptionsLookup() {

        final Map<String, JsonProxyPrinterOpt> lookup = new HashMap<>();

        for (final JsonProxyPrinterOptGroup group : groups) {

            for (final JsonProxyPrinterOpt option : group.getOptions()) {
                lookup.put(option.getKeyword(), option);
            }

            for (final JsonProxyPrinterOptGroup subgroup : group
                    .getSubgroups()) {

                for (final JsonProxyPrinterOpt option : subgroup.getOptions()) {
                    lookup.put(option.getKeyword(), option);
                }
            }
        }
        return lookup;
    }

    /**
     * Check if this printer has the same signature as printer in parameter. The
     * signature is the combination of name (ignore case), PPD file and PPD
     * version.
     *
     * @param printer
     *            The printer to compare with.
     * @return {@code true} if printers have the same signature.
     */
    public final boolean hasSameSignature(final JsonProxyPrinter printer) {

        return (hasSameName(printer)
                && printer.getPpd().equals(printer.getPpd())
                && printer.getPpdVersion().equals(printer.getPpdVersion()));

    }

    /**
     * Check if this printer has the same name (ignore case) as printer in
     * parameter.
     *
     * @param printer
     *            The printer to compare with.
     * @return {@code true} if printers have the same name.
     */
    public final boolean hasSameName(final JsonProxyPrinter printer) {
        return (printer.getName().equalsIgnoreCase(printer.getName()));
    }

    public URI getDeviceUri() {
        return deviceUri;
    }

    public void setDeviceUri(URI deviceUri) {
        this.deviceUri = deviceUri;
    }

    @JsonIgnore
    public URI getPrinterUri() {
        return printerUri;
    }

    @JsonIgnore
    public void setPrinterUri(final URI printerUri) {
        this.printerUri = printerUri;
    }

    /**
     * @return If this is a CUPS printer class, the number of printer members.
     *         If {@code 0} (zero) this is <i>not</i> a printer class.
     */
    @JsonIgnore
    public int getCupsClassMembers() {
        return cupsClassMembers;
    }

    /**
     * @param nMembers
     *            If this is a CUPS printer class, the number of printer
     *            members. If {@code 0} (zero) this is <i>not</i> a printer
     *            class.
     */
    @JsonIgnore
    public void setCupsClassMembers(final int nMembers) {
        this.cupsClassMembers = nMembers;
    }

    /**
     * @return {@code true} when info was injected from a SavaPage PPD
     *         extension.
     */
    public boolean isInjectPpdExt() {
        return injectPpdExt;
    }

    /**
     * @param injectPpdExt
     *            {@code true} when info was injected from a SavaPage PPD
     *            extension.
     */
    public void setInjectPpdExt(boolean injectPpdExt) {
        this.injectPpdExt = injectPpdExt;
    }

    /**
     * @return {@code true} when page-scaling was injected from a SavaPage PPD
     *         extension.
     */
    public boolean isPrintScalingExt() {
        return printScalingExt;
    }

    /**
     * @param printScalingExt
     *            {@code true} when page-scaling was injected from a SavaPage
     *            PPD extension.
     */
    public void setPrintScalingExt(boolean printScalingExt) {
        this.printScalingExt = printScalingExt;
    }

    /**
     * @return {@code true} when this is a job ticket printer.
     */
    public Boolean getJobTicket() {
        return jobTicket;
    }

    /**
     * @param jobTicket
     *            {@code true} when this is a job ticket printer.
     */
    public void setJobTicket(Boolean jobTicket) {
        this.jobTicket = jobTicket;
    }

    /**
     * Creates a deep copy instance.
     *
     * @return The new copy.
     */
    @JsonIgnore
    public JsonProxyPrinter copy() {

        final JsonProxyPrinter copy = new JsonProxyPrinter();

        copy.acceptingJobs = this.acceptingJobs;
        copy.colorDevice = this.colorDevice;
        copy.dbPrinter = this.dbPrinter;
        copy.dfault = this.dfault;
        copy.duplexDevice = this.duplexDevice;
        copy.info = this.info;
        copy.location = this.location;
        copy.manualMediaSource = this.manualMediaSource;
        copy.autoMediaSource = this.autoMediaSource;
        copy.sheetCollate = this.sheetCollate;
        copy.manufacturer = this.manufacturer;
        copy.modelName = this.modelName;
        copy.name = this.name;
        copy.ppd = this.ppd;
        copy.injectPpdExt = this.injectPpdExt;
        copy.jobTicket = this.jobTicket;
        copy.ppdVersion = this.ppdVersion;
        copy.printerUri = this.printerUri;
        copy.state = this.state;
        copy.stateChangeTime = this.stateChangeTime;
        copy.stateReasons = this.stateReasons;
        copy.customCostRulesCopy = this.customCostRulesCopy;
        copy.customCostRulesMedia = this.customCostRulesMedia;

        copy.groups = new ArrayList<>();
        copy.groups.addAll(this.getGroups());

        return copy;
    }

    /**
     * Calculates Media Cost of IPP choices according to the list of cost rules.
     *
     * @param ippChoices
     *            The IPP attribute key/choices.
     * @return {@code null} when none of the rules apply.
     */
    public BigDecimal
            calcCustomCostMedia(final Map<String, String> ippChoices) {
        return calcCost(this.getCustomCostRulesMedia(), ippChoices, false);
    }

    /**
     * Calculates Copy Cost of IPP choices according to the list of cost rules.
     *
     * @param ippChoices
     *            The IPP attribute key/choices.
     * @return {@code null} when none of the rules apply.
     */
    public BigDecimal calcCustomCostCopy(final Map<String, String> ippChoices) {
        return calcCost(this.getCustomCostRulesCopy(), ippChoices, true);
    }

    /**
     * Gets the cover costs according to the list of cost rules.
     *
     * @param ippCoverChoice
     *            The IPP choice of attribute
     *            {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_COVER_TYPE}.
     * @return {@code null} when none of the rules apply.
     */
    public BigDecimal getCustomCostCover(final String ippCoverChoice) {

        if (ippCoverChoice == null) {
            return null;
        }

        final Map<String, String> singleIppChoice = new HashMap<>();

        singleIppChoice.put(IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_COVER_TYPE,
                ippCoverChoice);

        return calcCost(this.getCustomCostRulesCopy(), singleIppChoice, false);
    }

    /**
     * Calculates cost of IPP choices according to a list of cost rules.
     *
     * @param rules
     *            The list of cost rules.
     * @param ippChoices
     *            The IPP attribute key/choices.
     * @param accumulate
     *            If {@code true} all rules are checked, and the cost result of
     *            each rule is accumulated. When {@code false} the cost of the
     *            first rule with a non-null result is returned.
     * @return {@code null} when none of the rules apply.
     */
    private static BigDecimal calcCost(final List<IppCostRule> rules,
            final Map<String, String> ippChoices, final boolean accumulate) {

        if (rules == null) {
            return null;
        }

        BigDecimal total = null;

        for (final IppCostRule rule : rules) {

            final BigDecimal cost = rule.calcCost(ippChoices);

            if (cost != null) {
                if (accumulate) {
                    if (total == null) {
                        total = cost;
                    } else {
                        total = total.add(cost);
                    }
                } else {
                    return cost;
                }
            }
        }
        return total;
    }

    /**
     * @return {@code true} when custom media cost rules are present.
     */
    public boolean hasCustomCostRulesMedia() {
        final List<IppCostRule> rules = this.getCustomCostRulesMedia();
        return rules != null && !rules.isEmpty();
    }

    /**
     * @return {@code true} when custom copy cost rules are present.
     */
    public boolean hasCustomCostRulesCopy() {
        final List<IppCostRule> rules = this.getCustomCostRulesCopy();
        return rules != null && !rules.isEmpty();
    }

}
