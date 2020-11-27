package com.github.rozyhead.devy.boardy.service

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import com.github.rozyhead.akka.testkit.ActorStub
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardAggregate
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpecLike
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.concurrent.{Future, TimeoutException}

class TaskBoardAggregateServiceImplTest
    extends ScalaTestWithActorTestKit
    with AsyncFreeSpecLike {

  "TaskBoardを生成する" - {
    "TaskBoardAggregateProxyを呼び出す" in fixture(replyAck) { (sut, stub) =>
      sut.createTaskBoard(TaskBoardId("test"), "title").map { _ =>
        val msg =
          stub.probe.expectMessageType[TaskBoardAggregate.CreateTaskBoard]
        msg.taskBoardId shouldBe TaskBoardId("test")
        msg.title shouldBe "title"
        succeed
      }
    }

    "TaskBoardAggregateProxyが正常終了する場合" - {
      "正常を返す" in fixture(replyAck) { (sut, _) =>
        sut.createTaskBoard(TaskBoardId("test"), "title").map { _ =>
          succeed
        }
      }
    }

    "TaskBoardAggregateProxyが異常終了する場合" - {
      "エラーを返す" in fixture(replyError) { (sut, _) =>
        recoverToSucceededIf[StatusReply.ErrorMessage] {
          sut.createTaskBoard(TaskBoardId("test"), "title")
        }
      }
    }

    "TaskBoardAggregateProxyがタイムアウトする場合" - {
      "エラーを返す" in fixture(Behaviors.ignore) { (sut, _) =>
        recoverToSucceededIf[TimeoutException] {
          sut.createTaskBoard(TaskBoardId("test"), "title")
        }
      }
    }
  }

  override implicit def timeout: Timeout = Timeout(100.millis)

  def fixture(behavior: Behavior[TaskBoardAggregate.Command])(
      f: (
          TaskBoardAggregateService,
          ActorStub[TaskBoardAggregate.Command]
      ) => Future[Assertion]
  ): Future[Assertion] = {
    val stub = ActorStub[TaskBoardAggregate.Command](system, behavior)
    val sut = new TaskBoardAggregateServiceImpl(stub.ref)
    f(sut, stub)
  }

  def replyAck: Behaviors.Receive[TaskBoardAggregate.Command] =
    Behaviors.receiveMessage[TaskBoardAggregate.Command] { msg =>
      msg.replyTo ! StatusReply.ack()
      Behaviors.same
    }

  def replyError: Behaviors.Receive[TaskBoardAggregate.Command] =
    Behaviors.receiveMessage[TaskBoardAggregate.Command] { msg =>
      msg.replyTo ! StatusReply.error("error")
      Behaviors.same
    }

}
