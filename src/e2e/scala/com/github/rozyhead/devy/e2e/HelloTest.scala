package com.github.rozyhead.devy.e2e

import io.restassured.RestAssured._
import io.restassured.http.ContentType
import io.restassured.module.scala.RestAssuredSupport._
import org.scalatest.freespec.AsyncFreeSpecLike

/**
  * @author takeshi
  */
class HelloTest extends DevyServerTestKit with AsyncFreeSpecLike {

  "Hello endpoint" - {
    "return 'Hello' text" in {
      val text = given()
        .baseUri(server.baseUri)
        .when()
        .get("/hello")
        .Then()
        .statusCode(200)
        .contentType(ContentType.TEXT)
        .extract()
        .asString()

      assert(text == "Hello")
    }
  }

}
