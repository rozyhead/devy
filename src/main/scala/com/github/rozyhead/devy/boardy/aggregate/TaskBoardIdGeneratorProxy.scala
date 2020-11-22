package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.persistence.typed.PersistenceId

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
