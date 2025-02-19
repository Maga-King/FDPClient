/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.ui.client.gui

import net.ccbluex.liquidbounce.FDPClient.CLIENT_NAME
import net.ccbluex.liquidbounce.FDPClient.clientVersionText
import net.ccbluex.liquidbounce.features.module.modules.client.HUDModule.guiColor
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.gui.button.ImageButton
import net.ccbluex.liquidbounce.ui.client.gui.button.QuitButton
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.Fonts.minecraftFont
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.bugs
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.canConnect
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.changelogs
import net.ccbluex.liquidbounce.utils.io.APIConnectorUtils.isLatest
import net.ccbluex.liquidbounce.utils.io.GitUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBloom
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawShadowRect
import net.ccbluex.liquidbounce.utils.ui.AbstractScreen
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.GuiModList
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*

class GuiMainMenu : AbstractScreen(), GuiYesNoCallback {

    private var logo: ResourceLocation? = null

    private lateinit var btnSinglePlayer: GuiButton
    private lateinit var btnMultiplayer: GuiButton
    private lateinit var btnClientOptions: GuiButton
    private lateinit var btnCheckUpdate: GuiButton

    private lateinit var btnClickGUI: ImageButton
    private lateinit var btnCommitInfo: ImageButton
    private lateinit var btnCosmetics: ImageButton
    private lateinit var btnMinecraftOptions: ImageButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var btnForgeModList: ImageButton
    private lateinit var btnAddAccount: ImageButton

    private lateinit var btnQuit: QuitButton

