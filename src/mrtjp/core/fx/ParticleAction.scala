/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx

import codechicken.lib.vec.Vector3
import mrtjp.core.fx.particles.CoreParticle

abstract class ParticleAction
{
    var isFinished = false
    var life = 0
    var lastTime = 0.0

    def tickLife() { life += 1}

    def canOperate(p:CoreParticle) = true

    def runOn(p:CoreParticle, frame:Float)
    {
        if (!canOperate(p))
            throw new RuntimeException("Particle action was run on an incompatible particle class.")
        if (!isFinished)
        {
            val t = life+frame
            operate(p, t)
            lastTime = t
        }
    }

    def operate(p:CoreParticle, time:Double)

    def compile(){}

    def reset()
    {
        isFinished = false
        life = 0
        lastTime = 0
    }

    def deltaTime(t:Double) = t-lastTime
}

object ParticleAction
{
    def delay(ticks:Double):ParticleAction =
    {
        val a = new DelayAction
        a.delay = ticks
        a
    }

    def kill():ParticleAction = new KillAction

    def group(actions:ParticleAction*):ParticleAction =
    {
        val a = new GroupAction
        a.actions ++= actions
        a
    }

    def sequence(actions:ParticleAction*):ParticleAction =
    {
        val a = new SequenceAction
        a.actions ++= actions
        a
    }

    def repeat(action:ParticleAction, times:Int):ParticleAction =
    {
        val a = new RepeatAction
        a.action = action
        a.repeatTimes = times
        a
    }

    def repeatForever(action:ParticleAction):ParticleAction =
    {
        val a = new RepeatForeverAction
        a.action = action
        a
    }

    def moveTo(x:Double, y:Double, z:Double, duration:Double):ParticleAction =
    {
        val a = new PositionChangeToAction
        a.target = new Vector3(x, y, z)
        a.duration = duration
        a
    }

    def moveBy(x:Double, y:Double, z:Double, duration:Double):ParticleAction =
    {
        val a = new PositionChangeByAction
        a.delta = new Vector3(x, y, z)
        a.duration = duration
        a
    }

    def scaleTo(x:Double, y:Double, z:Double, duration:Double):ParticleAction =
    {
        val a = new ScaleToAction
        a.target = new Vector3(x, y, z)
        a.duration = duration
        a
    }

    def scaleBy(x:Double, y:Double, z:Double, duration:Double):ParticleAction =
    {
        val a = new ScaleByAction
        a.delta = new Vector3(x, y, z)
        a.duration = duration
        a
    }

    def changeColourTo(r:Double, g:Double, b:Double, a:Double, duration:Double):ParticleAction =
    {
        val a1 = new ColourChangeToAction
        a1.target = new Vector3(r, g, b)
        a1.duration = duration
        val a2 = new AlphaChangeToAction
        a2.target = a
        a2.duration = duration
        group(a1, a2)
    }

    def changeColourBy(r:Double, g:Double, b:Double, a:Double, duration:Double):ParticleAction =
    {
        val a1 = new ColourChangeByAction
        a1.delta = new Vector3(r, g, b)
        a1.duration = duration
        val a2 = new AlphaChangeByAction
        a2.delta = a
        a2.duration = duration
        group(a1, a2)
    }

    def changeAlphaTo(alpha:Double, duration:Double):ParticleAction =
    {
        val a = new AlphaChangeToAction
        a.target = alpha
        a.duration = duration
        a
    }

    def changeTexture(texture:String):ParticleAction =
    {
        val a = new TextureChangeAction
        a.tex = texture
        a
    }

    def changeTargetTo(x:Double, y:Double, z:Double, duration:Double):ParticleAction =
    {
        val a = new TargetChangeToAction
        a.target = new Vector3(x, y, z)
        a.duration = duration
        a
    }

    def changeTargetBy(x:Double, y:Double, z:Double, duration:Double):ParticleAction =
    {
        val a = new TargetChangeByAction
        a.delta = new Vector3(x, y, z)
        a.duration = duration
        a
    }

    def orbitAround(x:Double, z:Double, speed:Double, duration:Double):ParticleAction =
    {
        val a = new OrbitAction
        a.target = new Vector3(x, 0, z)
        a.duration = duration
        a.speed = speed
        a
    }
}