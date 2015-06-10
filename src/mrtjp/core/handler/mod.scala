/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import java.io.File

import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.{Loader, Mod}
import net.minecraftforge.common.config.Configuration
import org.apache.logging.log4j.LogManager

@Mod(modid = "MrTJPCoreMod", useMetadata = true, modLanguage = "scala")
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

object MrTJPConfig
{
    var retro_gen = false
    var retro_gen_id = "mrtjp_gen"

    var check_versions = true
    var check_unstable = false

    def loadConfig()
    {
        val config = new Configuration(new File(Loader.instance.getConfigDir, "MrTJPCore.cfg"))
        config.load()

        check_versions = config.get("General", "check_versions", true, "Flag to enable or disable the update checker.").getBoolean
        check_unstable = config.get("General", "check_unstable", false, "Flag to set if the update checker should consider unstable builds as a new version.").getBoolean

        retro_gen = config.get("World Gen", "retro_gen", true, "Toggle to enable retrogeneration, a feature that would allow ores to be generated after the world has been created.").getBoolean
        retro_gen_id = config.get("World Gen", "retro_gen_id", "mrtjp_gen", "The database ID that is used to store which chunks have been generated already. Changing this will cause generation to run again on the same chunk.").getString

        config.save()
    }
}