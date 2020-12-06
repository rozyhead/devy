package com.github.rozyhead.devy

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn

sealed trait DevyServerState

case object Starting extends DevyServerState

case class Started(binding: ServerBinding, system: ActorSystem[_])
    extends DevyServerState

case object Stopping extends DevyServerState

case object Stopped extends DevyServerState

class DevyServer(val interface: String = "localhost", val port: Int = 8080) {
  private var state: DevyServerState = Stopped

  def baseUri: String = s"http://$interface:$port"

  def start(): Future[Unit] = {
    state match {
      case Stopped =>
        doStart()

      case _ =>
        Future.failed(
          new IllegalArgumentException(s"server state is $state")
        )
    }
  }

  private def doStart(): Future[Unit] = {
    state = Starting

    implicit val system: ActorSystem[Nothing] =
      ActorSystem(Behaviors.empty, "devy-system")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Hello"))
        }
      }

    val future = for {
      binding <- Http().newServerAt(interface, port).bind(route)
    } yield {
      state = Started(binding, system)
    }

    future.recoverWith {
      case error: Throwable =>
        state = Stopped
        Future.failed(error)
    }
  }

  def stop(): Future[Unit] = {
    state match {
      case Started(binding, system) =>
        doStop(binding, system)

      case _ =>
        Future.failed(
          new IllegalArgumentException(s"server state is $state")
        )
    }
  }

  private def doStop(
      binding: ServerBinding,
      system: ActorSystem[_]
  ): Future[Unit] = {
    state = Stopping

    implicit val ec: ExecutionContextExecutor = system.executionContext

    val future = for {
      _ <- binding.unbind()
    } yield {
      system.terminate()
      system.whenTerminated
      state = Stopped
    }

    future.recoverWith {
      case error: Throwable =>
        system.terminate()
        state = Stopped
        Future.failed(error)
    }
  }
}

object DevyServer {

  def main(args: Array[String]): Unit = {
    val server = new DevyServer()
    Await.ready(server.start(), 5.seconds)
    StdIn.readLine()
    Await.ready(server.stop(), 5.seconds)
  }

}
