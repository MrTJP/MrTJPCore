/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import java.util.{List => JList}

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.inventory.InvWrapper
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{Container, ICrafting, IInventory, Slot}
import net.minecraft.item.ItemStack

import scala.collection.JavaConversions._
import scala.collection.mutable.{Buffer => MBuffer}

class NodeContainer extends Container
{
    var startWatchDelegate = {(p:EntityPlayer) => }
    var stopWatchDelegate = {(p:EntityPlayer) => }
    var slotChangeDelegate = {(slot:Int) => }

    def slots:MBuffer[TSlot3] = asScalaBuffer[TSlot3](inventorySlots.asInstanceOf[JList[TSlot3]])

    override def canInteractWith(player:EntityPlayer) = true

    override def canDragIntoSlot(slot:Slot) = slot match
    {
        case s:TSlot3 => !s.phantomSlot
        case _ => super.canDragIntoSlot(slot)
    }

    override def addSlotToContainer(slot:Slot) =
    {
        if (!slot.isInstanceOf[TSlot3])
            throw new IllegalArgumentException("NodeContainers can only except slots of type Slot3")
        super.addSlotToContainer(slot)

        slot.asInstanceOf[TSlot3].slotChangeDelegate2 =
                {() => slotChangeDelegate(slot.slotNumber)}
        slot
    }

    @SideOnly(Side.CLIENT)
    def addPlayerInv(x:Int, y:Int){addPlayerInv(Minecraft.getMinecraft.thePlayer, x, y)}
    def addPlayerInv(player:EntityPlayer, x:Int, y:Int)
    {
        var next = 0
        def up() = {next+=1;next-1}

        for ((x, y) <- GuiLib.createSlotGrid(x, y+58, 9, 1, 0, 0))
            addSlotToContainer(new Slot3(player.inventory, up(), x, y)) //hotbar

        for ((x, y) <- GuiLib.createSlotGrid(x, y, 9, 3, 0, 0))
            addSlotToContainer(new Slot3(player.inventory, up(), x, y)) //slots
    }

    override def addCraftingToCrafters(c:ICrafting)
    {
        super.addCraftingToCrafters(c)
        c match {
            case p:EntityPlayer if !p.worldObj.isRemote =>
                startWatchDelegate(p)
            case _ =>
        }
    }

    override def removeCraftingFromCrafters(c :ICrafting)
    {
        super.removeCraftingFromCrafters(c)
        c match {
            case p:EntityPlayer if !p.worldObj.isRemote =>
                stopWatchDelegate(p)
            case _ =>
        }
    }

    override def onContainerClosed(p:EntityPlayer)
    {
        super.onContainerClosed(p)
        if (!p.worldObj.isRemote)
            stopWatchDelegate(p)
    }

    override def slotClick(id:Int, mouse:Int, shift:Int, player:EntityPlayer):ItemStack =
    {
        try { //Ignore exceptions raised from client-side only slots that wont be found here. To be removed.
            if (slots.isDefinedAt(id))
            {
                val slot = slots(id)
                if (slot.phantomSlot) return handleGhostClick(slot, mouse, shift, player)
            }
            super.slotClick(id, mouse, shift, player)
        } catch {
            case e:Exception => null
        }
    }

    private def handleGhostClick(slot:TSlot3, mouse:Int, shift:Int, player:EntityPlayer):ItemStack =
    {
        val inSlot = slot.getStack
        val inCursor = player.inventory.getItemStack
        if (inCursor != null && !slot.isItemValid(inCursor)) return inCursor

        val stackable = InvWrapper.areItemsStackable(inSlot, inCursor)
        if (stackable)
        {
            if (inSlot != null && inCursor == null) slot.putStack(null)
            else if (inSlot == null && inCursor != null)
            {
                val newStack = inCursor.copy
                newStack.stackSize = if (mouse == 0) math.min(inCursor.stackSize, slot.getSlotStackLimit) else 1
                slot.putStack(newStack)
            }
            else if (inSlot != null)
            {
                val toAdd = if (shift == 1) 10 else 1
                if (mouse == 0) inSlot.stackSize = math.min(slot.getSlotStackLimit, inSlot.stackSize+toAdd)
                else if (mouse == 1) inSlot.stackSize = math.max(0, inSlot.stackSize-toAdd)
                if (inSlot.stackSize > 0) slot.putStack(inSlot)
                else slot.putStack(null)
            }
        }
        else
        {
            val newStack = inCursor.copy
            newStack.stackSize = if (mouse == 0) math.min(inCursor.stackSize, slot.getSlotStackLimit) else 1
            slot.putStack(newStack)
        }

        inCursor
    }

