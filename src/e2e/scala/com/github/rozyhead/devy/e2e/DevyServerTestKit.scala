package com.github.rozyhead.devy.e2e

import com.github.rozyhead.devy.DevyServer
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{AsyncTestSuite, FutureOutcome}

import scala.concurrent.Future

/**
  * @author takeshi
  */
abstract class DevyServerTestKit(val server: DevyServer)
    extends AsyncTestSuite {

  def this() = this(new DevyServer(config = DevyServerTestKit.config))

  def this(interface: String) =
    this(
      new DevyServer(interface = interface, config = DevyServerTestKit.config)
    )

  def this(port: Int) = this(new DevyServer(port = port))

  def this(interface: String, port: Int) =
    this(
      new DevyServer(
        interface = interface,
        port = port,
        config = DevyServerTestKit.config
      )
    )

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

object DevyServerTestKit {
  val config: Config = ConfigFactory.parseString("""
      |akka {
      |  actor {
      |    provider = cluster
      |  }
      |  remote {
      |    artery {
      |      transport = tcp
      |      canonical {
      |        hostname = 127.0.0.1
      |        port = 25520
      |      }
      |    }
      |  }
      |  cluster {
      |    seed-nodes = [
      |      "akka://devy-system@127.0.0.1:25520"
      |    ]
      |    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
      |  }
      |  persistence {
      |    journal {
      |      plugin = "akka.persistence.journal.leveldb"
      |      leveldb {
      |        dir = "target/journal"
      |      }
      |    }
      |    snapshot-store {
      |      plugin = "akka.persistence.snapshot-store.local"
      |      local {
      |        dir = "target/snapshots"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
