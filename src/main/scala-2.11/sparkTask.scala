/**
  * Created by scott on 6/12/16.
  */

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import com.datastax.spark.connector._

object sparkTask {

  case class Record(addedDate: String, title: String, description: Option[String])

  case class GenresInfo(title: String, release_date: String, genres: Set[String])

  def main(args: Array[String]) = {

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", "172.17.0.2")
      .setJars(Seq("/home/scott/IdeaProjects/sparkCassandra/target/scala-2.11/hello-assembly-1.0.jar"))
    val sc = new SparkContext("spark://172.17.0.2:7077", "test", conf)
    optimalRepartition(sc)
  }

  def optimalRepartition(sc: SparkContext): Unit = {
    val movies = sc.cassandraTable("killr_video", "videos")
      .keyBy(row => row.getString("release_date"))
      .partitionBy(
        new org.apache.spark.HashPartitioner(2 * sc.defaultParallelism)
      )
      .cache

    val movieCountByYear = movies.countByKey.foreach(println)

    val moviesByYear = movies.groupByKey.collect.foreach(println)
  }

  def repartitioning(sc: SparkContext): Unit = {
    println(sc.defaultParallelism)

    val movies = sc.parallelize(List(
      ("Alice in Wonderland", 2016),
      ("Alice Through the Looking Glass", 2010)
    ))
    println(movies.partitions.size)

    println(movies.repartition(2 * sc.defaultParallelism).partitions.size)

    println(movies.coalesce(1).partitions.size)


    //for sorting use range partitioner
    val movies2 = sc.parallelize(List(
      ("Alice in Wonderland", 2016),
      ("Alice Through the Looking Glass", 2010)
    ))
      .partitionBy(new org.apache.spark.HashPartitioner(9))
    println(movies2.partitioner)

  }

  def partitions(sc: SparkContext): Unit = {
    val videos = sc.parallelize(List(
      ("Alice in Wonderland", 2016),
      ("Alice Through the Looking Glass", 2010)
    ))
    println(videos.partitions.size)

    val videos2 = sc.cassandraTable("killr_video", "videos")
    println(videos2.partitions.size)
    println(videos2.partitioner)
  }

  def setBasedIntegrity(sc: SparkContext): Unit = {
    val ratings = sc.cassandraTable("killr_video", "ratings_by_video")
      .keyBy(row => row.getUUID("video_id"))

    val movies = sc.cassandraTable("killr_video", "videos")
      .select("video_id")
      .keyBy(row => row.getUUID("video_id"))

    ratings.subtractByKey(movies)
      .collect
      .foreach(println)
  }

  def useSets(sc: SparkContext): Unit = {
    val A = sc.parallelize(Array(("k1", "v1"), ("k2", "v2"), ("k3", "v3"), ("k4", "v4")))
    val B = sc.parallelize(Array(("k1", "w1"), ("k2", "w2"), ("k3", "w3"), ("k4", "w4")))

    A.union(B)
      .collect
      .foreach(println)

    A.groupByKey
      .join(B.groupByKey)
      .flatMapValues { case (aList, bList) => aList ++ bList }
      .collect
      .foreach(println)
  }

  def referentialIntegrity(sc: SparkContext): Unit = {
    // Also see Joins: join, leftOuterJoin ...
    val playlists = sc.cassandraTable("killr_video", "playlists_by_user")
      .keyBy(row => row.getUUID("movie_id"))

    val movies = sc.cassandraTable("killr_video", "movies")
      .select("movie_id")
      .keyBy(row => row.getUUID("movie_id"))

    playlists.leftOuterJoin(movies)
      .filter { case (m, (rowP, rowM)) => !rowM.isDefined }
      .map { case (m, (rowP, rowM)) => rowP }
      .collect
      .foreach(println)

  }

  def schemaEvolution(sc: SparkContext): Unit = {

    val playlists = sc.cassandraTable("killr_video", "playlists_by_user")
      .select("user_id", "playlist_name", "release_year", "title", "movie_id")
      .as((u: java.util.UUID, p: String, y: Int, t: String, m: java.util.UUID) =>
        (m, (u, p, y, t)))

    val movies = sc.cassandraTable("killr_video", "movies")
      .select("movie_id", "genres", "rating")
      .as((m: java.util.UUID, g: Set[String], r: Option[Float]) =>
        (m, (g, r)))

    playlists.join(movies)
      .map { case (m, ((u, p, y, t), (g, r))) => (u, p, y, t, m, g, r) }
      .saveToCassandra("killr_video", "playlists_by_user")

  }

