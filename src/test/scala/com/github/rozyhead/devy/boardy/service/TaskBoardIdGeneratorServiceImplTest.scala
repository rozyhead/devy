package com.github.rozyhead.devy.boardy.service

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import com.github.rozyhead.akka.testkit.ActorStub
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardIdGenerator
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.scalatest.freespec.AsyncFreeSpecLike
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.concurrent.TimeoutException

class TaskBoardIdGeneratorServiceImplTest
    extends ScalaTestWithActorTestKit
    with AsyncFreeSpecLike {

  "TaskBoardIdを生成する" - {
    "TaskBoardIdGeneratorProxyが正常終了する場合" - {
      def stub =
        ActorStub[TaskBoardIdGenerator.Command[_]](
          system,
          replySuccess("task-board-id")
        )

      def sut = new TaskBoardIdGeneratorServiceImpl(stub.ref)

      "生成したTaskBoardIdを返す" in {
        sut.generate().map { taskBoardId =>
          assert(taskBoardId == TaskBoardId("task-board-id"))
        }
      }
    }

    "TaskBoardIdGeneratorProxyが異常終了する場合" - {
      def stub =
        ActorStub[TaskBoardIdGenerator.Command[_]](
          system,
          replyFailure("error")
        )

      def sut = new TaskBoardIdGeneratorServiceImpl(stub.ref)

      "エラーを返す" in {
        recoverToSucceededIf[StatusReply.ErrorMessage] {
          sut.generate()
        }
      }
    }

    "TaskBoardIdGeneratorProxyがタイムアウトする場合" - {
      def stub =
        ActorStub[TaskBoardIdGenerator.Command[_]](
          system,
          Behaviors.ignore
        )

      def sut = new TaskBoardIdGeneratorServiceImpl(stub.ref)

      "エラーを返す" in {
        recoverToSucceededIf[TimeoutException] {
          sut.generate()
        }
      }
    }
  }

  override implicit def timeout: Timeout = Timeout(100.millis)

  def replySuccess(
      taskBoardId: => String
  ): Behavior[TaskBoardIdGenerator.Command[_]] = {
    Behaviors.receiveMessage[TaskBoardIdGenerator.Command[_]] { msg =>
      msg match {
        case TaskBoardIdGenerator.GenerateTaskBoardId(replyTo) =>
          replyTo ! StatusReply.success(
            TaskBoardIdGenerator.GenerateTaskBoardIdResponse(
              TaskBoardId(taskBoardId)
            )
          )
      }
      Behaviors.same
    }
  }

  def replyFailure(
      errorMessage: => String
  ): Behavior[TaskBoardIdGenerator.Command[_]] = {
    Behaviors.receiveMessage[TaskBoardIdGenerator.Command[_]] { msg =>
      msg match {
        case TaskBoardIdGenerator.GenerateTaskBoardId(replyTo) =>
          replyTo ! StatusReply.error(errorMessage)
      }
      Behaviors.same
    }
  }

}
