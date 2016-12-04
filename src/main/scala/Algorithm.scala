package org.template.vanilla

import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.predictionio.controller.Params

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkContext


import org.apache.spark.SparkContext
import grizzled.slf4j.Logger

import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.SQLContext
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.ml.regression.LinearRegressionModel
import org.apache.spark.ml.Pipeline

//import org.apache.spark.mllib.linalg.DenseVector
//case class Request(features: DenseVector)

case class AlgorithmParams(val intercept: Double) extends Params

// extends P2LAlgorithm if Model contains RDD[]
class algo(val ap: AlgorithmParams)
    extends P2LAlgorithm[PreparedData, LinearRegressionModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def showModel(model: LinearRegressionModel) = {
    val summary = model.summary
    val modelSummary = Map(
      "coefficientStandardErrors" -> s"[${summary.coefficientStandardErrors.mkString(", ")}]",
      "devianceResiduals"         -> s"[${summary.devianceResiduals.mkString(", ")}]",
      "explainedVariance"         -> summary.explainedVariance,
      "meanAbsoluteError"         -> summary.meanAbsoluteError,
      "meanSquaredError"          -> summary.meanSquaredError,
      "numInstances"              -> summary.numInstances,
      "objectiveHistory"          -> s"[${summary.objectiveHistory.mkString(", ")}]",
      "pValues"                   -> s"[${summary.pValues.mkString(", ")}]",
      "r2"                        -> summary.r2,
      "rootMeanSquaredError"      -> summary.rootMeanSquaredError,
      "tValues"                   -> s"[${summary.tValues.mkString(", ")}]",
      "totalIterations"           -> summary.totalIterations)
    
    val thetas = Array(model.intercept) ++ model.coefficients.toArray
    logger.info("Model Description\n"
      + "Coefficients: " + thetas.mkString(", ") + "\n"
      + modelSummary.toList.mkString("\n"))

  }

  def train(sc: SparkContext, data: PreparedData): LinearRegressionModel = {

    val trainRdd = data.training_points
    val len = trainRdd.count()
    require(len > 0, s"RDD[labeldPoints] in PreparedData cannot be empty.")
    
        
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    val df = data.training_points.toDF().cache()
    df.rdd.name = "TrainingDF"
    df.show()

    val vectorizer = new VectorAssembler()
    vectorizer.setInputCols(Array("AT", "V", "AP", "RH"))
    vectorizer.setOutputCol("features")

    val lr = new LinearRegression()

    lr.setPredictionCol("PE_Predict")
      .setLabelCol("PE")
      .setMaxIter(100)
      .setRegParam(0.1)

    val lrPipeline = new Pipeline()
    lrPipeline.setStages(Array(vectorizer, lr))
    val lrModel = lrPipeline.fit(df)
    val model = lrModel.stages(1).asInstanceOf[LinearRegressionModel]
    showModel(model)
    
    model
  }

  def predict(model: LinearRegressionModel, query: Query): PredictedResult = {
    showModel(model)
    logger.info("Query: " + query.features.mkString(", "))
    logger.info("Coefficients: " + model.coefficients.toArray.mkString(", "))
    
    /* SparkML(not MLLib) library does offer any function to predict based on a Vector */
    
    val prediction = model.intercept + model.coefficients.toArray.zip(query.features).map(e => e._1 * e._2).sum
    logger.info(s"Prediction: $prediction")
    
    new PredictedResult(prediction)
  }

}
