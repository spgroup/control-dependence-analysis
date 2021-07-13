# CDA (Control Dependence Analysis) implementation based on Soot

This is a scala implementation of a framework that builds a control dependence edge using Soot and an Exceptional Block Graph Construction.

## Status

   * experimental

## Usage

   * clone this repository or download an stable release
   * you will need to add a github token to your **~/.gitconfig**.
     ```
     [github]
             token = TOKEN
             user = USER_NAME
     ```
   * build this project using sbt (`sbt compile test`)
   * publish the artifact as a JAR file in your m2 repository (`sbt publish`)
   * create a dependency to the control-dependence-analysis artifact in your maven project. 

```{xml}
<dependency>
  <groupId>br.ufpe.cin.control-dependence-analysis_2</groupId>
  <artifactId>12</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

   * implement a class that extends the `JCDA class` (see some examples in the scala tests). you must provide implementations to the following methods
      * `getEntryPoints()` to set up the "main" methods. This implementation must return a list of Soot methods
      * `sootClassPath()` to set up the soot classpath. This implementation must return a string
      * `analyze(unit)` to identify the type of a node  (source, sink, simple node) in the graph; given a statement (soot unit)


## Dependencies

This project use some of the [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) test cases. The FlowDroid test cases in `src/test/java/securibench` are under [LGPL-2.1](https://github.com/secure-software-engineering/FlowDroid/blob/develop/LICENSE) license.
