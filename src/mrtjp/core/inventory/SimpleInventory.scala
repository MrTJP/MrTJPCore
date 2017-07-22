/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.inventory

import net.minecraft.item.ItemStack

abstract class SimpleInventory(size:Int, name:String, stackLimit:Int) extends TInventory
{
    def this(size:Int, lim:Int) = this(size, "", lim)
    def this(size:Int, name:String) = this(size, name, 64)
    def this(size:Int) = this(size, 64)

    override protected val storage  = new Array[ItemStack](size)

    override def getInventoryStackLimit = stackLimit

    override def getName = name

    override def markDirty(){}

    override def isEmpty = storage.forall(_.isEmpty)
}

abstract class ArrayWrapInventory(override protected val storage:Array[ItemStack], name:String, stackLimit:Int) extends TInventory
{
    override def getInventoryStackLimit = stackLimit

    override def markDirty(){}

    override def getName = name

    override def isEmpty = storage.forall(_.isEmpty)
}