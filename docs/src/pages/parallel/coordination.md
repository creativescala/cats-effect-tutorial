# Concurrent Coordination

```scala mdoc:invisible
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
```

There aren't many concurrent programs that don't require some interaction between the different parts. The Cats Effects [standard library][std] provides useful tools for coordinating concurrent processes. There is also [Ref][ref], which is part of the `kernel`, and is one of the most basic tools on which many others are built.


## Constructing Ref

The simplest way to create a `Ref` is to use `IO.ref`.

```scala mdoc:silent
val ref: IO[Ref[IO, Int]] = IO.ref(1)
```

The type looks a bit complicated. Unpacking it we have:

- an `IO[Stuff]`, meaning an `IO` that produces `Stuff` when run; and
- `Stuff` is `Ref[IO, Int]`, meaning a `Ref` that stores an `Int` and works with `IO`. 

You'll have to get used to these kind of types when using Cats Effect.


## Using Ref

[std]: https://typelevel.org/cats-effect/api/3.x/cats/effect/std/index.html
[ref]: https://typelevel.org/cats-effect/api/3.x/cats/effect/kernel/Ref.html
