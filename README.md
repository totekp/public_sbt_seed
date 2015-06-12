[![Build Status](https://travis-ci.org/totekp/sbt_seed.svg)](https://travis-ci.org/totekp/sbt_seed)

# Sbt_seed
Seed project for scala and other sbt projects

## Shortcuts
- [`/project/Dependencies.scala`](/project/Dependencies.scala)
- [`/build.sbt`](/build.sbt)
- [`/project/build.properties`](/project/build.properties)


## Usage
1. Create a directory for sbt project.
2. In project root, create directory `project/`, then copy `Dependencies.scala` and `build.properties` into directory.
2. In project root, create file `build.sbt`, then use `build.sbt` as a template to define your own project build.
3. Using a shell in project root, run command `sbt` to open sbt REPL. Use command `test` to test the project build is valid.
4. Create missing directories such as `src/[main|test]/[scala|resources]/` for a typical scala project. If using IntelliJ, empty directories can be created automatically when importing/opening a new project.
5. Direct any questions or contributions to kfer38@gmail.com or Issues/PR