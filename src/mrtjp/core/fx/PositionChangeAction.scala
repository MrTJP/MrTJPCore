/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx

import codechicken.lib.vec.{BlockCoord, Vector3}
import mrtjp.core.fx.particles.CoreParticle

trait TPositionedParticle extends CoreParticle
{
    def x = posX
    def y = posY
    def z = posZ

    def x_=(x:Double){setPosition(x, y, z)}
    def y_=(y:Double){setPosition(x, y, z)}
    def z_=(z:Double){setPosition(x, y, z)}

    def px = prevPosX
    def py = prevPosY
    def pz = prevPosZ

    def px_=(x:Double){prevPosX = x}
    def py_=(y:Double){prevPosY = y}
    def pz_=(z:Double){prevPosZ = z}

    def dx = x-px
    def dy = y-py
    def dz = z-pz

    def position = new Vector3(x, y, z)
    def prevPosition = new Vector3(px, py, pz)

    def setPos(pos:Vector3)
    {
        setPosition(pos.x, pos.y, pos.z)
    }

    def setPrevPos(pos:Vector3)
    {
        px = pos.x
        py = pos.y
        pz = pos.z
    }

    def blockPosition = new BlockCoord(math.floor(x).toInt, math.floor(y).toInt, math.floor(z).toInt)

    abstract override def onUpdate()
    {
        super.onUpdate()
        px = x
        py = y
        pz = z
    }
}

class PositionChangeToAction extends ParticleAction
{
    var target = Vector3.zero
    var duration = 0.0

    override def canOperate(p:CoreParticle) = p.isInstanceOf[TPositionedParticle]

    override def operate(p:CoreParticle, time:Double)
    {
        val pp = p.asInstanceOf[TPositionedParticle]

        val pos = pp.position
        if (time < duration)
        {
            val dpos = target.copy.subtract(pos)
            val speed = dpos.copy.multiply(1/(duration-time)).multiply(deltaTime(time))
            pp.setPos(pos.add(speed))
        }
        else isFinished = true
    }

    override def compile(p:CoreParticle){}

    override def copy = ParticleAction.moveTo(target.x, target.y, target.z, duration)
}

class PositionChangeForAction extends ParticleAction
{
    var delta = Vector3.zero
    var duration = 0.0

    override def canOperate(p:CoreParticle) = p.isInstanceOf[TPositionedParticle]

    override def operate(p:CoreParticle, time:Double)
    {
        val pp = p.asInstanceOf[TPositionedParticle]
        if (time < duration) pp.setPos(pp.position.add(delta.copy.multiply(deltaTime(time))))
        else isFinished = true
    }

    override def compile(p:CoreParticle){}

    override def copy = ParticleAction.moveFor(delta.x, delta.y, delta.z, duration)
}