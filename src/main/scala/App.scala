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
    case GET -> Root / "list" =>
      Ok(FakeList.list)
    case POST -> Root / "add" =>
      Ok("Adding new node")
    case DELETE -> Root / nameIP =>
      Ok(s"Deleting $nameIP")
  }
}

object FakeList {
  val list = """[
  {
    "name": "tenant1",
    "endpoints": {
      "minecraft": "128.124.90.15:25565",
      "rcon": "128.124.90.15:25575"
    }
  },
  {
    "name": "tenant2",
    "endpoints": {
      "minecraft": "128.194.90.16:25565",
      "rcon": "128.194.90.16:25575"
    }
  },
  {
    "name": "tenant3",
    "endpoints": {
      "minecraft": "128.194.90.15:25565",
      "rcon": "128.194.90.15:25575"
    }
  }
]"""
}
