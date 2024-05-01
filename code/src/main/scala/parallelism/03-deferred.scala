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
import doodle.core.*
import doodle.java2d.*
import doodle.random.*
import doodle.syntax.all.*

import scala.concurrent.duration.*

// The goal is to understand how `Deferred` works in Cats Effects, and to use it to
// coordinate between multiple concurrent processes.
object Deferrence extends IOApp.Simple {

  // Evaluate a Stage, creating an IO that runs the Stage correctly.
  def evalStage(stage: Stage): IO[Int] =
    stage match {
      case Stage.Sequential(work) => work
      case Stage.Parallel(repeats, work) =>
        List.fill(repeats)(work).parSequence.map(list => list.sum)
    }

  // Evaluate the Job, creating an IO that runs all the stages in the correct
  // order and returns the sum of the values computed by each stage.
  def eval(job: Job): IO[Int] =
    job.stages.traverse(evalStage).map(_.sum)

  //----------------------------------------------------------------------------
  // Job generation
  //----------------------------------------------------------------------------

  // A Job is a sequences of Stages. Stages should be executed from
  // left-to-right, and the no stage should start until all the preceding
  // stages have completed.
  final case class Job(
      stages: Seq[Stage]
  )

  // A Stage is either Sequential, or Parallel. All the work in a Parallel stage
  // should be run in parallel.
  enum Stage {
    case Sequential(work: IO[Int])
    case Parallel(repeats: Int, work: IO[Int])
  }

  val randomWork: Random[IO[Int]] = {
    val snooze = Random.natural(5).map(_ + 1)
    val result = Random.natural(10).map(_ + 1)

    (snooze, result).mapN((s, r) => IO.sleep(s.seconds) *> IO.pure(r))
  }

  val randomStage: Random[Stage] =
    Random
      .discrete(
        (randomWork.map(Stage.Sequential.apply), 0.75),
        (
          (Random.natural(10), randomWork).mapN((r, w) => Stage.Parallel(r, w)),
          0.25
        )
      )
      .flatten

  val randomJob: Random[Job] =
    Random
      .natural(10)
      .flatMap(n => List.fill(n + 1)(randomStage).sequence)
      .map(Job.apply)

  // When run this IO will produce a random Job
  val makeJob: IO[Job] = IO(randomJob.run)

  //----------------------------------------------------------------------------
  // Job visualization
  //----------------------------------------------------------------------------

  enum StageResult {
    case Sequential
    case Parallel(complete: Int, total: Int)

    /** Non-destructively increment the completed number in this Parallel
      * StageResult.
      */
    def increment: StageResult =
      this match {
        case Sequential =>
          throw new IllegalStateException(
            "Cannot increment a StageResult that is not a Parallel result"
          )
        case Parallel(complete, total) =>
          if complete < total then Parallel(complete + 1, total)
          else
            throw new IllegalStateException(
              s"This parallel StageResult already has $complete results of $total and cannot be incremented."
            )
      }

  }

  def visualizeStage(stage: StageResult): Picture[Unit] =
    stage match {
      case StageResult.Sequential => Picture.square(60)
      case StageResult.Parallel(complete, total) =>
        Picture.rectangle(60, 60 * complete / total)
    }

  //----------------------------------------------------------------------------
  // Go go go!
  //----------------------------------------------------------------------------

  val run =
    for {
      job <- makeJob
      _ <- IO.println("Starting job...")
      sum <- eval(job)
      _ <- IO.println("Stopping job...")
      _ <- IO.println(s"Result was $sum")
    } yield ()
}
