/*
 * LunaClient - A best client on world.
 *  Copyright (C) 2024 Team PaichaLover
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.lunaclient.lunaclientmod.commands

import cc.polyfrost.oneconfig.gui.OneConfigGui
import cc.polyfrost.oneconfig.utils.commands.annotations.Command
import cc.polyfrost.oneconfig.utils.commands.annotations.Main
import cc.polyfrost.oneconfig.utils.gui.GuiUtils
import net.lunaclient.lunaclientmod.core.FPSMod

@Command("lc", aliases = ["lunaclient"])
class LunaClientCommand {

    @Main
    fun execCommand() {
        GuiUtils.displayScreen(OneConfigGui())
    }
}