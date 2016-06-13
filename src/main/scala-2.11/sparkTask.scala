/**
  * Created by scott on 6/12/16.
  */

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import com.datastax.spark.connector._

object sparkTask {
  def main(args: Array[String]) = {

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", "127.0.0.1")
//      .setJars(Seq("/home/scott/IdeaProjects/sparkCassandra/out/artifacts/hello_jar/hello.jar"))
    val sc = new SparkContext("spark://127.0.0.1:7077", "test", conf)
//    sc.addJar("/home/scott/.ivy2/cache/com.datastax.spark/spark-cassandra-connector_2.11/jars/spark-cassandra-connector_2.11-1.6.0.jar")
//    sc.addJar("/home/scott/.ivy2/cache/com.datastax.cassandra/cassandra-driver-core/jars/cassandra-driver-core-3.0.2.jar")
//    sc.addJar("/home/scott/.ivy2/cache/joda-time/joda-time/jars/joda-time-2.3.jar")
    val rdd = sc.cassandraTable("test", "kv")
    println(rdd.count)
    println(rdd.first)
    println(rdd.map(_.getInt("value")).sum)
  }
}
