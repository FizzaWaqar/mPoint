import java.sql.Timestamp
import java.text.{ParseException, SimpleDateFormat}
import java.util
import java.util.ArrayList

import com.vividsolutions.jts.geom.{util => _, _}
import com.vividsolutions.jts.io.WKTReader
import main.scala.jtsSpark.mPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.sql.{Row, _}
import org.apache.spark.{SparkConf, SparkContext}

object Main {

  val conf = new SparkConf()
  conf.setMaster("local")
  conf.setAppName("Word Count")
  val sc = new SparkContext(conf)
  val sqlContext = SparkSession.builder().appName("Spark In Action").master("local").getOrCreate()

  val factory: GeometryFactory = new GeometryFactory
  def dateParse(DT: String): Timestamp = {
    val dateFormats: util.List[SimpleDateFormat] = new util.ArrayList[SimpleDateFormat](3)
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd-HH:mm"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss"))
    import scala.collection.JavaConversions._
    for (formatString <- dateFormats) {
      try {
        return new Timestamp(formatString.parse(DT).getTime)
      }
      catch {
        case e: ParseException => {
        }
      }
    }
    return null
  }

  def lineIntersectionRDD(geometries: RDD[mPoint], intersectingLine: LineString): RDD[MultiPoint]= {
    val lineStrings: RDD[Geometry]= geometries.map(x=> x.getLineString)
    val geom = lineStrings.collect()
    val geomList : util.ArrayList[Geometry]= new util.ArrayList[Geometry]()
    for (e <- geom) geomList.add(e)
    val geometryCollection: GeometryCollection = factory.buildGeometry(geomList).asInstanceOf[GeometryCollection]
    val union: Geometry = geometryCollection.union
    val ab: MultiPoint= geometryCollection.intersection(intersectingLine).asInstanceOf[MultiPoint]

    val pointRDD: RDD[MultiPoint]= sc.parallelize(Seq(ab))

    return pointRDD

  }

  def main(args: Array[String]) {

    val textFile = sc.textFile("src/main/resources/mpoints.csv")
    val header = textFile.first();
    System.out.println(header);
    val delimiter = ","
    val schemaString = header.split(delimiter) //csv header

    val schema = StructType(schemaString.map(fieldName => StructField(fieldName, StringType, true)))
    System.out.println(schema);
    val mpointLines = textFile.flatMap(x => x.split("\n"))
    val rowRDD = mpointLines.map(p => {
      Row.fromSeq(p.split(delimiter))
    }).mapPartitionsWithIndex { (idx, iter) => if (idx == 0) iter.drop(1) else iter }
    val mpointDF = sqlContext.createDataFrame(rowRDD, schema)
    System.out.print(mpointDF.show());

  val aggregatedRdd: RDD[mPoint] = mpointDF.rdd.groupBy(r => r.getAs[String]("TID").toInt)
    .map(row => mPoint(row._1, row._2.map(_.getAs[String]("X")).toArray.map(x => x.toDouble),
      row._2.map(_.getAs[String]("Y")).toArray.map(x => x.toDouble), row._2.map(_.getAs[String]("time")).toArray.map(x => dateParse(x))))


  val lineCords: Array[Coordinate] = Array(new Coordinate(72.850527194320122, 33.366328743877844), new Coordinate(72.853311163696944, 33.370611773688339), new Coordinate(72.85728486357668, 33.370516595248105), new Coordinate(72.859093253941111, 33.366447716928135), new Coordinate(72.862591061619682, 33.366614279198544), new Coordinate(72.864946728015454, 33.368969945594316), new Coordinate(72.870562255989213, 33.367351912110351))
  val intersectingLine: LineString = factory.createLineString(lineCords)

    System.out.println("mPoint intersection with Linestring" + "\n")
    lineIntersectionRDD(aggregatedRdd,intersectingLine).foreach(println)

    System.out.println("mPoint intersection with Points" + "\n")

  }
}

