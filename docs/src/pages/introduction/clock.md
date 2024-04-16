# Clock

In this exercise we put together the basics we've just learned about using Cats Effect to create a complete program that will make a clock like the one shown below.

@:doodle("clock-mount", "Clock.draw")

Complete the challenge in [`code/src/main/scala/introduction/02-clock.scala`][clock].

As a creative person, you probably want to put your own mark on the clock. For example, you might want to rotate and flip the arcs that display the time, so that they start at the twelve o'clock position and rotate clockwise, or perhaps you want to change the colors. Go ahead and do this! Make it your own!

@:solution
The core of the clock is the following loop

```scala 
def clock(canvas: Canvas): IO[Nothing] =
  IO.sleep(1.second) *> IO.realTimeInstant
    .flatMap(instant => draw(instantToLocalTime(instant), canvas))
    .flatMap(_ => clock(canvas))
```

Why do we have to use `flatMap` for the final recursive call back to `clock`? It seems we could just write

```scala
*> clock(canvas)
```

this doesn't work. Why not?
@:@

[clock]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/introduction/02-clock.scala
