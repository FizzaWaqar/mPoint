name := "SparkScala"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val sparkVer = "2.1.0"

  Seq(
    "org.apache.spark" %% "spark-core" % sparkVer,
    "org.datasyslab" % "geospark" % "0.8.0",
    "com.vividsolutions" % "jts" % "1.13",
    "org.apache.spark" % "spark-sql_2.11" % sparkVer
  )
}
