
package org.template.vanilla


import org.apache.predictionio.controller.IEngineFactory
import org.apache.predictionio.controller.Engine



case class PredictedResult(val prediction: Double)

object VanillaEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("algo" -> classOf[algo]),
      classOf[Serving])
  }
}
