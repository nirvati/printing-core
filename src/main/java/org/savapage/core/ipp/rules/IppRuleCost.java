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
package org.savapage.core.ipp.rules;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A rule to calculate cost according to the presence (or negation) of IPP
 * attribute choices.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRuleCost {

    /**
     * The name of the rule.
     */
    private final String name;

    /**
     * The map of IPP attribute (key) choices (value) that make up this rule.
     */
    private final Map<String, String> ippRuleChoices = new HashMap<>();

    /**
     * The set of IPP attribute keys with a value that must not be chosen.
     */
    private final Set<String> ippRuleAttrNegate = new HashSet<>();

    /**
     * The cost when all conditions of this rule are satisfied.
     */
    private final BigDecimal cost;

    /**
     *
     * @param rule
     *            The name of the rule.
     * @param amount
     *            The cost amount when rule applies.
     */
    public IppRuleCost(final String rule, final BigDecimal amount) {
        this.name = rule;
        this.cost = amount;
    }

    /**
     *
     * @param ippAttr
     *            The IPP attribute (key).
     * @param ippChoice
     *            The IPP choice.
     * @param mustBeChosen
     *            {@code true} when the rule requires this attribute/choice to
     *            be chosen, {@code false} when another choice of the same
     *            attribute must be chosen.
     */
    public void addRuleChoice(final String ippAttr, final String ippChoice,
            final boolean mustBeChosen) {
        this.ippRuleChoices.put(ippAttr, ippChoice);
        if (!mustBeChosen) {
            this.ippRuleAttrNegate.add(ippAttr);
        }
    }

    /**
     * Checks if this rule applies to an option, and is valid in the full
     * context of IPP choices.
     *
     * @param option
     *            The IPP key/value option pair to check.
     * @param ippChoices
     *            The full context of IPP choices.
     * @return {@code null} when the rule does not apply to the option.
     *         {@link Boolean#TRUE} when rules applies and is valid.
     */
    public Boolean isOptionValid(final Pair<String, String> option,
            final Map<String, String> ippChoices) {

        /*
         * Traverse the IPP options of the rule.
         */
        for (final Entry<String, String> entry : ippRuleChoices.entrySet()) {

            /*
             * Check if option is part of the rule.
             */
            if (entry.getKey().equals(option.getKey())
                    && entry.getValue().equals(option.getValue())) {
                /*
                 * Check if the rule applies.
                 */
                return calcCost(ippChoices) != null;
            }
        }
        return null;
    }

    /**
     * Calculates the cost for a collection of IPP attribute/choices according
     * to this rule. When the rule does not apply, {@code null} is returned.
     *
     * @param ippChoices
     *            A map of IPP attribute (key) choices (value).
     *
     * @return {@code null} if the rule does not apply.
     */
    public BigDecimal calcCost(final Map<String, String> ippChoices) {

        /*
         * Traverse the IPP options of the rule, and check if each option value
         * equals the options offered.
         */
        for (final Entry<String, String> entry : ippRuleChoices.entrySet()) {

            final String ippRuleAttr = entry.getKey();
            final String choiceOffered = ippChoices.get(ippRuleAttr);

            if (choiceOffered != null) {

                final String ippRuleChoice = entry.getValue();
                final boolean isChosen = ippRuleChoice.equals(choiceOffered);

                final boolean mustBeChosen =
                        !ippRuleAttrNegate.contains(ippRuleAttr);

                if ((isChosen && mustBeChosen)
                        || (!isChosen && !mustBeChosen)) {
                    continue;
                }
            }
            /*
             * The rule options is not offered, or the offered choice does not
             * match the rule.
             */
            return null;
        }
        return this.cost;
    }

    /**
     *
     * @return The identifying name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return UI text for logging.
     */
    public String uiText() {

        final StringBuilder txt = new StringBuilder();

        txt.append(getName()).append(" : ").append(this.cost);

        for (final Entry<String, String> entry : this.ippRuleChoices
                .entrySet()) {
            txt.append(" ").append(entry.getKey()).append("/");

            if (this.ippRuleAttrNegate.contains(entry.getKey())) {
                txt.append("!");
            }
            txt.append(entry.getValue());
        }
        return txt.toString();
    }
}
