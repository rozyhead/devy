package com.github.rozyhead.akka.testkit

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.reflect.ClassTag

class ActorStub[M: ClassTag](testKit: ActorTestKit, behavior: Behavior[M]) {
  val probe: TestProbe[M] = testKit.createTestProbe()

  val ref: ActorRef[M] = testKit.spawn(
    Behaviors.monitor(
      probe.ref,
      behavior
    )
  )
}

object ActorStub {
  def apply[M: ClassTag](
      system: ActorSystem[_],
      behavior: Behavior[M]
  ): ActorStub[M] = new ActorStub[M](ActorTestKit(system), behavior)
}
