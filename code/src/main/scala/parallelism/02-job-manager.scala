/*
 * Copyright 2024 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package parallelism

import cats.effect.*
import cats.syntax.all.*

import scala.concurrent.duration.*

// The goal is use parMapN and friends in a more complex example, which simulates a job manager.
object JobManager extends IOApp.Simple {

  // Evaluate the Job, creating an IO that runs all the stages in the correct
  // order and returns the sum of the values computed by each stage.
  def eval(job: Job): IO[Int] =
    ???

  //----------------------------------------------------------------------------
  // Job generation
  //----------------------------------------------------------------------------

  // A Job is a sequences of Stages. Stages should be executed from
  // left-to-right, and the no stage should start until all the preceding
  // stages have completed.
  final case class Job(
      stages: Seq[Stage]
  )

  // A Stage is either Sequential or Parallel. A Parallel stage includes the
  // number of parallel instances of the work that should run concurrenty.
  enum Stage {
    case Sequential(work: IO[Int])
    case Parallel(repeats: Int, work: IO[Int])
  }

  val random = scala.util.Random

  val randomSleep: IO[Unit] = IO(random.nextInt(6).seconds).flatMap(IO.sleep)
  val randomResult: IO[Int] = IO(random.nextInt(10))

  val randomWork: IO[Int] = randomSleep *> randomResult

  val randomStage: IO[Stage] =
    IO(random.nextBoolean()).ifM(
      ifTrue = IO.pure(Stage.Sequential(randomWork)),
      ifFalse =
        IO(random.nextInt(6)).map(n => Stage.Parallel(n + 1, randomWork))
    )

  val randomJob: IO[Job] =
    IO(random.nextInt(10))
      .flatMap(n => List.fill(n + 1)(randomStage).sequence)
      .map(Job.apply)

  //----------------------------------------------------------------------------
  // Go go go!
  //----------------------------------------------------------------------------

  val run =
    for {
      job <- randomJob
      _ <- IO.println("Starting job...")
      sum <- eval(job)
      _ <- IO.println("Stopping job...")
      _ <- IO.println(s"Result was $sum")
    } yield ()
}
