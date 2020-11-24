package com.github.rozyhead.devy.boardy.aggregate

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import com.github.rozyhead.devy.boardy.domain.model.{TaskBoard, TaskBoardId}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpecLike

class TaskBoardAggregateTest
    extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    with AnyFreeSpecLike
    with BeforeAndAfterEach {

  import TaskBoardAggregate._

  "TaskBoard集約" - {

    "ステータスがInitの場合" - {
      "コマンドCreateTaskBoardを受け取ると" - {
        "成功を返す" in {
          // Given
          // When
          val result = runCreateTaskBoard("test", "title")
          // Then
          assert(result.reply == StatusReply.ack())
        }

        "イベントTaskBoardCreatedを記録する" in {
          // Given
          // When
          val result = runCreateTaskBoard("test", "title")
          // Then
          assert(result.event == TaskBoardCreated(TaskBoardId("test"), "title"))
        }

        "ステータスがCreatedに変わる" in {
          // Given
          // When
          val result = runCreateTaskBoard("test", "title")
          // Then
          assert(result.state == Created(TaskBoard(TaskBoardId("test"))))
        }
      }
    }

    "ステータスがCreatedの場合" - {
      "(異常系)コマンドCreateTaskBoardを受け取ると" - {
        "失敗を返す" in {
          // Given
          runCreateTaskBoard("test", "title")
          // When
          val result = runCreateTaskBoard("test", "title")
          // Then
          assert(result.reply.isError)
        }

        "イベントは記録されない" in {
          // Given
          runCreateTaskBoard("test", "title")
          // When
          val result = runCreateTaskBoard("test", "title")
          // Then
          assert(result.hasNoEvents)
        }
      }
    }
  }

  val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, State] =
    EventSourcedBehaviorTestKit(
      system,
      TaskBoardAggregate(PersistenceId("TaskBoard", "test"))
    )

  override def beforeEach(): Unit = {
    println("beforeEach")
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  private def runCreateTaskBoard(taskBoardId: String, title: String) =
    eventSourcedTestKit.runCommand[StatusReply[Done]](
      CreateTaskBoard(TaskBoardId(taskBoardId), title, _)
    )

}
