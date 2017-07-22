/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import java.util.{List => JList}

import mrtjp.core.inventory.InvWrapper
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory._
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

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
    def addPlayerInv(x:Int, y:Int){addPlayerInv(Minecraft.getMinecraft.player, x, y)}
    def addPlayerInv(player:EntityPlayer, x:Int, y:Int)
    {
        var next = 0
        def up() = {next+=1;next-1}

        for ((x, y) <- GuiLib.createSlotGrid(x, y+58, 9, 1, 0, 0))
            addSlotToContainer(new Slot3(player.inventory, up(), x, y)) //hotbar

        for ((x, y) <- GuiLib.createSlotGrid(x, y, 9, 3, 0, 0))
            addSlotToContainer(new Slot3(player.inventory, up(), x, y)) //slots
    }

    override def addListener(listener:IContainerListener)
    {
        super.addListener(listener)
        listener match {
            case p:EntityPlayer if !p.world.isRemote =>
                startWatchDelegate(p)
            case _ =>
        }
    }


    override def removeListener(listener:IContainerListener)
    {
        super.removeListener(listener)
        listener match {
            case p:EntityPlayer if !p.world.isRemote =>
                stopWatchDelegate(p)
            case _ =>
        }
    }

    override def onContainerClosed(p:EntityPlayer)
    {
        super.onContainerClosed(p)
        if (!p.world.isRemote)
            stopWatchDelegate(p)
    }

    /**
      * Called when a slot in this container is clicked, or any other event that occurs
      * that results in a item stack moving such as hotbar swapping w/ numbers 0-8.
      * @param id The slot ID
      * @param dragType Changes meaning depending on clickType:
      *                 PICKUP: The mouse button 0 = left click, 1 = right click
      *                 QUICK_MOVE: The mouse button
      *                 SWAP: The keyboard key that was pressed 0 to 8
      *                 QUICK_CRAFT: Some type of mask. Look at super method.
      *                 PICKUP_ALL: The mouse button
      *
      * @param clickType The type of event that is being performed:
      *                  PICKUP: the stack is trying to be picked up to or dropped from in the player's cursor.
      *                          If the ID is -999, the player has clicked outside the window and wants to drop
      *                          the item in-world.
      *                  QUICK_MOVE: the stack was shift-clicked.
      *                  SWAP: a number was pressed to swap a hotbar item.
      *                  CLONE: Player has cloned item while in creative mode.
      *                  THROW: Hotkey to throw item into the world was pressed.
      *                  QUICK_CRAFT: The stack in-hand was dragged across several slots to split them up, or just simply
      *                               right-clicked to deposit one of the items. Handled in super method. Too convoluted
      *                               to understand, as Forge's documentation sucks. Best to just let MC handle this...
      *                  PICKUP_ALL: When a slot with an item is double-clicked to pick up all matching items in the container.
      *
      *
      *
      * @param player The player that has this container open.
      * @return
      */
    override def slotClick(id:Int, dragType:Int, clickType:ClickType, player:EntityPlayer):ItemStack =
    {
        try { //Ignore exceptions raised from client-side only slots that wont be found here. To be removed.
            if (slots.isDefinedAt(id) && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
                val slot = slots(id)
                if (slot.phantomSlot)
                    return handleGhostClick(slot, dragType, clickType, player)
            }
            super.slotClick(id, dragType, clickType, player)
        } catch {
            case e:Exception => null
        }
    }

    private def handleGhostClick(slot:TSlot3, mouse:Int, clickType:ClickType, player:EntityPlayer):ItemStack =
    {
        val inSlot = slot.getStack
        val inCursor = player.inventory.getItemStack
        if (inCursor != ItemStack.EMPTY && !slot.isItemValid(inCursor)) return inCursor

        val stackable = InvWrapper.areItemsStackable(inSlot, inCursor)
        if (stackable)
        {
            if (inSlot != ItemStack.EMPTY && inCursor == ItemStack.EMPTY) slot.putStack(ItemStack.EMPTY)
            else if (inSlot == ItemStack.EMPTY && inCursor != ItemStack.EMPTY)
            {
                val newStack = inCursor.copy
                newStack.setCount(if (mouse == 0) math.min(inCursor.getCount, slot.getSlotStackLimit) else 1)
                slot.putStack(newStack)
            }
            else if (inSlot != ItemStack.EMPTY)
            {
                val toAdd = if (clickType == ClickType.QUICK_MOVE) 10 else 1
                if (mouse == 0) inSlot.setCount(math.min(slot.getSlotStackLimit, inSlot.getCount+toAdd))
                else if (mouse == 1) inSlot.setCount(math.max(0, inSlot.getCount-toAdd))
                if (inSlot.getCount > 0) slot.putStack(inSlot)
                else slot.putStack(ItemStack.EMPTY)
            }
        }
        else
        {
            val newStack = inCursor.copy
            newStack.setCount(if (mouse == 0) math.min(inCursor.getCount, slot.getSlotStackLimit) else 1)
            slot.putStack(newStack)
        }

        inCursor
    }

    override def transferStackInSlot(player:EntityPlayer, i:Int):ItemStack =
    {
        var stack:ItemStack = ItemStack.EMPTY
        if (slots.isDefinedAt(i))
        {
            val slot = slots(i)
            if (slot != null && slot.getHasStack)
            {
                stack = slot.getStack
                val manipStack = stack.copy

                if (!doMerge(manipStack, i) || stack.getCount == manipStack.getCount) return null

                if (manipStack.getCount <= 0) slot.putStack(null)
                else slot.putStack(manipStack)

                slot.onTake(player, stack)
            }
        }
        stack
    }

    def doMerge(stack:ItemStack, from:Int):Boolean =
    {
        if (slots.size > 36) { //run standarm impl on containers w/ player inventory
            if (slots.size-36 until slots.size contains from) { //if item is from player inventory...
                return tryMergeItemStack(stack, 0, slots.size-36, false) //merge to rest of container
            }
            else { //else if item from outside player inventory...
                if (tryMergeItemStack(stack, slots.size-36, slots.size-27, true)) return true //try merge to hotbar from back
                if (tryMergeItemStack(stack, slots.size-27, slots.size, true)) return true //then try player inventory from back
            }
        }

        false
    }

    def tryMergeItemStack(stack:ItemStack, start:Int, end:Int, reverse:Boolean) =
    {
        var flag1 = false
        var k = if(reverse) end-1 else start

        var slot:TSlot3 = null
        var inslot:ItemStack = null
        if(stack.isStackable)
        {
            while(stack.getCount > 0 && (!reverse && k < end || reverse && k >= start))
            {
                slot = slots(k)
                inslot = slot.getStack
                if (!slot.phantomSlot && inslot != null && inslot.getItem == stack.getItem &&
                        (!stack.getHasSubtypes || stack.getItemDamage == inslot.getItemDamage) &&
                        ItemStack.areItemStackTagsEqual(stack, inslot))
                {
                    val space = math.min(slot.getSlotStackLimit, stack.getMaxStackSize)-inslot.getCount()
                    if (space >= stack.getCount)
                    {
                        val stk:Int = inslot.getCount() + stack.getCount()
                        inslot.setCount(stk)
                        stack.setCount(0)
                        slot.onSlotChanged()
                        flag1 = true
                    }
                    else if (space > 0)
                    {
                        val stk1:Int = stack.getCount() - space
                        stack.setCount(stk1)
                        val stk2:Int = inslot.getCount() + space
                        inslot.setCount(stk2)
                        slot.onSlotChanged()
                        flag1 = true
                    }
                }
                if(reverse) k -= 1 else k += 1
            }
        }

        if(stack.getCount() > 0)
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
                        if (space >= stack.getCount())
                        {
                            slot.putStack(stack.copy)
                            slot.onSlotChanged()
                            stack.setCount(0)
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
    var canRemoveDelegate = {() => !phantomSlot}
    var canPlaceDelegate = {(stack:ItemStack) => inventory.isItemValidForSlot(getSlotIndex, stack)}
    var slotLimitCalculator = {() => inventory.getInventoryStackLimit}

    var phantomSlot = false

    var slotChangeDelegate2 = {() =>} //used for container change delegate, do not set yourself!

    //additional methods required for this trait to work are located in class Slot3
}