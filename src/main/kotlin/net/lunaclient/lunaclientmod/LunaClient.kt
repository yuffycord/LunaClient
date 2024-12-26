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
package net.lunaclient.lunaclientmod

import cc.polyfrost.oneconfig.libs.universal.UMinecraft
import cc.polyfrost.oneconfig.utils.commands.CommandManager
import net.lunaclient.lunaclientmod.commands.LunaClientCommand
import net.lunaclient.lunaclientmod.core.*
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.opengl.Display
import javax.imageio.ImageIO


@Mod(
    modid = PolySprint.MODID,
    name = PolySprint.MOD_NAME,
    version = PolySprint.VERSION,
    clientSideOnly = true,
    modLanguageAdapter = "cc.polyfrost.oneconfig.utils.KotlinLanguageAdapter"
)
object PolySprint {

    const val MODID = "@ID@"
    const val MOD_NAME = "@NAME@"
    const val VERSION = "@VER@"
    val player
        get() = UMinecraft.getPlayer()
    val gameSettings
        get() = UMinecraft.getSettings()

    var sprintHeld = false
    var sneakHeld = false

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        FPSMod
        PingMod
        MinecraftForge.EVENT_BUS.register(this)
    }

    @Mod.EventHandler
    fun onPreInit(event: FMLInitializationEvent) {
        Display.setTitle("LunaClient 1.8.9")
        Display.setIcon(
            IconLoader.load(
                ImageIO.read(
                        PolySprint::class.java.getResourceAsStream(
                            "/assets/lunaclientmod/lunaclient_icon.png"
                        )
                )
            ))
    }

    @Mod.EventHandler
    fun onPostInit(event: FMLPostInitializationEvent) {
        // Open OneConfig
        CommandManager.INSTANCE.registerCommand(LunaClientCommand())
    }

    @SubscribeEvent
    fun onInput(event: InputEvent) {
        if (!SprintMod.enabled) return
        val sprint = gameSettings.keyBindSprint.keyCode
        val sneak = gameSettings.keyBindSneak.keyCode
        if (!SprintMod.keybindToggleSprint && checkKeyCode(sprint)) {
            if (SprintMod.enabled && SprintMod.toggleSprint && !sprintHeld) {
                SprintMod.toggleSprintState = !SprintMod.toggleSprintState
                SprintMod.save()
            }
            sprintHeld = true
        } else {
            sprintHeld = false
        }
        if (!SprintMod.keybindToggleSneak && checkKeyCode(sneak)) {
            if (SprintMod.enabled && SprintMod.toggleSneak && !sneakHeld) {
                SprintMod.toggleSneakState = !SprintMod.toggleSneakState
                SprintMod.save()
            }
            sneakHeld = true
        } else {
            sneakHeld = false
        }
    }

}