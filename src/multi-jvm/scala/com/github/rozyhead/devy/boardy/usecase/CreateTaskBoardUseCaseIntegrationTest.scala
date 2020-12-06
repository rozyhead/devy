package com.github.rozyhead.devy.boardy.usecase

import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.util.Timeout
import com.github.rozyhead.akka.testkit.MultiNodeTypedClusterSpec
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregateProxy,
  TaskBoardIdGeneratorProxy
}
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.github.rozyhead.devy.boardy.service.{
  TaskBoardAggregateServiceImpl,
  TaskBoardIdGeneratorServiceImpl
}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.DurationInt

abstract class CreateTaskBoardUseCaseIntegrationTest
    extends MultiNodeSpec(CreateTaskBoardUseCaseIntegrationTestConfig)
    with MultiNodeTypedClusterSpec {

  import CreateTaskBoardUseCaseIntegrationTestConfig._

  var sut: CreateTaskBoardUseCase = _

  "CreateTaskBoardUseCase" must {
    "be able to form" in {
      formCluster(node1, node2)
    }
  }

  "be able to spawn" in {
    implicit val timeout: Timeout = Timeout(5.second)
    implicit val ec: ExecutionContextExecutor = typedSystem.executionContext

    val taskBoardIdGeneratorProxy =
      spawn(TaskBoardIdGeneratorProxy(), "taskBoardIdGeneratorProxy")
    val taskBoardIdGeneratorService =
      new TaskBoardIdGeneratorServiceImpl(taskBoardIdGeneratorProxy)

    val taskBoardAggregateProxy =
      spawn(TaskBoardAggregateProxy(), "taskBoardAggregateProxy")
    val taskBoardAggregateService =
      new TaskBoardAggregateServiceImpl(taskBoardAggregateProxy)

    sut = new CreateTaskBoardUseCaseImpl(
      taskBoardIdGeneratorService,
      taskBoardAggregateService
    )

    enterBarrier("deployed")
  }

  var taskBoardId1: TaskBoardId = _
  var taskBoardId2: TaskBoardId = _

  "be able to execute use case" in {
    runOn(node1) {
      val future = sut.run(CreateTaskBoardRequest("test"))
      val response = Await.result(future, 5.second)
      taskBoardId1 = response.asInstanceOf[CreateTaskBoardSuccess].taskBoardId
      enterBarrier("created")
    }
    runOn(node2) {
      val future = sut.run(CreateTaskBoardRequest("test"))
      val response = Await.result(future, 5.second)
      taskBoardId2 = response.asInstanceOf[CreateTaskBoardSuccess].taskBoardId
      enterBarrier("created")
    }

    assert(taskBoardId1 != null)
    assert(taskBoardId2 != null)
    assert(taskBoardId1 != taskBoardId2)
    enterBarrier("asserted")
  }
}

object CreateTaskBoardUseCaseIntegrationTestConfig extends MultiNodeConfig {
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

class CreateTaskBoardUseCaseIntegrationTestMultiJvmNode1
    extends CreateTaskBoardUseCaseIntegrationTest
class CreateTaskBoardUseCaseIntegrationTestMultiJvmNode2
    extends CreateTaskBoardUseCaseIntegrationTest
