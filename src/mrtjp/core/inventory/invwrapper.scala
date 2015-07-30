/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.inventory

import codechicken.lib.vec.BlockCoord
import mrtjp.core.item.{ItemEquality, ItemKey}
import mrtjp.core.world.WorldLib
import net.minecraft.inventory.{IInventory, ISidedInventory, InventoryLargeChest}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.world.World

object InvWrapper
{
    var wrappers = Seq[IInvWrapperRegister]()

    def register(w:IInvWrapperRegister)
    {
        for (wr <- wrappers) if (wr.wrapperID == w.wrapperID) return
        wrappers :+= w
    }

    def wrap(inv:IInventory):InvWrapper =
    {
        for (w <- wrappers) if (w.matches(inv)) return w.create(inv)
        new VanillaWrapper(inv)
    }

    def areItemsStackable(stack1:ItemStack, stack2:ItemStack):Boolean =
    {
        stack1 == null || stack2 == null || areItemsSame(stack1, stack2) && stack1.isStackable && stack2.isStackable
    }

    def areItemsSame(stack1:ItemStack, stack2:ItemStack):Boolean =
    {
        if (stack1 == null || stack2 == null) return stack1 == stack2
        stack1.getItem == stack2.getItem && stack2.getItemDamage == stack1.getItemDamage && ItemStack.areItemStackTagsEqual(stack2, stack1)
    }

    def getInventory(world:World, wc:BlockCoord):IInventory =
    {
        val inv = WorldLib.getTileEntity(world, wc, classOf[IInventory])
        if (inv.isInstanceOf[TileEntityChest])
        {
            val chest = inv.asInstanceOf[TileEntityChest]

            var lower:TileEntityChest = null
            var upper:TileEntityChest = null
            if (chest.adjacentChestXNeg != null)
            {
                upper = chest.adjacentChestXNeg
                lower = chest
            }
            if (chest.adjacentChestXPos != null)
            {
                upper = chest
                lower = chest.adjacentChestXPos
            }
            if (chest.adjacentChestZNeg != null)
            {
                upper = chest.adjacentChestZNeg
                lower = chest
            }
            if (chest.adjacentChestZPos != null)
            {
                upper = chest
                lower = chest.adjacentChestZPos
            }
            if (lower != null && upper != null) return new HashableLargeChest("Large Chest", upper, lower)
            return inv
        }
        inv
    }
}

class HashableLargeChest(name:String, val inv1:IInventory, val inv2:IInventory) extends InventoryLargeChest(name, inv1, inv2)
{
    override def hashCode = inv1.hashCode^inv2.hashCode

    override def equals(other:Any) = other match
    {
        case that:HashableLargeChest => inv1 == that.inv1 && inv2 == that.inv2
        case _ => false
    }
}

trait IInvWrapperRegister
{
    /**
     * Unique ID for each type of wrapper.
     */
    def wrapperID:String

    /**
     * Returns true if this wrapper should be used for the given inventory.
     */
    def matches(inv:IInventory):Boolean

    /**
     * Returns a new instance of this wrapper.
     */
    def create(inv:IInventory):InvWrapper
}

abstract class InvWrapper(val inv:IInventory)
{
    val sidedInv = inv match
    {
        case inv2:ISidedInventory => inv2
        case _ => null
    }
    var side = -1

    var slots:Seq[Int] = (0 until inv.getSizeInventory).toSeq

    var hidePerSlot = false
    var hidePerType = false

    var eq = new ItemEquality

    var internalMode = false

    def setSlotsFromSide(s:Int) =
    {
        if (sidedInv != null)
        {
            side = s
            slots = sidedInv.getAccessibleSlotsFromSide(s)
        }
        else setSlotsAll()
        this
    }

    def setSlotsFromRange(r:Range) =
    {
        side = -1
        slots = r
        this
    }

    def setSlotsAll() =
    {
        side = -1
        slots = (0 until inv.getSizeInventory).toSeq
        this
    }

    def setSlotSingle(s:Int) =
    {
        side = -1
        slots = Seq(s)
        this
    }

    def setMatchOptions(meta:Boolean, nbt:Boolean, ore:Boolean) =
    {
        eq.matchMeta = meta
        eq.matchNBT = nbt
        eq.matchOre = ore
        this
    }

    def setDamageGroup(percent:Int) =
    {
        eq.damageGroup = percent
        this
    }

    def setHidePerSlot(flag:Boolean) =
    {
        hidePerSlot = flag
        if (flag) hidePerType = false
        this
    }

    def setHidePerType(flag:Boolean) =
    {
        hidePerType = flag
        if (flag) hidePerSlot = false
        this
    }

    def setInternalMode(flag:Boolean) =
    {
        internalMode = flag
        this
    }

    /**
     * Get a count for how many items of this type can be shoved into the
     * inventory.
     *
     * @param item The item to count free space for. Not manipulated in any way.
     * @return The number of those items this inventory can still take.
     */
    def getSpaceForItem(item:ItemKey):Int

    /**
     * Check if at least one of this item can fit. Failfast for
     * getSpaceForItem
     * @param item The item to count free space for. Not manipulated in any way.
     * @return True if one of these items can fit
     */
    def hasSpaceForItem(item:ItemKey):Boolean

    /**
     * Counts how many of those items this inventory contains.
     *
     * @param item The item to count. Not manipulated in any way.
     * @return The number of those items this inventory contains.
     */
    def getItemCount(item:ItemKey):Int

    /**
     * Returns if the given item is in the inventory somewhere. Failfast of
     * getItemCount
     *
     * @param item the item. Not manipulated in any way.
     * @return
     */
    def hasItem(item:ItemKey):Boolean

