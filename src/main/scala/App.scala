package info.ditrapani

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Request, Response, StaticFile}
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext

object Main extends StreamApp[IO] {
  def stream(args: List[String], requestShutdown: IO[Unit]) = {
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(new Server().service, "/")
      .serve(Effect[IO], ExecutionContext.global)
  }
}

class Server extends Http4sDsl[IO] {
  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  def static(file: String, request: Request[IO]): IO[Response[IO]] =
    StaticFile.fromResource("/" + file, Some(request)).getOrElseF(NotFound())

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  val service: HttpService[IO] = HttpService[IO] {
    case request @ GET -> Root =>
      static("index.html", request)
    case request @ GET -> Root / "js" / file =>
      static(s"js/$file", request)
  }
}
