/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.ipp.rules;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A rule to add extra PPD option values.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRuleExtra implements IppRuleChecker {

    /**
     * The name of the rule.
     */
    private final String name;

    /**
     * Main IPP attribute.
     */
    private Pair<String, String> mainIpp;

    /**
     * Dependent IPP attributes.
     */
    private List<Pair<String, String>> dependentIpp;

    /**
     * Extra PPD option (key) values.
     */
    private List<Pair<String, String>> extraPPD;

    /**
     *
     * @param rule
     *            The name of the rule.
     */
    public IppRuleExtra(final String rule) {
        this.name = rule;
    }

    /**
     *
     * @return The identifying name.
     */
    public String getName() {
        return name;
    }

    public Pair<String, String> getMainIpp() {
        return mainIpp;
    }

    public void setMainIpp(Pair<String, String> mainIpp) {
        this.mainIpp = mainIpp;
    }

    public List<Pair<String, String>> getDependentIpp() {
        return dependentIpp;
    }

    public void setDependentIpp(List<Pair<String, String>> dependentIpp) {
        this.dependentIpp = dependentIpp;
    }

    public List<Pair<String, String>> getExtraPPD() {
        return extraPPD;
    }

    public void setExtraPPD(List<Pair<String, String>> extraPPD) {
        this.extraPPD = extraPPD;
    }

    @Override
    public boolean doesRuleApply(final Map<String, String> ippOptionValues) {

        final String mainIppValue =
                ippOptionValues.get(this.getMainIpp().getKey());

        if (mainIppValue == null
                || !mainIppValue.equals(this.getMainIpp().getValue())) {
            return false;
        }

        boolean ruleApply = true;

        for (final Pair<String, String> pair : this.getDependentIpp()) {

            final String ippValue = ippOptionValues.get(pair.getKey());

            if (ippValue != null && ippValue.equals(pair.getValue())) {
                continue;
            }
            ruleApply = false;
            break;
        }
        return ruleApply;
    }

}
