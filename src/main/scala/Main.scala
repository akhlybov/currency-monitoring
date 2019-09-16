import java.util.concurrent.TimeUnit

import cats.data.NonEmptyList
import cats.effect.{ContextShift, Effect, ExitCase, ExitCode, IO, IOApp, SyncIO, Timer}
import cats.syntax.all._
import cats.syntax.list._
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.io.Source

object Main extends IOApp {

  import RatesUpdater._

  def run(args: List[String]): IO[ExitCode] = {
    implicit val backend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend[cats.effect.IO]()

    val conf = ConfigFactory.load()
    val fetchInterval = conf.getDuration("jobs.fetch_rates.schedule.interval").toMillis
    val finiteFetchInterval = FiniteDuration(fetchInterval, TimeUnit.MILLISECONDS)
    val cleanUpInterval = conf.getDuration("jobs.rates_cleanup.schedule.interval").toMillis
    val finiteCleanUpInterval = FiniteDuration(cleanUpInterval, TimeUnit.MILLISECONDS)

    val apiKey = Source.fromResource("api_key.txt").getLines().next()
    val requestBody = s"http://api.coinlayer.com/live?access_key=$apiKey&target="
    val nenUri = RatesUpdater.currencies.map(c => uri"$requestBody$c")
    val nenResponses: IO[NonEmptyList[Response[String]]] = nenUri parTraverse { _uri => sttp.get(_uri).send() }

    val x = for {
      lr <- nenResponses
      fr <- IO.shift *> IO.sleep(finiteFetchInterval) >> IO { addNewRates(lr.map(_.unsafeBody)) }.start
      fc <- IO.shift *> IO.sleep(finiteCleanUpInterval) >> IO { cleanUp(cleanUpInterval) }.start
      _ <- fr.join
      _ <- fc.join
    } yield ()

    x as ExitCode.Success
  }
}
