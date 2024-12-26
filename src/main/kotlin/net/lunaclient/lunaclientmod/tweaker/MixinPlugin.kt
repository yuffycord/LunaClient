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

package net.lunaclient.lunaclientmod.tweaker

import cc.polyfrost.oneconfig.utils.Notifications
import net.minecraftforge.fml.relauncher.CoreModManager
import org.spongepowered.asm.lib.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

class MixinPlugin : IMixinConfigPlugin {

    private var hasPlayerAPI = false
    override fun onLoad(mixinPackage: String) {
        for ((key, value) in CoreModManager.getTransformers()) {
            if (key.startsWith("PlayerAPIPlugin") && value.contains("api.player.forge.PlayerAPITransformer")) {
                println("PlayerAPI detected.")
                Notifications.INSTANCE.send("PolySprint", "PlayerAPI has been detected.\nAlthough supported, it is not recommended or needed for any recent mod.\nIt is recommended you delete it.")
                hasPlayerAPI = true
                break
            }
        }
    }

    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean {
        return if (mixinClassName.startsWith("org.polyfrost.polysprint.mixins.playerapi.")) {
            hasPlayerAPI
        } else true
    }

    override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) {}
    override fun getMixins(): List<String>? {
        return null
    }

    override fun preApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

    override fun postApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

}
