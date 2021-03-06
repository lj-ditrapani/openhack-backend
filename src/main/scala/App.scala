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
import io.fabric8.kubernetes.api.model.ServiceBuilder
import org.json4s.JArray
import org.json4s.native.JsonMethods.{compact, render}
import io.fabric8.kubernetes.api.model.IntOrString

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
      .withOauthToken("e8be1230840c7071c06eb1c72e01698c")
      .withMasterUrl(
        "https://lipikaclus-lipikaresourcegr-198f62-bee76327.hcp.eastus.azmk8s.io")
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
    case DELETE -> Root / name =>
      client.services().withName(name).delete()
      client.extensions().deployments().withName(name).delete()
      Ok(s"Deleting $name")
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
    val name = s"mc-$count"
    import scala.collection.JavaConverters._
    val selector = Map("app" -> name).asJava
    val deployment = new DeploymentBuilder()
      .withNewMetadata()
      .withName(name)
      .endMetadata()
      .withNewSpec()
      .withReplicas(1)
      .withNewTemplate()
      .withNewMetadata()
      .addToLabels("app", name)
      .endMetadata()
      .withNewSpec()
      .addNewContainer()
      .withName("minecraft")
      .withImage("openhack/minecraft-server:2.0")
      .addNewEnv()
      .withName("EULA")
      .withValue("TRUE")
      .endEnv()
      .addNewPort()
      .withContainerPort(25565)
      .withName("game")
      .endPort()
      .addNewPort()
      .withContainerPort(25575)
      .withName("rcon")
      .endPort()
      .endContainer()
      .endSpec()
      .endTemplate()
      .endSpec()
      .build()
    client.extensions().deployments().inNamespace("default").create(deployment)
    val service = new ServiceBuilder()
      .withNewMetadata()
      .withName(name)
      .endMetadata()
      .withNewSpec()
      .withType("LoadBalancer")
      .addNewPort()
      .withPort(25565)
      .withTargetPort(new IntOrString(25565))
      .withName("game")
      .endPort()
      .addNewPort()
      .withPort(25575)
      .withTargetPort(new IntOrString(25575))
      .withName("rcon")
      .endPort()
      .withSelector(selector)
      .endSpec()
      .build()
    client.services().inNamespace("default").create(service)
    (): Unit
  }
}
