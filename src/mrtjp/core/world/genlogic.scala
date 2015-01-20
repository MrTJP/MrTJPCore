/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import java.util.Random

import mrtjp.core.math.MathLib
import net.minecraft.block.Block
import net.minecraft.world.gen.feature.WorldGenerator
import net.minecraft.world.{World, WorldType}
import net.minecraftforge.common.BiomeDictionary

trait TGenerationLogic extends ISimpleStructureGen
{
    var name = ""

    var dimensionBlacklist = true
    var dimensions = Set[Int](-1, 1)

    var biomeBlacklist = true
    var biomes = Set[Set[BiomeDictionary.Type]]()

    var typeBlacklist = true
    var types = Set[WorldType](WorldType.FLAT)

    var resistance = 0
    var allowRetroGen = false

    override def genID = name

    def preFiltCheck(w:World, chunkX:Int, chunkZ:Int, rand:Random, isRetro:Boolean):Boolean =
    {
        if (isRetro && !allowRetroGen) return false
        if (dimensionBlacklist == dimensions.contains(w.provider.dimensionId)) return false
        if (typeBlacklist == types.contains(w.provider.terrainType)) return false
        if (resistance > 1 && rand.nextInt(resistance) != 0) return false
        true
    }

    def postFiltCheck(w:World, x:Int, z:Int, rand:Random):Boolean =
    {
        val types = BiomeDictionary.getTypesForBiome(w.getBiomeGenForCoords(x, z)).toSet
        if (biomeBlacklist == biomes.contains(types)) return false
        true
    }

    override def generate(w:World, chunkX:Int, chunkZ:Int, rand:Random, isRetro:Boolean):Boolean =
    {
        if (!preFiltCheck(w, chunkX, chunkZ, rand, isRetro)) return false
        generate_impl(w, chunkX, chunkZ, rand)
    }

    def generate_impl(w:World, chunkX:Int, chunkZ:Int, rand:Random):Boolean
}

trait TWorldGenerator extends WorldGenerator
{
    override def generate(w:World, rand:Random, x:Int, y:Int, z:Int):Boolean

    protected def canSetBlock(w:World, x:Int, y:Int, z:Int, material:Set[(Block, Int)]):Boolean =
    {
        if (material.isEmpty) return true
        val block = w.getBlock(x, y, z)
        material.exists(pair => (pair._2 == -1 || pair._2 == w.getBlockMetadata(x, y, z)) &&
                (block.isReplaceableOreGen(w, x, y, z, pair._1) || block.isAssociatedBlock(pair._1)))
    }

    protected def setBlock(w:World, x:Int, y:Int, z:Int, cluster:Set[((Block, Int), Int)], material:Set[(Block, Int)]):Boolean =
    {
        if (canSetBlock(w, x, y, z, material))
        {
            val genBlock = MathLib.weightedRandom(cluster, w.rand)
            w.setBlock(x, y, z, genBlock._1, genBlock._2, 2)
            true
        }
        else false
    }

    protected def setBlock(w:World, x:Int, y:Int, z:Int, cluster:(Block, Int), material:Set[(Block, Int)]) =
    {
        if (canSetBlock(w, x, y, z, material))
        {
            w.setBlock(x, y, z, cluster._1, cluster._2, 2)
            true
        }
        else false
    }
}

class GenLogicUniform extends TGenerationLogic
{
    var gen:WorldGenerator = null

    var attempts = 1
    var minY = 0
    var maxY = 0

    override def generate_impl(w:World, chunkX:Int, chunkZ:Int, rand:Random) =
    {
        var generated = false
        for (i <- 0 until attempts)
        {
            val x = chunkX*16+rand.nextInt(16)
            val y = minY+rand.nextInt(maxY-minY)
            val z = chunkZ*16+rand.nextInt(16)
            if (postFiltCheck(w, x, z, rand)) generated |= gen.generate(w, rand, x, y, z)
        }
        generated
    }
}

class GenLogicSurface extends TGenerationLogic
{
    var gen:WorldGenerator = null

    var attempts = 1

    override def generate_impl(w:World, chunkX:Int, chunkZ:Int, rand:Random) =
    {
        var generated = false
        for (i <- 0 until attempts)
        {
            val x = chunkX*16+rand.nextInt(16)
            val z = chunkZ*16+rand.nextInt(16)
            if (postFiltCheck(w, x, z, rand))
            {
                val y = WorldLib.findSurfaceHeight(w, x, z)
                if (y > 0) generated |= gen.generate(w, rand, x, y, z)
            }
        }
        generated
    }
}