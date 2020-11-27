package com.github.rozyhead.devy.boardy.service

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardIdGenerator
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait TaskBoardIdGeneratorService {
  def generate(): Future[TaskBoardId]
}

class TaskBoardIdGeneratorServiceImpl(
    taskBoardIdGeneratorProxy: ActorRef[TaskBoardIdGenerator.Command[_]]
)(implicit val system: ActorSystem[_], val timeout: Timeout)
    extends TaskBoardIdGeneratorService {

  private val logger = LoggerFactory.getLogger(getClass)

  private implicit val ec: ExecutionContext = system.executionContext

  override def generate(): Future[TaskBoardId] = {
    for {
      generated <- taskBoardIdGeneratorProxy.askWithStatus(
        TaskBoardIdGenerator.GenerateTaskBoardId
      )
    } yield {
      logger.info("TaskBoardId generated: {}", generated.taskBoardId)
      generated.taskBoardId
    }
  }
}
