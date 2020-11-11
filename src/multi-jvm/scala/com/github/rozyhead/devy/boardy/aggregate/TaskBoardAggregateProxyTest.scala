package com.github.rozyhead.devy.boardy.aggregate

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.pattern.StatusReply
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import com.github.rozyhead.akka.testkit.MultiNodeTypedClusterSpec
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.typesafe.config.ConfigFactory

class TaskBoardAggregateProxyTestMultiJvmNode1
    extends TaskBoardAggregateProxyTest
class TaskBoardAggregateProxyTestMultiJvmNode2
    extends TaskBoardAggregateProxyTest

object TaskBoardAggregateProxyTestConfig extends MultiNodeConfig {
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

/**
  * @author takeshi
  */
abstract class TaskBoardAggregateProxyTest
    extends MultiNodeSpec(TaskBoardAggregateProxyTestConfig)
    with MultiNodeTypedClusterSpec {

  import TaskBoardAggregate._
  import TaskBoardAggregateProxyTestConfig._

  var sut: ActorRef[Command] = _

  "TaskBoardAggregateProxy" must {
    "be able to form" in {
      formCluster(node1, node2)
    }

    "be able to spawn" in {
      sut = spawn(TaskBoardAggregateProxy(), "taskBoardAggregateProxy")
      enterBarrier("deployed")
    }

    "be able to create TaskBoardAggregate" in {
      for (i <- 1 to 5) {
        runOn(node1) {
          // given
          val probe = TestProbe[StatusReply[Done]]
          val command =
            CreateTaskBoard(TaskBoardId(s"test-$i"), s"test-$i", probe.ref)

          // when
          sut ! command

          // then
          probe.expectMessage(StatusReply.ack())
        }
      }

      enterBarrier("created")
    }
  }
}
