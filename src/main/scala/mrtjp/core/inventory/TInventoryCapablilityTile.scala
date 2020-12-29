package mrtjp.core.inventory

import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.items.CapabilityItemHandler._
import net.minecraftforge.items.wrapper.{InvWrapper => MCFInvWrapper, SidedInvWrapper}

trait TInventoryCapablilityTile extends TileEntity with IInventory
{
    override def hasCapability(capability:Capability[_], facing:EnumFacing) =
        capability == ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing)

    override def getCapability[T](capability:Capability[T], facing:EnumFacing):T =
    {
        if (capability == ITEM_HANDLER_CAPABILITY) {
            if (this.isInstanceOf[ISidedInventory] && facing != null) {
                return ITEM_HANDLER_CAPABILITY.cast(new SidedInvWrapper(this.asInstanceOf[ISidedInventory], facing))
            } else {
                return ITEM_HANDLER_CAPABILITY.cast(new MCFInvWrapper(this))
            }
        }
        super.getCapability(capability, facing)
    }
}
