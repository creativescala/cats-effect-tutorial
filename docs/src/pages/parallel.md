# Parallelism

```scala mdoc:invisible
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
```

If we have two `IO` values that don't depend on each other, we should be able to run them in parallel.
For example, the following two values could be run in parallel.

```scala mdoc:silent
val a = IO.println("A")
val b = IO.println("B")
```

We know that `mapN` expresses a computation that depends on two independent `IO` values, but always runs them from left to right.

```scala mdoc:silent
(a, b).mapN((_, _) => "Done").unsafeRunSync()
// A
// B
```

The reason for this is **type class coherence**, which is a fancy of way of saying "if you can implement a type class in different ways, all those different ways should give the same result." To understand this we need to know what a type class is, the two type classes in use here (monad and applicative), and their relationship (monad can implement applicative.) This is out of scope for us here. If you want to know more, *Functional Programming Strategies* has the details.

To get parallelism we can use `parMapN`. Try the following, and you should see `b` occasionally runs before `a`.

```scala mdoc:silent
(a, b).parMapN((_, _) => "Done").unsafeRunSync()
```


@:callout(info)
#### Parallelism versus Concurrency

Let's be clear on the difference between **parallelism** and **concurrency**:

- Parallelism means multiple things running at the same time, even though we might not be able to see this. For example, [SIMD](https://en.wikipedia.org/wiki/Single_instruction,_multiple_data) instructions on your CPU do multiple operations at the same time, but you cannot observe the intermediate results and hence cannot observe the parallelism. From the programmer's point of view you call the instruction on the CPU and get back the result.

- Concurrency means we can observe multiple things happening in overlapping time periods, even though they might not actually be running at the same time (i.e. they might not be parallel.) An example is Javascript. It has a single-threaded runtime but we can still observe race conditions between different callback functions.
@:@
