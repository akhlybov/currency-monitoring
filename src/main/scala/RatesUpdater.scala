import java.io.PrintWriter

import cats.data.NonEmptyList
import com.typesafe.scalalogging.LazyLogging
import io.circe.{HCursor, Json}
import io.circe.jawn.CirceSupportParser

object RatesUpdater extends LazyLogging {

  def addNewRates(jsonList: NonEmptyList[String]): Unit = {
    val currentList = parser.parseFromPath(fileName).toOption
    val resJson = jsonList.foldLeft(currentList)(addNewRate)

    resJson foreach { newJson =>
      new PrintWriter(fileName) { write(newJson.spaces2); close() }
    }
  }

  def cleanUp(retentionInterval: Long): Unit = {
    val currentList = parser.parseFromPath(fileName).toOption
    val cleaner = cleanUpCurrency(retentionInterval) _
    val cleanedUpJson = currencies.foldLeft(currentList)(cleaner)

    cleanedUpJson foreach { newJson =>
      new PrintWriter(fileName) { write(newJson.spaces2); close() }
    }
  }

  private def addNewRate(currentList: Option[Json], jsonStr: String): Option[Json] =
    (for {
      parsedJson <- parser.parseFromString(jsonStr).toOption
      cList <- currentList
      currencyName <- parsedJson.hcursor.get[String]("target").toOption
      } yield {
        cList.hcursor.downField(currencyName).withFocus(_.mapArray(ar => parsedJson +: ar)).top
    }).flatten

  private def cleanUpCurrency(retentionInterval: Long)(json: Option[Json], currencyName: String): Option[Json] = {
    json flatMap { json =>
      val curTime = new java.util.Date().getTime
      val cursor: HCursor = json.hcursor

      cursor.downField(currencyName).withFocus { nestedJson =>
        nestedJson.mapArray(_.filter { j =>
          j.hcursor.get[Long]("timestamp").exists(tmstp => (curTime - tmstp) / 1000 < retentionInterval)
        })
      }.top
    }
  }

  val currencies = NonEmptyList.of("GBP", "USD")
  val parser = new CirceSupportParser(None)
  private val fileName = "latest_rates.json"
}