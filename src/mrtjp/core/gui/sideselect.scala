/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import mrtjp.core.vec.{Point, Rect}

class SideSelect(x:Int, y:Int, w:Int, h:Int) extends TWidget
{
    override val bounds = new Rect().setMin(x, y).setWH(w, h)

    var sides = 0
    var exclusiveSides = false

    override def runInit_Impl()
    {
        add(new WidgetButtonMC(0, 0, w/3, h/3).setText("u").setAction("sidesel_"+1))
        add(new WidgetButtonMC((w/5)*2, 0, w/3, h/3).setText("n").setAction("sidesel_"+2))
        add(new WidgetButtonMC(0, (h/5)*2, w/3, h/3).setText("w").setAction("sidesel_"+4))
        add(new WidgetButtonMC((w/5)*4, (h/5)*1*2, w/3, h/3).setText("e").setAction("sidesel_"+5))
        add(new WidgetButtonMC((w/5)*2, (h/5)*2*2, w/3, h/3).setText("s").setAction("sidesel_"+3))
        add(new WidgetButtonMC((w/5)*4, (h/5)*2*2, w/3, h/3).setText("d").setAction("sidesel_"+0))
    }

    override def drawBack_Impl(mouse:Point, frame:Float)
    {
        super.drawBack_Impl(mouse, frame)
    }

    override def receiveMessage_Impl(message:String)
    {
        if (message.startsWith("sidesel_"))
            onSidePresed(Integer.parseInt(message.substring(8)))
    }

    def onSidePresed(side:Int)
    {
        val old = sides
        sides ^= 1<<side
        if (exclusiveSides) sides &= 1<<side
        if (old != sides) onSideChanged(side)
    }

    def onSideChanged(oldside:Int){}
}
