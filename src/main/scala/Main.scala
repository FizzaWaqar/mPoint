import java.sql.Timestamp
import java.text.{ParseException, SimpleDateFormat}
import java.util
import java.util.ArrayList

import com.vividsolutions.jts.geom.{util => _, _}
import com.vividsolutions.jts.io.WKTReader
import main.scala.jtsSpark.mPoint
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.sql.{Row, _}
import org.apache.spark.{Partition, SparkConf, SparkContext, TaskContext}

object Main {
  val conf = new SparkConf()
  conf.setMaster("local")
  conf.setAppName("mPoints Intersection")
  val sc = new SparkContext(conf)
  val sqlContext = SparkSession.builder().appName("Spark In Action").master("local").getOrCreate()

  val factory: GeometryFactory = new GeometryFactory

  def dateParse(DT: String): Timestamp = {
    val dateFormats: util.List[SimpleDateFormat] = new util.ArrayList[SimpleDateFormat](3)
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd-HH:mm"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss"))
    dateFormats.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
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

 //Intersection Operation
  def mPointIntersection(allMPoints: RDD[mPoint], mp :mPoint) = {
    val intersectionResult = allMPoints.filter(x => (x.getStartTime.getTime >= mp.getStartTime.getTime
      && x.getEndTime.getTime <= mp.getEndTime.getTime
      && x.getLineString.intersects(mp.getLineString)==true) //Data filtering for intersection
    ).map(x => (x.TID, x.getLineString.intersection(mp.getLineString))).collect() // Final action to execute all transformations

  }

  def main(args: Array[String]) {

    val t0 = System.nanoTime() //get initial time
    val textFile = sc.textFile("src/main/resources/trips37.csv") //Trajectory Data
    val header = textFile.first();
    System.out.println(header);
    val delimiter = ",";
    val schemaString = header.split(delimiter); //csv header
    val schema = StructType(schemaString.map(fieldName => StructField(fieldName, StringType, true))) //creating schema from file header
    System.out.println(schema);


    val textFile2 = sc.textFile("src/main/resources/prepared.csv")  // new mPoint
    val mpointLines2 = textFile2.flatMap(x => x.split("\n"));
    val rowRDD2 = mpointLines2.map(p => {
      Row.fromSeq(p.split(delimiter))
    }).mapPartitionsWithIndex { (idx, iter) => if (idx == 0) iter.drop(1) else iter };
    val mpointDF2 = sqlContext.createDataFrame(rowRDD2, schema);
    // mpointDF2.collect().foreach(println);

    val newMpoint= new mPoint(1, mpointDF2.select("X").rdd.map(r => r(0)).collect().map(x=>x.toString).map(x=>x.toDouble),
      mpointDF2.select("Y").rdd.map(r => r(0)).collect().map(x=>x.toString).map(x=>x.toDouble),
      mpointDF2.select("time").rdd.map(r => r(0)).collect().map(x=>x.toString).map(x=>dateParse(x))); //creating mPoint object from dataframe



    val mpointLines = textFile.flatMap(x => x.split("\n"))
    val rowRDD = mpointLines.map(p => { Row.fromSeq(p.split(delimiter))
    }).mapPartitionsWithIndex { (idx, iter) => if (idx == 0) iter.drop(1) else iter }
    val mpointDF = sqlContext.createDataFrame(rowRDD, schema) //creating dataframe for trajectory data


    var aggregatedRdd: RDD[mPoint]= mpointDF.rdd.groupBy(r => r.getAs[String]("TID").toInt).filter(_._2.toSeq.length > 1)
      .map(row => mPoint(row._1, row._2.map(_.getAs[String]("X")).toArray.map(x => x.toDouble),
        row._2.map(_.getAs[String]("Y")).toArray.map(x => x.toDouble), row._2.map(_.getAs[String]("Time")).toArray.map(x => dateParse(x))))
    //mPoint objects RDD to store all mPoints

    mPointIntersection(aggregatedRdd,newMpoint) // Intersection operation on trajectory data with new mPoint
    val t1 = System.nanoTime() //final time
    println("Elapsed time: " + ((t1 - t0)/1000000000) + " secs") // Get Elaspsed time
  }
}

