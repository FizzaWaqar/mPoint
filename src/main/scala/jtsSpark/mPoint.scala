package main.scala


package jtsSpark
import java.sql.Timestamp

import scala.collection.mutable.ArrayBuffer
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.LineString
import java.util.{ArrayList, Collections}
/**
  * Created by fizza on 4/12/2018.
  */

case class mPoint(TID: Integer, X: Array[Double], Y:Array[Double], Time:Array[Timestamp])
{
 // System.out.println("Creating mPoint Object")

 // System.out.println("TID: " + TID + "\n X: " + X.deep.mkString("\n") + "\n Y: " + Y.deep.mkString("\n") + "\n Time: " + Time.deep.mkString("\n"))

  private[jtsSpark] val geometryFactory: GeometryFactory = new GeometryFactory

  val cords: ArrayList[Coordinate]= new ArrayList[Coordinate]

  def getTimeStamps: Array[Timestamp] = {
    return Time
  }


  def getLineString: LineString = {
    val temp: Array[Coordinate] = new Array[Coordinate](X.size)
    var i: Int = 0
    while (i < X.size) {
      {
        cords.add(new Coordinate(X(i), Y(i)))
      }
      ({
        i += 1; i - 1
      })
    }
    val lineString: LineString = geometryFactory.createLineString(cords.toArray(temp))
    return lineString

  }



}
