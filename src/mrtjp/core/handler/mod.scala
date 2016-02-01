/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import mrtjp.core.data.{ModConfig, SpecialConfigGui, TModGuiFactory}
import net.minecraft.client.gui.GuiScreen
import org.apache.logging.log4j.LogManager

@Mod(modid = "MrTJPCoreMod", useMetadata = true, modLanguage = "scala", guiFactory = "mrtjp.core.handler.GuiConfigFactory")
object MrTJPCoreMod
{
    final val version = "@VERSION@"
    final val build = "@BUILD_NUMBER@"

    val log = LogManager.getFormatterLogger("MrTJPCoreMod")

    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        MrTJPConfig.loadConfig()
        MrTJPCoreProxy.preInit()
    }

    @Mod.EventHandler
    def init(event:FMLInitializationEvent)
    {
        MrTJPCoreProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        MrTJPCoreProxy.postInit()
    }
}

object MrTJPConfig extends ModConfig("MrTJPCoreMod")
{
    var retro_gen = false
    var retro_gen_id = "mrtjp_gen"

    var check_versions = true
    var check_unstable = false

    override def getFileName = "MrTJPCore"

    override protected def initValues()
    {
        val general = new BaseCategory("General", "General settings for MrTJPCore")
        check_versions = general.put("Version Checking", check_versions, "Flag to enable or disable the update checker.")
        check_unstable = general.put("Include Unstable", check_unstable, "Flag to set if the update checker should consider unstable builds as a new version.")

        val gen = new BaseCategory("World Gen", "Settings related to world generation")
        retro_gen = gen.put("Retroactive World Generation", retro_gen, "Toggle to enable retrogeneration, a feature that would allow ores to be generated after the world has been created.")
        retro_gen_id = gen.put("RetroGen ID", retro_gen_id, "The database ID that is used to store which chunks have been generated already. Changing this will cause generation to run again on the same chunk.")
    }
}

class MrTJPConfigGui(parent:GuiScreen) extends SpecialConfigGui(parent, "MrTJPCoreMod", MrTJPConfig.config)
class GuiConfigFactory extends TModGuiFactory
{
    override def mainConfigGuiClass() = classOf[MrTJPConfigGui]
}