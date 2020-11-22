package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class TaskBoardIdGeneratorTest
    extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  import TaskBoardIdGenerator._

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

  "TaskBoardIdGenerator" must {
    "generate new TaskBoardId" in {
      val result1 = eventSourcedTestKit
        .runCommand[StatusReply[GenerateTaskBoardIdResponse]](
          GenerateTaskBoardId
        )

      val result2 = eventSourcedTestKit
        .runCommand[StatusReply[GenerateTaskBoardIdResponse]](
          GenerateTaskBoardId
        )

      assert(result1.reply.isSuccess)
      assert(result2.reply.isSuccess)
      assert(result1.reply.getValue !== result2.reply.getValue)
    }
  }
}
