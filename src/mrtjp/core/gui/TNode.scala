/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.{FontRenderer, Gui}
import net.minecraft.client.renderer.texture.TextureManager
import org.lwjgl.opengl.GL11

trait TNode extends Gui
{
    var parent:TNode = null
    var children = Seq[TNode]()

    def frame = Rect(position, Size.zeroSize)
    var position = Point.zeroPoint
    var zPosition = 0.0
    var hidden = false
    var userInteractionEnabled = true

    def mcInst:Minecraft = Minecraft.getMinecraft
    def renderEngine:TextureManager = mcInst.renderEngine
    def fontRenderer:FontRenderer = mcInst.fontRenderer

    def isRoot = this.isInstanceOf[NodeGui]
    def getRoot:NodeGui =
    {
        def iterate(node:TNode):NodeGui = node match
        {
            case ng:NodeGui => ng
            case null => throw new Exception("Gui not found")
            case _ => iterate(node.parent)
        }
        iterate(this)
    }

    def buildParentHierarchy(to:TNode) =
    {
        var hierarchy = Seq[TNode]()
        def iterate(node:TNode)
        {
            hierarchy :+= node
            if (node.isRoot || node == to) return
            iterate(node.parent)
        }
        iterate(this)
        hierarchy
    }

    def isDecendantOf(someParent:TNode) =
        someParent != this && buildParentHierarchy(someParent).contains(someParent)

    def convertPointToScreen(p:Point) = getRoot.position+convertPointTo(p, getRoot)
    def convertPointFromScreen(p:Point) = convertPointFrom(p, getRoot)-getRoot.position

    def convertPointTo(p:Point, to:TNode):Point =
    {
        def fold(low:TNode, high:TNode)(op:(Point, TNode) => Point) =
            low.buildParentHierarchy(high).dropRight(1).foldLeft(p)(op)

        def convertUp(low:TNode, high:TNode) = fold(low, high){_+_.position}
        def convertDown(low:TNode, high:TNode) = fold(low, high){_-_.position}

        if (this == to) p
        else if (this.isDecendantOf(to)) convertUp(this, to)
        else if (to.isDecendantOf(this)) convertDown(to, this) //TODO sibling conversion by conv to screen, then conv from screen on other node
        else throw new Exception("Attempted to convert points between unrelated nodes.")
    }
    def convertPointFrom(p:Point, from:TNode):Point = from.convertPointTo(p, this)

    def convertRectTo(r:Rect, to:TNode):Rect = Rect(convertPointTo(r.origin, to), r.size)
    def convertRectFrom(r:Rect, from:TNode):Rect = Rect(convertPointFrom(r.origin, from), r.size)
    def convertRectToScreen(r:Rect):Rect = Rect(convertPointToScreen(r.origin), r.size)
    def convertRectFromScreen(r:Rect):Rect = Rect(convertPointFromScreen(r.origin), r.size)

    def calculateChildrenFrame:Rect =
    {
        val rect = if (children.isEmpty) Rect.zeroRect
            else children.filterNot(_.hidden).map(_.calculateAccumulatedFrame).reduceLeft(_ union _)
        Rect(convertPointTo(rect.origin, parent), rect.size)
    }

    def calculateAccumulatedFrame:Rect = frame.union(calculateChildrenFrame)

    def traceHit(absPoint:Point) =
    {
        val f = frame
        val af = Rect(parent.convertPointToScreen(f.origin), f.size)
        af.contains(absPoint)
    }

    def hitTest(point:Point):Seq[TNode] =
    {
        if (parent == null) throw new Exception("Cannot hittest a node without a parent.")
        if (isRoot) throw new Exception("Cannot hittest a root node.")

        var test = Seq.newBuilder[TNode]
        val ap = parent.convertPointToScreen(point)
        for (c <- getRoot.subTree(true))
            if (c.traceHit(ap)) test += c

        test.result().sortBy(_.zPosition).reverse
    }

    def rayTest(point:Point):Boolean =
    {
        val s = hitTest(point)
        s.nonEmpty && s.head == this
    }

    def subTree(activeOnly:Boolean = false) =
    {
        val s = Seq.newBuilder[TNode]
        def gather(children:Seq[TNode])
        {
            val ac = if (activeOnly) children.filter(c => !c.hidden && c.userInteractionEnabled) else children
            s ++= ac
            for (c <- ac) gather(c.children)
        }
        if (!activeOnly || (!hidden && userInteractionEnabled)) gather(children)
        s.result()
    }

