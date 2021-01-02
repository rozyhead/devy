package com.github.rozyhead.devy

import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.{
  TaskBoardAggregate,
  TaskBoardAggregateProxy,
  TaskBoardIdGenerator,
  TaskBoardIdGeneratorProxy
}
import com.github.rozyhead.devy.boardy.service.{
  TaskBoardAggregateService,
  TaskBoardAggregateServiceImpl,
  TaskBoardIdGeneratorService,
  TaskBoardIdGeneratorServiceImpl
}
import com.github.rozyhead.devy.boardy.usecase.{
  CreateTaskBoardUseCase,
  CreateTaskBoardUseCaseImpl
}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author takeshi
  */
class ServiceLocator(
    val taskBoardAggregateService: TaskBoardAggregateService,
    val taskBoardIdGeneratorService: TaskBoardIdGeneratorService,
    val createTaskBoardUseCase: CreateTaskBoardUseCase
) {}

object ServiceLocator {
  import akka.actor.typed.scaladsl.AskPattern._

  def apply(implicit
      system: ActorSystem[SpawnProtocol.Command],
      timeout: Timeout
  ): Future[ServiceLocator] = {
    implicit val ec: ExecutionContext = system.executionContext
    for {
      taskBoardAggregateProxy <-
        system.ask[ActorRef[TaskBoardAggregate.Command]](
          SpawnProtocol.Spawn(
            behavior = TaskBoardAggregateProxy(),
            name = "taskBoardAggregateProxy",
            props = Props.empty,
            _
          )
        )
      taskBoardIdGeneratorProxy <-
        system.ask[ActorRef[TaskBoardIdGenerator.Command[_]]](
          SpawnProtocol.Spawn(
            behavior = TaskBoardIdGeneratorProxy(),
            name = "taskBoardIdGeneratorProxy",
            props = Props.empty,
            _
          )
        )
    } yield {
      val taskBoardAggregateService = new TaskBoardAggregateServiceImpl(
        taskBoardAggregateProxy
      )
      val taskBoardIdGeneratorService = new TaskBoardIdGeneratorServiceImpl(
        taskBoardIdGeneratorProxy
      )
      val createTaskBoardUseCase = new CreateTaskBoardUseCaseImpl(
        taskBoardIdGeneratorService = taskBoardIdGeneratorService,
        taskBoardAggregateService = taskBoardAggregateService
      )

      new ServiceLocator(
        taskBoardAggregateService = taskBoardAggregateService,
        taskBoardIdGeneratorService = taskBoardIdGeneratorService,
        createTaskBoardUseCase = createTaskBoardUseCase
      )
    }
  }
}
