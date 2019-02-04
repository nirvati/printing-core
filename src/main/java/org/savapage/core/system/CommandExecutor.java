/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.system;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CommandExecutor {

    /**
     *
     */
    private CommandExecutor() {
    }

    /**
     *
     * @param commandInformation
     *            Command.
     * @return {@link ICommandExecutor}.
     */
    private static ICommandExecutor
            createSimple(final List<String> commandInformation) {
        return new SimpleCommandExecutor(commandInformation);
    }

    /**
     *
     * @param commandInformation
     *            Command.
     * @return {@link ICommandExecutor}.
     */
    private static ICommandExecutor
            create(final List<String> commandInformation) {
        return new SystemCommandExecutor(commandInformation);
    }

    /**
     *
     * @param commandInformation
     *            Command.
     * @param stdIn
     *            Input for {@code stdin}.
     * @return {@link ICommandExecutor}.
     */
    private static ICommandExecutor
            create(final List<String> commandInformation, final String stdIn) {
        return new SystemCommandExecutor(commandInformation, stdIn);
    }

    /**
     *
     * @param command
     *            Command.
     * @return List of commands.
     */
    private static List<String> createCommandInfo(final String command) {
        List<String> commandInfo = new ArrayList<String>();
        commandInfo.add("/bin/sh");
        commandInfo.add("-c");
        commandInfo.add(command);
        return commandInfo;
    }

    /**
     *
     * @param command
     *            Command.
     * @return {@link ICommandExecutor}.
     */
    public static ICommandExecutor createSimple(final String command) {
        return createSimple(createCommandInfo(command));
    }

    /**
     *
     * @param command
     *            Command.
     * @return {@link ICommandExecutor}.
     */
    public static ICommandExecutor create(final String command) {
        return create(createCommandInfo(command));
    }

    /**
     * Creates a command with stdin input.
     *
     * @param command
     *            Command.
     * @param stdIn
     *            Input for {@code stdin}.
     * @return {@link ICommandExecutor}.
     */
    public static ICommandExecutor create(final String command,
            final String stdIn) {
        return create(createCommandInfo(command), stdIn);
    }

}
