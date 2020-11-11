package com.github.rozyhead.devy.boardy.aggregate

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import com.github.rozyhead.devy.boardy.domain.model.{TaskBoard, TaskBoardId}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

/**
  * @author takeshi
  */
class TaskBoardAggregateTest
    extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  import TaskBoardAggregate._

  val eventSourcedTestKit: EventSourcedBehaviorTestKit[Command, Event, State] =
    EventSourcedBehaviorTestKit(
      system,
      TaskBoardAggregate(PersistenceId("TaskBoard", "test"))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "TaskBoardAggregate" when {
    "Init" must {
      "accept CreateTaskBoard" in {
        val result = eventSourcedTestKit.runCommand[StatusReply[Done]](
          CreateTaskBoard(TaskBoardId("test"), "test", _)
        )

        assert(result.reply.isSuccess)
        assert(result.event == TaskBoardCreated(TaskBoardId("test"), "test"))
        assert(result.state == Created(
          TaskBoard(TaskBoardId("test"))
        ))
      }
    }

    "Created" must {
      "not accept CreateTaskBoard" in {
        eventSourcedTestKit.runCommand[StatusReply[Done]](
          CreateTaskBoard(TaskBoardId("test"), "test", _)
        )

        val result = eventSourcedTestKit.runCommand[StatusReply[Done]](
          CreateTaskBoard(TaskBoardId("test"), "test", _)
        )

        assert(result.reply.isError)
        assert(result.hasNoEvents)
      }
    }
  }
}
