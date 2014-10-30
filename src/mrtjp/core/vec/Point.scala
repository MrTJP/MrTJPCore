/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.vec

class Point(var x:Int, var y:Int)
{
     def set(x:Int, y:Int) = {this.x = x;this.y = y;this}

     def equals(that:Point) = x == that.x && y == that.y

     def copy = new Point(x, y)

     def negate = new Point(-x, -y)
     def invert = new Point(y, x)

     def add(that:Point) = new Point(x+that.x, y+that.y)
     def subtract(that:Point) = add(-that)
     def multiply(that:Point) = new Point(x*that.x, y*that.y)

     def unary_- = negate
     def unary_~ = invert

     def +(that:Point) = add(that)
     def -(that:Point) = subtract(that)
     def *(that:Point) = multiply(that)

     def >(that:Point) = x>that.x && y>that.y
     def <(that:Point) = x<that.x && y<that.y
     def >=(that:Point) = this>that || this == that
     def <=(that:Point) = this<that || this == that
 }
