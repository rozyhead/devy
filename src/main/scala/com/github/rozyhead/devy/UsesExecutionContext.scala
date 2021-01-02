package com.github.rozyhead.devy

import scala.concurrent.ExecutionContext

trait UsesExecutionContext {
  implicit val executionContext: ExecutionContext
}
