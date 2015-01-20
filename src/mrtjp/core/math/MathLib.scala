/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.math

import java.lang
import java.util.Random

import codechicken.lib.vec.{BlockCoord, Vector3}
import net.minecraft.entity.Entity
import net.minecraft.world.gen.NoiseGeneratorPerlin
import org.lwjgl.util.vector.{Matrix4f, Vector3f}

object MathLib
{
    def clamp(min:Float, max:Float, v:Float) = Math.min(max, Math.max(min, v))

    def normal(bc:BlockCoord, dir:Int) = dir match
    {
        case 0 => (bc.x, bc.z)
        case 1 => (bc.x, bc.z)
        case 2 => (bc.x, bc.y)
        case 3 => (bc.x, bc.y)
        case 4 => (bc.y, bc.z)
        case 5 => (bc.y, bc.z)
    }

    def basis(bc:BlockCoord, dir:Int) = dir match
    {
        case 0 => bc.y
        case 1 => bc.y
        case 2 => bc.z
        case 3 => bc.z
        case 4 => bc.x
        case 5 => bc.x
    }

    def splitLine(xs:Seq[Int], shift:Int) =
    {
        if (xs.isEmpty) Seq()
        else
        {
            var start = 0
            val ret = for
            {
                (x, i) <- xs.zipWithIndex
                if i > 0
                if x != xs(i-1)+shift
            } yield
            {
                val size = i-start
                start = i
                (xs(i-1), size)
            }
            ret :+ ((xs.last, xs.length-start))
        }
    }

    def rhrAxis(dir:Int, normal:(Int, Int), basis:Int):BlockCoord = dir match
    {
        case 0 => new BlockCoord(normal._1, basis, normal._2)
        case 1 => new BlockCoord(normal._1, basis, normal._2)
        case 2 => new BlockCoord(normal._1, normal._2, basis)
        case 3 => new BlockCoord(normal._1, normal._2, basis)
        case 4 => new BlockCoord(basis, normal._1, normal._2)
        case 5 => new BlockCoord(basis, normal._1, normal._2)
    }

    def createEntityRotateMatrix(entity:Entity):Matrix4f =
    {
        val yaw = Math.toRadians(entity.rotationYaw - 180)
        val pitch = Math.toRadians(entity.rotationPitch)
        val initial = new Matrix4f
        initial.rotate(pitch.asInstanceOf[Float], new Vector3f(1, 0, 0))
        initial.rotate(yaw.asInstanceOf[Float], new Vector3f(0, 1, 0))
        initial
    }

    def bezier(s:Vector3, c1:Vector3, c2:Vector3, e:Vector3, t:Float):Vector3 =
    {
        if ((t < 0.0F) || (t > 1.0F)) return s
        val one_minus_t = 1.0F-t
        val retValue = new Vector3(0.0D, 0.0D, 0.0D)
        val terms = new Array[Vector3](4)

        def calcNewVector(scaler:Float, base:Vector3) = base.copy.multiply(scaler)

        terms(0) = calcNewVector(one_minus_t*one_minus_t*one_minus_t, s)
        terms(1) = calcNewVector(3.0F*one_minus_t*one_minus_t*t, c1)
        terms(2) = calcNewVector(3.0F*one_minus_t*t*t, c2)
        terms(3) = calcNewVector(t*t*t, e)

        for (i <- 0 until 4) retValue.add(terms(i))
        retValue
    }

    private val random = new Random
    def randomFromIntRange(az:Range, rand:Random = random) = az(rand.nextInt(az.size))

    def leastSignificant(mask:Int) =
    {
        var bit = 0
        var m = mask
        while ((m&1) == 0 && m != 0){ bit += 1; m <<= 1 }
        bit
    }

    def weightedRandom[T](xs:Traversable[(T, Int)], rand:Random = random):T =
    {
        if (xs.size == 1) return xs.head._1
        var weight = rand.nextInt(xs.map(_._2).sum)
        xs.find(x => {weight -= x._2; weight < 0}).get._1
    }
}