  def usePairCombineByKey(sc: SparkContext): Unit = {
    // won't work on current docker schema
    sc.cassandraTable[(Int, Option[Float])]("killr_video", "movies_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("release_year", "rating")
      .filter(_._2.isDefined).mapValues(r => r.get)
      .combineByKey(
        (rating: Float) => (rating, 1),
        (res: (Float, Int), rating: Float) => (res._1 + rating, res._2 + 1),
        (res1: (Float, Int), res2: (Float, Int)) => (res1._1 + res2._1, res1._2 + res2._2)
      )
      .mapValues { case (sum, count) => val avg = sum / count; f"$avg%1.1f" }
      .collect
      .foreach(println)
  }

  def usePairFoldByKey(sc: SparkContext): Unit = {
    // won't work on current docker schema
    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("release_year", "title", "rating")
      .as((y: Int, t: String, r: Option[Float]) => (y, (t, r)))
      .filter { case (y, (t, r)) => r.isDefined }
      .mapValues { case (t, r) => (t, r.get) }
      .foldByKey(("", 0.0f)) { case ((maxT, maxR), (t, r)) =>
        if (maxR < r) (t, r)
        else (maxT, maxR)
      }
  }

  def usePairReduceByKey(sc: SparkContext): Unit = {
    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("added_date")
      .as((year: String) => (year, 1))
      .reduceByKey(_ + _)
      .collect
      .foreach(println)
  }

  def usePairRDDs(sc: SparkContext): Unit = {

    sc.cassandraTable[(String, Option[String])]("killr_video", "videos")
      .select("type", "title")
      .mapValues(v => v.getOrElse(0.0))
      .lookup("Movie")
      .foreach(println)

  }

  def cacheRDDs(sc: SparkContext): Unit = {
    // This won't run on present the docker schema
    val movies = sc.cassandraTable("killr_video", "videos")
      .select("release_date", "genres")
      .cache

    val movies2014 = movies.filter(row => row.getInt("release_date") == 2014)
      .cache

    val total2014 = movies2014.count

    val comedy2014 = movies2014.filter(row => row.getSet[String]("genres")
      contains "Comedy").count
    val percentage2014 = 100.0 * comedy2014 / total2014

    val movies2013 = movies.filter(row => row.getInt("release_date") == 2013)
      .cache

    val total2013 = movies2013.count

    val comedy2013 = movies2013.filter(row => row.getSet[String]("genres")
      contains "Comedy").count
    val percentage2013 = 100.0 * comedy2013 / total2013

  }

  def useAccumulatorVariables(sc: SparkContext): Unit = {
    val numRatings = sc.accumulator(0)
    val sumRatings = sc.accumulator(0.0)

    sc.cassandraTable("killr_video", "videos")
      .select("avg_rating")
      .filter(row => row.getStringOption("avg_rating").isDefined) //doesn't filter empty strings
      .foreach { row => numRatings += 1; sumRatings += row.getString("avg_rating").toFloat }

    val avgRating = sumRatings.value / numRatings.value

    println(f"$avgRating%1.1f")
  }

  def useBroadcastVariables(sc: SparkContext): Unit = {
    val popularTitles = sc.broadcast(Set(
      "Alice in Wonderland", "Alice Through the Looking Glass", "..."))

    val movies = sc.cassandraTable("killr_video", "videos")
      .select("title", "release_date", "mpaa_rating", "genres")
      .cache //persist result set in memory only, contrast .persist()

    movies.filter(row => popularTitles.value contains row.getString("title"))
      .saveToCassandra("killr_video", "favorite_movies",
        SomeColumns("title", "release_date", "mpaa_rating", "genres"))

    movies.filter(row => !(popularTitles.value contains row.getString("title")))
      .saveToCassandra("killr_video", "other_movies",
        SomeColumns("title", "release_date", "mpaa_rating", "genres"))
  }

  def saveGenres(sc: SparkContext): Unit = {
    val genres = sc.parallelize(Seq(
      new GenresInfo("Alice in Wonderland", "2010", Set("Adventure", "Family"))
    ))

    genres.saveToCassandra("killrvideo", "favorite_movies",
      SomeColumns("title", "added_date", "genres"))

    val details = sc.parallelize(Seq(
      ("Alive in Wonderland", "2010",
        UDTValue.fromMap(Map("country" -> "USA", "language" -> "English", "runtime" -> 108)))
    ))

    details.saveToCassandra("killrvideo", "favorite_movies",
      SomeColumns("title", "added_date", "details"))

  }

  def saveFavorites(sc: SparkContext): Unit = {
    val movies = sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("title", "added_date", "description")

    movies.saveToCassandra("killrvideo", "favorite_movies",
      SomeColumns("title", "added_date", "description"))

  }

  def rowsToObjects(sc: SparkContext): Unit = {
    sc.cassandraTable[Record]("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("added_date", "title", "description")
      .collect
      .foreach(println)

    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")
      .select("added_date", "title", "description")
      .as((y: String, t: String, d: Option[String]) => new Record(y, t, d))
      .collect
      .foreach(println)
  }

  def rowsToTuples(sc: SparkContext): Unit = {
    sc.cassandraTable[(String, String, Option[String])](
      "killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'").select("added_date", "title", "description")
      .collect
      .foreach(println)

    sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'").select("added_date", "title", "description")
      .as((y: String, t: String, d: Option[String]) => (y, t, d))
      .collect
      .foreach(println)
  }

  def filterVideosByActor(sc: SparkContext): Unit = {
    val movies = sc.cassandraTable("killrvideo", "videos_by_actor")
      .where("actor = 'Brad Pitt'")

    movies.filter(row => row.getString("title").toLowerCase.contains("river"))
      .map { row => row.getString("title") +
        " (" + row.getString("added_date") + ")" +
        " [" + row.getStringOption("description").getOrElse("No description") + "]"
      }
      .collect
      .foreach(println)
  }

  def videosByActor(sc: SparkContext): Unit = {
    sc.cassandraTable("killrvideo", "videos_by_actor")
      .select("title", "added_date")
      .where("actor = 'Brad Pitt'")
      .withDescOrder
      .limit(5)
      .collect
      .foreach(println)
  }

  def filterMovies(sc: SparkContext): Unit = {
    val movies = sc.parallelize(
      Array("Frozen, 2013", "Toy Story, 1995", "WALL-E, 2008",
        "Despicable Me, 2010", "Shrek, 2001",
        "The Lego Movie, 2014", "Alice in Wonderland, 2010")
    )

    val movies2010 = movies.filter(m => m.split(",").last.trim.toInt == 2010)
    movies2010.collect().foreach(println)

    val familyMovies = movies2010.map(m => (m, Set("Family", "Animation")))
    familyMovies.collect().foreach(println)

    val familyGenres = familyMovies.flatMap { case (m, g) => g }
      .distinct
    familyGenres.collect().foreach(println)

    val pairs = familyGenres.cartesian(familyGenres)
      .filter { case (g1, g2) => g1 != g2 }
    pairs.collect().foreach(println)

    val totalLength1 = movies.map(m => m.substring(0, m.length - 6).length)
      .reduce { case (x, y) => x + y }
    println(totalLength1)

    val totalCount = sc.accumulator(0)
    val totalLength = sc.accumulator(0)

    movies.map(m => m.substring(0, m.length - 6).length)
      .foreach { l => totalCount += 1; totalLength += l }
    println(totalLength.value / totalCount.value)
    println(totalLength.value.toDouble / totalCount.value)

  }

  def countWords(sc: SparkContext): Unit = {
    val records = sc.textFile("file:///home/videos.csv")
    records.flatMap(record => record.split(",").drop(1))
      .map(word => (word, 1)).reduceByKey { case (x, y) => x + y }
      .collect().foreach(println)
  }

  def printTest(sc: SparkContext): Unit = {
    val rdd = sc.cassandraTable("test", "kv")
    println(rdd.count)
    println(rdd.first)
    println(rdd.map(_.getInt("value")).sum)
  }

}
