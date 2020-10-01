# scala-migrat3
it's a work-in progress tool to help migrating projects to Scala 3.
The goal is to provide a tool that will find the minimum set of types required explicitly to make dotty compiling a project without changing its meaning.

## Technical solution
The solution consists of 3 independent steps that will be packaged in a long-running script The compile inputs that are needed to compile both with scala 2 and dotty, will be provided by a sbt-plugin that we will need to develop. 

**First step**: Make sure that all methods/functions of the public API have an explicit result type. If not, we require to run `Explicit ResultType` rule, which will type explicitly public and protected methods. Those types will be kept even after scala 3 migration. This is a good practice anyway, even in the context of a single Scala version, so that the APIs do not change based on the whims of the compiler's type inference. It will ensure that the following steps do not alter the public API.

**Second step**: Find the minimum set of types required explicitly to make dotty compiling a project. The solution will be responsible for: 

Adding type annotations showing the types inferred by Scala 2 to the rest of the codebase. This rule should return a list of patches including the changes. This will be implemented as a Scalafix rule, technically similar to ExplicitResultTypes but covering many more subexpressions and type arguments.
If adding only inferred types does not allow compilation to succeed, we will add resolved implicit params and conversions using another scalafix rule.
Compiling with dotty (with -source:3.0-migration) after applying the patches on memory. 
Following a dichotomic algorithm to remove half of the type annotations  (ie patches) 
Repeating steps 2 and 3, until we find  the minimum set of types required for compilation.

At this point, we have a codebase that a) compiles with Dotty and b) preserves the public APIs (because of step 1). However, we have no guarantee that it preserves the semantics of the bodies of the methods, as different terms can be inferred due to different inferred types and/or implicit resolution rules. This is addressed by the third step.

**Third step**: Compare synthetics and more precisely implicit parameters and implicit conversions inferred in scala 2 and dotty, and produce a report. 

# Acknowledgments

<img src="https://scala.epfl.ch/resources/img/scala-center-swirl.png" width="40px" /> This tool is develloped by [Scala Center](https://scala.epfl.ch)
