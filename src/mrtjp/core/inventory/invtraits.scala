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
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentString
import net.minecraft.world.World

trait TInventory extends IInventory
{
    protected val storage:Array[ItemStack]

    override def getSizeInventory = storage.length

    override def isEmpty = storage.forall(_.isEmpty)

    override def hasCustomName = true
    override def getDisplayName = new TextComponentString(getName)

    override def isUsableByPlayer(player:EntityPlayer) = true
    override def isItemValidForSlot(slot:Int, item:ItemStack) = true

    override def openInventory(player:EntityPlayer){}
    override def closeInventory(player:EntityPlayer){}

    override def getStackInSlot(slot:Int) = storage(slot)

    override def setInventorySlotContents(slot:Int, item:ItemStack)
    {
        storage(slot) = item
        markDirty()
    }

    override def removeStackFromSlot(slot:Int) =
    {
        val stack = storage(slot)
        if (stack != null) {
            storage(slot) = null
            markDirty()
        }
        stack
    }

    override def decrStackSize(slot:Int, count:Int):ItemStack =
    {
        val stack = storage(slot)
        if (stack == null) return null

        if (stack.getCount > count)
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

    override def clear()
    {
        for (i <- 0 until storage.length)
            storage(i) = null
    }

    override def getFieldCount = 0
    override def getField(id:Int) = 0
    override def setField(id:Int, value:Int){}

    def loadInv(tag:NBTTagCompound){ loadInv(tag, getName) }
    def loadInv(tag:NBTTagCompound, prefix:String)
    {
        val tag1 = tag.getTagList(prefix+"items", 10)
        for (i <- 0 until tag1.tagCount())
        {
            val tag2 = tag1.getCompoundTagAt(i)

            val index = tag2.getInteger("index")
            if (storage.isDefinedAt(index))
                storage(index) = new ItemStack(tag2)
        }
    }

    def saveInv(tag:NBTTagCompound){ saveInv(tag, getName) }
    def saveInv(tag:NBTTagCompound, prefix:String)
    {
        val itemList = new NBTTagList
        for (i <- 0 until storage.length) if (storage(i) != null && storage(i).getCount > 0)
        {
            val tag2 = new NBTTagCompound
            tag2.setInteger("index", i)
            storage(i).writeToNBT(tag2)
            itemList.appendTag(tag2)
        }

        tag.setTag(prefix+"items", itemList)
        tag.setInteger(prefix+"itemsCount", storage.length)
    }

    def dropInvContents(w:World, pos:BlockPos)
    {
        for (i <- storage) if (i != null) WorldLib.dropItem(w, pos, i)
        for (i <- 0 until storage.length) storage(i) = null
        markDirty()
    }
}
