/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import org.lwjgl.opengl.{GL11, GL12}

import scala.collection.JavaConversions
import scala.collection.mutable.ListBuffer

class TabNode(wMin:Int, hMin:Int, wMax:Int, hMax:Int, val color:Int) extends TNode
{
    def this(wMin:Int, hMin:Int, wMax:Int, hMax:Int) = this(wMin, hMin, wMax, hMax, Colors.LIGHT_GREY.rgb)

    var currentW = wMin.asInstanceOf[Double]
    var currentH = wMin.asInstanceOf[Double]

    def getControl = parent.asInstanceOf[TabControlNode]

    var size = Size(wMin, hMin)
    override def frame = Rect(position, size)
    private val startBounds = frame

    var active = false
    def isOpen = active && size.width==wMax && size.height==hMax

    override def drawBack_Impl(mouse:Point, rframe:Float)
    {
        val w = if (active) wMax else wMin
        val h = if (active) hMax else hMin

        if (w != size.width) currentW += (w-currentW)/8
        if (h != size.height) currentH += (h-currentH)/8

        size = Size(currentW.round.toInt, currentH.round.toInt)

        drawBox()
        drawIcon()
        if (isOpen)
        {
            drawTab()
            children.foreach(_.hidden = false)
        }
        else children.foreach(_.hidden = true)
    }

    override def drawFront_Impl(mouse:Point, rframe:Float)
    {
        if (rayTest(mouse))
        {
            val list = ListBuffer[String]()
            buildToolTip(list)
            GuiDraw.drawMultilineTip(mouse.x+12, mouse.y-12, JavaConversions.bufferAsJavaList(list))
        }
    }

    def drawTab(){}

    def drawIcon(){}

    def buildToolTip(list:ListBuffer[String]){}

    def drawBox()
    {
        val r = (color>>16&255)/255.0F
        val g = (color>>8&255)/255.0F
        val b = (color&255)/255.0F
        GL11.glColor4f(r, g, b, 1)

        GuiLib.drawGuiBox(position.x, position.y, size.width, size.height, 0)
    }

    override def mouseClicked_Impl(p:Point, button:Int, consumed:Boolean) =
    {
        if (!consumed && startBounds.contains(p))
        {
            getControl.onTabClicked(this)
            true
        }
        else false
    }
}

trait TStackTab extends TabNode
{
    var iconStack:ItemStack = null
    def setIconStack(stack:ItemStack):this.type = {iconStack = stack; this}

    abstract override def drawIcon()
    {
        super.drawIcon()
        GL11.glColor4f(1, 1, 1, 1)
        RenderHelper.enableGUIStandardItemLighting()
        GL11.glEnable(GL12.GL_RESCALE_NORMAL)
        TStackTab.itemRender.zLevel = (zPosition+25).toFloat
        TStackTab.itemRender.renderItemAndEffectIntoGUI(fontRenderer, renderEngine, iconStack, position.x+3, position.y+3)
        GL11.glDisable(GL12.GL_RESCALE_NORMAL)
        GL11.glDisable(GL11.GL_LIGHTING)
        RenderHelper.disableStandardItemLighting()
    }
}

object TStackTab
{
    val itemRender = new RenderItem
}

trait TIconTab extends TabNode
{
    var icon:IIcon = null
    def setIcon(i:IIcon):this.type = {icon = i; this}

    abstract override def drawIcon()
    {
        super.drawIcon()
        drawTexturedModelRectFromIcon(position.x+3, position.x+3, icon, 16, 16)
    }
}


class TabControlNode(x:Int, y:Int) extends TNode
{
    position = Point(x, y)
    override def frame = Rect(position, Size.zeroSize)

    var active:TabNode = null

    def onTabClicked(tab:TabNode)
    {
        if (tab != active)
        {
            if (active != null) active.active = false
            tab.active = true
            active = tab
        }
        else
        {
            tab.active = false
            active = null
        }
    }

    override def frameUpdate_Impl(mouse:Point, rframe:Float)
    {
        var dx = 0
        for (w <- children)
        {
            w.position = Point(w.position.x, dx)
            dx += w.frame.height
        }
    }
}