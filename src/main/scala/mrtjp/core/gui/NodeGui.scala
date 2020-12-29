/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.colour.EnumColour
import codechicken.lib.gui.GuiDraw
import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager._
import net.minecraft.inventory.Container
import org.lwjgl.input.Mouse

/**
  * Represents the root node in a GUI node tree.
  *
  * == Overview ==
  * The GUI rendering system provided in this library is based off of a render tree concept. Elements of the GUI are
  * implementations of `TNode`. Each node can draw things and be interacted with. It can also have child nodes, also
  * implementations of `TNode`.
  *
  * == Building a GUI ==
  * You can build a GUI by subclassing `NodeGui` or using it as is. Here is a simple example of creating a 100x100 window
  * with a text box and button inside. When the button is clicked, a custom function called onMyButtonClicked()
  * is called.
  *
  * {{{
  *
  * import mrtjp.core.gui._
  *
  * val rootNode = new NodeGui(100, 100) //Create centered window of size 100x100 on screen
  *
  * //add text box
  * val textBox = new SimpleTextBoxNode
  * textBox.size = Size(50, 16)
  * textBox.position = Point(25, 42) //Position is relative to parent
  * rootNode.addChild(textBox)
  *
  * //add button
  * val button = new MCButtonNode
  * button.position = Point(80, 42)
  * button.size = Size(16, 16)
  * button.text = "OK"
  * button.clickDelegate = {() => onMyButtonClicked()} //Click handler
  * rootNode.addChild(button)
  *
  * //... gui ready to present
  *
  * }}}
  *
  * == Using Custom Nodes ==
  * There are several predefined and flexible `TNode` implementations. Custom implementations can also be created
  * if none suit your needs. See [[TNode]] for more information.
  *
  *
  * @constructor
  * @param c The inventory container object that this GUI is representing. Typically a subclass of @class NodeContainer.
  * @param w The width of this GUI window.
  * @param h The height of this GUI window.
  */
class NodeGui(c:Container, w:Int, h:Int) extends GuiContainer(c) with TNode
{
    /**
      * @constructor Used for creating a default sized GUI window
      * @param c The inventory container object that this GUI is representing. Typically a subclass of @class NodeContainer.
      */
    def this(c:Container) = this(c, 176, 166)

    /**
      * @constructor Used for creating a GUI with custom sized window that is not backed by an inventory.
      * @param w The width of this GUI window.
      * @param h The height of this GUI window.
      */
    def this(w:Int, h:Int) = this(new NodeContainer, w, h)

    xSize = w
    ySize = h

    /**
      * Flag used for debugging. Enabling will cause all nodes in tree to render visible outline.
      */
    var debugDrawFrames = false

    /** Represents size of the window. Initially set to width and height */
    var size = Size.zeroSize //todo initialize this to xSize x ySize
    override def frame = new Rect(position, size)

    final override def initGui()
    {
        super.initGui()
        position = Point(guiLeft, guiTop)
        if (size == Size.zeroSize) size = Size(xSize, ySize) //TODO Legacy (size should be set directly)
        else
        {
            xSize = size.width
            ySize = size.height
        }
    }

    final override def updateScreen()
    {
        super.updateScreen()
        update()
    }

    final override def drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
        renderHoveredToolTip(mouseX, mouseY)
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


    final override def mouseReleased(x:Int, y:Int, button:Int)
    {
        super.mouseReleased(x, y, button)
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

        super.keyTyped(c, keycode)
    }

    /**
      * Used to check if the `keycode` should close the GUI.
      *
      * @param keycode The keycode to check.
      * @return True if this keycode corresponds to a close gui keybind.
      */
    def isClosingKey(keycode:Int) =
        keycode == 1 || keycode == mc.gameSettings.keyBindInventory.getKeyCode //esc or inv key

    private var lastFrame = 0.0F

    // Front/back rendering overridden, because at root, we dont push the children to our pos, because its zero.
    final override def drawGuiContainerBackgroundLayer(f:Float, mx:Int, my:Int)
    {
        lastFrame = f
        val mouse = new Point(mx, my)
        frameUpdate(mouse, f)
        disableDepth()
        color(1, 1, 1, 1)
        rootDrawBack(mouse, f)
        color(1, 1, 1, 1)
        enableDepth()
    }

    final override def drawGuiContainerForegroundLayer(mx:Int, my:Int)
    {
        val mouse = new Point(mx, my)
        disableDepth()
        color(1, 1, 1, 1)
        rootDrawFront(mouse, lastFrame)
        color(1, 1, 1, 1)
        enableDepth()

        if (debugDrawFrames)
        {
            translate(-position.x, -position.y, 0)
            def render(node:TNode)
            {
                if (!node.hidden)
                {
                    val f = node.frame
                    val absF = Rect(node.parent.convertPointToScreen(f.origin), f.size)
                    GuiDraw.drawLine(absF.x, absF.y, absF.x, absF.maxY, 3, EnumColour.RED.rgba())
                    GuiDraw.drawLine(absF.x, absF.maxY, absF.maxX, absF.maxY, 3, EnumColour.RED.rgba())
                    GuiDraw.drawLine(absF.maxX, absF.maxY, absF.maxX, absF.y, 3, EnumColour.RED.rgba())
                    GuiDraw.drawLine(absF.maxX, absF.y, absF.x, absF.y, 3, EnumColour.RED.rgba())
                }
                for (c <- node.children) render(c)
            }
            for (c <- children) render(c)
            translate(position.x, position.y, 0)
        }
    }
}
