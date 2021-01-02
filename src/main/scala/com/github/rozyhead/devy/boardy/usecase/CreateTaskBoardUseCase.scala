package com.github.rozyhead.devy.boardy.usecase

import com.github.rozyhead.devy.UsesExecutionContext
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.github.rozyhead.devy.boardy.service.{
  TaskBoardAggregateService,
  TaskBoardIdGeneratorService,
  UsesTaskBoardAggregateService,
  UsesTaskBoardIdGeneratorService
}
import org.slf4j.LoggerFactory

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
    taskBoardAggregateService: TaskBoardAggregateService
)(implicit val ec: ExecutionContext)
    extends CreateTaskBoardUseCase {

  private val logger = LoggerFactory.getLogger(getClass)

  override def run(
      request: CreateTaskBoardRequest
  ): Future[CreateTaskBoardResponse] = {
    val future = for {
      taskBoardId <- taskBoardIdGeneratorService.generate()
      _ <- taskBoardAggregateService.createTaskBoard(taskBoardId, request.title)
    } yield {
      logger.info("TaskBoard created: {}", taskBoardId)
      CreateTaskBoardSuccess(taskBoardId)
    }

    future.recover {
      case e: Throwable => CreateTaskBoardFailure(e)
    }
  }
}
trait UsesCreateTaskBoardUseCase {
  val createTaskBoardUseCase: Future[CreateTaskBoardUseCase]
}

trait MixinCreateTaskBoardUseCase
    extends UsesCreateTaskBoardUseCase
    with UsesTaskBoardAggregateService
    with UsesTaskBoardIdGeneratorService
    with UsesExecutionContext {
  override lazy val createTaskBoardUseCase: Future[CreateTaskBoardUseCase] =
    for {
      taskBoardAggregateService <- taskBoardAggregateService
      taskBoardIdGeneratorService <- taskBoardIdGeneratorService
    } yield new CreateTaskBoardUseCaseImpl(
      taskBoardIdGeneratorService,
      taskBoardAggregateService
    )
}
