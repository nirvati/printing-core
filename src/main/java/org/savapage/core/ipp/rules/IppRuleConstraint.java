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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A constraint rule of IPP options that cannot coexist.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRuleConstraint implements IppRuleChecker {

    /**
     * The name of the constraint.
     */
    private final String name;

    /**
     * IPP attribute/value pairs that cannot coexist.
     */
    private List<Pair<String, String>> ippContraints;

    /**
     * The set of IPP rule attribute keys with the negated value.
     */
    private Set<String> ippNegateSet;

    /**
     * @param rule
     *            The name of the rule.
     */
    public IppRuleConstraint(final String rule) {
        this.name = rule;
    }

    /**
     * @return The identifying name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return IPP attribute/value pairs that cannot coexist.
     */
    public List<Pair<String, String>> getIppContraints() {
        return ippContraints;
    }

    /**
     * @param contraints
     *            IPP attribute/value pairs that cannot coexist.
     */
    public void setIppContraints(final List<Pair<String, String>> contraints) {
        this.ippContraints = contraints;
    }

    /**
     * @return The set of IPP rule attribute keys with the negated value.
     */
    public Set<String> getIppNegateSet() {
        return ippNegateSet;
    }

    /**
     * @param negateSet
     *            The set of IPP rule attribute keys with the negated value.
     */
    public void setIppNegateSet(final Set<String> negateSet) {
        this.ippNegateSet = negateSet;
    }

    @Override
    public boolean doesRuleApply(final Map<String, String> ippOptionValues) {
        return IppRuleGeneric.doesRuleApply(ippOptionValues, this.ippContraints,
                this.ippNegateSet);
    }

}
