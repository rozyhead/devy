package com.github.rozyhead.devy.boardy.usecase

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregate,
  TaskBoardIdGenerator
}
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait CreateTaskBoardUseCase {
  def run(request: CreateTaskBoardRequest): Future[CreateTaskBoardResponse]
}

case class CreateTaskBoardRequest(
    title: String
)

sealed trait CreateTaskBoardResponse

case class CreateTaskBoardSuccess(
    taskBoardId: TaskBoardId
) extends CreateTaskBoardResponse

case class CreateTaskBoardFailure(
    error: Throwable
) extends CreateTaskBoardResponse

class CreateTaskBoardUseCaseImpl(
    taskBoardIdGeneratorProxy: ActorRef[TaskBoardIdGenerator.Command[_]],
    taskBoardAggregateProxy: ActorRef[TaskBoardAggregate.Command]
)(implicit val system: ActorSystem[_])
    extends CreateTaskBoardUseCase {

  private val logger = LoggerFactory.getLogger(getClass)

  private implicit val ec: ExecutionContext = system.executionContext
  private implicit val timeout: Timeout = Timeout(5.seconds)

  override def run(
      request: CreateTaskBoardRequest
  ): Future[CreateTaskBoardResponse] = {
    val future = for {
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
      logger.info("TaskBoard created: {}", generated.taskBoardId)
      CreateTaskBoardSuccess(generated.taskBoardId)
    }

    future.recover {
      case e: Throwable => CreateTaskBoardFailure(e)
    }
  }
}
