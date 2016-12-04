package org.template.vanilla

import org.apache.predictionio.controller.LServing

case class Query(val features: Array[Double])

class Serving extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query,
    predictedResults: Seq[PredictedResult]): PredictedResult = {
    predictedResults.head
  }
}
