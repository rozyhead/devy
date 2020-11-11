package com.github.rozyhead.devy.boardy.usecase

import com.github.rozyhead.devy.boardy.domain.model.{TaskCardId, TaskListId}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author takeshi
  */
trait CreateTaskCardUseCase {
  def run(request: CreateTaskCardRequest)(implicit
      ec: ExecutionContext
  ): Future[CreateTaskCardResponse]
}

case class CreateTaskCardRequest(
    taskListId: TaskListId,
    title: String
)

case class CreateTaskCardResponse(
    taskCardId: TaskCardId
)
