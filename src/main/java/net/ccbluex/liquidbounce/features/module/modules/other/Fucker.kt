/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isBlockBBValid
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.client.ClientThemesUtils.getColorWithAlpha
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockDamageText
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.performRaytrace
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.block.Block
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.*
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color

object Fucker : Module("Fucker", Category.OTHER, hideModule = false) {

    /**
     * SETTINGS
     */

    private val hypixel by boolean("Hypixel", false)

    private val block by block("Block", 26)
    private val throughWalls by choices("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None") { !hypixel }
    private val range by float("Range", 5F, 1F..7F)

    private val action by choices("Action", arrayOf("Destroy", "Use"), "Destroy")
    private val surroundings by boolean("Surroundings", true) { !hypixel }
    private val instant by boolean("Instant", false) { (action == "Destroy" || surroundings) && !hypixel }

    private val switch by int("SwitchDelay", 250, 0..1000)
    private val swing by boolean("Swing", true)
    val noHit by boolean("NoHit", false)

    private val options = RotationSettings(this).withoutKeepRotation()

    private val blockProgress by boolean("BlockProgress", true)

    private val scale by float("Scale", 2F, 1F..6F) { blockProgress }
    private val font by font("Font", Fonts.font40) { blockProgress }
    private val fontShadow by boolean("Shadow", true) { blockProgress }

    private val colorRed by int("R", 200, 0..255) { blockProgress }
    private val colorGreen by int("G", 100, 0..255) { blockProgress }
    private val colorBlue by int("B", 0, 0..255) { blockProgress }

    private val ignoreOwnBed by boolean("IgnoreOwnBed", true)
    private val ownBedDist by int("MaxBedDistance", 16, 1..32) { ignoreOwnBed }

    private val renderPos by boolean("Render-Pos", false)
    private val clientTheme by boolean("RenderPos Color", true) { renderPos }
    private val posProcess by boolean("PosProcess", false) { renderPos }

    private val posOutline by boolean("PosOutline", false)
    /**
     * VALUES
     */

    var pos: BlockPos? = null
        private set
    private var spawnLocation: Vec3? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private val switchTimer = MSTimer()
    var currentDamage = 0F
    private var damageAnim = 0F

    // Surroundings
    private var areSurroundings = false

    override fun onToggle(state: Boolean) {
        if (pos != null && !mc.thePlayer.capabilities.isCreativeMode) {
            sendPacket(C07PacketPlayerDigging(ABORT_DESTROY_BLOCK, pos, EnumFacing.DOWN))
        }

        currentDamage = 0F
        pos = null
        areSurroundings = false
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mc.thePlayer == null || mc.theWorld == null)
            return@handler
        val packet = event.packet

