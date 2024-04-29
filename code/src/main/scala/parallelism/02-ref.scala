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

// The goal is to understand how `Ref` works in Cats Effects, and to use it to
// coordinate between multiple concurrent processes.
object References extends IOApp.Simple {
  val ref = IO.ref(0)

  def printAndIncrement(name: String)(ref: Ref[IO, Int]): IO[Int] =
    for {
      v <- ref.updateAndGet(x => x + 1)
      _ <- IO.println(s"$name: Ref's value was $v")
    } yield v

  val first =
    (
      ref.flatMap(printAndIncrement("Left")),
      ref.flatMap(printAndIncrement("Right"))
    )
      .parMapN((l, r) => s"Values were $l and $r")
      .flatMap(IO.println)

  val second =
    ref.flatMap { r =>
      (printAndIncrement("Left")(r), printAndIncrement("Right")(r))
        .parMapN((l, r) => s"Values were $l and $r")
        .flatMap(IO.println)
    }

  // first and second are two programs that attempt to use a shared `Ref`. What
  // behaviour do you observe when these two programs are run. Why do you see
  // that behaviour? Which one do you think would be correct if we wanted to use
  // a `Ref` to coordinate two concurrent processes?
  val run = IO.println("First") *> first *> IO.println("Second") *> second
}
