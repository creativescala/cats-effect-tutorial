# Job Manager

Our final challenge for this section returns to the Job Manager. In this case we're going to the concurrent tools we've just learned about to visualize the progress of running a job. Below you'll see an example of the output you should end up with.

@:doodle("job-manager-canvas", "JobManager.go")

@:exercise(Job Manager)
Complete [`code/src/main/scala/parallelism/04-job-manager-visualize.scala`][job-manager].
@:@

@:solution
There are many different ways you could solve this problem. Part of the challenge is coming up with a way to solve it, using the available tools. Here's an example implementation that uses just a `Semaphore`.

```scala
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
          if count == repeats then
            draw(results :+ Result.StageInProgress(count, repeats), canvas)
          else
            s.acquire *>
              draw(
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

val run =
  Frame.default
    .withSize(800, 200)
    .canvas()
    .flatMap(c =>
      (randomJob.flatMap(job => eval(job, c)) *> IO.sleep(5.seconds))
        .replicateA_(5)
    )
```
@:@


[job-manager]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/parallelism/04-job-manager-visualize.scala