    override fun initGui() {
        logo = ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/logo.png")
        val centerY = height / 2 - 80
        val buttonWidth = 133
        val buttonHeight = 20
        btnSinglePlayer = +GuiButton(
            0,        // ID
            width / 2 - 66,
            centerY + 70,
            buttonWidth, buttonHeight,
            "SINGLE PLAYER"
        )
        btnMultiplayer = +GuiButton(
            1,
            width / 2 - 66,
            centerY + 95 - 2,
            buttonWidth, buttonHeight,
            "MULTI PLAYER"
        )
        btnClientOptions = +GuiButton(
            2,
            width / 2 - 66,
            centerY + 120 - 4,
            buttonWidth, buttonHeight,
            "SETTINGS"
        )
        btnCheckUpdate = +GuiButton(
            3,
            width / 2 - 66,
            centerY + 145 - 6,
            buttonWidth, buttonHeight,
            "CHECK UPDATE"
        )

        val bottomY = height - 20
        btnClickGUI = ImageButton("CLICKGUI", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/clickgui.png"), width / 2 - 45, bottomY)
        btnCommitInfo = ImageButton("COMMIT INFO", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/github.png"), width / 2 - 30, bottomY)
        btnCosmetics = ImageButton("COSMETICS", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/cosmetics.png"), width / 2 - 15, bottomY)
        btnMinecraftOptions = ImageButton("MINECRAFT SETTINGS", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/cog.png"), width / 2, bottomY)
        btnLanguage = ImageButton("LANGUAGE", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/globe.png"), width / 2 + 15, bottomY)
        btnForgeModList = ImageButton("FORGE MODS", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/forge.png"), width / 2 + 30, bottomY)

        btnAddAccount = ImageButton("ALT MANAGER", ResourceLocation("${CLIENT_NAME.lowercase()}/mainmenu/add-account.png"), width - 55, 7)
        btnQuit = QuitButton(width - 17, 7)

        buttonList.addAll(listOf(btnSinglePlayer, btnMultiplayer, btnClientOptions, btnCheckUpdate))
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        buttonList.forEach { guiButton ->
            if (guiButton.mousePressed(mc, mouseX, mouseY)) {
                actionPerformed(guiButton)
            }
        }

        when {
            btnQuit.hoverFade > 0 -> mc.shutdown()
            btnMinecraftOptions.hoverFade > 0 ->
                mc.displayGuiScreen(GuiOptions(this, mc.gameSettings))
            btnLanguage.hoverFade > 0 ->
                mc.displayGuiScreen(GuiLanguage(this, mc.gameSettings, mc.languageManager))
            btnCommitInfo.hoverFade > 0 ->
                mc.displayGuiScreen(GuiCommitInfo())
            btnForgeModList.hoverFade > 0 ->
                mc.displayGuiScreen(GuiModList(mc.currentScreen))
            btnCosmetics.hoverFade > 0 ->
                mc.displayGuiScreen(GuiCommitInfo())
            btnClickGUI.hoverFade > 0 ->
                mc.displayGuiScreen(ClickGui)
            btnAddAccount.hoverFade > 0 ->
                mc.displayGuiScreen(GuiAltManager(this))
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(GuiSelectWorld(this))
            1 -> mc.displayGuiScreen(GuiMultiplayer(this))
            2 -> mc.displayGuiScreen(GuiInfo(this))
            3 -> mc.displayGuiScreen(GuiUpdate())
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        assumeNonVolatile = true

        drawBackground(0)

        if (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            mc.displayGuiScreen(ClickGui)
        }

        GlStateManager.pushMatrix()

        drawShadowRect(
            (width / 2 - 130).toFloat(),
            (height / 2 - 90).toFloat(),
            (width / 2 + 130).toFloat(),
            (height / 2 + 90).toFloat(),
            15F,
            Color(44, 43, 43, 100).rgb
        )

        GlStateManager.disableAlpha()
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f)
        mc.textureManager.bindTexture(logo)
        drawModalRectWithCustomSizedTexture(
            width / 2 - 25,
            height / 2 - 68,
            0f,
            0f,
            49,
            49,
            49f,
            49f
        )

        val apiMessage = if (canConnect) "§eOK" else "§cNo"
        val aply = width - 10f - minecraftFont.getStringWidth("API Connection: $apiMessage")
        minecraftFont.drawStringWithShadow(
            "API Connection: $apiMessage",
            aply,
            32f,
            Color(255, 255, 255, 140).rgb
        )

        val textClientNameX = width - 4f - minecraftFont.getStringWidth(CLIENT_NAME)
        minecraftFont.drawStringWithShadow(
            CLIENT_NAME,
            textClientNameX,
            height - 23f,
            Color(255, 255, 255, 140).rgb
        )

        val uiMessage = when {
            canConnect && isLatest -> " §e(Latest)"
            !canConnect && isLatest -> " §c(API Dead)"
            else -> " §c(Outdated)"
        }
        val buildInfoText = "Your currently build is $clientVersionText$uiMessage"
        val buildInfoX = width - 4f - minecraftFont.getStringWidth(buildInfoText)
        minecraftFont.drawStringWithShadow(
            buildInfoText,
            buildInfoX,
            height - 12f,
            Color(255, 255, 255, 140).rgb
        )

        minecraftFont.drawStringWithShadow(
            "Changelogs:",
            3f,
            32f,
            Color(255, 255, 255, 150).rgb
        )

        var changeY = 48
        val changeDetails = changelogs.split("\n")
        for (line in changeDetails) {
            val formatted = formatChangelogLine(line)
            minecraftFont.drawStringWithShadow(
                formatted,
                4f,
                changeY.toFloat(),
                Color(255, 255, 255, 150).rgb
            )
            changeY += 8
        }

        val knownBugsText = "Known Bugs:"
        val mess = width - 10f - minecraftFont.getStringWidth(knownBugsText)
        minecraftFont.drawStringWithShadow(
            knownBugsText,
            mess,
            43f,
            Color(255, 255, 255, 140).rgb
        )

        var bugsY = 55
        val bugDetails = bugs.split("\n")
        for (line in bugDetails) {
            val lineWidth = minecraftFont.getStringWidth(line)
            val xPos = width - 12f - lineWidth
            minecraftFont.drawStringWithShadow(
                line,
                xPos,
                bugsY.toFloat(),
                Color(255, 255, 255, 140).rgb
            )
            bugsY += 11
        }

        Fonts.fontSmall.drawCenteredStringWithoutShadow(
            "by SkidderMC with love ",
            width / 2f,
            height / 2f - 19,
            Color(255, 255, 255, 100).rgb
        )

        listOf(btnSinglePlayer, btnMultiplayer, btnClientOptions, btnCheckUpdate).forEach {
            it.drawButton(mc, mouseX, mouseY)
        }

        listOf(
            btnClickGUI, btnCommitInfo, btnCosmetics, btnMinecraftOptions,
            btnLanguage, btnForgeModList, btnAddAccount, btnQuit
        ).forEach {
            it.drawButton(mouseX, mouseY)
        }

        val branch = GitUtils.gitBranch
        val commitIdAbbrev = GitUtils.gitInfo.getProperty("git.commit.id.abbrev")
        val infoStr = "$CLIENT_NAME($branch/$commitIdAbbrev) | Minecraft 1.8.9"
        Fonts.font35.drawCenteredStringWithShadow(
            infoStr,
            7F,
            (this.height - 11).toFloat(),
            Color(255, 255, 255, 100).rgb
        )

        drawBloom(mouseX - 5, mouseY - 5, 10, 10, 16, Color(guiColor))

        GlStateManager.popMatrix()

        assumeNonVolatile = false

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Quick method to colorize changelog lines by prefix:
     * "~ " => Uppercase them
     * "+ " => "[+]"
     * "- " => "[-]"
     * "* " => "[*]"
     */
    private fun formatChangelogLine(line: String): String {
        return when {
            line.startsWith("~ ") -> "§r " + line.uppercase(Locale.getDefault())
            line.startsWith("+ ") -> "§7[§a+§7]  §r" + line.removePrefix("+ ").trim()
            line.startsWith("- ") -> "§7[§c-§7]  §r" + line.removePrefix("- ").trim()
            line.startsWith("* ") -> "§7[§e*§7]  §r" + line.removePrefix("* ").trim()
            else -> line
        }
    }
}