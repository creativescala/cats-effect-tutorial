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

package introduction

// You will probably need this import
// import scala.concurrent.duration.*

import java.time.*

import cats.effect.{IO, IOApp}
import doodle.core.*
import doodle.core.font.Font
import doodle.java2d.*
import doodle.syntax.all.*

object Clock extends IOApp.Simple {
  // This describes the background on which we'll render the clock
  val frame: Frame =
    Frame.default
      .withSize(800, 300)
      .withBackground(Color.midnightBlue)
      .withTitle("Clock")
  // A Canvas is an area of the screen we can draw onto
  val canvas: IO[Canvas] = frame.canvas()

  /** Convert an Instant to a LocalTime */
  def instantToLocalTime(instant: Instant): LocalTime =
    instant.atZone(ZoneOffset.UTC).toLocalTime()

  def clock(canvas: Canvas): IO[Nothing] =
    // This code should repeat forever:
    //
    // 1. Sleep for one second
    // 2. Get the current time (an Instant)
    // 3. Convert the current time to a LocalTime
    // 4. Draw the clock given the LocalTime
    //
    // Note: You should use methods on IO, and methods defined here, to accomplish
    // this. You should not directly access any Java APIs.
    ???

  /** Given the current time and a canvas, draw a representation of the current
    * time on the canvas
    */
  def draw(time: LocalTime, canvas: Canvas): IO[Unit] = {
    val seconds = time.getSecond()
    val minutes = time.getMinute()
    val hours = time.getHour()

    drawTimeUnit(hours, 24)
      .beside(drawTimeUnit(minutes, 60))
      .beside(drawTimeUnit(seconds, 60))
      .drawWithCanvasToIO(canvas)
  }

  // Palette from coolors.co
  val palette = Array(
    Color.fromHex("f72585"),
    Color.fromHex("b5179e"),
    Color.fromHex("7209b7"),
    Color.fromHex("560bad"),
    Color.fromHex("480ca8"),
    Color.fromHex("3a0ca3"),
    Color.fromHex("3f37c9"),
    Color.fromHex("4361ee"),
    Color.fromHex("4895ef"),
    Color.fromHex("4cc9f0"),
    Color.fromHex("4895ef"),
    Color.fromHex("4361ee"),
    Color.fromHex("3f37c9"),
    Color.fromHex("3a0ca3"),
    Color.fromHex("480ca8"),
    Color.fromHex("560bad"),
    Color.fromHex("7209b7"),
    Color.fromHex("b5179e"),
    Color.fromHex("f72585")
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

  val run = canvas.flatMap(c => clock(c))
}
