package com.github.rozyhead.akka.testkit

import java.util.concurrent.ConcurrentHashMap

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Address, Scheduler}
import akka.cluster.typed.{Cluster, Join}
import akka.cluster.{ClusterEvent, MemberStatus}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import akka.util.Timeout
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

/**
  * @author takeshi
  */
trait MultiNodeTypedClusterSpec
    extends Suite
    with ScalaTestMultiNodeSpec
    with Matchers {
  self: MultiNodeSpec =>

  override def initialParticipants: Int = roles.size

  implicit def typedSystem: ActorSystem[Nothing] = system.toTyped

  implicit def scheduler: Scheduler = system.scheduler

  private val cachedAddress = new ConcurrentHashMap[RoleName, Address]()

  def cluster: Cluster = Cluster(system.toTyped)

  def clusterView: ClusterEvent.CurrentClusterState = cluster.state

  implicit def address(role: RoleName): Address = {
    cachedAddress.computeIfAbsent(
      role, {
        node(_).address
      }
    )
  }

  def formCluster(first: RoleName, rest: RoleName*): Unit = {
    runOn(first) {
      cluster.manager ! Join(cluster.selfMember.address)
      awaitAssert(cluster.state.members exists { m =>
        m.uniqueAddress == cluster.selfMember.uniqueAddress && m.status == MemberStatus.Up
      } should be(true))
    }

    enterBarrier(s"${first.name}-joined")

    rest.foreach { node =>
      runOn(node) {
        cluster.manager ! Join(address(first))
        awaitAssert(cluster.state.members exists { m =>
          m.uniqueAddress == cluster.selfMember.uniqueAddress && m.status == MemberStatus.Up
        } should be(true))
      }
    }

    enterBarrier("all-joined")
  }

  private lazy val spawnActor =
    system.actorOf(PropsAdapter(SpawnProtocol())).toTyped[SpawnProtocol.Command]

  def spawn[T](behavior: Behavior[T], name: String): ActorRef[T] = {
    implicit val timeout: Timeout = testKitSettings.DefaultTimeout
    val f: Future[ActorRef[T]] =
      spawnActor.ask(SpawnProtocol.Spawn(behavior, name, Props.empty, _))
    Await.result(f, timeout.duration * 2)
  }
}
