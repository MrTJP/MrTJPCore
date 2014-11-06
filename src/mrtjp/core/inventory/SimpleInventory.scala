/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.inventory

class SimpleInventory(override val size:Int, override val name:String, override val stackLimit:Int) extends TInventory
{
    def this(size:Int, lim:Int) = this(size, "", lim)
    def this(size:Int, name:String) = this(size, name, 64)
    def this(size:Int) = this(size, 64)

    override def markDirty(){}
}