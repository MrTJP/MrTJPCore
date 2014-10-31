/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText

import scala.xml.XML

trait UpdateChecker extends Thread
{
    setName(project+" version checker")
    setDaemon(true)
    start()
    FMLCommonHandler.instance().bus().register(this)

    def mavenRootURL:String
    def group:String
    def project:String
    def changelogURL:String

    def currentVersion:String
    def shouldRun:Boolean
    def checkUnstable:Boolean

    def projectRoot = mavenRootURL+"/"+group+"/"+project
    def projectMD = XML.loadString(IO.forceRead(projectRoot+"/"+"maven-metadata.xml"))
    def versionMD(v:String) = XML.loadString(IO.forceRead(projectRoot+"/"+v+"/"+project+"-"+v+".xml"))

    val availableVersions =
    {
        val seq = Seq.newBuilder[String]
        val meta = projectMD
        for (v <- meta\"versioning"\"versions") seq += v.text.trim
        seq.result().filter(isVerAvailable)

        def isVerAvailable(v:String) =
        {
            val meta = versionMD(v)
            (meta\"isPublic").text.trim == "true" &&
                    (checkUnstable || (meta\"isRecommended").text.trim == "true")
        }
    }

    val availableMainVersions = availableVersions.map(_.split("-")(1))

    val allChanges =
    {
        var cMap = Map[String, Seq[String]]().withDefault(_ => Seq())
        val reader = IO.readBuffer(changelogURL)
        var v = ""
        var in = ""
        while ({in = reader.readLine(); in} != null)
        {
            if (in.startsWith("v")) v = in.substring(1)
            else if (!in.isEmpty) cMap += v -> (cMap(v):+in.substring(2))
        }
        cMap
    }

    def isVersionOutdated(v:String) = v != latestVersion && !availableMainVersions.contains(v)

    def latestVersion = availableMainVersions(0)

    def compileChanges(since:String) =
    {
        val builder = Seq.newBuilder[String]
        for ((v, seq) <- allChanges) if (v != since)
        {
            builder += "   --- as of v"+v+" ---   "
            for (c <- seq) builder += c
        }
        builder.result()
    }

    var updatesChecked = false
    override def run()
    {
        if (updatesChecked) return
        if (!shouldRun) return
        if (isVersionOutdated(currentVersion))
        {
            val message = Seq.newBuilder[String]
            message += "Version "+latestVersion+" of "+project+" is available. Changes:"
            message ++= compileChanges(currentVersion)
        }
        updatesChecked = true
    }

    var messageDisplayed = false
    var updateMessage:Seq[String] = null
    @SubscribeEvent
    def tickEnd(event:PlayerTickEvent)
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