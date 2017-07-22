package mrtjp.core.util

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

/**
  * Created by covers1624 on 26/12/2016.
  * TODO Move to CCL.
  */
object CCLConversions {

    implicit def createTriple[A, B, C](a: A, b: B, c: C): TripleABC[A, B, C] = new TripleABC[A, B, C](a, b, c)

    implicit def toEnumFacing(dir: Int): EnumFacing = EnumFacing.VALUES(dir)
}

class TripleABC[A, B, C](val a: A, val b: B, val c: C) {
    def getA: A = a

    def getB: B = b

    def getC: C = c
}