    def pushZTo(z:Double)
    {
        pushZBy(z-zPosition)
    }

    def pushZBy(z:Double)
    {
        for (c <- subTree():+this)
            c.zPosition += z
    }

    def addChild(w:TNode) =
    {
        w.parent = this
        children :+= w
        w.onAddedToParent_Impl()
    }

    def removeFromParent()
    {
        parent.children = parent.children.filterNot(_ == this)
        parent = null
    }

    def childrenByZ = children.sortBy(_.zPosition)
    def familyByZ = (Seq(this)++children).sortBy(_.zPosition)

    protected[gui] final def update()
    {
        update_Impl()
        for (c <- childrenByZ) c.update()
    }

    protected[gui] final def frameUpdate(mouse:Point, rframe:Float)
    {
        frameUpdate_Impl(mouse, rframe)
        for (c <- childrenByZ) c.frameUpdate(mouse-position, rframe)
    }

    private final def operate2(consumed:Boolean)(self:(Boolean) => Boolean)(sub:(TNode, Boolean) => Boolean) =
    {
        familyByZ.reverse.foldLeft(consumed)((c, w) => (if (w == this) self(c) else sub(w, c)) || c)
    }

    protected[gui] def mouseClicked(p:Point, button:Int, consumed:Boolean):Boolean =
    {
        if (hidden || !userInteractionEnabled) return false
        val dp = p-position
        operate2(consumed){mouseClicked_Impl(p, button, _)}{_.mouseClicked(dp, button, _)}
    }

    protected[gui] def mouseReleased(p:Point, button:Int, consumed:Boolean):Boolean =
    {
        if (hidden || !userInteractionEnabled) return false
        val dp = p-position
        operate2(consumed){mouseReleased_Impl(p, button, _)}{_.mouseReleased(dp, button, _)}
    }

    protected[gui] def mouseDragged(p:Point, button:Int, time:Long, consumed:Boolean):Boolean =
    {
        if (hidden || !userInteractionEnabled) return false
        val dp = p-position
        operate2(consumed){mouseDragged_Impl(p, button, time, _)}{_.mouseDragged(dp, button, time, _)}
    }

    protected[gui] def mouseScrolled(p:Point, dir:Int, consumed:Boolean):Boolean =
    {
        if (hidden || !userInteractionEnabled) return false
        val dp = p-position
        operate2(consumed){mouseScrolled_Impl(p, dir, _)}{_.mouseScrolled(dp, dir, _)}
    }

    protected[gui] def keyPressed(ch:Char, keycode:Int, consumed:Boolean):Boolean =
    {
        if (hidden || !userInteractionEnabled) return false
        operate2(consumed){keyPressed_Impl(ch, keycode, _)}{_.keyPressed(ch, keycode, _)}
    }

    protected[gui] def drawBack(mouse:Point, rframe:Float)
    {
        if (!hidden)
        {
            val dp = mouse-position
            for (n <- familyByZ)
            {
                if (n == this) drawBack_Impl(mouse, rframe)
                else
                {
                    translateTo()
                    n.drawBack(dp, rframe)
                    translateFrom()
                }
            }
        }
    }
    protected[gui] def rootDrawBack(mouse:Point, rframe:Float)
    {
        if (!hidden)
        {
            translateTo()
            val dp = mouse-position
            for (n <- familyByZ)
            {
                if (n == this) drawBack_Impl(mouse, rframe)
                else n.drawBack(dp, rframe)
            }
            translateFrom()
        }
    }

    protected[gui] def drawFront(mouse:Point, rframe:Float)
    {
        if (!hidden)
        {
            val dp = mouse-position
            for (n <- familyByZ)
            {
                if (n == this) drawFront_Impl(mouse, rframe)
                else
                {
                    translateTo()
                    n.drawFront(dp, rframe)
                    translateFrom()
                }
            }
        }
    }
    protected[gui] def rootDrawFront(mouse:Point, rframe:Float)
    {
        if (!hidden)
        {
            val dp = mouse-position
            for (n <- familyByZ)
            {
                if (n == this) drawFront_Impl(mouse, rframe)
                else n.drawFront(dp, rframe)
            }
        }
    }

