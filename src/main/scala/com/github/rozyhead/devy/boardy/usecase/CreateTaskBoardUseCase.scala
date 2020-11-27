package com.github.rozyhead.devy.boardy.usecase

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardAggregate
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.github.rozyhead.devy.boardy.service.TaskBoardIdGeneratorService
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
    taskBoardIdGeneratorService: TaskBoardIdGeneratorService,
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
      taskBoardId <- taskBoardIdGeneratorService.generate()
      _ <- taskBoardAggregateProxy.askWithStatus(
        TaskBoardAggregate.CreateTaskBoard(
          taskBoardId,
          request.title,
          _
        )
      )
    } yield {
      logger.info("TaskBoard created: {}", taskBoardId)
      CreateTaskBoardSuccess(taskBoardId)
    }

    future.recover {
      case e: Throwable => CreateTaskBoardFailure(e)
    }
  }
}
