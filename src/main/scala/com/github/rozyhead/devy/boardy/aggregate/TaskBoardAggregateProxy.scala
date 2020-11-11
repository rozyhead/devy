package com.github.rozyhead.devy.boardy.aggregate

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{
  ClusterSharding,
  Entity,
  EntityTypeKey
}
import akka.persistence.typed.PersistenceId

/**
  * @author takeshi
  */
object TaskBoardAggregateProxy {
  import TaskBoardAggregate._

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("TaskBoard")

  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)
      sharding.init(Entity(TypeKey) { entityContext =>
        TaskBoardAggregate(
          PersistenceId(
            entityContext.entityTypeKey.name,
            entityContext.entityId
          )
        )
      })

      // すべてのメッセージを転送
      Behaviors.receiveMessage { command =>
        val entityRef =
          sharding.entityRefFor(TypeKey, command.taskBoardId.value)
        context.log.debug("Forwarding message [{}] to [{}]", command, entityRef)
        entityRef ! command
        Behaviors.same
      }
    }
  }

}