    /**
     * Inject the ItemStack into the inventory, starting with merging, then to
     * empty slots.
     *
     * @param item The item to try and merge. Not manipulated in any way.
     * @param toAdd Amount to try to add.
     * @return The number of items that were merged in, between 0 and toAdd.
     */
    def injectItem(item:ItemKey, toAdd:Int):Int

    /**
     * Extract the item a specified number of times.
     *
     * @param item Item to extract from inventory. Not manipulated in any way.
     * @param toExtract Amount to try to extract.
     * @return Amount extracted, between 0 and toExtract.
     */
    def extractItem(item:ItemKey, toExtract:Int):Int

    /**
     * Return an ordered map of all available [ItemStack, Amount] in the
     * inventory. The actual inventory is not manipulated.
     *
     * @return
     */
    def getAllItemStacks:Map[ItemKey, Int]

    protected def canInsertItem(slot:Int, item:ItemStack):Boolean =
    {
        if (internalMode) return true
        if (side < 0) inv.isItemValidForSlot(slot, item) else sidedInv.canInsertItem(slot, item, side)
    }

    protected def canExtractItem(slot:Int, item:ItemStack):Boolean =
    {
        if (internalMode) return true
        if (side < 0) inv.isItemValidForSlot(slot, item) else sidedInv.canExtractItem(slot, item, side)
    }
}

trait TDefWrapHandler extends InvWrapper
{
    override def getSpaceForItem(item:ItemKey):Int =
    {
        var space = 0
        val item2 = item.testStack
        val slotStackLimit = math.min(inv.getInventoryStackLimit, item.getMaxStackSize)
        for (slot <- slots)
        {
            val s = inv.getStackInSlot(slot)
            if (canInsertItem(slot, item2))
            {
                if (s == null) space += slotStackLimit
                else if (InvWrapper.areItemsStackable(s, item2)) space += slotStackLimit-s.stackSize
            }
        }
        space
    }

    override def hasSpaceForItem(item:ItemKey):Boolean =
    {
        val item2 = item.testStack
        val slotStackLimit = math.min(inv.getInventoryStackLimit, item2.getMaxStackSize)
        for (slot <- slots)
        {
            val s = inv.getStackInSlot(slot)
            if (canInsertItem(slot, item2))
            {
                if (s == null) return true
                else if (InvWrapper.areItemsStackable(s, item2) && slotStackLimit-s.stackSize > 0) return true
            }
        }
        false
    }

    override def getItemCount(item:ItemKey) =
    {
        var count = 0

        var first = true

        for (slot <- slots)
        {
            val inSlot = inv.getStackInSlot(slot)
            if (inSlot != null && eq.matches(item, ItemKey.get(inSlot)))
            {
                val toAdd = inSlot.stackSize-(if (hidePerSlot || hidePerType && first) 1 else 0)
                first = false
                count += toAdd
            }
        }
        count
    }

    override def hasItem(item:ItemKey):Boolean =
    {
        for (slot <- slots)
        {
            val inSlot = inv.getStackInSlot(slot)
            if (inSlot != null && eq.matches(item, ItemKey.get(inSlot))) return true
        }

        false
    }

    override def injectItem(item:ItemKey, toAdd:Int):Int =
    {
        var itemsLeft = toAdd
        val slotStackLimit = math.min(inv.getInventoryStackLimit, item.getMaxStackSize)

        for (pass <- Seq(0, 1)) for (slot <- slots) if (canInsertItem(slot, item.testStack))
        {
            val inSlot = inv.getStackInSlot(slot)

            if (inSlot != null && InvWrapper.areItemsStackable(item.testStack, inSlot))
            {
                val fit = math.min(slotStackLimit-inSlot.stackSize, itemsLeft)
                inSlot.stackSize += fit
                itemsLeft -= fit
                inv.setInventorySlotContents(slot, inSlot)
            }
            else if (pass == 1 && inSlot == null)
            {
                val toInsert = item.makeStack(math.min(inv.getInventoryStackLimit, itemsLeft))
                itemsLeft -= toInsert.stackSize
                inv.setInventorySlotContents(slot, toInsert)
            }

            if (itemsLeft == 0) return toAdd
        }

        toAdd-itemsLeft
    }

    override def extractItem(item:ItemKey, toExtract:Int):Int =
    {
        if (toExtract <= 0) return 0
        var left = toExtract
        var first = true
        for (slot <- slots) if (canExtractItem(slot, item.testStack))
        {
            val inSlot = inv.getStackInSlot(slot)
            if (inSlot != null && eq.matches(item, ItemKey.get(inSlot))) //TODO extraction shouldnt rely on eq matches..?
            {
                left -= inv.decrStackSize(slot, math.min(left, inSlot.stackSize-(if (hidePerSlot || hidePerType&&first) 1 else 0))).stackSize
                first = false
            }
            if (left <= 0) return toExtract
        }
        toExtract - left
    }

    override def getAllItemStacks =
    {
        var items = Map[ItemKey, Int]()
        for (slot <- slots)
        {
            val inSlot = inv.getStackInSlot(slot)
            if (inSlot != null)
            {
                val key = ItemKey.get(inSlot)
                val stackSize = inSlot.stackSize-(if (hidePerSlot) 1 else 0)
                val currentSize = items.getOrElse(key, 0)

                if (!items.keySet.contains(key)) items += key -> (stackSize-(if (hidePerType) 1 else 0))
                else items += key -> (currentSize+stackSize)
            }
        }
        items
    }
}

class VanillaWrapper(inv:IInventory) extends InvWrapper(inv) with TDefWrapHandler