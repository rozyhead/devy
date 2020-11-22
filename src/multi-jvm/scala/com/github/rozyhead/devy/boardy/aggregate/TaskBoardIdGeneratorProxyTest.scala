package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import com.github.rozyhead.akka.testkit.MultiNodeTypedClusterSpec
import com.typesafe.config.ConfigFactory

abstract class TaskBoardIdGeneratorProxyTest
    extends MultiNodeSpec(TaskBoardIdGeneratorProxyTestConfig)
    with MultiNodeTypedClusterSpec {

  import TaskBoardIdGenerator._
  import TaskBoardIdGeneratorProxyTestConfig._

  var sut: ActorRef[Command[_]] = _

  "TaskBoardIdGeneratorProxy" must {
    "be able to form" in {
      formCluster(node1, node2)
    }

    "be able to spawn" in {
      sut = spawn(TaskBoardIdGeneratorProxy(), "taskBoardIdGeneratorProxy")
      enterBarrier("deployed")
    }

    "be able to generate id" in {
      val probe = TestProbe[StatusReply[GenerateTaskBoardIdResponse]]()
      sut ! GenerateTaskBoardId(probe.ref)
      probe.expectMessageType[StatusReply[GenerateTaskBoardIdResponse]]

      enterBarrier("generated")
    }
  }
}

object TaskBoardIdGeneratorProxyTestConfig extends MultiNodeConfig {
  val node1: RoleName = role("node1")
  val node2: RoleName = role("node2")

  commonConfig(
    ConfigFactory
      .parseString("""
        |akka.actor.provider = "cluster"
        |""".stripMargin)
      .withFallback(PersistenceTestKitPlugin.config)
  )
}

class TaskBoardIdGeneratorProxyTestMultiJvmNode1
    extends TaskBoardIdGeneratorProxyTest
class TaskBoardIdGeneratorProxyTestMultiJvmNode2
    extends TaskBoardIdGeneratorProxyTest
