package com.github.rozyhead.devy.boardy.e2e

import com.github.rozyhead.devy.e2e.DevyServerTestKit
import io.restassured.RestAssured._
import io.restassured.filter.log.{RequestLoggingFilter, ResponseLoggingFilter}
import io.restassured.http.ContentType
import io.restassured.module.scala.RestAssuredSupport._
import org.scalatest.freespec.AsyncFreeSpecLike

/**
  * @author takeshi
  */
class CreateTaskBoardE2eTest extends DevyServerTestKit with AsyncFreeSpecLike {

  "タスクボード作成エンドポイント" - {
    "作成したボードIDを返す" in {
      val response = given()
        .baseUri(server.baseUri)
        .contentType(ContentType.JSON)
        .filters(new RequestLoggingFilter(), new ResponseLoggingFilter())
        .when()
        .body("""
            |{
            |  "title": "test"
            |}
            |""".stripMargin)
        .post("/boardy/task-boards")
        .Then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .extract()
        .as(classOf[CreateTaskBoardResponse])

      assert(response.id != null)
    }
  }

}

case class CreateTaskBoardResponse(
    id: String
)
