/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.visual

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category

object NoBob : Module("NoBob", Category.VISUAL, gameDetecting = false, hideModule = false) {

    @EventTarget
    fun onMotion(event: MotionEvent) {
        mc.thePlayer?.distanceWalkedModified = -1f
    }
}