    override def transferStackInSlot(player:EntityPlayer, i:Int):ItemStack =
    {
        var stack:ItemStack = null
        if (slots.isDefinedAt(i))
        {
            val slot = slots(i)
            if (slot != null && slot.getHasStack)
            {
                stack = slot.getStack
                val manipStack = stack.copy

                if (!doMerge(manipStack, i) || stack.stackSize == manipStack.stackSize) return null

                if (manipStack.stackSize <= 0) slot.putStack(null)
                else slot.putStack(manipStack)

                slot.onPickupFromSlot(player, stack)
            }
        }
        stack
    }

    def doMerge(stack:ItemStack, from:Int) =
    {
        if (slots.size-36 until slots.size contains from) tryMergeItemStack(stack, 0, slots.size-36, false)
        else tryMergeItemStack(stack, slots.size-36, slots.size, false)
    }

    def tryMergeItemStack(stack:ItemStack, start:Int, end:Int, reverse:Boolean) =
    {
        var flag1 = false
        var k = if(reverse) end-1 else start

        var slot:TSlot3 = null
        var inslot:ItemStack = null
        if(stack.isStackable)
        {
            while(stack.stackSize > 0 && (!reverse && k < end || reverse && k >= start))
            {
                slot = slots(k)
                inslot = slot.getStack
                if (!slot.phantomSlot && inslot != null && inslot.getItem == stack.getItem &&
                        (!stack.getHasSubtypes || stack.getItemDamage == inslot.getItemDamage) &&
                        ItemStack.areItemStackTagsEqual(stack, inslot))
                {
                    val space = math.min(slot.getSlotStackLimit, stack.getMaxStackSize)-inslot.stackSize
                    if (space >= stack.stackSize)
                    {
                        inslot.stackSize += stack.stackSize
                        stack.stackSize = 0
                        slot.onSlotChanged()
                        flag1 = true
                    }
                    else if (space > 0)
                    {
                        stack.stackSize -= space
                        inslot.stackSize += space
                        slot.onSlotChanged()
                        flag1 = true
                    }
                }
                if(reverse) k -= 1 else k += 1
            }
        }

        if(stack.stackSize > 0)
        {
            var k = if(reverse) end-1 else start

            import scala.util.control.Breaks._
            breakable
            {
                while(!reverse && k < end || reverse && k >= start)
                {
                    slot = slots(k)
                    inslot = slot.getStack
                    if(!slot.phantomSlot && inslot == null && slot.isItemValid(stack))
                    {
                        val space = math.min(slot.getSlotStackLimit, stack.getMaxStackSize)
                        if (space >= stack.stackSize)
                        {
                            slot.putStack(stack.copy)
                            slot.onSlotChanged()
                            stack.stackSize = 0
                            flag1 = true
                            break()
                        }
                        else
                        {
                            slot.putStack(stack.splitStack(space))
                            slot.onSlotChanged()
                            flag1 = true
                        }
                    }
                    if(reverse) k -= 1 else k += 1
                }
            }
        }

        flag1
    }

    //Hack to allow empty containers for use with guis without inventories
    override def putStackInSlot(slot:Int, stack:ItemStack)
    {
        if (inventorySlots.isEmpty || inventorySlots.size < slot) return
        else super.putStackInSlot(slot, stack)
    }
}

class Slot3(inv:IInventory, i:Int, x:Int, y:Int) extends Slot(inv, i, x, y) with TSlot3
{
    override def getSlotStackLimit:Int = slotLimitCalculator()
    override def canTakeStack(player:EntityPlayer):Boolean = canRemoveDelegate()
    override def isItemValid(stack:ItemStack):Boolean = canPlaceDelegate(stack)

    override def onSlotChanged()
    {
        super.onSlotChanged()
        slotChangeDelegate()
        slotChangeDelegate2()
    }
}

trait TSlot3 extends Slot
{
    var slotChangeDelegate = {() =>}
    var canRemoveDelegate = {() => true}
    var canPlaceDelegate = {(stack:ItemStack) => inventory.isItemValidForSlot(getSlotIndex, stack)}
    var slotLimitCalculator = {() => inventory.getInventoryStackLimit}

    var phantomSlot = false

    var slotChangeDelegate2 = {() =>} //used for container change delegate, do not set yourself!

    //additional methods required for this trait to work are located in class Slot3
}