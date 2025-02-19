/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.canClickInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.hasScheduledInLastLoop
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.float
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object InvMove : Module("InvMove", Category.MOVEMENT, gameDetecting = false, hideModule = false) {

    private val fullMovements by boolean("FullMovements", false)
    private val notInChests by boolean("NotInChests", false)
    private val noDetectableValue by boolean("NoDetectable", false)
    private val noMoveClicksValue by boolean("NoMoveClicks", false)

    private val rotate by boolean("Rotate", false)

    val aacAdditionPro by boolean("AACAdditionPro", false)
    private val intave by boolean("Intave", false)

    private val isIntave = (mc.currentScreen is GuiInventory || mc.currentScreen is GuiChest) && intave

    private val noMove by InventoryManager.noMoveValue
    private val noMoveAir by InventoryManager.noMoveAirValue
    private val noMoveGround by InventoryManager.noMoveGroundValue
    private val undetectable by InventoryManager.undetectableValue

    // If player violates nomove check and inventory is open, close inventory and reopen it when still
    private val silentlyCloseAndReopen by boolean("SilentlyCloseAndReopen", false)
    { noMove && (noMoveAir || noMoveGround) }

    // Reopen closed inventory just before a click (could flag for clicking too fast after opening inventory)
    private val reopenOnClick by boolean("ReopenOnClick", false)
    { silentlyCloseAndReopen && noMove && (noMoveAir || noMoveGround) }

    private val inventoryMotion by float("InventoryMotion", 1F, 0F..2F)

    private val affectedBindings = arrayOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindJump,
        mc.gameSettings.keyBindSprint
    )

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val screen = mc.currentScreen

        // Don't make player move when chat or ESC menu are open
        if (screen is GuiChat || screen is GuiIngameMenu)
            return@handler

        if (undetectable && (screen != null && screen !is GuiHudDesigner && screen !is ClickGui))
            return@handler

        if (notInChests && screen is GuiChest)
            return@handler

        if (screen is GuiInventory || screen is GuiChest) {
            player.motionX *= inventoryMotion
            player.motionZ *= inventoryMotion
        }

        if (!fullMovements && (screen is GuiChat || screen is GuiIngameMenu)) return@handler

        if (silentlyCloseAndReopen && screen is GuiInventory) {
            if (canClickInventory(closeWhenViolating = true) && !reopenOnClick)
                serverOpenInventory = true
        }

        for (affectedBinding in affectedBindings)
            affectedBinding.pressed = isButtonPressed(affectedBinding)
                    || (affectedBinding == mc.gameSettings.keyBindSprint && handleEvents() && Sprint.mode == "Legit" && (!Sprint.onlyOnSprintPress || mc.thePlayer.isSprinting))
    }

    private fun updateKeyState() {
        if (mc.currentScreen != null && mc.currentScreen !is GuiChat &&
            (!noDetectableValue || mc.currentScreen !is GuiContainer)
        ) {
            MovementUtils.updateControls()

            if (rotate) {
                when {
                    Keyboard.isKeyDown(Keyboard.KEY_UP) && mc.thePlayer.rotationPitch > -90 -> {
                        mc.thePlayer.rotationPitch -= 5
                    }
                    Keyboard.isKeyDown(Keyboard.KEY_DOWN) && mc.thePlayer.rotationPitch < 90 -> {
                        mc.thePlayer.rotationPitch += 5
                    }
                    Keyboard.isKeyDown(Keyboard.KEY_LEFT) -> {
                        mc.thePlayer.rotationYaw -= 5
                    }
                    Keyboard.isKeyDown(Keyboard.KEY_RIGHT) -> {
                        mc.thePlayer.rotationYaw += 5
                    }
                }
            }
        }
    }

    val onStrafe = handler<StrafeEvent> { event ->
        if (isIntave) {
            mc.gameSettings.keyBindSneak.pressed = true
        }
    }

    val onJump = handler<JumpEvent> { event ->
        if (isIntave) event.cancelEvent()
    }

    val onClick = handler<ClickWindowEvent> { event ->
        if (!canClickInventory()) event.cancelEvent()
        else if (reopenOnClick) {
            hasScheduledInLastLoop = false
            serverOpenInventory = true
        }

        if (noMoveClicksValue && mc.thePlayer.isMoving) {
            event.cancelEvent()
        }
    }

    val onMotion = handler<MotionEvent> { event ->
        updateKeyState()
    }

    val onScreen = handler<ScreenEvent> { event ->
        updateKeyState()
    }

    override fun onDisable() {
        for (affectedBinding in affectedBindings)
            affectedBinding.pressed = isButtonPressed(affectedBinding)
    }

    private fun isButtonPressed(keyBinding: KeyBinding): Boolean {
        return if (keyBinding.keyCode < 0) {
            Mouse.isButtonDown(keyBinding.keyCode + 100)
        } else {
            GameSettings.isKeyDown(keyBinding)
        }
    }

    override val tag
        get() = when {
            aacAdditionPro -> "AACAdditionPro"
            inventoryMotion != 1F -> inventoryMotion.toString()
            else -> null
        }
}