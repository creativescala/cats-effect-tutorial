# Parallelism and Concurrency

```scala mdoc:invisible
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
```

We will now start exploring one of the main effects that people use Cats Effect for: parallelism and concurrency.

@:exercise(Parallelism vs Concurrency)
What's the difference between parallelism and concurrency?
@:@
@:solution
Let's be clear on the difference between **parallelism** and **concurrency**:

- Parallelism means multiple things running at the same time, even though we might not be able to see this. For example, [SIMD](https://en.wikipedia.org/wiki/Single_instruction,_multiple_data) instructions on your CPU do multiple operations at the same time, but you cannot observe the intermediate results and hence cannot observe the parallelism. From the programmer's point of view you call the instruction on the CPU and get back the result.

- Concurrency means we can observe multiple things happening in overlapping time periods, even though they might not actually be running at the same time (i.e. they might not be parallel.) An example is Javascript. It has a single-threaded runtime but we can still observe race conditions between different callback functions.
@:@

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

@:exercise(Parallelism or Concurrency?)
Does `parMapN` display parallelism, concurrency, neither, or both?
@:@
@:solution
It's both. It depends on the effects you run with `parMapN`, so it's not really a property of `parMapN` but of both `parMapN` and the effects that are being run.

If the effects being run only compute values, so we have no way of observing that are doing stuff, then we have just parallelism. However, if we can observe them being run, as we can when we use `IO.println`, then we have concurrency.
@:@


How does `parMap` work? It's either *dark magic* or an instance of the [Parallel](https://typelevel.org/cats/typeclasses/parallel.html) type class. Despite it's name, `Parallel` is not about running code in parallel. It's just about having a different implementation of `mapN` and friends with different semantics. For `IO` these different semantics are, coincidently, about parallelism, but you can call `parMapN` on `Either` and get something else.

@:exercise(Parallel Either)
What does `parMapN` do on `Either`? Create some code examples illustrating the difference between the usual `mapN`.

Note: you will need to `import cats.syntax.all.*` to make `mapN` etc. available.

@:@
@:solution
`parMapN` accumulates errors. Here's an example. Start by defining two failed `Eithers`.

```scala mdoc:silent
import cats.syntax.all.*

val failed1: Either[List[String], Int] = Left(List("Oh no! I failed!"))
val failed2: Either[List[String], Int] = Left(List("Oh no! I also failed!"))
```

Now let's see what happens when we use `mapN`.

```scala mdoc
(failed1, failed2).mapN((a, b) => a + b)
```

We get only the first failure. 

Now we use `parMapN`.

```scala mdoc
(failed1, failed2).parMapN((a, b) => a + b)
```

We get *both* failures.
@:@


There are a bunch of `par` methods. Here are the most useful ones, with simplified type signatures.

* `(F[A], ..., F[Z]).parTupled: F[(A, ..., Z)]`
* `(F[A], ..., F[Z]).parMapN((A, ..., Z) => AA): AA`
* `G[F[A]].parSequence: F[G[A]]`
* `G[A].parTraverse(A => F[B]): F[G[B]]`

Complete the challenge in [`code/src/main/scala/parallelism/01-parallelism.scala`][parallelism] to increase your familiarity with `parMapN` and friends, and then complete [`code/src/main/scala/parallelism/02-job-manager.scala`][job-manager] to use the tools in a somewhat realistic context.

[parallelism]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/parallelism/01-parallelism.scala
[job-manager]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/parallelism/02-job-manager.scala
