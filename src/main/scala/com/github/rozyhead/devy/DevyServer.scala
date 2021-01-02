package com.github.rozyhead.devy

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn

sealed trait DevyServerState

case object Starting extends DevyServerState

case class Started(binding: ServerBinding, system: ActorSystem[_])
    extends DevyServerState

case object Stopping extends DevyServerState

case object Stopped extends DevyServerState

class DevyServer(val interface: String = "localhost", val port: Int = 8080)
    extends Directives
    with JsonSupport {
  private val logger = LoggerFactory.getLogger(classOf[DevyServer])
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
    logger.info("Starting {}", this)

    state = Starting

    implicit val system: ActorSystem[SpawnProtocol.Command] =
      ActorSystem(SpawnProtocol(), "devy-system")
    implicit val ec: ExecutionContextExecutor = system.executionContext
    implicit val timeout: Timeout = Timeout(10.seconds)

    val future = for {
      serviceLocator <- ServiceLocator(system, timeout)
      binding <- Http()
        .newServerAt(interface, port)
        .bind(new RootController(serviceLocator.createTaskBoardUseCase).route)
    } yield {
      state = Started(binding, system)
      logger.info("Started {}", this)
    }

    future.recoverWith {
      case error: Throwable =>
        state = Stopped
        logger.info("Stopped {}", this)
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

    def terminate(system: ActorSystem[_]) = {
      system.terminate()
      system.whenTerminated
    }

    val future = for {
      _ <- binding.unbind()
      _ <- terminate(system)
    } yield {
      state = Stopped
    }

    future.recoverWith {
      case error: Throwable =>
        system.terminate()
        state = Stopped
        Future.failed(error)
    }
  }

  override def toString: String = s"DevyServer($baseUri)"
}

object DevyServer {

  def main(args: Array[String]): Unit = {
    val port = if (args.length > 0) args(0) else "8080"
    val server = new DevyServer(port = port.toInt)
    Await.ready(server.start(), 5.seconds)
    StdIn.readLine()
    Await.ready(server.stop(), 5.seconds)
  }

}
