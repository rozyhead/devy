package com.github.rozyhead.devy.boardy.usecase

import com.github.rozyhead.devy.boardy.domain.model.{TaskBoardId, TaskListId}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author takeshi
  */
trait CreateTaskListUseCase {
  def run(request: CreateTaskListRequest)(implicit
      ec: ExecutionContext
  ): Future[CreateTaskListResponse]
}

case class CreateTaskListRequest(
    taskBoardId: TaskBoardId,
    title: String
)

case class CreateTaskListResponse(
    taskListId: TaskListId
)
