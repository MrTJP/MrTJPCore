/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx

import codechicken.lib.vec.Vector3
import mrtjp.core.fx.particles.CoreParticle

trait TScalableParticle extends CoreParticle
{
    var scale = Vector3.one.copy

    def scaleX = scale.x
    def scaleY = scale.y
    def scaleZ = scale.z

    def scaleX_=(x:Double){scale.x = x}
    def scaleY_=(y:Double){scale.y = y}
    def scaleZ_=(z:Double){scale.z = z}
}

class ScaleToAction extends ParticleAction
{
    var target = Vector3.zero
    var duration = 0.0

    override def canOperate(p:CoreParticle) = p.isInstanceOf[TScalableParticle]

    override def operate(p:CoreParticle, time:Double)
    {
        val s = p.asInstanceOf[TScalableParticle]

        if (time < duration)
        {
            val dscale = target.copy.subtract(s.scale)
            val speed = dscale.copy.multiply(1/(duration-time)).multiply(deltaTime(time))
            s.scale.add(speed)
        }
        else isFinished = true
    }

    override def compile(p:CoreParticle){}

    override def copy = ParticleAction.scaleTo(target.x, target.y, target.z, duration)
}

class ScaleForAction extends ParticleAction
{
    var delta = Vector3.zero
    var duration = 0.0

    override def canOperate(p:CoreParticle) = p.isInstanceOf[TScalableParticle]

    override def operate(p:CoreParticle, time:Double)
    {
        val s = p.asInstanceOf[TScalableParticle]
        if (time < duration) s.scale.add(delta.copy.multiply(deltaTime(time)))
        else isFinished = true
    }

    override def compile(p:CoreParticle){}

    override def copy = ParticleAction.scaleFor(delta.x, delta.y, delta.z, duration)
}