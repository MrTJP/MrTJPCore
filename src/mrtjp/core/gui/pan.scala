/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.vec.{Point, Rect, Size, Vec2}

class PanNode extends TNode
{
    var size = Size.zeroSize
    override def frame = Rect(position, size)

    var clampSlack = 0

    var scrollModifier = Vec2(1, 1)
    var scrollBarThickness = 4
    var scrollBarBGColour = Colors.LIGHT_GREY.argb(0x66)
    var scrollBarColour = Colors.GREY.argb(0x99)
    var scrollBarVertical = true
    var scrollBarHorizontal = true

    var dragTestFunction = {() => false}
    var panDelegate = {() => }

    var debugShowClampBox = false

    private var cFrame = Rect.zeroRect
    private var mouseDown = false
    private var mouseDownRight = false
    private var mouseDownBelow = false
    private var lastMousePos = Point.zeroPoint

    private var raytestMode = 0

    override def frameUpdate_Impl(mouse:Point, rframe:Float)
    {
        cFrame = calculateChildrenFrame
        val delta = mouse-lastMousePos
        lastMousePos = mouse

        if (mouseDownRight || mouseDownBelow)
        {
            val sf2 = size.vectorize/cFrame.size.vectorize
            val modVec = if (mouseDownRight) Vec2.up else Vec2.left
            panChildren(delta.vectorize*scrollModifier/sf2*modVec)
        }
        else if (mouseDown)
        {
            panChildren(delta.vectorize*scrollModifier)
        }
        else
        {
            //x disp
            val l = 0+clampSlack
            val lc = if (cFrame.size.width > size.width) cFrame.origin.x max l else cFrame.origin.x min l
            val ld = l-lc
            val r = size.width-clampSlack
            val rc = if (cFrame.size.width > size.width) cFrame.maxX min r else cFrame.maxX max r
            val rd = r-rc

            //y disp
            val t = 0+clampSlack
            val tc = if (cFrame.size.height > size.height) cFrame.origin.y max t else cFrame.origin.y min t
            val td = t-tc
            val b = size.height-clampSlack
            val bc = if (cFrame.size.height > size.height) cFrame.maxY min b else cFrame.maxY max b
            val bd = b-bc

            panChildren(Vec2(ld+rd, td+bd)*0.1*scrollModifier)
        }
    }

    def panChildren(d:Vec2)
    {
        val d2 = d
        if (d2 != Vec2.zeroVec)
        {
            for (c <- children) c.position = Point(c.position.vectorize+d2)
            panDelegate()
        }
    }

    override def drawBack_Impl(mouse:Point, rframe:Float)
    {
        drawScrollBars()
    }

    override def drawFront_Impl(mouse:Point, rframe:Float){}

    private def drawScrollBars()
    {
        if (scrollBarVertical)
        {
            GuiDraw.drawRect(position.x+size.width-scrollBarThickness, position.y, scrollBarThickness, size.height, scrollBarBGColour)
            val s = getScrollBarRight
            GuiDraw.drawRect(s.x, s.y, s.width, s.height, scrollBarColour)
        }
        if (scrollBarHorizontal)
        {
            GuiDraw.drawRect(position.x, position.y+size.height-scrollBarThickness, size.width, scrollBarThickness, scrollBarBGColour)
            val s = getScrollBarBelow
            GuiDraw.drawRect(s.x, s.y, s.width, s.height, scrollBarColour)
        }
    }

    def getScrollBarRight:Rect =
    {
        if (cFrame.size.height == 0) return Rect.zeroRect
        val sf = size.height/cFrame.height.toDouble
        val s = Size(scrollBarThickness, (size.height*sf).toInt)
        val p = Point(position.x+size.width-scrollBarThickness, ((position.y-cFrame.y)*sf).toInt)
        Rect(p, s)
    }

    def getScrollBarBelow:Rect =
    {
        if (cFrame.size.width == 0) return Rect.zeroRect
        val sf = size.width/cFrame.width.toDouble
        val s = Size((size.width*sf).toInt, scrollBarThickness)
        val p = Point(((position.x-cFrame.x)*sf).toInt, position.y+size.height-scrollBarThickness)
        Rect(p, s)
    }

    override def mouseClicked_Impl(p:Point, button:Int, consumed:Boolean):Boolean =
    {
        def doRayTest(mode:Int) =
        {
            raytestMode = mode
            val hit = rayTest(p)
            raytestMode = 0
            hit
        }

        if (!consumed)
        {
            if (scrollBarVertical && doRayTest(1)) mouseDownRight = true
            else if (scrollBarHorizontal && doRayTest(2)) mouseDownBelow = true
            else if (doRayTest(3)) mouseDown = true
            else return false

            lastMousePos = p
            true
        }
        else false
    }

    override def mouseReleased_Impl(p:Point, button:Int, consumed:Boolean) =
    {
        mouseDown = false
        mouseDownRight = false
        mouseDownBelow = false
        false
    }

    override def traceHit(absPoint:Point) = raytestMode match
    {
        case 0 => mouseDown || mouseDownRight || mouseDownBelow
        case 1 => getScrollBarRight.contains(parent.convertPointFromScreen(absPoint))
        case 2 => getScrollBarBelow.contains(parent.convertPointFromScreen(absPoint))
        case 3 => dragTestFunction()
    }
}