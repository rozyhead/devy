package com.github.rozyhead.devy.boardy.usecase

import com.github.rozyhead.devy.boardy.domain.model.{Account, TaskBoardId}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author takeshi
  */
trait CreateTaskBoardUseCase {
  def run(request: CreateTaskBoardRequest)(implicit
      ec: ExecutionContext
  ): Future[CreateTaskBoardResponse]
}

case class CreateTaskBoardRequest(
    title: String,
    owner: Account
)

case class CreateTaskBoardResponse(
    taskBoardId: TaskBoardId
)
