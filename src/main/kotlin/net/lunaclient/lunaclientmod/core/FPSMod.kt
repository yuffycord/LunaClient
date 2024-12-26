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

package net.lunaclient.lunaclientmod.core

import cc.polyfrost.oneconfig.config.Config
import cc.polyfrost.oneconfig.config.annotations.*
import cc.polyfrost.oneconfig.config.data.Mod
import cc.polyfrost.oneconfig.config.data.ModType
import cc.polyfrost.oneconfig.events.EventManager
import cc.polyfrost.oneconfig.hud.SingleTextHud
import net.minecraft.client.Minecraft


object FPSMod : Config(
    Mod(
        "FPS",
        ModType.HUD,
        "/assets/lunaclientmod/lunaclient_vector.svg",
        ),
    "lc-fps.json"
) {


    @HUD(
        name = "FPS",
        subcategory = "HUD"
    )
    var hud = FPSHud()

    init {
        initialize()
    }

    class FPSHud : SingleTextHud("FPS", true) {
        
        init {
            EventManager.INSTANCE.register(this)
        }

        override fun getText(example: Boolean): String {
           return Minecraft.getDebugFPS().toString()
        }
    }

}
