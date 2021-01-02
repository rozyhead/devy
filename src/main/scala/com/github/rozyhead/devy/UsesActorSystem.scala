package com.github.rozyhead.devy

import akka.actor.typed.{ActorSystem, SpawnProtocol}

/**
  * @author takeshi
  */
trait UsesActorSystem {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command]
}
