/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import mrtjp.core.vec.{Point, Rect, Size}

class InventorySlotNode extends TNode
{
    var slotIdx = -1

    var size = Size(16, 16)
    override def frame = Rect(position, size)

    override def frameUpdate_Impl(mouse:Point, rframe:Float)
    {
        val root = getRoot
        val slot = root.inventorySlots.asInstanceOf[NodeContainer].slots(slotIdx)

        if (hidden || buildParentHierarchy(root).exists(_.hidden))
        {
            slot.xDisplayPosition = 9999
            slot.yDisplayPosition = 9999
        }
        else
        {
            val absPos = parent.convertPointTo(position, getRoot)
            slot.xDisplayPosition = absPos.x
            slot.yDisplayPosition = absPos.y
        }
    }
}