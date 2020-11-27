package com.github.rozyhead.devy.boardy.service

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.TaskBoardAggregate
import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait TaskBoardAggregateService {
  def createTaskBoard(taskBoardId: TaskBoardId, title: String): Future[Unit]
}

class TaskBoardAggregateServiceImpl(
    taskBoardAggregateProxy: ActorRef[TaskBoardAggregate.Command]
)(implicit val system: ActorSystem[_], val timeout: Timeout)
    extends TaskBoardAggregateService {

  private val logger = LoggerFactory.getLogger(getClass)

  private implicit val ec: ExecutionContext = system.executionContext

  override def createTaskBoard(
      taskBoardId: TaskBoardId,
      title: String
  ): Future[Unit] =
    for {
      _ <- taskBoardAggregateProxy.askWithStatus(
        TaskBoardAggregate.CreateTaskBoard(
          taskBoardId,
          title,
          _
        )
      )
    } yield {
      logger.info("TaskBoard generated: {}", taskBoardId)
      ()
    }
}
