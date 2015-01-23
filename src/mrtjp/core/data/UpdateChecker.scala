/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText

import scala.collection.immutable.ListMap
import scala.util.control.Breaks
import scala.xml.XML

trait UpdateChecker extends Thread
{
    setName(project+" version checker")
    setDaemon(true)

    try { if (shouldRun) FMLCommonHandler.instance.bus.register(this) } catch { case t:Throwable => }

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

    val availableVersions = downloadVersions
    val availableMainVersions = availableVersions.map(_.split("-")(1))
    val allChanges = downloadChanges

    def latestVersion = availableMainVersions(0)

    def downloadVersions:Seq[String] =
    {
        try
        {
            val seq = Seq.newBuilder[String]
            val meta = projectMD
            for (v <- meta\"versioning"\"versions"\"version") seq += v.text.trim

            def isVerAvailable(v:String) =
            {
                val meta = versionMD(v)
                (meta\"isPublic").text.trim == "true" &&
                        (checkUnstable || (meta\"isRecommended").text.trim == "true")
            }

            seq.result().filter(isVerAvailable).reverse
        }
        catch
        {
            case t:Throwable =>
                t.printStackTrace()
                Seq()
        }
    }

    def downloadChanges:Map[String, Seq[String]] =
    {
        try
        {
            var cMap = ListMap[String, Seq[String]]().withDefault(_ => Seq())
            val reader = IO.forceReadBuffer(changelogURL)
            var v = ""
            var in = ""
            while ({in = reader.readLine(); in} != null)
            {
                if (in.startsWith("v")) v = in.substring(1)
                else if (!in.isEmpty) cMap += v -> (cMap(v):+in.substring(2))
            }
            cMap
        }
        catch
        {
            case t:Throwable =>
                t.printStackTrace()
                Map()
        }
    }

    def isVersionOutdated(v:String) = v != latestVersion && availableMainVersions.contains(v)

    def compileChanges(since:String) =
    {
        val builder = Seq.newBuilder[String]
        val vsince = since.split('.').take(3).mkString(".")
        Breaks.breakable { for ((v, seq) <- allChanges)
        {
            if (v == vsince) Breaks.break()
            builder += "   --- as of v"+v+" ---   "
            for (c <- seq) builder += c
        }}
        builder.result()
    }

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
                message += "Version "+latestVersion+" of "+project+" is available. Changes:"
                message ++= compileChanges(currentVersion)
                updateMessage = message.result()
            }
        }
        catch
        {
            case t:Throwable => t.printStackTrace()
        }
        updatesChecked = true
    }

    var messageDisplayed = false
    var updateMessage:Seq[String] = null
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