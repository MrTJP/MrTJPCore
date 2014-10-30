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

    def getSizeInventory = inv.getSizeInventory
    def getStackInSlot(i:Int) = inv.getStackInSlot(i)
    def decrStackSize(i:Int, j:Int) = inv.decrStackSize(i, j)
    def getStackInSlotOnClosing(i:Int) = inv.getStackInSlotOnClosing(i)
    def setInventorySlotContents(i:Int, itemstack:ItemStack) = inv.setInventorySlotContents(i, itemstack)
    def getInventoryName:String = inv.getInventoryName
    def hasCustomInventoryName = inv.hasCustomInventoryName
    def getInventoryStackLimit = inv.getInventoryStackLimit
    def markDirty() = inv.markDirty()
    def isUseableByPlayer(entityplayer:EntityPlayer) = inv.isUseableByPlayer(entityplayer)
    def openInventory() = inv.openInventory()
    def closeInventory() = inv.closeInventory()
    def isItemValidForSlot(i:Int, itemstack:ItemStack) = inv.isItemValidForSlot(i, itemstack)

    def createInv:SimpleInventory
}