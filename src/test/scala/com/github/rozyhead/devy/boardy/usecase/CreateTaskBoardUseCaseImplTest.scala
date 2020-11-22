package com.github.rozyhead.devy.boardy.usecase

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardIdGenerator.GenerateTaskBoardIdResponse
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregate,
  TaskBoardIdGenerator
}
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class CreateTaskBoardUseCaseImplTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with MockFactory {

  implicit val ec: ExecutionContext = testKit.system.executionContext

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

  private trait Fixture
      extends MockedTaskBoardIdGenerator
      with MockedTaskBoardAggregateProxy

  "CreateTaskBoardUseCaseImpl" must {
    "return response with generated TaskBoardId" in new Fixture {
      override val mockedTaskBoardIdGeneratorProxyBehavior
          : Behavior[TaskBoardIdGenerator.Command[_]] =
        replySuccess("task-board-id")

      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      val sut = new CreateTaskBoardUseCaseImpl(
        mockedTaskBoardIdGeneratorProxy,
        mockedTaskBoardAggregateProxy
      )

      private val future = sut.run(CreateTaskBoardRequest("test"))
      private val response = Await.result(future, 1.second)

      assert(
        response == CreateTaskBoardResponse(taskBoardId =
          TaskBoardId("task-board-id")
        )
      )
    }

    "call taskBoardAggregateProxy" in new Fixture {
      override val mockedTaskBoardIdGeneratorProxyBehavior
          : Behavior[TaskBoardIdGenerator.Command[_]] =
        replySuccess("task-board-id")

      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      val sut = new CreateTaskBoardUseCaseImpl(
        mockedTaskBoardIdGeneratorProxy,
        mockedTaskBoardAggregateProxy
      )

      private val future = sut.run(CreateTaskBoardRequest("test"))
      Await.result(future, 1.second)

      private val command = taskBoardAggregateProxyProbe
        .expectMessageType[TaskBoardAggregate.CreateTaskBoard]

      assert(command.taskBoardId == TaskBoardId("task-board-id"))
      assert(command.title == "test")
    }
  }
}
