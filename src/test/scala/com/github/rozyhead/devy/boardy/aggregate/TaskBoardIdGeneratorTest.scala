package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpecLike

class TaskBoardIdGeneratorTest
    extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    with AnyFreeSpecLike
    with BeforeAndAfterEach {

  import TaskBoardIdGenerator._

  "TaskBoardIdジェネレーター" - {

    "コマンドGenerateTaskBoardIdを受け取ると" - {

      "`TaskBoard:${番号}`のTaskBoardIdを生成してGenerateTaskBoardIdResponseを返す" - {
        "初回は`TaskBoard:1`を返す" in {
          // Given
          // When
          val result = runGenerateTaskBoardId()
          // Then
          assert(result.reply.getValue.taskBoardId.value == "TaskBoard:1")
        }
        "2回目は`TaskBoard:2`を返す" in {
          // Given
          runGenerateTaskBoardId()
          // When
          val result = runGenerateTaskBoardId()
          // Then
          assert(result.reply.getValue.taskBoardId.value == "TaskBoard:2")
        }
      }

      "TaskBoardId生成に使用した番号をイベントTaskBoardIdGeneratedに含めて記録する" - {
        "初回は1を記録する" in {
          // Given
          // When
          val result = runGenerateTaskBoardId()
          // Then
          assert(result.eventOfType[TaskBoardIdGenerated].seed == 1L)
        }
        "2回目は2を記録する" in {
          // Given
          runGenerateTaskBoardId()
          // When
          val result = runGenerateTaskBoardId()
          // Then
          assert(result.eventOfType[TaskBoardIdGenerated].seed == 2L)
        }
      }

      "TaskBoardId生成に使用した番号をステータスに持つ" - {
        "初回は1をステータスに持つ" in {
          // Given
          // When
          val result = runGenerateTaskBoardId()
          // Then
          assert(result.state == 1L)
        }
        "2回目は2をステータスに持つ" in {
          // Given
          runGenerateTaskBoardId()
          // When
          val result = runGenerateTaskBoardId()
          // Then
          assert(result.state == 2L)
        }
      }
    }
  }

  val eventSourcedTestKit
      : EventSourcedBehaviorTestKit[Command[_], Event, State] =
    EventSourcedBehaviorTestKit(
      system,
      TaskBoardIdGenerator(PersistenceId("TaskBoardIdGenerator", "test"))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  private def runGenerateTaskBoardId() =
    eventSourcedTestKit
      .runCommand[StatusReply[GenerateTaskBoardIdResponse]] {
        GenerateTaskBoardId
      }
}