        if (packet is S08PacketPlayerPosLook) {
            val pos = BlockPos(packet.x, packet.y, packet.z)

            spawnLocation = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        }
    }
    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        if (noHit && KillAura.handleEvents() && KillAura.target != null) {
            return@handler
        }

        val targetId = block

        if (pos == null || pos!!.block!!.id != targetId || getCenterDistance(pos!!) > range) {
            pos = find(targetId)
        }

        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            areSurroundings = false
            return@handler
        }

        var currentPos = pos ?: return@handler
        var spot = faceBlock(currentPos) ?: return@handler

        // Check if it is the player's own bed
        if (ignoreOwnBed && isBedNearSpawn(currentPos)) {
            return@handler
        }

        if (surroundings || hypixel) {
            val eyes = player.eyes

            val blockPos = if (hypixel) {
                currentPos.up()
            } else {
                world.rayTraceBlocks(eyes, spot.vec, false, false, true)?.blockPos
            }

            if (blockPos != null && blockPos.block != Blocks.air) {
                if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z) {
                    areSurroundings = true
                }

                pos = blockPos
                currentPos = pos ?: return@handler
                spot = faceBlock(currentPos) ?: return@handler
            }
        }

        // Reset switch timer when position changed
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
            switchTimer.reset()
        }

        oldPos = currentPos

        if (!switchTimer.hasTimePassed(switch)) {
            return@handler
        }

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return@handler
        }

        // Face block
        if (options.rotationsActive) {
            setTargetRotation(spot.rotation, options = options)
        }
    }

    /**
     * Check if the bed at the given position is near the spawn location
     */
    private fun isBedNearSpawn(currentPos: BlockPos): Boolean {
        if (currentPos.block != Block.getBlockById(block) || spawnLocation == null) {
            return false
        }

        return spawnLocation!!.squareDistanceTo(currentPos.center) < ownBedDist * ownBedDist
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        val controller = mc.playerController ?: return@handler

        val currentPos = pos ?: return@handler

        val targetRotation = if (options.rotationsActive) {
            currentRotation ?: player.rotation
        } else {
            toRotation(currentPos.center, false).fixedSensitivity()
        }

        val raytrace = performRaytrace(currentPos, targetRotation, range) ?: return@handler

        when {
            // Destroy block
            action == "Destroy" || areSurroundings -> {
                // Check if it is the player's own bed
                if (ignoreOwnBed && isBedNearSpawn(currentPos)) {
                    return@handler
                }

                EventManager.call(ClickBlockEvent(currentPos, raytrace.sideHit))

                // Break block
                if (instant && !hypixel) {
                    // CivBreak style block breaking
                    sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, raytrace.sideHit))

                    if (swing) {
                        player.swingItem()
                    }

                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    currentDamage = 0F
                    return@handler
                }

                // Minecraft block breaking
                val block = currentPos.block ?: return@handler

                if (currentDamage == 0F) {
                    // Prevent from flagging FastBreak
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    WaitTickUtils.schedule(1) {
                        sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    }

                    if (player.capabilities.isCreativeMode || block.getPlayerRelativeBlockHardness(
                            player,
                            world,
                            currentPos
                        ) >= 1f
                    ) {
                        if (swing) {
                            player.swingItem()
                        }

                        controller.onPlayerDestroyBlock(currentPos, raytrace.sideHit)

                        currentDamage = 0F
                        pos = null
                        areSurroundings = false
                        return@handler
                    }
                }

                if (swing) {
                    player.swingItem()
                }

                currentDamage += block.getPlayerRelativeBlockHardness(player, world, currentPos)
                world.sendBlockBreakProgress(player.entityId, currentPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    controller.onPlayerDestroyBlock(currentPos, raytrace.sideHit)
                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                    areSurroundings = false
                }
            }

            // Use block
            action == "Use" -> {
                if (player.onPlayerRightClick(currentPos, raytrace.sideHit, raytrace.hitVec, player.heldItem)) {
                    if (swing) player.swingItem()
                    else sendPacket(C0APacketAnimation())

                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                    areSurroundings = false
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val pos = pos ?: return@handler

        // Check if it is the player's own bed
        if (mc.thePlayer == null || ignoreOwnBed && isBedNearSpawn(pos)) {
            return@handler
        }

        if (block.blockById == Blocks.air) return@handler

        if (blockProgress) {
            pos.drawBlockDamageText(
                currentDamage,
                font,
                fontShadow,
                ColorUtils.packARGBValue(colorRed, colorGreen, colorBlue),
                scale,
            )
        }

        val x = pos.x - mc.renderManager.renderPosX
        val y = pos.y - mc.renderManager.renderPosY
        val z = pos.z - mc.renderManager.renderPosZ
        val c = if (clientTheme) getColorWithAlpha(1, 80) else if (pos.block != Blocks.bed) Color(
            255,
            0,
            0,
            50
        ) else Color(0, 255, 0, 50)
        if (renderPos) {
            if (posOutline) {
                RenderUtils.renderOutlines(x + 0.5, y - 0.5, z + 0.5, if (posProcess) damageAnim.animSmooth(
                    currentDamage, 0.5F) else 1.0f, if (posProcess) damageAnim.animSmooth(
                    currentDamage, 0.5F) else 1.0f, c, 1.5F)
                GlStateManager.resetColor()
            } else {
                RenderUtils.renderBox(x + 0.5, y - 0.5, z + 0.5, if (posProcess) currentDamage else 1.0f, if (posProcess) currentDamage else 1.0f, c)
                GlStateManager.resetColor()
            }
        }

        // Render block box
        drawBlockBox(pos, Color.RED, true)
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(targetID: Int): BlockPos? {
        val eyes = mc.thePlayer?.eyes ?: return null

        var nearestBlockDistanceSq = Double.MAX_VALUE
        val nearestBlock = BlockPos.MutableBlockPos()

        val rangeSq = range * range

        eyes.getAllInBoxMutable(range + 1.0).forEach {
            val distSq = it.distanceSqToCenter(eyes.xCoord, eyes.yCoord, eyes.zCoord)

            if (it.block?.id != targetID
                || distSq > rangeSq || distSq > nearestBlockDistanceSq
                || !isHittable(it) && !surroundings && !hypixel
            ) return@forEach

            nearestBlockDistanceSq = distSq
            nearestBlock.set(it)
        }

        return nearestBlock.takeIf { nearestBlockDistanceSq != Double.MAX_VALUE }
    }

    /**
     * Check if block is hittable (or allowed to hit through walls)
     */
    private fun isHittable(blockPos: BlockPos): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        return when (throughWalls.lowercase()) {
            "raycast" -> {
                val eyesPos = thePlayer.eyes
                val movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, blockPos.center, false, true, false)

                movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
            }

            "around" -> EnumFacing.entries.any { !isBlockBBValid(blockPos.offset(it)) }

            else -> true
        }
    }

    override val tag
        get() = getBlockName(block)
}