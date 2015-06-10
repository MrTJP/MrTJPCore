/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.inventory

import mrtjp.core.world.WorldLib
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraft.world.World

trait TInventory extends IInventory
{
    def size:Int
    def name:String
    def stackLimit = 64

    private val storage = new Array[ItemStack](size)

    override def getSizeInventory = storage.length
    override def getInventoryStackLimit = stackLimit
    override def hasCustomInventoryName = true
    override def getInventoryName = name
    override def isUseableByPlayer(player:EntityPlayer) = true
    override def isItemValidForSlot(slot:Int, item:ItemStack) = true

    override def openInventory(){}
    override def closeInventory(){}

    override def getStackInSlot(slot:Int) = storage(slot)
    override def getStackInSlotOnClosing(slot:Int):ItemStack =
    {
        val stack = storage(slot)
        if (stack == null) return null
        storage(slot) = null
        markDirty()
        stack
    }

    override def setInventorySlotContents(slot:Int, item:ItemStack)
    {
        storage(slot) = item
        markDirty()
    }

    override def decrStackSize(slot:Int, count:Int):ItemStack =
    {
        val stack = storage(slot)
        if (stack == null) return null

        if (stack.stackSize > count)
        {
            val out = stack.splitStack(count)
            markDirty()
            out
        }
        else
        {
            val out = stack
            storage(slot) = null
            markDirty()
            out
        }
    }

    def loadInv(tag:NBTTagCompound){ loadInv(tag, name) }
    def loadInv(tag:NBTTagCompound, prefix:String)
    {
        val tag1 = tag.getTagList(prefix+"items", 10)
        for (i <- 0 until tag1.tagCount())
        {
            val tag2 = tag1.getCompoundTagAt(i)

            val index = tag2.getInteger("index")
            if (storage.isDefinedAt(index)) storage(index) = ItemStack.loadItemStackFromNBT(tag2)
        }
    }

    def saveInv(tag:NBTTagCompound){ saveInv(tag, name) }
    def saveInv(tag:NBTTagCompound, prefix:String)
    {
        val itemList = new NBTTagList
        for (i <- 0 until storage.length) if (storage(i) != null && storage(i).stackSize > 0)
        {
            val tag2 = new NBTTagCompound
            tag2.setInteger("index", i)
            storage(i).writeToNBT(tag2)
            itemList.appendTag(tag2)
        }

        tag.setTag(prefix+"items", itemList)
        tag.setInteger(prefix+"itemsCount", storage.length)
    }

    def dropInvContents(w:World, x:Int, y:Int, z:Int)
    {
        if (w.isRemote) return
        for (i <- storage) if (i != null) WorldLib.dropItem(w, x, y, z, i)
        for (i <- 0 until storage.length) storage(i) = null
        markDirty()
    }
}