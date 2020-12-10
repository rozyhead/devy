import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys
import sbt.Keys._
import sbt.{Def, _}

/**
  * @author takeshi
  */
object Testing {
  lazy val IntegrationTest: Configuration = config("it") extend Test
  lazy val EndToEndTest: Configuration = config("e2e") extend Test
  lazy val configs: Seq[Configuration] =
    Seq(IntegrationTest, EndToEndTest, MultiJvmKeys.MultiJvm)

  lazy val testAll: TaskKey[Unit] = TaskKey[Unit]("test-all")

  private lazy val itSettings =
    inConfig(IntegrationTest)(
      Defaults.testSettings ++ Seq(
        fork := false,
        parallelExecution := false,
        scalaSource := baseDirectory.value / "src/it/scala"
      )
    )

  private lazy val e2eSettings =
    inConfig(EndToEndTest)(
      Defaults.testSettings ++ Seq(
        fork := false,
        parallelExecution := false,
        scalaSource := baseDirectory.value / "src/e2e/scala"
      )
    )

  lazy val settings: Seq[Def.Setting[_]] = itSettings ++ e2eSettings ++ Seq(
    testAll := (EndToEndTest / test)
      .dependsOn(IntegrationTest / test)
      .dependsOn(MultiJvmKeys.MultiJvm / test)
      .dependsOn(Test / test)
      .value
  )
}
