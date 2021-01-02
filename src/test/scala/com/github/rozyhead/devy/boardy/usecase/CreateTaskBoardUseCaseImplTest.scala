package com.github.rozyhead.devy.boardy.usecase

import com.github.rozyhead.devy.boardy.domain.model.TaskBoardId
import com.github.rozyhead.devy.boardy.service.{
  TaskBoardAggregateService,
  TaskBoardIdGeneratorService
}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.freespec.AsyncFreeSpec

import scala.concurrent.{ExecutionContext, Future}

class CreateTaskBoardUseCaseImplTest
    extends AsyncFreeSpec
    with AsyncMockFactory {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val mockedTaskBoardIdGeneratorService =
    mock[TaskBoardIdGeneratorService]

  private val mockedTaskBoardAggregateService =
    mock[TaskBoardAggregateService]

  private val sut = new CreateTaskBoardUseCaseImpl(
    mockedTaskBoardIdGeneratorService,
    mockedTaskBoardAggregateService
  )

  "TaskBoard作成ユースケース" - {
    "生成したTaskBoardのIdを返却する" in {
      // Given
      (mockedTaskBoardIdGeneratorService.generate _)
        .expects()
        .returning(Future {
          TaskBoardId("task-board-id")
        })
      (mockedTaskBoardAggregateService.createTaskBoard _)
        .expects(TaskBoardId("task-board-id"), "test")
        .returning(Future(()))

      // When
      sut.run(CreateTaskBoardRequest("test")).map { response =>
        // Then
        assert(
          response == CreateTaskBoardSuccess(
            taskBoardId = TaskBoardId("task-board-id")
          )
        )
      }
    }

    "(異常系)TaskBoardIdGeneratorServiceがエラーを返却した場合" - {
      "実行が失敗する" in {
        // Given
        (mockedTaskBoardIdGeneratorService.generate _)
          .expects()
          .returning(Future.failed(new IllegalStateException()))

        // When
        sut.run(CreateTaskBoardRequest("test")).map { response =>
          // Then
          assert(response.isInstanceOf[CreateTaskBoardFailure])
        }
      }
    }

    "(異常系)TaskBoardAggregateProxyがエラーを返却した場合" - {
      "実行が失敗する" in {
        // Given
        (mockedTaskBoardIdGeneratorService.generate _)
          .expects()
          .returning(Future {
            TaskBoardId("task-board-id")
          })
        (mockedTaskBoardAggregateService.createTaskBoard _)
          .expects(TaskBoardId("task-board-id"), "test")
          .returning(Future.failed(new Exception()))

        // When
        sut.run(CreateTaskBoardRequest("test")).map { response =>
          // Then
          assert(response.isInstanceOf[CreateTaskBoardFailure])
        }
      }
    }
  }

}
