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
      .set("spark.cassandra.connection.host", "172.17.0.2")
      .setJars(Seq("/home/scott/IdeaProjects/sparkCassandra/target/scala-2.11/hello-assembly-1.0.jar"))
    val sc = new SparkContext("spark://172.17.0.2:7077", "test", conf)
    val rdd = sc.cassandraTable("test", "kv")
    println(rdd.count)
    println(rdd.first)
    println(rdd.map(_.getInt("value")).sum)
  }
}
