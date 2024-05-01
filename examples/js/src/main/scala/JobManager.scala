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

package jobmanager

import cats.effect.IO
import cats.effect.std.Semaphore
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import doodle.core.*
import doodle.core.font.Font
import doodle.svg.*
import doodle.syntax.all.*

import scala.concurrent.duration.*
import scala.scalajs.js.annotation.*

@JSExportTopLevel("JobManager")
object JobManagerVisualize {

  // Evaluate a Stage, creating an IO that runs the Stage correctly.
  def evalStage(
      stage: Stage,
      results: List[Result],
      canvas: Canvas
  ): IO[(Int, Result)] =
    stage match {
      case Stage.Sequential(work) =>
        draw(results :+ Result.StageInProgress(0, 1), canvas)
          .flatMap(_ => work)
          .map(i => (i, Result.StageComplete(1)))
      case Stage.Parallel(repeats, work) =>
        Semaphore[IO](0).flatMap { s =>
          def monitor(count: Int): IO[Unit] =
            if count == repeats then IO.unit
            else
              s.acquire *> draw(
                results :+ Result.StageInProgress(count, repeats),
                canvas
              ) *> monitor(count + 1)

          val parallelWork =
            List
              .fill(repeats)(work.flatMap(r => s.release *> IO.pure(r)))
              .parSequence

          draw(results :+ Result.StageInProgress(0, repeats), canvas) *>
            (parallelWork, monitor(0)).parMapN((results, _) =>
              (results.sum, Result.StageComplete(repeats))
            )
        }
    }

  // Evaluate the Job, creating an IO that runs all the stages in the correct
  // order and returns the sum of the values computed by each stage.
  def eval(job: Job, canvas: Canvas): IO[Int] =
    job.stages
      .foldLeftM[IO, (Int, List[Result])]((0, List.empty[Result]))(
        (accum, elt) =>
          val (total, results) = accum
          evalStage(elt, results, canvas)
            .map { case (i, r) => (total + i, results :+ r) }
            .flatTap { case (_, results) => draw(results, canvas) }
      )
      .flatMap { case (sum, results) =>
        draw(results :+ Result.Complete(sum), canvas).as(sum)
      }

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
  // Job visualization
  //----------------------------------------------------------------------------

  enum Result {
    case StageComplete(workers: Int)
    case StageInProgress(complete: Int, total: Int)
    case Complete(result: Int)
  }

  val boxSize = 60
  val spacerSize = 3
  val box = Picture.square(boxSize)

  def stack(count: Int, total: Int): Picture[Unit] =
    if count == 0 then Picture.empty
    else if total == 1 then box
    else {
      val nSpacers = total - 1
      val spaceRequired = nSpacers * spacerSize
      val remaining = boxSize - spaceRequired
      val height = remaining.toDouble / total.toDouble

      (0.until(count)
        .map(i =>
          if i == 0 then Picture.rectangle(boxSize, height)
          else
            Picture
              .rectangle(boxSize, height)
              .margin(spacerSize, 0, 0, 0)
        ))
        .toList
        .allAbove
    }

  def visualize(results: List[Result]): Picture[Unit] =
    results
      .map {
        case Result.StageComplete(workers) =>
          stack(workers, workers).fillColor(Color.green).margin(5)

        case Result.StageInProgress(complete, total) =>
          val base =
            stack(total, total)
              .fillColor(Color.orange.alpha(0.3.normalized))
              .originAt(Landmark.bottomCenter)
          val top =
            stack(complete, total)
              .fillColor(Color.green)
              .originAt(Landmark.bottomCenter)

          top.on(base).originAt(Landmark.percent(0, 50)).margin(5)

        case Result.Complete(result) =>
          Picture
            .text(result.toString)
            .font(Font.defaultSansSerif.size(28))
            .fillColor(Color.green)
            .strokeColor(Color.green)
            .on(Picture.square(60).noFill.noStroke)
      }
      .allBeside
      .noStroke

  def draw(results: List[Result], canvas: Canvas): IO[Unit] =
    visualize(results).drawWithCanvasToIO(canvas)

  //----------------------------------------------------------------------------
  // Go go go!
  //----------------------------------------------------------------------------

  @JSExport
  def go(id: String): Unit = {
    // This describes the background on which we'll render the clock
    val frame = Frame(id).withSize(800, 300)
    // A Canvas is an area of the screen we can draw onto
    val canvas: IO[Canvas] = frame.canvas()

    canvas
      .flatMap(c =>
        (randomJob.flatMap(job => eval(job, c)) *> IO.sleep(5.seconds)).foreverM
      )
      .unsafeRunAndForget()
  }
}
