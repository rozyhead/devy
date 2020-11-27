package com.github.rozyhead.devy.boardy.service

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import com.github.rozyhead.akka.testkit.ActorStub
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardIdGenerator
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpecLike
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.concurrent.{Future, TimeoutException}

class TaskBoardIdGeneratorServiceImplTest
    extends ScalaTestWithActorTestKit
    with AsyncFreeSpecLike {

  "TaskBoardIdを生成する" - {
    "TaskBoardIdGeneratorProxyを呼び出す" in fixture(replySuccess("test")) {
      (sut, stub) =>
        sut.generate().map { _ =>
          stub.probe
            .expectMessageType[TaskBoardIdGenerator.GenerateTaskBoardId]
          succeed
        }
    }

    "TaskBoardIdGeneratorProxyが正常終了する場合" - {
      "生成したTaskBoardIdを返す" in fixture(replySuccess("task-board-id")) {
        (sut, _) =>
          sut.generate().map { taskBoardId =>
            assert(taskBoardId == TaskBoardId("task-board-id"))
          }
      }
    }

    "TaskBoardIdGeneratorProxyが異常終了する場合" - {
      "エラーを返す" in fixture(replyFailure("error")) { (sut, _) =>
        recoverToSucceededIf[StatusReply.ErrorMessage] {
          sut.generate()
        }
      }
    }

    "TaskBoardIdGeneratorProxyがタイムアウトする場合" - {
      "エラーを返す" in fixture(Behaviors.ignore) { (sut, _) =>
        recoverToSucceededIf[TimeoutException] {
          sut.generate()
        }
      }
    }
  }

  override implicit def timeout: Timeout = Timeout(100.millis)

  def fixture(behavior: Behavior[TaskBoardIdGenerator.Command[_]])(
      f: (
          TaskBoardIdGeneratorService,
          ActorStub[TaskBoardIdGenerator.Command[_]]
      ) => Future[Assertion]
  ): Future[Assertion] = {
    val stub =
      ActorStub[TaskBoardIdGenerator.Command[_]](system, behavior)
    val sut = new TaskBoardIdGeneratorServiceImpl(stub.ref)
    f(sut, stub)
  }

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
