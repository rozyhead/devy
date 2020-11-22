package com.github.rozyhead.devy.boardy.usecase

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregate,
  TaskBoardIdGenerator
}
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait CreateTaskBoardUseCase {
  def run(request: CreateTaskBoardRequest): Future[CreateTaskBoardResponse]
}

case class CreateTaskBoardRequest(
    title: String
)

case class CreateTaskBoardResponse(
    taskBoardId: TaskBoardId
)

class CreateTaskBoardUseCaseImpl(
    taskBoardIdGeneratorProxy: ActorRef[TaskBoardIdGenerator.Command[_]],
    taskBoardAggregateProxy: ActorRef[TaskBoardAggregate.Command]
)(implicit val system: ActorSystem[_])
    extends CreateTaskBoardUseCase {

  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(5.seconds)

  override def run(
      request: CreateTaskBoardRequest
  ): Future[CreateTaskBoardResponse] =
    for {
      generated <- taskBoardIdGeneratorProxy.askWithStatus(
        TaskBoardIdGenerator.GenerateTaskBoardId
      )
      _ <- taskBoardAggregateProxy.askWithStatus(
        TaskBoardAggregate.CreateTaskBoard(
          generated.taskBoardId,
          request.title,
          _
        )
      )
    } yield {
      CreateTaskBoardResponse(generated.taskBoardId)
    }
}
