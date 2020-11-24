package com.github.rozyhead.devy.boardy.usecase

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardIdGenerator.GenerateTaskBoardIdResponse
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregate,
  TaskBoardIdGenerator
}
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike

import scala.concurrent.ExecutionContext

class CreateTaskBoardUseCaseImplTest
    extends ScalaTestWithActorTestKit
    with AnyFreeSpecLike
    with ScalaFutures {

  implicit val ec: ExecutionContext = testKit.system.executionContext

  "TaskBoard作成ユースケース" - {

    "生成したTaskBoardIdを返却する" in new Fixture {
      // Given
      override val mockedTaskBoardIdGeneratorProxyBehavior
          : Behavior[TaskBoardIdGenerator.Command[_]] =
        replySuccess("task-board-id")

      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      // When
      whenReady(sut.run(CreateTaskBoardRequest("test"))) { response =>
        // Then
        assert(
          response == CreateTaskBoardResponse(
            taskBoardId = TaskBoardId("task-board-id")
          )
        )
      }
    }

    "TaskBoardAggregateProxyにコマンドCreateTaskBoardを送信する" in new Fixture {
      // Given
      override val mockedTaskBoardIdGeneratorProxyBehavior
          : Behavior[TaskBoardIdGenerator.Command[_]] =
        replySuccess("task-board-id")

      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      // When
      whenReady(sut.run(CreateTaskBoardRequest("test"))) { _ =>
        // Then
        val command = taskBoardAggregateProxyProbe
          .expectMessageType[TaskBoardAggregate.CreateTaskBoard]

        assert(command.taskBoardId == TaskBoardId("task-board-id"))
        assert(command.title == "test")
      }
    }
  }

  private trait Fixture
      extends MockedTaskBoardIdGenerator
      with MockedTaskBoardAggregateProxy {

    lazy val sut = new CreateTaskBoardUseCaseImpl(
      mockedTaskBoardIdGeneratorProxy,
      mockedTaskBoardAggregateProxy
    )
  }

  private trait MockedTaskBoardIdGenerator {
    protected def replySuccess(
        taskBoardId: => String
    ): Behavior[TaskBoardIdGenerator.Command[_]] = {
      Behaviors.receiveMessage[TaskBoardIdGenerator.Command[_]] { msg =>
        msg match {
          case TaskBoardIdGenerator.GenerateTaskBoardId(replyTo) =>
            replyTo ! StatusReply.success(
              GenerateTaskBoardIdResponse(
                TaskBoardId(taskBoardId)
              )
            )
        }
        Behaviors.same
      }
    }

    protected val mockedTaskBoardIdGeneratorProxyBehavior: Behavior[
      TaskBoardIdGenerator.Command[_]
    ]

    protected val taskBoardIdGeneratorProxyProbe
        : TestProbe[TaskBoardIdGenerator.Command[_]] =
      testKit.createTestProbe[TaskBoardIdGenerator.Command[_]]()

    protected def mockedTaskBoardIdGeneratorProxy
        : ActorRef[TaskBoardIdGenerator.Command[_]] = {
      testKit.spawn(
        Behaviors.monitor(
          taskBoardIdGeneratorProxyProbe.ref,
          mockedTaskBoardIdGeneratorProxyBehavior
        )
      )
    }
  }

  private trait MockedTaskBoardAggregateProxy {
    protected val replyAck: Behaviors.Receive[TaskBoardAggregate.Command] =
      Behaviors.receiveMessage[TaskBoardAggregate.Command] { msg =>
        msg.replyTo ! StatusReply.ack()
        Behaviors.same
      }

    protected val mockedTaskBoardAggregateProxyBehavior: Behavior[
      TaskBoardAggregate.Command
    ]

    protected val taskBoardAggregateProxyProbe
        : TestProbe[TaskBoardAggregate.Command] =
      testKit.createTestProbe[TaskBoardAggregate.Command]()

    protected def mockedTaskBoardAggregateProxy
        : ActorRef[TaskBoardAggregate.Command] =
      testKit.spawn(
        Behaviors.monitor(
          taskBoardAggregateProxyProbe.ref,
          mockedTaskBoardAggregateProxyBehavior
        )
      )
  }

}
