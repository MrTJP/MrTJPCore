/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import mrtjp.core.vec.{Point, Rect, Size}

class SideSelectNode(x:Int, y:Int, w:Int, h:Int) extends TNode
{
    position = Point(x, y)
    var size = Size(w, h)
    override def frame = Rect(position, size)

    var sides = 0
    var exclusiveSides = false

    private val buttons = new Array[ButtonNode](6)

    {
        addChild(buildButton(0, 0, "u", 1))
        addChild(buildButton((w/5)*2, 0, "n", 2))
        addChild(buildButton(0, (h/5)*2, "w", 4))
        addChild(buildButton((w/5)*4, (h/5)*1*2, "e", 5))
        addChild(buildButton((w/5)*2, (h/5)*2*2, "s", 3))
        addChild(buildButton((w/5)*4, (h/5)*2*2, "d", 0))
    }

    private def buildButton(x:Int, y:Int, text:String, side:Int) =
    {
        val b = new MCButtonNode
        b.position = Point(x, y)
        b.size = size/3
        b.text = text
        b.clickDelegate = {() => onSidePresed(side)}
        buttons(side) = b
        b
    }

    def onSidePresed(side:Int)
    {
        val old = sides
        sides ^= 1<<side
        if (exclusiveSides) sides &= 1<<side
        if (old != sides) onSideChanged(side)

        for (s <- 0 until 6)
            buttons(s).mouseoverLock = (sides&1<<s) != 0
    }

    def onSideChanged(oldside:Int){}
}