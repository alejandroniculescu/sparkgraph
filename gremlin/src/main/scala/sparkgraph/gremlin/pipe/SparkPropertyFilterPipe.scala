package sparkgraph.gremlin.pipe

//import collection.JavaConverters._

import sparkgraph.blueprints.{SparkEdge, SparkVertex, SparkGraphElement}
import com.tinkerpop.blueprints.Predicate
import com.tinkerpop.pipes.util.PipeHelper
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._
import sparkgraph.gremlin._
import org.apache.spark.graphx

/**
 * Created by kellrott on 2/8/14.
 */
object SparkPropertyFilterPipe {
   def filterVertex( vert : SparkVertex, state : GremlinVertex, key:String, predicate : Predicate, value : AnyRef ) : Boolean = {
     return predicate.evaluate(vert.getProperty(key), value)
   }

   def filterVertexID( vert:SparkVertex, state: GremlinVertex, predicate: Predicate, value: AnyRef) : Boolean = {
     val vid : java.lang.Long = value match {
       case x : java.lang.Long => x;
       case _ => value.toString.toLong
     }
     val out = predicate.evaluate(vert.id, vid);
     return out;
   }

   def filterEdges( edge: SparkEdge, key:String, predicate: Predicate, value: AnyRef) : Boolean = {
     return predicate.evaluate(edge.getProperty(key), value)
   }

   def filterEdgesLabels( edge: SparkEdge, predicate: Predicate, value: AnyRef) : Boolean = {
     return predicate.evaluate(edge.label, value)
   }

   def filterEdgesID( edge:SparkEdge, predicate: Predicate, value: AnyRef) : Boolean = {
     val vid : java.lang.Long = value match {
       case x : java.lang.Long => x;
       case _ => value.toString.toLong
     }
     return predicate.evaluate(edge.id, vid)
   }
 }



class SparkPropertyFilterPipe extends BulkPipe[SparkGraphElement, SparkGraphElement] {
  val id : AnyRef = null;
  val label : String = null;
  var key : String = null;
  var value: AnyRef = null
  var predicate: Predicate = null

  def this(key: String, predicate: Predicate, value: AnyRef) {
    this()
    this.key = key;
    this.value = value
    this.predicate = predicate
  }

  override def toString: String = {
    return PipeHelper.makePipeString(this, this.predicate, this.key)
  }

  def bulkReader(input: java.util.Iterator[SparkGraphElement]) : BulkPipeData[SparkGraphElement] = {
    throw new SparkPipelineException(SparkPipelineException.NON_READER);
  }

  def bulkProcess() : SparkGraphBulkData[SparkGraphElement] = {
    val y = bulkStarts.asInstanceOf[SparkGraphBulkData[SparkGraphElement]];
    val searchKey = key;
    val searchPredicate = predicate;

    if (y.elementType == BulkDataType.VERTEX_DATA) {
      if (this.id != null || this.key == "id") {
        val searchValue = if (id != null) {
          this.id
        } else {
          this.value
        }
        val step = y.stateGraph.mapVertices( (vid,x) => {
          if ( SparkPropertyFilterPipe.filterVertexID( x._1, x._2, searchPredicate, searchValue ) ) {
            (x._1, x._2)
          } else {
            (x._1, x._2.zero())
          }
        })
        return new SparkGraphBulkData[SparkGraphElement](y.graphData, step, y.asColumns, BulkDataType.VERTEX_DATA, null ) {
          def currentRDD(): RDD[SparkGraphElement] = stateGraph.vertices.filter(_._2._2.travelerCount > 0).map(_._2._1)
        }
      } else {
        val searchValue = value
        val step = y.stateGraph.mapVertices( (vid,x) => {
          if ( SparkPropertyFilterPipe.filterVertex(x._1, x._2, searchKey, searchPredicate, searchValue ) ) {
            (x._1, x._2)
          } else {
            (x._1, x._2.zero())
          }
        })
        return new SparkGraphBulkData[SparkGraphElement](y.graphData, step, y.asColumns, BulkDataType.VERTEX_DATA, null ) {
          def currentRDD(): RDD[SparkGraphElement] = stateGraph.vertices.filter(_._2._2.travelerCount > 0).map(_._2._1)
        }
      }
    }
    if (y.elementType == BulkDataType.EDGE_DATA) {
      val newStateGraph = if (label != null || key == "label") {
        val searchValue = if (label != null) {
          label;
        } else {
          value ;
        }
        y.stateGraph.mapEdges( x=> (x.attr._1, SparkPropertyFilterPipe.filterEdgesLabels(x.attr._1, searchPredicate, searchValue) ) )
      } else if (id != null || key == "id") {
        val searchValue = if (id != null) {
          id;
        } else {
          value;
        }
        y.stateGraph.mapEdges( x=> (x.attr._1, SparkPropertyFilterPipe.filterEdgesID(x.attr._1, searchPredicate, searchValue)) )
      } else {
        val searchValue = value
        y.stateGraph.mapEdges( x=> (x.attr._1, SparkPropertyFilterPipe.filterEdges(x.attr._1, searchKey, searchPredicate, searchValue)) )
      }
      return new SparkGraphBulkData[SparkGraphElement](
        y.graphData, newStateGraph, y.asColumns, BulkDataType.EDGE_DATA, null
      ) {
        def currentRDD(): RDD[SparkGraphElement] = {
          newStateGraph.edges.filter( _.attr._2 ).map( _.attr._1 )
        }
      }
    }
    return null;
  }
}
