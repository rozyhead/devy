package com.github.rozyhead.devy.boardy.aggregate

import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  ReplyEffect
}
import com.github.rozyhead.akka.util.JsonSerializable
import com.github.rozyhead.devy.boardy.domain.model.{TaskBoard, TaskBoardId}

/**
  * @author takeshi
  */
object TaskBoardAggregate {

  sealed trait Command extends JsonSerializable {
    val taskBoardId: TaskBoardId
    val replyTo: ActorRef[StatusReply[Done]]
  }

  case class CreateTaskBoard(
      taskBoardId: TaskBoardId,
      title: String,
      replyTo: ActorRef[StatusReply[Done]]
  ) extends Command

  sealed trait Event extends JsonSerializable {
    val taskBoardId: TaskBoardId
  }

  case class TaskBoardCreated(
      taskBoardId: TaskBoardId,
      title: String
  ) extends Event

  sealed trait State extends JsonSerializable {
    def applyEvent(event: Event): State
  }

  case object Init extends State {
    override def applyEvent(event: Event): State =
      event match {
        case TaskBoardCreated(taskBoardId, _) => Created(TaskBoard(taskBoardId))
        case _ =>
          throw new IllegalStateException(
            s"unexpected event <$event> in state <Init>"
          )
      }
  }

  case class Created(taskBoard: TaskBoard) extends State {
    override def applyEvent(event: Event): State =
      event match {
        case _ =>
          throw new IllegalStateException(
            s"unexpected event <$event> in state <Created>"
          )
      }
  }

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior.withEnforcedReplies(
      persistenceId = persistenceId,
      emptyState = Init,
      commandHandler = commandHandler(),
      eventHandler = eventHandler()
    )

  private def commandHandler()
      : (State, Command) => ReplyEffect[Event, State] = { (state, command) =>
    state match {
      case Init =>
        command match {
          case c: CreateTaskBoard => createTaskBoard(c)
          case _ =>
            Effect.reply(command.replyTo)(
              StatusReply.error(
                s"unexpected command <$command> in state <$state>"
              )
            )
        }
      case Created(taskBoard) =>
        command match {
          case _ =>
            Effect.reply(command.replyTo)(
              StatusReply.error(
                s"unexpected command <$command> in state <$state>"
              )
            )
        }
    }
  }

  private def eventHandler(): (State, Event) => State = { (state, event) =>
    state.applyEvent(event)
  }

  private def createTaskBoard(c: CreateTaskBoard): ReplyEffect[Event, State] = {
    Effect
      .persist(TaskBoardCreated(c.taskBoardId, c.title))
      .thenReply(c.replyTo)(_ => StatusReply.ack())
  }

}
