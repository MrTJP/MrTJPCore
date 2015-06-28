/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.gui.GuiDraw
import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.Container
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

class NodeGui(c:Container, w:Int, h:Int) extends GuiContainer(c) with TNode
{
    def this(c:Container) = this(c, 176, 166)
    def this(x:Int, y:Int) = this(new NodeContainer, x, y)

    xSize = w
    ySize = h

    var debugDrawFrames = false

    var size = Size.zeroSize
    override def frame = new Rect(position, size)

    //TODO get rid of this nonsense
    var prevGui:GuiScreen = null
    def setJumpBack(p:GuiScreen){prevGui = p}
    def jumpTo(g:GuiScreen, containerHack:Boolean)
    {
        mcInst.displayGuiScreen(g)
        if (containerHack) g match
        {
            case cont:GuiContainer =>
                cont.inventorySlots.windowId = inventorySlots.windowId
            case _ =>
        }
    }

    override def initGui()
    {
        super.initGui()
        position = Point(guiLeft, guiTop)
        size = Size(xSize, ySize)
    }

    final override def updateScreen()
    {
        super.updateScreen()
        update()
    }

    final override def setWorldAndResolution(mc:Minecraft, i:Int, j:Int)
    {
        val init = this.mc == null
        super.setWorldAndResolution(mc, i, j)
        if (init) onAddedToParent_Impl()
    }

    final override def mouseClicked(x:Int, y:Int, button:Int)
    {
        super.mouseClicked(x, y, button)
        mouseClicked(new Point(x, y), button, false)
    }

    final override def mouseMovedOrUp(x:Int, y:Int, button:Int)
    {
        super.mouseMovedOrUp(x, y, button)
        if (button != -1) mouseReleased(new Point(x, y), button, false)
    }

    final override def mouseClickMove(x:Int, y:Int, button:Int, time:Long)
    {
        super.mouseClickMove(x, y, button, time)
        mouseDragged(new Point(x, y), button, time, false)
    }

    final override def handleMouseInput()
    {
        super.handleMouseInput()
        val i = Mouse.getEventDWheel
        if (i != 0)
        {
            val p = GuiDraw.getMousePosition
            mouseScrolled(new Point(p.x, p.y), if (i > 0) 1 else -1, false)
        }
    }

    final override def keyTyped(c:Char, keycode:Int)
    {
        if (keyPressed(c, keycode, false)) return

        if (isClosingKey(keycode) && prevGui != null) //esc
        {
            jumpTo(prevGui, prevGui.isInstanceOf[GuiContainer])
            return
        }
        super.keyTyped(c, keycode)
    }

    def isClosingKey(keycode:Int) =
        keycode == 1 || keycode == mc.gameSettings.keyBindInventory.getKeyCode //esc or inv key

    /**
     * Front/back rendering overridden, because at root, we dont push the children to our pos, because its zero.
     */
    private var lastFrame = 0.0F
    final override def drawGuiContainerBackgroundLayer(f:Float, mx:Int, my:Int)
    {
        lastFrame = f
        val mouse = new Point(mx, my)
        frameUpdate(mouse, f)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        rootDrawBack(mouse, f)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    final override def drawGuiContainerForegroundLayer(mx:Int, my:Int)
    {
        val mouse = new Point(mx, my)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        rootDrawFront(mouse, lastFrame)
        GL11.glEnable(GL11.GL_DEPTH_TEST)

        if (debugDrawFrames)
        {
            GL11.glTranslated(-position.x, -position.y, 0)
            def render(node:TNode)
            {
                if (!node.hidden)
                {
                    val f = node.frame
                    val absF = Rect(node.parent.convertPointToScreen(f.origin), f.size)
                    GuiLib.drawLine(absF.x, absF.y, absF.x, absF.maxY)
                    GuiLib.drawLine(absF.x, absF.maxY, absF.maxX, absF.maxY)
                    GuiLib.drawLine(absF.maxX, absF.maxY, absF.maxX, absF.y)
                    GuiLib.drawLine(absF.maxX, absF.y, absF.x, absF.y)
                }
                for (c <- node.children) render(c)
            }
            for (c <- children) render(c)
            GL11.glTranslated(position.x, position.y, 0)
        }
    }
}