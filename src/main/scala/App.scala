package info.ditrapani

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpService, Request, Response, StaticFile}
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder
import org.json4s.JArray
import org.json4s.native.JsonMethods.{compact, render}

object Main extends StreamApp[IO] {
  def stream(args: List[String], requestShutdown: IO[Unit]) = {
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(new Server().service, "/")
      .serve(Effect[IO], ExecutionContext.global)
  }
}

final case class Minecraft(name: String, ip: String)

class Server extends Http4sDsl[IO] {
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var count = new scala.util.Random().nextInt()

  private val client = {
    val config = new ConfigBuilder()
      .withOauthToken("44a9cc2e9def072af95cace024b393c2")
      .withMasterUrl(
        "https://akscluster-amstradcpc-198f62-0d9ac1c4.hcp.eastus.azmk8s.io")
      .build()
    new DefaultKubernetesClient(config)
  }

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
      import org.json4s.JsonDSL._
      val json = getServerList().map(
        mc =>
          ("name" -> mc.name) ~
            ("endpoints" ->
              (("minecraft" -> s"${mc.ip}:25565") ~
                ("rcon" -> s"${mc.ip}:25575"))))
      Ok(compact(render(JArray(json))))
    case POST -> Root / "add" =>
      addNode()
      Ok("Adding new node")
    case DELETE -> Root / nameIP =>
      Ok(s"Deleting $nameIP")
  }

  def getServerList(): List[Minecraft] = {
    import scala.collection.JavaConverters._
    val items = client.services().list().getItems().asScala
    items
      .map(service => {
        val name = service.getMetadata().getName()
        val ingresses = service.getStatus().getLoadBalancer().getIngress()
        (name, ingresses)
      })
      .filter(pair => pair._2.size > 0)
      .map(pair => Minecraft(pair._1, pair._2.get(0).getIp()))
      .toList
  }

  def addNode(): Unit = {
    count += 1
    val deployment = new DeploymentBuilder()
      .withNewMetadata()
      .withName(s"mc-$count")
      .endMetadata()
      .withNewSpec()
      .withReplicas(1)
      .withNewTemplate()
      .withNewSpec()
      .addNewContainer()
      .withName("minecraft")
      .withImage("openhack/minecraft-server:2.0")
      .addNewPort()
      .withContainerPort(25565)
      .endPort()
      .endContainer()
      .endSpec()
      .endTemplate()
      .endSpec()
      .build()
  }
}
