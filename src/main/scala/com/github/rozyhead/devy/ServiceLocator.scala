package com.github.rozyhead.devy

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.github.rozyhead.devy.boardy.aggregate.{
  MixinTaskBoardAggregateProxy,
  MixinTaskBoardIdGeneratorProxy
}
import com.github.rozyhead.devy.boardy.service.{
  MixinTaskBoardAggregateService,
  MixinTaskBoardIdGeneratorService
}
import com.github.rozyhead.devy.boardy.usecase.{
  MixinCreateTaskBoardUseCase,
  UsesCreateTaskBoardUseCase
}

import scala.concurrent.ExecutionContext

/**
  * @author takeshi
  */
trait ServiceLocator
    extends UsesActorSystem
    with UsesExecutionContext
    with UsesCreateTaskBoardUseCase

object ServiceLocator {
  def apply(
      anActorSystem: ActorSystem[SpawnProtocol.Command],
      aTimeout: Timeout
  ): ServiceLocator =
    new ServiceLocator
      with MixinCreateTaskBoardUseCase
      with MixinTaskBoardAggregateProxy
      with MixinTaskBoardAggregateService
      with MixinTaskBoardIdGeneratorProxy
      with MixinTaskBoardIdGeneratorService {

      override implicit val actorSystem: ActorSystem[SpawnProtocol.Command] =
        anActorSystem

      override implicit val executionContext: ExecutionContext =
        anActorSystem.executionContext

      override implicit val timeout: Timeout = aTimeout
    }
}
