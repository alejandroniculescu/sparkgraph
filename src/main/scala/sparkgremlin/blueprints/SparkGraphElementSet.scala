package sparkgremlin.blueprints

import org.apache.spark.graphx.{Graph => GraphXGraph, EdgeRDD, VertexRDD}
import org.apache.spark.rdd.RDD

/**
 * Created by kellrott on 2/8/14.
 */
abstract class SparkGraphElementSet[E] extends RDDIterator[E] {
  def graphX() : GraphXGraph[SparkVertex, SparkEdge]
  def selectVertexRDD() : VertexRDD[Boolean]
  def selectEdgeRDD() : EdgeRDD[Boolean]
}
