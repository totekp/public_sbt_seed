package hello


import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.Matchers

import scala.util.control.NonFatal

object HelloSparkGraphX extends Matchers {

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  case class Person(name: String, age: Int)

  def run(sc: SparkContext): Unit = {
    val vertexArray = Array(
      (1L, Person("Alice", 28)),
      (2L, Person("Bob", 27)),
      (3L, Person("Charlie", 65)),
      (4L, Person("David", 42)),
      (5L, Person("Ed", 55)),
      (6L, Person("Fran", 50))
    )
    val edgeArray = Array(
      Edge(2L, 1L, 7),
      Edge(2L, 4L, 2),
      Edge(3L, 2L, 4),
      Edge(3L, 6L, 3),
      Edge(4L, 1L, 1),
      Edge(5L, 2L, 2),
      Edge(5L, 3L, 8),
      Edge(5L, 6L, 3)
    )
    val vertexRDD: RDD[(VertexId, Person)] = sc.parallelize(vertexArray)
    val edgeRDD: RDD[Edge[Int]] = sc.parallelize(edgeArray)
    val graph: Graph[Person, Int] = Graph(vertexRDD, edgeRDD)
    val result1: Array[String] = graph.vertices.filter { case (id, Person(name, age)) => age > 30 }.collect().map {
      case (id, Person(name, age)) => s"$name is $age"
    }
    assert(result1.toSet == "David is 42;;Fran is 50;;Ed is 55;;Charlie is 65".split(";;").toSet)

    val result2 = (for (triplet <- graph.triplets.collect()) yield {
      s"${triplet.srcAttr.name} likes ${triplet.dstAttr.name}"
    }).toSet
    assert(result2 == "Bob likes Alice;;Bob likes David;;Charlie likes Bob;;Charlie likes Fran;;David likes Alice;;Ed likes Bob;;Ed likes Charlie;;Ed likes Fran".split(";;").toSet)
    // loves more than 5 times
    val result3 = (for (triplet <- graph.triplets.filter(t => t.attr > 5).collect()) yield {
      s"${triplet.srcAttr.name} loves ${triplet.dstAttr.name}"
    }).toSet
    assert(result3 == Set("Bob loves Alice", "Ed loves Charlie"))

    // Define a class to more clearly model the user property
    case class User(name: String, age: Int, inDeg: Int, outDeg: Int)
    // Create a user Graph
    val initialUserGraph: Graph[User, Int] = graph.mapVertices{ case (id, Person(name, age)) => User(name, age, 0, 0) }
    // Fill in the degree information
    val userGraph = initialUserGraph.outerJoinVertices(initialUserGraph.inDegrees) {
      case (id, u, inDegOpt) => User(u.name, u.age, inDegOpt.getOrElse(0), u.outDeg)
    }.outerJoinVertices(initialUserGraph.outDegrees) {
      case (id, u, outDegOpt) => User(u.name, u.age, u.inDeg, outDegOpt.getOrElse(0))
    }
    val userGraphResult: Set[String] = {
      for ((id, property) <- userGraph.vertices.collect()) yield {
        s"User $id is called ${property.name} and is liked by ${property.inDeg} people."
      }
    }.toSet
    assert(userGraphResult ==
      """User 1 is called Alice and is liked by 2 people.
        |User 2 is called Bob and is liked by 2 people.
        |User 3 is called Charlie and is liked by 1 people.
        |User 4 is called David and is liked by 1 people.
        |User 5 is called Ed and is liked by 0 people.
        |User 6 is called Fran and is liked by 2 people."""
        .stripMargin
        .split("\n")
        .toSet
    )
    val equalLikesResult = userGraph.vertices.filter {
      case (id, u) => u.inDeg == u.outDeg
    }.collect().map {
      case (id, property) => property.name
    }
    equalLikesResult.toSet should be (Set("David", "Bob"))

    // Find the oldest follower for each user
    val oldestFollower: VertexRDD[Person] = userGraph.aggregateMessages[Person](
      // For each edge send a message to the destination vertex with the attribute of the source vertex
      (triplet: EdgeContext[User, PartitionID, Person]) => {
        triplet.sendToDst(Person(triplet.srcAttr.name, triplet.srcAttr.age))
      },
      // To combine messages take the message for the older follower
      (a, b) => if (a.age > b.age) a else b
    )
    val oldestFollowersResult: Array[String] = userGraph.vertices.leftJoin(oldestFollower) { (id, user, optOldestFollower) =>
      optOldestFollower match {
        case None => s"${user.name} does not have any followers."
        case Some(p@Person(name, age)) => s"${name} is the oldest follower of ${user.name}."
      }
    }.collect().map { case (id, str) => str }
    oldestFollowersResult.sorted should be(
      """David is the oldest follower of Alice.
        |Charlie is the oldest follower of Bob.
        |Ed is the oldest follower of Charlie.
        |Bob is the oldest follower of David.
        |Ed does not have any followers.
        |Charlie is the oldest follower of Fran."""
      .stripMargin
      .split("\n")
      .sorted
    )

    val averageAge: VertexRDD[Double] = userGraph.aggregateMessages[(Int, Double)](
      // map function returns a tuple of (1, Age)
      (triplet: EdgeContext[User, PartitionID, (PartitionID, Double)]) => {
        triplet.sendToDst((1, triplet.srcAttr.age.toDouble))
      },
      // reduce function combines (sumOfFollowers, sumOfAge)
      (a, b) => (a._1 + b._1, a._2 + b._2)
    ).mapValues((id, p) => p._2 / p._1)

    // Display the results
    userGraph.vertices.leftJoin(averageAge) { (id, user, optAverageAge) =>
      optAverageAge match {
        case None => s"${user.name} does not have any followers."
        case Some(avgAge) => s"The average age of ${user.name}\'s followers is $avgAge."
      }
    }.collect().foreach { case (id, str) => println(str) }

    val olderGraph = userGraph.subgraph(vpred = (id, user) => user.age >= 30)

    // compute the connected components
    val cc = olderGraph.connectedComponents()

    // display the component id of each user:
    olderGraph.vertices.leftJoin(cc.vertices) {
      case (id, user, comp) => s"${user.name} is in component ${comp.get}"
    }.collect().foreach{ case (id, str) => println(str) }
  }

  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("local[2]").setAppName("HelloSparkGraphX")
    val sc = new SparkContext(conf)
    try {
      run(sc)
      println("Enter any key to finish the job...")
      Console.in.read()
    } catch {
      case NonFatal(t) =>
        throw t
    } finally  {
      sc.stop()
    }
  }
}