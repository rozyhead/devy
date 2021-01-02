package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.persistence.typed.PersistenceId
import com.github.rozyhead.devy.{UsesActorSystem, UsesTimeout}

import scala.concurrent.Future

object TaskBoardIdGeneratorProxy {
  import TaskBoardIdGenerator._

  def apply(): Behavior[Command[_]] = {
    Behaviors.setup { context =>
      val singletonManager = ClusterSingleton(context.system)
      val proxy = singletonManager.init(
        SingletonActor(
          Behaviors
            .supervise(
              TaskBoardIdGenerator(
                PersistenceId("TaskBoardIdGenerator", "singleton")
              )
            )
            .onFailure[Exception](SupervisorStrategy.restart),
          "TaskBoardIdGeneratorProxy"
        )
      )

      // すべてのメッセージを転送
      Behaviors.receiveMessage { command =>
        context.log.debug("Forwarding message [{}] to [{}]", command, proxy)
        proxy ! command
        Behaviors.same
      }
    }
  }

}

trait UsesTaskBoardIdGeneratorProxy {
  val taskBoardIdGeneratorProxy: Future[
    ActorRef[TaskBoardIdGenerator.Command[_]]
  ]
}

trait MixinTaskBoardIdGeneratorProxy
    extends UsesTaskBoardIdGeneratorProxy
    with UsesActorSystem
    with UsesTimeout {

  import akka.actor.typed.scaladsl.AskPattern._

  override lazy val taskBoardIdGeneratorProxy
      : Future[ActorRef[TaskBoardIdGenerator.Command[_]]] =
    actorSystem.ask[ActorRef[TaskBoardIdGenerator.Command[_]]](
      SpawnProtocol.Spawn(
        behavior = TaskBoardIdGeneratorProxy(),
        name = "taskBoardIdGenerator",
        props = Props.empty,
        _
      )
    )
}
