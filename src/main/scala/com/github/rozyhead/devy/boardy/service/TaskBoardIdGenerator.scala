package com.github.rozyhead.devy.boardy.service

import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId

import scala.concurrent.Future

/**
  * @author takeshi
  */
trait TaskBoardIdGenerator {
  def generate: Future[TaskBoardId]
}
