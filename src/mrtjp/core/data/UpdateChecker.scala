/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText

trait UpdateChecker extends Thread
{
    setName(project+" version checker")
    setDaemon(true)

    try { if (shouldRun) FMLCommonHandler.instance.bus.register(this) } catch { case t:Throwable => }

    def project:String
    def changelogURL:String

    def currentVersion:String
    def shouldRun:Boolean

    val availableVersions = downloadVersions

    def downloadVersions:Seq[String] =
    {
        try
        {
            val url = new URL(changelogURL)
            val in = new BufferedReader(new InputStreamReader(url.openStream()))

            var line:String = ""
            def next() = {line = in.readLine(); line}

            val builder = Seq.newBuilder[String]

            while (next() != null)
            {
                if (line.startsWith("v"))
                    builder += line.substring(1)
            }
            builder.result()
        }
        catch
        {
            case e:Exception => Seq.empty
        }
    }

    def isVersionOutdated(v:String) = v != availableVersions.head && availableVersions.contains(v)

    var updatesChecked = false
    override def run()
    {
        if (updatesChecked) return
        try
        {
            if (!shouldRun) return
            if (isVersionOutdated(currentVersion))
            {
                val message = Seq.newBuilder[String]
                message += s"$project ${availableVersions.head} available."
                updateMessage = message.result()
            }
        }
        catch
        {
            case t:Throwable => t.printStackTrace()
        }
        updatesChecked = true
    }

    private var messageDisplayed = false
    private var updateMessage:Seq[String] = null

    @SubscribeEvent
    def tickEnd(event:PlayerTickEvent)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            if (messageDisplayed) return
            if (updateMessage != null)
            {
                val p = Minecraft.getMinecraft.thePlayer
                for (s <- updateMessage) p.addChatMessage(new ChatComponentText(s))
            }
            messageDisplayed = true
        }
    }

    start()
}