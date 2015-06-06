
/**
 * Created by chandana on 6/4/15.
 */

import org.apache.spark.SparkConf
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

//import org.apache.spark.streaming.twitter.TwitterUtils

object TwitterPopularTags {
  def main(args: Array[String]) {
    //    if (args.length < 4) {
    //      System.err.println("Usage: TwitterPopularTags <consumer key> <consumer secret> " +
    //        "<access token> <access token secret> [<filters>]")
    //      System.exit(1)
    //    }

//    StreamingExamples.setStreamingLogLevels()

    //    val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)
    val filters = args.takeRight(args.length)

    // Set the system properties so that Twitter4j library used by twitter stream
    // can use them to generat OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", "xxxxxxxxxxxxxxxxxxxxx")
    System.setProperty("twitter4j.oauth.consumerSecret", "xxxxxxxxxxxxxxxxxxxxx")
    System.setProperty("twitter4j.oauth.accessToken", "xxxxxxxxxxxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxx")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "xxxxxxxxxxxxxxxxxxxxx")

    val sparkConf = new SparkConf().setAppName("TwitterPopularTags")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    val stream = TwitterUtils.createStream(ssc, None, filters)

    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))

    val topCounts60 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(60))
      .map{case (topic, count) => (count, topic)}
      .transform(_.sortByKey(false))

    val topCounts10 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(10))
      .map{case (topic, count) => (count, topic)}
      .transform(_.sortByKey(false))


    // Print popular hashtags
    topCounts60.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nPopular topics in last 60 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

    topCounts10.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nPopular topics in last 10 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
