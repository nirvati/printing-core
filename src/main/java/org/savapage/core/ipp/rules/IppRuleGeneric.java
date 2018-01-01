/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A generic IPP rule.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class IppRuleGeneric implements IppRuleChecker {

    /**
     * The name of the rule.
     */
    private final String name;

    /**
     * Independent main IPP attribute.
     */
    private Pair<String, String> mainIpp;

    /**
     * Extra independent IPP attributes.
     */
    private List<Pair<String, String>> extraIpp;

    /**
     * The set of extra independent IPP attribute keys with a value that must
     * not be chosen.
     */
    private Set<String> extraIppNegate = new HashSet<>();

    /**
     * @param rule
     *            The name of the rule.
     */
    public IppRuleGeneric(final String rule) {
        this.name = rule;
    }

    /**
     * @return The identifying name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Independent main IPP attribute.
     */
    public final Pair<String, String> getMainIpp() {
        return mainIpp;
    }

    /**
     * @param mainIppPair
     *            Independent main IPP attribute.
     */
    public final void setMainIpp(final Pair<String, String> mainIppPair) {
        this.mainIpp = mainIppPair;
    }

    /**
     * @return Extra independent IPP attributes.
     */
    public final List<Pair<String, String>> getExtraIpp() {
        return extraIpp;
    }

    /**
     * @param list
     *            Extra independent IPP attributes.
     */
    public final void setExtraIpp(final List<Pair<String, String>> list) {
        this.extraIpp = list;
    }

    /**
     * @return The set of IPP rule attribute keys with the negated value.
     */
    public final Set<String> getExtraIppNegate() {
        return extraIppNegate;
    }

    /**
     * @param ippNegate
     *            The set of IPP rule attribute keys with the negated value.
     */
    public final void setExtraIppNegate(final Set<String> ippNegate) {
        this.extraIppNegate = ippNegate;
    }

    @Override
    public final boolean
            doesRuleApply(final Map<String, String> ippOptionValues) {

        final String mainIppValue =
                ippOptionValues.get(this.getMainIpp().getKey());

        if (mainIppValue == null
                || !mainIppValue.equals(this.getMainIpp().getValue())) {
            return false;
        }
        return doesRuleApply(ippOptionValues, this.getExtraIpp(),
                this.getExtraIppNegate());
    }

    /**
     * Checks if a rule applies to a map of IPP options.
     *
     * @param ippOptionValues
     *            The IPP options.
     * @param ippPairRules
     *            List of rules as IPP option name/value pairs.
     * @param ippNegateSet
     *            The set of IPP rule attribute keys with the negated value.
     * @return {@code true} if rule applies.
     */
    public static boolean doesRuleApply(
            final Map<String, String> ippOptionValues,
            final List<Pair<String, String>> ippPairRules,
            final Set<String> ippNegateSet) {

        boolean ruleApply = true;

        for (final Pair<String, String> pair : ippPairRules) {

            final String ippRuleAttr = pair.getKey();
            final String ippValue = ippOptionValues.get(ippRuleAttr);

            final boolean isChosen =
                    ippValue != null && ippValue.equals(pair.getValue());
            final boolean mustBeChosen = !ippNegateSet.contains(ippRuleAttr);

            if ((isChosen && mustBeChosen) || (!isChosen && !mustBeChosen)) {
                continue;
            }

            ruleApply = false;
            break;
        }

        return ruleApply;
    }

}
