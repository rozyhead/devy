package com.github.rozyhead.devy.boardy.usecase

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregate,
  TaskBoardIdGenerator
}
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.github.rozyhead.devy.boardy.service.TaskBoardIdGeneratorService
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike

import scala.concurrent.{ExecutionContext, Future}

class CreateTaskBoardUseCaseImplTest
    extends ScalaTestWithActorTestKit
    with AnyFreeSpecLike
    with ScalaFutures
    with MockFactory {

  implicit val ec: ExecutionContext = testKit.system.executionContext

  "TaskBoard作成ユースケース" - {
    "生成したTaskBoardのIdを返却する" in new Fixture {
      // Given
      (mockedTaskBoardIdGeneratorService.generate _)
        .expects()
        .returning(Future {
          TaskBoardId("task-board-id")
        })

      override val mockedTaskBoardAggregateProxyBehavior
          : Behavior[TaskBoardAggregate.Command] = replyAck

      // When
      whenReady(sut.run(CreateTaskBoardRequest("test"))) { response =>
        // Then
        assert(
          response == CreateTaskBoardSuccess(
            taskBoardId = TaskBoardId("task-board-id")
          )
        )
      }
    }

    "TaskBoardAggregateProxyにコマンドCreateTaskBoardを送信する" in new Fixture {
      // Given
      (mockedTaskBoardIdGeneratorService.generate _)
        .expects()
        .returning(Future {
          TaskBoardId("task-board-id")
        })

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

    "(異常系)TaskBoardIdGeneratorServiceがエラーを返却した場合" - {
      "実行が失敗する" in new Fixture {
        // Given
        (mockedTaskBoardIdGeneratorService.generate _)
          .expects()
          .returning(Future.failed(new IllegalStateException()))

        override val mockedTaskBoardAggregateProxyBehavior
            : Behavior[TaskBoardAggregate.Command] = replyAck

        // When
        whenReady(sut.run(CreateTaskBoardRequest("test"))) { response =>
          // Then
          response shouldBe a[CreateTaskBoardFailure]
        }
      }
    }

    "(異常系)TaskBoardAggregateProxyがエラーを返却した場合" - {
      "実行が失敗する" in new Fixture {
        // Given
        (mockedTaskBoardIdGeneratorService.generate _)
          .expects()
          .returning(Future {
            TaskBoardId("task-board-id")
          })

        override val mockedTaskBoardAggregateProxyBehavior
            : Behavior[TaskBoardAggregate.Command] = replyError

        // When
        whenReady(sut.run(CreateTaskBoardRequest("test"))) { response =>
          // Then
          response shouldBe a[CreateTaskBoardFailure]
        }
      }
    }
  }

  private trait Fixture extends MockedTaskBoardAggregateProxy {
    lazy val mockedTaskBoardIdGeneratorService: TaskBoardIdGeneratorService =
      mock[TaskBoardIdGeneratorService]

    lazy val sut = new CreateTaskBoardUseCaseImpl(
      mockedTaskBoardIdGeneratorService,
      mockedTaskBoardAggregateProxy
    )
  }

  private trait MockedTaskBoardAggregateProxy {
    protected val replyAck: Behaviors.Receive[TaskBoardAggregate.Command] =
      Behaviors.receiveMessage[TaskBoardAggregate.Command] { msg =>
        msg.replyTo ! StatusReply.ack()
        Behaviors.same
      }

    protected val replyError: Behaviors.Receive[TaskBoardAggregate.Command] =
      Behaviors.receiveMessage[TaskBoardAggregate.Command] { msg =>
        msg.replyTo ! StatusReply.error("Failure")
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