    protected[gui] def translateTo(){GL11.glTranslated(position.x, position.y, 0)}//zPosition-(if (parent == null) 0 else parent.zPosition))}
    protected[gui] def translateFrom(){GL11.glTranslated(-position.x, -position.y, 0)}// -(zPosition-(if (parent == null) 0 else parent.zPosition)))}

    protected[gui] def translateToScreen()
    {
        val Point(sx, sy) = parent.convertPointToScreen(Point.zeroPoint)
        GL11.glTranslated(-sx, -sy, 0)
    }
    protected[gui] def translateFromScreen()
    {
        val Point(sx, sy) = parent.convertPointToScreen(Point.zeroPoint)
        GL11.glTranslated(sx, sy, 0)
    }

    @deprecated(message = "deprecated. Use delegation")
    protected final def startMessageChain(message:String)
    {
        if (!isRoot) parent.receiveMessage(message)
    }
    @deprecated(message = "deprecated. Use delegation")
    protected final def receiveMessage(message:String)
    {
        receiveMessage_Impl(message)
        if (!isRoot) parent.receiveMessage(message)
    }

    /** IMPLEMENTATION OVERRIDES **/

    /**
     * Called every tick from the main game loop.
     */
    def update_Impl(){}

    /**
     * Called every frame before background draw call
     * @param mouse The current position of the mouse, relative to the parent.
     * @param rframe The partial frame until the next frame.
     */
    def frameUpdate_Impl(mouse:Point, rframe:Float){}

    /**
     * Called when this node is added to another as a child. Should be used
     * as the main override point for initialization.
     */
    def onAddedToParent_Impl(){}

    /**
     * Called when the mouse button is clicked.
     *
     * @param p The current position of the mouse, relative to the parent.
     * @param button The button that was clicked. 0 is left button, 1 is right.
     * @param consumed If another node has consumed this event.
     * @return If this node has consumed this event.
     */
    def mouseClicked_Impl(p:Point, button:Int, consumed:Boolean) = false

    /**
     * Called when the mouse button is released.
     * @param p The current position of the mouse, relative to the parent.
     * @param button The button that was released. 0 is left button, 1 is right.
     * @param consumed If another node has consumed this event.
     * @return If this node has consumed this event.
     */
    def mouseReleased_Impl(p:Point, button:Int, consumed:Boolean) = false

    /**
     * Called constantly while the mouse is held down.
     * @param p The current position of the mouse, relative to the parent.
     * @param button The button that is currently held down. 0 is left, 1 is right
     * @param time Amount of time the button has been held down for.
     * @param consumed If another node has consumed this event.
     * @return If this node has consumed this event.
     */
    def mouseDragged_Impl(p:Point, button:Int, time:Long, consumed:Boolean) = false

    /**
     * Called when the mouse wheel is scrolled.
     * @param p The current position of the mouse, relative to the parent.
     * @param dir The direction of scroll. Negative for down, positive for up.
     * @param consumed If another node has consumed this event.
     * @return If this node has consumed this event.
     */
    def mouseScrolled_Impl(p:Point, dir:Int, consumed:Boolean) = false

    /**
     * Called when a key is pressed on the keyboard.
     * @param c The charecter that was pressed.
     * @param keycode The keycode for the button that was pressed.
     * @param consumed If another node has consumed this event.
     * @return If this node has consumed this event.
     */
    def keyPressed_Impl(c:Char, keycode:Int, consumed:Boolean) = false

    /**
     * Called to draw the background.
     * All drawing is done relative to the parent, as GL11 is translated to the
     * parents position during this operation.  However, for the root node,
     * drawing is relevant to itself.
     * @param mouse The current position of the mouse, relative to the parent.
     * @param rframe The partial frame until the next frame.
     */
    def drawBack_Impl(mouse:Point, rframe:Float){}

    /**
     * Called to draw the foreground.
     * All drawing is done relative to the parent, as GL11 is translated to the
     * parents position during this operation.  However, for the root node,
     * drawing is relevant to itself.
     * @param mouse The current position of the mouse, relative to the parent.
     * @param rframe The partial frame until the next frame.
     */
    def drawFront_Impl(mouse:Point, rframe:Float){}

    /**
     * Called when a subnode sends a message. This message is relayed to all
     * supernodes one by one and stops at the root node.
     *
     * @param message The message that was sent by a subnode using
     *                startMessageChain
     */
    @deprecated(message = "deprecated. Use delegation")
    def receiveMessage_Impl(message:String){}
}