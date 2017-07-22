/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import codechicken.lib.packet.PacketCustom
import mrtjp.core.handler.MrTJPCoreSPH
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager._
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.math.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

import scala.collection.mutable

object Messenger
{
    val messages = mutable.ListBuffer[Message]()
    val options = Seq[MailOption](Replace, Combine)

    def createPacket = new PacketCustom(MrTJPCoreSPH.channel, MrTJPCoreSPH.messagePacket)

    /**
     * Adds a string to the location. To apply an option, add a "/#" + an option
     * char anywhere in the string.
     *
     * f - Override a message already at that location.
     * c - Combine message if one already exists there.
     *
     * @param x
     * @param y
     * @param z
     * @param mail
     */
    def addMessage(x: Double, y: Double, z: Double, mail: String)
    {
        val location = new BlockPos(Math.floor(x).asInstanceOf[Int], Math.floor(y).asInstanceOf[Int], Math.floor(z).asInstanceOf[Int])

        val mess = new Message().set(location, x, y, z, mail)

        options.foreach(op => op.modify(mess))

        if (messages.size > 64) messages.remove(0)

        messages += mess
    }

    @SubscribeEvent
    def renderMessages(event:RenderWorldLastEvent)
    {
        val w = Minecraft.getMinecraft.theWorld
        if (w == null) return
        if (Messenger.messages.isEmpty) return

        val deathTime = System.currentTimeMillis-3000L

        val view = Minecraft.getMinecraft.getRenderViewEntity
        val cx = view.lastTickPosX+(view.posX-view.lastTickPosX)*event.getPartialTicks
        val cy = view.lastTickPosY+(view.posY-view.lastTickPosY)*event.getPartialTicks
        val cz = view.lastTickPosZ+(view.posZ-view.lastTickPosZ)*event.getPartialTicks

        pushMatrix()
        translate(-cx, -cy, -cz)
        pushAttrib()
        disableLighting()
        depthMask(false)
        disableDepth()
        enableBlend()
        blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        for (m <- Messenger.messages.clone())
            if (m == null || m.receivedOn < deathTime) Messenger.messages -= m
            else readMessage(m, Minecraft.getMinecraft.theWorld.getTotalWorldTime+event.getPartialTicks)

        enableLighting()
        disableBlend()
        color(1, 1, 1, 1)
        popMatrix()
        popAttrib()
    }

    private def readMessage(m:Message, time:Double)
    {
        var width = 0
        var height = 0
        val lines = m.msg.split("\n")
        val fr = Minecraft.getMinecraft.fontRendererObj
        for (line <- lines) {
            height += fr.FONT_HEIGHT + 4
            width = Math.max(width, fr.getStringWidth(line))
        }

        width += 2
        var scaling: Float = 0.02666667F
        scaling *= 0.6666667F
        val y = (m.y+0.04*Math.sin((m.x.asInstanceOf[Int]^m.z.asInstanceOf[Int])+time/4)+m.yOffset).asInstanceOf[Float]

        pushMatrix()
        translate(m.x + 0.5F, y, m.z + 0.5F)
        glNormal3f(0.0F, 1.0F, 0.0F)
        rotate((-Minecraft.getMinecraft.getRenderManager.playerViewY+8*Math.sin((m.x.asInstanceOf[Int]^m.z.asInstanceOf[Int])+time/6)).asInstanceOf[Float], 0.0F, 1.0F, 0.0F)
        rotate(Minecraft.getMinecraft.getRenderManager.playerViewX, 1.0F, 0.0F, 0.0F)
        scale(-scaling, -scaling, scaling)
        translate(0.0F, -10*lines.length, 0.0F)

        val var16 = (lines.length-1)*10
        val var17 = width/2

        disableTexture2D()
        color(0, 0, 0, 0.25f)

        val tess = Tessellator.getInstance()
        val vb = tess.getBuffer
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        vb.pos(-var17-1, -1.0D, 0.0D).endVertex()
        vb.pos(-var17-1, 8+var16, 0.0D).endVertex()
        vb.pos(var17+1, 8+var16, 0.0D).endVertex()
        vb.pos(var17+1, -1.0D, 0.0D).endVertex()
        tess.draw()

        enableTexture2D()

        var i = 0
        for (line <- lines) {
            fr.drawString(line, -fr.getStringWidth(line)/2, 10*i, -1)
            i += 1
        }
        popMatrix()
    }
}

abstract class MailOption
{
    def modify(mes:Message)
    {
        if (mes.msg contains tag)
        {
            change(mes)
            mes.msg = mes.msg.replace(tag, "")
        }
    }

    def change(mes:Message)

    def tag:String
}

object Replace extends MailOption
{
    override def tag = "/#f"

    override def change(mes: Message)
    {
        for (m <- Messenger.messages.clone()) if (m.location == mes.location)
        {
            Messenger.messages -= m
            return
        }
    }
}

object Combine extends MailOption
{
    override def tag = "/#c"

    override def change(mes: Message)
    {
        for (m <- Messenger.messages.clone()) if (m.location == mes.location)
        {
            Messenger.messages -= m
            mes.msg = m.msg+"\n"+mes.msg
            return
        }
    }
}

class Message
{
    def set(location:BlockPos, x: Double, y: Double, z: Double, msg: String) =
    {
        this.receivedOn = System.currentTimeMillis
        this.msg = msg
        this.location = location
        this.x = x
        this.y = y
        this.z = z
        this
    }

    def addY(y: Float) =
    {
        yOffset += y
        this
    }

    var location:BlockPos = null
    var x = 0.0D
    var y = 0.0D
    var z = 0.0D
    var msg:String = null
    var receivedOn = 0L
    var yOffset = 0F
}
