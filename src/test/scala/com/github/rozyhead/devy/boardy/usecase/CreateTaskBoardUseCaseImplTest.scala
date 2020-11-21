package com.github.rozyhead.devy.boardy.usecase

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardAggregate
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.github.rozyhead.devy.boardy.service.TaskBoardIdGenerator
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class CreateTaskBoardUseCaseImplTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with MockFactory {

  implicit val ec: ExecutionContext = testKit.system.executionContext

  private trait MockedTaskBoardIdGenerator {
    def generateTaskBoardId: String

    def mockedTaskBoardIdGenerator = {
      val mocked = mock[TaskBoardIdGenerator]

      (mocked.generate _)
        .expects()
        .returning(Future(TaskBoardId(generateTaskBoardId)))

      mocked
    }
  }

  private trait MockedTaskBoardAggregateProxy {
    val replyAck =
      Behaviors.receiveMessage[TaskBoardAggregate.Command] { msg =>
        msg.replyTo ! StatusReply.ack()
        Behaviors.same
      }

    val mockedTaskBoardAggregateProxyBehavior: Behavior[
      TaskBoardAggregate.Command
    ]

    val taskBoardAggregateProxyProbe =
      testKit.createTestProbe[TaskBoardAggregate.Command]()

    def mockedTaskBoardAggregateProxy =
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
      override def generateTaskBoardId: String = "task-board-id"
      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      val sut = new CreateTaskBoardUseCaseImpl(
        mockedTaskBoardIdGenerator,
        mockedTaskBoardAggregateProxy
      )

      val future = sut.run(CreateTaskBoardRequest("test"))
      val response = Await.result(future, 1.second)

      assert(
        response == CreateTaskBoardResponse(taskBoardId =
          TaskBoardId("task-board-id")
        )
      )
    }

    "call taskBoardAggregateProxy" in new Fixture {
      override def generateTaskBoardId: String = "task-board-id"
      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      val sut = new CreateTaskBoardUseCaseImpl(
        mockedTaskBoardIdGenerator,
        mockedTaskBoardAggregateProxy
      )

      val future = sut.run(CreateTaskBoardRequest("test"))
      Await.result(future, 1.second)

      val command = taskBoardAggregateProxyProbe
        .expectMessageType[TaskBoardAggregate.CreateTaskBoard]

      assert(command.taskBoardId == TaskBoardId("task-board-id"))
      assert(command.title == "test")
    }
  }
}
