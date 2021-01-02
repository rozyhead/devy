package com.github.rozyhead.devy

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode}
import akka.http.scaladsl.server.{Directives, Route}
import com.github.rozyhead.devy.boardy.usecase.{
  CreateTaskBoardFailure,
  CreateTaskBoardRequest,
  CreateTaskBoardSuccess,
  CreateTaskBoardUseCase
}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContextExecutor

/**
  * @author takeshi
  */
class RootController(
    createTaskBoardUseCase: CreateTaskBoardUseCase
)(implicit ec: ExecutionContextExecutor)
    extends Directives
    with JsonSupport {

  val route: Route =
    concat(
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Hello"))
        }
      },
      path("boardy" / "task-boards") {
        post {
          entity(as[CreateTaskBoardRequestJson]) { request =>
            val future =
              createTaskBoardUseCase.run(CreateTaskBoardRequest(request.title))
            onSuccess(future) {
              case CreateTaskBoardSuccess(taskBoardId) =>
                complete(CreateTaskBoardResponseJson(taskBoardId.value))
              case CreateTaskBoardFailure(error) =>
                complete(StatusCode.int2StatusCode(500), error.getMessage)
            }
          }
        }
      }
    )
}

case class CreateTaskBoardRequestJson(title: String)
case class CreateTaskBoardResponseJson(id: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val createTaskBoardRequestJsonFormat
      : RootJsonFormat[CreateTaskBoardRequestJson] =
    jsonFormat1(CreateTaskBoardRequestJson)

  implicit val createTaskBoardResponseJsonFormat
      : RootJsonFormat[CreateTaskBoardResponseJson] = jsonFormat1(
    CreateTaskBoardResponseJson
  )
}
