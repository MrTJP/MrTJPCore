/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.block

import net.minecraft.block.Block

object BlockLib
{
    def getModId(b:Block) =
    {
        val name = Block.blockRegistry.getNameForObject(b)
        name.indexOf(':') match
        {
            case -1 => "minecraft"
            case i => name.take(i)
        }
    }
}