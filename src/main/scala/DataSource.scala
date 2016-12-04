package org.template.vanilla

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.EmptyActualResult
import org.apache.predictionio.controller.Params
import org.apache.predictionio.data.storage.Event
import org.apache.predictionio.data.storage.Storage
import org.apache.predictionio.data.store.PEventStore

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors

import grizzled.slf4j.Logger

case class DataSourceParams(val appId: Int, val appName:String) extends Params
case class DataPoint(AT: Double, V: Double, AP: Double, RH: Double, PE: Double)
class TrainingData(val training_points: RDD[DataPoint]) extends Serializable


class DataSource(val dsp: DataSourceParams)
    extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override def readTraining(sc: SparkContext): TrainingData = {
    
    val appName = dsp.appName
    val validProperties = Some(Seq("AT", "V", "AP", "RH"))
    
    logger.info("Gathering data from the event server")
    val eventsRdd = PEventStore.aggregateProperties(appName = appName
                          , entityType = "iot", required = validProperties)(sc)
    
    logger.info(s"No of records: ${eventsRdd.count}")
    logger.info("Sample Records: \n" + eventsRdd.take(10).mkString("\n"))

    val dataPointsRdd: RDD[DataPoint] = eventsRdd.map{
        case (entityId, properties) =>
          try {
            DataPoint(
              properties.get[String]("AT").toDouble,
              properties.get[String]("V").toDouble,
              properties.get[String]("AP").toDouble,
              properties.get[String]("RH").toDouble,
              properties.get[String]("PE").toDouble)

          } catch {
            case e: Exception => {
              logger.error(s"Failed to get properties ${properties} of ${entityId}. Exception: ${e}.")
              throw e
            }
          }
      }
      new TrainingData(dataPointsRdd)
  }
}

