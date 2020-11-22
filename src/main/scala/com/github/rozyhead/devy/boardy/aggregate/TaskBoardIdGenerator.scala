package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}
import com.github.rozyhead.akka.util.JsonSerializable
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId

/**
  * @author takeshi
  */
object TaskBoardIdGenerator {

  sealed trait Command[R <: Response] extends JsonSerializable {
    val replyTo: ActorRef[StatusReply[R]]
  }

  sealed trait Response extends JsonSerializable

  case class GenerateTaskBoardId(
      replyTo: ActorRef[StatusReply[GenerateTaskBoardIdResponse]]
  ) extends Command[GenerateTaskBoardIdResponse]

  case class GenerateTaskBoardIdResponse(
      taskBoardId: TaskBoardId
  ) extends Response

  sealed trait Event extends JsonSerializable

  case class TaskBoardIdGenerated(
      seed: Long
  ) extends Event

  type State = Long

  def apply(persistenceId: PersistenceId): Behavior[Command[_]] =
    EventSourcedBehavior.withEnforcedReplies(
      persistenceId = persistenceId,
      emptyState = 0L,
      commandHandler = commandHandler(),
      eventHandler = eventHandler()
    )

  private def commandHandler()
      : (State, Command[_]) => ReplyEffect[Event, State] = { (state, command) =>
    command match {
      case GenerateTaskBoardId(replyTo) =>
        Effect
          .persist(TaskBoardIdGenerated(state + 1))
          .thenReply(replyTo) { newState =>
            val taskBoardId = TaskBoardId(s"TaskBoard:$newState")
            StatusReply.success(GenerateTaskBoardIdResponse(taskBoardId))
          }
      case _ =>
        Effect.reply(command.replyTo)(
          StatusReply.error(s"unexpected command <$command>")
        )
    }
  }

  private def eventHandler(): (State, Event) => State = { (state, event) =>
    event match {
      case TaskBoardIdGenerated(seed) => seed
    }
  }

}
