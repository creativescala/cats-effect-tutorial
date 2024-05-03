# Concurrent Coordination

```scala mdoc:invisible
import cats.effect.*
```

We can get quite far with `parMapN` and friends, but complex concurrent programs require coordination between different parts that extends beyond returning values. The Cats Effects [standard library][std] provides useful tools for communication between concurrent processes. There is also [Ref][ref] and [Deferred][deferred], which are part of the `kernel` and the most basic tools on which many others are built.


## Creating Concurrent Tools

We'll use `Ref` as an example of a concurrent tool. All the others work in the same way.

The simplest way to create a `Ref` is to use `IO.ref`.

```scala mdoc:silent
val ref: IO[Ref[IO, Int]] = IO.ref(1)
```

The type looks a bit complicated. Unpacking it we have:

- an `IO[Stuff]`, meaning an `IO` that produces `Stuff` when run; and
- `Stuff` is `Ref[IO, Int]`, meaning a `Ref` that stores an `Int` and works with `IO`. 

You'll have to get used to these kind of types when using Cats Effect.

We can also construct a `Ref` by calling the `apply` method on the companion object. In this case we have to specify the effect type (which is always `IO`, for us) to help out type inference.

```scala mdoc:silent
val ref2 = Ref[IO].of(1)
```

We could also write out the full type, as below, but this quickly gets tedious.

```scala mdoc:silent
val ref3: IO[Ref[IO, Int]] = Ref.of(1)
```

@:exercise(Putting Tools to Use)
Complete the challenge in [`code/src/main/scala/parallelism/02-tools.scala`][tools-exercise], which gets you to use some of the tools provided by Cats Effect.
@:@

@:solution
1. This exercise is focusing on the difference between description and action. The code in `first` uses a description twice, so it gets two *different* `Refs`. The code in `second` uses the same `Ref` twice, which is usually what you want.

2. The following code will do the job.

```scala
def generate(ref: Ref[IO, Int]) = smallRandomSleep
  .map(_ => random.nextInt(10))
  .flatMap(v => ref.getAndUpdate(a => a + v))
  .replicateA_(100)

def collector(ref: Ref[IO, Int]) =
  IO.sleep(1.second)
    .flatMap(_ => ref.get)
    .flatMap(v => IO.println(s"Value is $v"))

val run =
  ref.flatMap { r =>
    (
      generate(r),
      generate(r),
      generate(r),
      generate(r),
      generate(r),
      collector(r)
    ).parTupled.void
  }
```
@:@

[std]: https://typelevel.org/cats-effect/api/3.x/cats/effect/std/index.html
[ref]: https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Ref.html
[deferred]: https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Deferred.html
[deferred-doc]: https://typelevel.org/cats-effect/docs/std/deferred
[tools-exercise]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/parallelism/03-tools.scala
[deferred-exercise]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/parallelism/03-deferred.scala
