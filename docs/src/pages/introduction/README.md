# Introduction

To start with we should explain what an effect is, and hence the problem that Cats Effect is solving. There is already an [excellent blog post][effect] that does just that, so go and read it now.

Now you understand what problem Cats Effect is trying to solve, and what it means for `IO` to be an effect type, let's start using `IO`. Complete the challenge in [`code/src/main/scala/introduction/01-basics.scala`][basics].

Most exercises will have solutions, just like the one below. It's a good idea to read them *even if you successfully solved the exercise*, as they'll talk about the lessons you should learn from the exercise, not just the solution to the exercise itself.

@:solution
The distinction between the description of an effect and the act of carrying out that description is made clear by defining `run` as a `val`. It can be a `val` *because* an `IO` is just a description. It does nothing until it is run.

1. The different constructors get to the difference between effectful and pure programs. It doesn't matter if we run a pure program multiple time or only once: it always produces the same result. Hence `IO.pure` can evaluate its argument once (at the time of construction) and cache that value. This is not the case for effectful programs, which use the `apply` constructor. We can illustrate the difference by using, say, `IO.pure(println("Hello"))` and `IO(println("Hello"))`.

2. `IO.realTime.flatMap(IO.println)`

3. If you replace `1` and `2` with effects (e.g. `println`) you can see in what order they are run. It's always the same: left-to-right.

4. It's incorrect. This is a common error. `IO` is a description, to writing `val _ = io` does nothing.

5. This will do

   ```scala
   def log[A](io: IO[A]): IO[A] = {
     IO.println("Starting")
       .flatMap(_ => io)
       .flatMap(a => IO.println("Stopping").map(_ => a))
   }
   ```
   
   A more stylish implementation would use a `for` comprehension.
   
6. This is an exercise in reading type signatures. `a *> b` creates an `IO` that runs `a`, discards its result, and then runs `b`. We can write `log` as

   ```scala
   def log[A](io: IO[A]): IO[A] = {
     IO.println("Starting") *> io <* IO.println("Stopping")
   }
   ```
   
   Whether we should do this or not depends on the context in which we're writing code. The `*>` and `<*` operators are a bit obscure, so they make our code less accessible to others. Use with caution.
@:@

[basics]: https://github.com/creativescala/cats-effect-tutorial/blob/main/code/src/main/scala/introduction/01-basics.scala
