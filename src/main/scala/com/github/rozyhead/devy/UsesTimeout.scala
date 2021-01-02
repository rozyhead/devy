package com.github.rozyhead.devy

import akka.util.Timeout

/**
  * @author takeshi
  */
trait UsesTimeout {
  implicit val timeout: Timeout
}
