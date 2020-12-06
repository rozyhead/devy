package com.github.rozyhead.devy.e2e

import com.github.rozyhead.devy.DevyServer
import org.scalatest.{AsyncTestSuite, FutureOutcome}

import scala.concurrent.Future

/**
  * @author takeshi
  */
abstract class DevyServerTestKit(val server: DevyServer)
    extends AsyncTestSuite {

  def this() = this(new DevyServer())

  def this(interface: String) = this(new DevyServer(interface))

  def this(port: Int) = this(new DevyServer(port = port))

  def this(interface: String, port: Int) = this(new DevyServer(interface, port))

  def startServer(): Future[Unit] = server.start()

  def stopServer(): Future[Unit] = server.stop()

  override def withFixture(test: NoArgAsyncTest): FutureOutcome =
    new FutureOutcome(
      for {
        _ <- startServer()
        outcome <- super.withFixture(test).toFuture
        _ <- stopServer()
      } yield {
        outcome
      }
    )

}
