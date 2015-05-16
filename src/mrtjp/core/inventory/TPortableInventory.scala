/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.inventory

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait TPortableInventory extends IInventory
{
    val inv = createInv

    override def getSizeInventory = inv.getSizeInventory
    override def getStackInSlot(i:Int) = inv.getStackInSlot(i)
    override def decrStackSize(i:Int, j:Int) = inv.decrStackSize(i, j)
    override def getStackInSlotOnClosing(i:Int) = inv.getStackInSlotOnClosing(i)
    override def setInventorySlotContents(i:Int, itemstack:ItemStack) = inv.setInventorySlotContents(i, itemstack)
    override def getInventoryName:String = inv.getInventoryName
    override def hasCustomInventoryName = inv.hasCustomInventoryName
    override def getInventoryStackLimit = inv.getInventoryStackLimit
    abstract override def markDirty(){super.markDirty(); inv.markDirty()}
    override def isUseableByPlayer(entityplayer:EntityPlayer) = inv.isUseableByPlayer(entityplayer)
    override def openInventory() = inv.openInventory()
    override def closeInventory() = inv.closeInventory()
    override def isItemValidForSlot(i:Int, itemstack:ItemStack) = inv.isItemValidForSlot(i, itemstack)

    def createInv:SimpleInventory
}