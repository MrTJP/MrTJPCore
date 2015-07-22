/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import java.util.{List => JList}

import codechicken.lib.gui.GuiDraw
import codechicken.lib.vec.{Scale, Vector3}
import mrtjp.core.item.ItemKeyStack
import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.{OpenGlHelper, RenderHelper}
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12

class ItemListNode extends TNode
{
    var items = Seq[ItemKeyStack]()
    var itemSize = Size(16, 16)
    var gridWidth = 3

    var displayNodeFactory = {stack:ItemKeyStack => new ItemDisplayNode}
    var cullFrame = Rect.infiniteRect

    private var dispNodes = Seq[ItemDisplayNode]()

    override def frame = Rect(position, itemSize.multiply(math.min(items.size, gridWidth), items.size/gridWidth+1))

    def reset()
    {
        val it = items.iterator
        var (x, y) = (0, 0)

        dispNodes.foreach(_.removeFromParent())
        dispNodes = Seq()

        while(it.hasNext)
        {
            val i = it.next()
            val d = displayNodeFactory(i)
            if (d != null)
            {
                d.stack = i
                d.size = itemSize
                d.position = Point(itemSize.multiply(x, y))
                addChild(d)

                val df = convertRectToScreen(d.frame)
                if (cullFrame.intersects(df))
                {
                    dispNodes :+= d
                }
                else d.removeFromParent()

                x += 1
                if (x >= gridWidth){x = 0; y += 1}
            }
        }
    }
}

class ItemDisplayNode extends TNode
{
    var stack:ItemKeyStack = null
    var size = Size.zeroSize

    var drawNumber = true
    var drawTooltip = true

    var backgroundColour = 0
    var clickDelegate = {() => }

    override def frame = Rect(position, size)

    override def drawBack_Impl(mouse:Point, rframe:Float)
    {
        GuiDraw.drawRect(position.x, position.y, size.width, size.height, backgroundColour)
        ItemDisplayNode.renderItem(position, size, zPosition, drawNumber, stack.makeStack)
    }

    override def mouseClicked_Impl(p:Point, button:Int, consumed:Boolean) =
    {
        if (!consumed && rayTest(p))
        {
            clickDelegate()
            true
        }
        else false
    }

    override def drawFront_Impl(mouse:Point, rframe:Float)
    {
        if (drawTooltip && frame.contains(mouse) && rayTest(mouse))
            drawTooltip(mouse)
    }

    def drawTooltip(mouse:Point)
    {
        ClipNode.tempDisableScissoring()
        //draw tooltip with absolute coords to allow it to force-fit on screen
        translateToScreen()
        val Point(mx, my) = parent.convertPointToScreen(mouse)

        import scala.collection.JavaConversions._
        val lines = stack.makeStack.getTooltip(mcInst.thePlayer,
            mcInst.gameSettings.advancedItemTooltips).asInstanceOf[JList[String]]
        val l2 = Seq(lines.head)++lines.tail.map(EnumChatFormatting.GRAY+_)
        GuiDraw.drawMultilineTip(mx+12, my-12, l2)

        translateFromScreen()
        ClipNode.tempEnableScissoring()
    }
}

object ItemDisplayNode
{
    val renderItem = new RenderItem

    def renderItem(position:Point, size:Size, zPosition:Double, drawNumber:Boolean, stack:ItemStack)
    {
        val font = stack.getItem.getFontRenderer(stack) match
        {
            case null => Minecraft.getMinecraft.fontRenderer
            case r => r
        }
        val renderEngine = Minecraft.getMinecraft.renderEngine

        val f = font.getUnicodeFlag
        font.setUnicodeFlag(true)

        glItemPre()
        glPushMatrix()
        new Scale(size.width/16.0, size.height/16.0, 1)
                .at(new Vector3(position.x, position.y, 0)).glApply()

        renderItem.zLevel = (zPosition+10.0).toFloat
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_LIGHTING)
        renderItem.renderItemAndEffectIntoGUI(font, renderEngine, stack, position.x, position.y)
        renderItem.renderItemOverlayIntoGUI(font, renderEngine, stack, position.x, position.y, "")
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)
        renderItem.zLevel = zPosition.toFloat

        if (drawNumber)
        {
            val s =
                if (stack.stackSize == 1) ""
                else if (stack.stackSize < 1000) stack.stackSize+""
                else if (stack.stackSize < 100000) stack.stackSize/1000+"K"
                else if (stack.stackSize < 1000000) "0."+stack.stackSize/100000+"M"
                else stack.stackSize/1000000+"M"
            font.drawStringWithShadow(s, position.x+19-2-font.getStringWidth(s), position.y+6+3, 16777215)
        }

        glPopMatrix()
        glItemPost()

        font.setUnicodeFlag(f)
    }

    def glItemPre()
    {
        glPushMatrix()
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
        RenderHelper.enableGUIStandardItemLighting()
        glEnable(GL12.GL_RESCALE_NORMAL)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240/1.0F, 240/1.0F)
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_LIGHTING)
    }

    def glItemPost()
    {
        glEnable(GL_DEPTH_TEST)
        glPopMatrix()
    }

}