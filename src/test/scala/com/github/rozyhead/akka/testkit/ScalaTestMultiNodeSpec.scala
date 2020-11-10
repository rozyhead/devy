package com.github.rozyhead.akka.testkit

import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.implicitConversions

/**
  * @author takeshi
  */
trait ScalaTestMultiNodeSpec
    extends MultiNodeSpecCallbacks
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  self: MultiNodeSpec =>

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  override implicit def convertToWordSpecStringWrapper(
      s: String
  ): WordSpecStringWrapper =
    new WordSpecStringWrapper(s"$s (on node '${self.myself.name}', $getClass)")
}
