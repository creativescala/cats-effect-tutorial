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

package clock

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doodle.core.*
import doodle.core.font.Font
import doodle.svg.*
import doodle.syntax.all.*

import scala.concurrent.duration.*
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.*

@JSExportTopLevel("Clock")
object Clock {

  val now: IO[Date] = IO(new Date())

  def clock(canvas: Canvas): IO[Nothing] =
    IO.sleep(1.second) *> now
      .flatMap(date => draw(date, canvas))
      .flatMap(_ => clock(canvas))

  /** Given the current time and a canvas, draw a representation of the current
    * time on the canvas
    */
  def draw(date: Date, canvas: Canvas): IO[Unit] = {
    val seconds = date.getSeconds()
    val minutes = date.getMinutes()
    val hours = date.getHours()

    drawTimeUnit(hours.toInt, 24)
      .beside(drawTimeUnit(minutes.toInt, 60))
      .beside(drawTimeUnit(seconds.toInt, 60))
      .drawWithCanvasToIO(canvas)
  }

  /** Convert a hex string, such as "669933", to a Color */
  def hex(string: String): Color =
    Color.rgb(
      Integer.parseInt(string.substring(0, 2), 16),
      Integer.parseInt(string.substring(2, 4), 16),
      Integer.parseInt(string.substring(4, 6), 16)
    )

  // Palette from coolors.co
  val palette = Array(
    hex("f72585"),
    hex("b5179e"),
    hex("7209b7"),
    hex("560bad"),
    hex("480ca8"),
    hex("3a0ca3"),
    hex("3f37c9"),
    hex("4361ee"),
    hex("4895ef"),
    hex("4cc9f0"),
    hex("4895ef"),
    hex("4361ee"),
    hex("3f37c9"),
    hex("3a0ca3"),
    hex("480ca8"),
    hex("560bad"),
    hex("7209b7"),
    hex("b5179e"),
    hex("f72585")
  )

  def drawTimeUnit(current: Int, max: Int): Picture[Unit] = {
    val percentage = current.toDouble / max.toDouble
    val color = palette((percentage * palette.size).floor.toInt)
    val spacer = Picture.square(230).noStroke.noFill
    val text =
      Picture
        .text(current.toString)
        .font(Font.defaultSansSerif.size(36))
        .strokeColor(color)
        .fillColor(color)
    val bg = text.on(spacer)

    if percentage == 0.0 then bg
    else
      Picture
        .arc(200, Angle.turns(percentage))
        .strokeColor(color)
        .strokeWidth(13.0)
        .on(bg)
  }

  @JSExport
  def draw(id: String): Unit = {
    // This describes the background on which we'll render the clock
    val frame: Frame = Frame(id).withSize(800, 300)
    // A Canvas is an area of the screen we can draw onto
    val canvas: IO[Canvas] = frame.canvas()

    canvas.flatMap(c => clock(c)).unsafeRunAndForget()
  }
}
