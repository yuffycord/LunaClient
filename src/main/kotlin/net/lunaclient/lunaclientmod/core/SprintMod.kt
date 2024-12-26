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
import cc.polyfrost.oneconfig.config.core.OneKeyBind
import cc.polyfrost.oneconfig.config.data.Mod
import cc.polyfrost.oneconfig.config.data.ModType
import cc.polyfrost.oneconfig.config.migration.VigilanceMigrator
import cc.polyfrost.oneconfig.events.EventManager
import cc.polyfrost.oneconfig.events.event.Stage
import cc.polyfrost.oneconfig.events.event.TickEvent
import cc.polyfrost.oneconfig.hud.TextHud
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe
import cc.polyfrost.oneconfig.libs.universal.UKeyboard
import net.minecraft.entity.player.EntityPlayer
import net.lunaclient.lunaclientmod.PolySprint
import net.lunaclient.lunaclientmod.core.SprintMod.ToggleSprintHud.DisplayState.Companion.activeDisplay
import java.io.File
import java.math.RoundingMode


object SprintMod : Config(
    Mod(
        "Sprint",
        ModType.PVP,
        "/assets/lunaclientmod/others/sprint.svg",
        VigilanceMigrator(File("./config/simpletogglesprint.toml").absolutePath),
        ),
    "lc-sprint.json"
) {

    @Switch(
        name = "Toggle Sprint"
    )
    var toggleSprint = true

    @Switch(
        name = "Toggle Sneak"
    )
    var toggleSneak = false

    @Switch(
        name = "Disable W-Tap Sprint"
    )
    var disableWTapSprint = true

    @JvmField
    var toggleSprintState = false

    @JvmField
    var toggleSneakState = false

    @Switch(
        name = "Seperate Keybind for Toggle Sprint",
        subcategory = "Toggle Sprint",
        description = "Use a seperate keybind for Toggle Sprint."
    )
    var keybindToggleSprint = false

    @KeyBind(
        name = "Toggle Sprint Keybind",
        subcategory = "Toggle Sprint"
    )
    var keybindToggleSprintKey = OneKeyBind(UKeyboard.KEY_NONE)

    @Switch(
        name = "Seperate Keybind for Toggle Sneak",
        subcategory = "Toggle Sneak",
        description = "Use a seperate keybind for Toggle Sneak."
    )
    var keybindToggleSneak = false

    @KeyBind(
        name = "Toggle Sneak Keybind",
        subcategory = "Toggle Sneak"
    )
    var keybindToggleSneakKey = OneKeyBind(UKeyboard.KEY_NONE)

    @Switch(
        name = "Fly Boost",
        subcategory = "Fly Boost"
    )
    var toggleFlyBoost = false

    @Slider(
        name = "Fly Boost Amount",
        subcategory = "Fly Boost",
        min = 1.0F,
        max = 10.0F
    )
    var flyBoostAmount = 4.0F

    @HUD(
        name = "HUD",
        subcategory = "HUD"
    )
    var hud = ToggleSprintHud()

    init {
        initialize()
        addDependency("keybindToggleSprint", "toggleSprint")
        addDependency("keybindToggleSneak", "toggleSneak")
        addDependency("flyBoostAmount", "toggleFlyBoost")
        addDependency("keybindToggleSprintKey", "keybindToggleSprint")
        addDependency("keybindToggleSneakKey", "keybindToggleSneak")

        registerKeyBind(keybindToggleSprintKey) {
            if (keybindToggleSprint) {
                if (enabled && toggleSprint && !PolySprint.sprintHeld) {
                    toggleSprintState = !toggleSprintState
                    SprintMod.save()
                }
                PolySprint.sprintHeld = !PolySprint.sprintHeld
            }
        }
        registerKeyBind(keybindToggleSneakKey) {
            if (keybindToggleSneak) {
                if (enabled && toggleSneak && !PolySprint.sneakHeld) {
                    toggleSneakState = !toggleSneakState
                    SprintMod.save()
                }
                PolySprint.sneakHeld = !PolySprint.sneakHeld
            }
        }
    }

    class ToggleSprintHud : TextHud(true, 40, (1080 + 6.5).toInt()) {

        @Switch(name = "Brackets")
        private var brackets = false

        @Button(
            name = "Reset Text on ALL HUDs",
            text = "Reset"
        )
        var resetText = Runnable {
            descendingHeld = ""
            descendingToggled = ""
            descending = ""
            flying = "no u"
            flyBoostText = "no u"
            riding = ""
            sneakHeld = ""
            sneakToggle = ""
            sneak = ""
            sprintHeld = "ありがとう"
            sprintToggle = "ありがとう"
            sprint = "ありがとう"
        }

        @Text(
            name = "Descending Held Text",
            category = "Display",
            subcategory = "Text"
        )
        var descendingHeld = ""

        @Text(
            name = "Descending Toggled Text",
            category = "Display",
            subcategory = "Text"
        )
        var descendingToggled = ""

        @Text(
            name = "Descending Text",
            category = "Display",
            subcategory = "Text"
        )
        var descending = ""

        @Text(
            name = "Flying Text",
            category = "Display",
            subcategory = "Text"
        )
        var flying = "no u"

        @Exclude var flyBoost = ""

        @Text(
            name = "Fly Boost Text",
            category = "Display",
            subcategory = "Text"
        )
        var flyBoostText = "no u"

        @Text(
            name = "Riding Text",
            category = "Display",
            subcategory = "Text"
        )
        var riding = ""

        @Text(
            name = "Sneak Held Text",
            category = "Display",
            subcategory = "Text"
        )
        var sneakHeld = ""

        @Text(
            name = "Sneak Toggle Text",
            category = "Display",
            subcategory = "Text"
        )
        var sneakToggle = ""

        @Text(
            name = "Sneaking Text",
            category = "Display",
            subcategory = "Text"
        )
        var sneak = ""

        @Text(
            name = "Sprint Held Text",
            category = "Display",
            subcategory = "Text"
        )
        var sprintHeld = "ありがとう"

        @Text(
            name = "Sprint Toggle Text",
            category = "Display",
            subcategory = "Text"
        )
        var sprintToggle = "ありがとう"

        @Text(
            name = "Sprinting Text",
            category = "Display",
            subcategory = "Text"
        )
        var sprint = "ありがとう"
        
        init {
            EventManager.INSTANCE.register(this)
        }

        @Subscribe
        fun onTick(e: TickEvent) {
            if (e.stage == Stage.START) {
                flyBoost = if (shouldFlyBoost()) {
                    "$flying (${flyBoostAmount.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)}$flyBoostText)"
                } else {
                    flying
                }
            }
        }

        override fun getLines(lines: MutableList<String>, example: Boolean) {
            getCompleteText(activeDisplay)?.let { lines.add(it) }
        }

        private fun getCompleteText(text: String?) = if (brackets && text?.isNotEmpty() == true) "[$text]" else text

        private enum class DisplayState(val displayText: ToggleSprintHud.() -> String, val displayCheck: (EntityPlayer) -> Boolean) {
            DESCENDINGHELD({ descendingHeld }, { it.capabilities.isFlying && it.isSneaking && PolySprint.sneakHeld }),
            DESCENDINGTOGGLED({ descendingToggled }, { it.capabilities.isFlying && SprintMod.enabled && toggleSprint && toggleSneakState }),
            DESCENDING({ descending }, { it.capabilities.isFlying && it.isSneaking }),
            FLYING({ flying }, { it.capabilities.isFlying && !shouldFlyBoost() }),
            FLYBOOST({ flyBoost }, { it.capabilities.isFlying && shouldFlyBoost() }),
            RIDING({ riding }, { it.isRiding }),
            SNEAKHELD({ sneakHeld }, { it.isSneaking && PolySprint.sneakHeld }),
            TOGGLESNEAK({ sneakToggle }, { SprintMod.enabled && toggleSneak && toggleSneakState }),
            SNEAKING({ sneak }, { it.isSneaking }),
            SPRINTHELD({ sprintHeld }, { it.isSprinting && PolySprint.sprintHeld }),
            TOGGLESPRINT({ sprintToggle }, { SprintMod.enabled && toggleSprint && toggleSprintState }),
            SPRINTING({ sprint }, { it.isSprinting });

            val isActive: Boolean
                get() = displayCheck(PolySprint.player!!)

            companion object {
                private val items by lazy {
                    return@lazy if (KotlinVersion.CURRENT.isAtLeast(1, 9)) entries else values().toList()
                }

                
                val ToggleSprintHud.activeDisplay: String?
                    get() {
                        if (PolySprint.player == null) return null
                        return items.find { it.isActive }?.displayText?.invoke(this)
                    }
            }
        }
    }

}
