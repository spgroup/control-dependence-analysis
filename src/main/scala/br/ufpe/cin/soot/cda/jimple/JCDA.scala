package br.ufpe.cin.soot.cda.jimple

import br.ufpe.cin.soot.graph.{FalseEdge, LambdaNode, SimpleNode, Stmt, StmtNode, StringLabel, TrueEdge, UnitDummy, UnitGraphNodes}
import br.ufpe.cin.soot.cda.CDA
import br.ufpe.cin.soot.cda.rules.ArrayCopyRule

import java.util
import soot.toolkits.graph._
import br.ufpe.cin.soot.cda.SourceSinkDef
import com.typesafe.scalalogging.LazyLogging
import soot.jimple.GotoStmt
import soot.toolkits.graph.LoopNestTree
import soot.toolkits.graph.ExceptionalBlockGraph
import soot.toolkits.graph.pdg.HashMutablePDG
import soot.toolkits.scalar.SimpleLocalDefs
import soot.{Local, Scene, SceneTransformer, SootMethod, Transform}

import scala.collection.mutable.ListBuffer

/**
 * A Jimple based implementation of
 * CDA.
 */
abstract class JCDA extends CDA with Analysis with FieldSensitiveness with SourceSinkDef with LazyLogging {

  var methods = 0
  val traversedMethods = scala.collection.mutable.Set.empty[SootMethod]
  val allocationSites = scala.collection.mutable.HashMap.empty[soot.Value, soot.Unit]
  val arrayStores = scala.collection.mutable.HashMap.empty[Local, List[soot.Unit]]
  val phantomMethodRules = List(new ArrayCopyRule)
  def createSceneTransform(): (String, Transform) = ("wjtp", new Transform("wjtp.cda", new Transformer()))

  def configurePackages(): List[String] = List("cg", "wjtp")

  def beforeGraphConstruction(): Unit = { }

  def afterGraphConstruction() { }

  def initAllocationSites(): Unit = {
    val listener = Scene.v().getReachableMethods.listener()
  }

  class Transformer extends SceneTransformer {
    override def internalTransform(phaseName: String, options: util.Map[String, String]): Unit = {
      pointsToAnalysis = Scene.v().getPointsToAnalysis
      initAllocationSites()

      Scene.v().getEntryPoints.forEach(method => {
        traverse(method)
        methods = methods + 1
      })
    }
  }

  def traverse(method: SootMethod, forceNewTraversal: Boolean = false) : Unit = {
    if((!forceNewTraversal) && (method.isPhantom || traversedMethods.contains(method))) {
      return
    }

    try {

      traversedMethods.add(method)
      val body = method.retrieveActiveBody()

      val unitGraph= new UnitGraphNodes(body)

      val analysis = new MHGPostDominatorsFinder(unitGraph)

      unitGraph.forEach(unit => {
        var edges = unitGraph.getSuccsOf(unit)
        var ADominators = analysis.getDominators(unit)

        //Find a path with from unit to edges, using the post-dominator tree, excluding the LCA node
        //Add True and False edge
        var typeEd = true
        var count = 0
        edges.forEach(unitAux =>{
          var BDominators = analysis.getDominators(unitAux)
          var dItB = BDominators.iterator
          while (dItB.hasNext()) {
            val dsB = dItB.next()
            if (!ADominators.contains(dsB)){
              if (count > 0){
                typeEd = false
              } else {
                typeEd = true //The first time is true
              }
              addControlDependenceEdge(unit, dsB, typeEd, method)
            }
          }
          count = count + 1
        })
      })
    } catch {
      case e: NullPointerException => {
        println ("Error creating node, an invalid statement.")
      }
      case e: Exception => {
        println ("An invalid statement.")
      }
    }
  }

  def addControlDependenceEdge(s: soot.Unit, t: soot.Unit, typeEdge: Boolean, method: SootMethod): Unit = {
    if (s.isInstanceOf[GotoStmt] || t.isInstanceOf[GotoStmt]) return
    var source = createNode(method, s)
    var target = createNode(method, t)

    if (s.isInstanceOf[UnitDummy]) {
      source = createDummyNode(s, method)
    }

    if (t.isInstanceOf[UnitDummy]){
      target = createDummyNode(t, method)
    }

    addEdgeControlDependence(source, target, typeEdge)
  }

  def createDummyNode(unit: soot.Unit, method: SootMethod): StmtNode = {
    var node = createNode(method, unit)

    if (unit.toString().contains("EntryPoint")) {
      node = createEntryPointNode(method)
    } else if (unit.toString().contains("Start")) {
      node = createStartNode(method)
    } else if (unit.toString().contains("Stop")) {
      node = createStopNode(method)
    }
    return node
  }

  def addEdgeControlDependence(source: LambdaNode, target: LambdaNode, typeEdge: Boolean): Boolean = {
    var res = false
    if(!runInFullSparsenessMode() || true) {
      val label = new StringLabel( if (typeEdge) (TrueEdge.toString) else (FalseEdge.toString))
      svg.addEdge(source, target, label)
      res = true
    }
    return res
  }

  def addEdge(source: LambdaNode, target: LambdaNode): Boolean = {
    var res = false
    if(!runInFullSparsenessMode() || true) {
      svg.addEdge(source, target)
      res = true
    }
    return res
  }

  /*
     * This rule deals with the following situation:
     *
     * (*) p = q
     *
     * In this case, we create an edge from defs(q)
     * to the statement p = q.
     */
  private def copyRule(targetStmt: soot.Unit, local: Local, method: SootMethod, defs: SimpleLocalDefs) = {
    defs.getDefsOfAt(local, targetStmt).forEach(sourceStmt => {
      val source = createNode(method, sourceStmt)
      val target = createNode(method, targetStmt)
      updateGraph(source, target)
    })
  }


  /*
   * creates a graph node from a sootMethod / sootUnit
   */
  def createNode(method: SootMethod, stmt: soot.Unit): StmtNode = {
    try{
      return new StmtNode(Stmt(method.getDeclaringClass.toString, method.getSignature, stmt.toString, stmt.getJavaSourceStartLineNumber), analyze(stmt))
    }catch {
      case e: NullPointerException => {
        println("Error creating node, an invalid statement.")
        return null
      }
    }
  }


  def createEntryPointNode(method: SootMethod): StmtNode = {
    try {
      return new StmtNode(Stmt(method.getDeclaringClass.toString, method.getSignature, "Entry Point", 0), SimpleNode)
    } catch {
    case e: NullPointerException => {
        println ("Error creating node, an invalid statement.")
        return null
      }
    }
  }

  def createStartNode(method: SootMethod): StmtNode = {
    try {
      return new StmtNode(Stmt(method.getDeclaringClass.toString, method.getSignature, "Start", 0), SimpleNode)
    } catch {
      case e: NullPointerException => {
        println ("Error creating node, an invalid statement.")
        return null
      }
    }
  }

  def createStopNode(method: SootMethod): StmtNode = {
    try {
      return new StmtNode(Stmt(method.getDeclaringClass.toString, method.getSignature, "Stop", 0), SimpleNode)
    } catch {
      case e: NullPointerException => {
        println ("Error creating node, an invalid statement.")
        return null
      }
    }
  }

  /**
   * Override this method in the case that
   * a complete graph should be generated.
   *
   * Otherwise, only nodes that can be reached from
   * source nodes will be in the graph
   *
   * @return true for a full sparse version of the graph.
   *         false otherwise.
   * @deprecated
   */
  def runInFullSparsenessMode() = true

  //  /*
  //   * It either updates the graph or not, depending on
  //   * the types of the nodes.
  //   */
  private def updateGraph(source: LambdaNode, target: LambdaNode, forceNewEdge: Boolean = false): Boolean = {
    var res = false
    if(!runInFullSparsenessMode() || true) {
      svg.addEdge(source, target)
      res = true
    }

    // this first case can still introduce irrelevant nodes
    //    if(svg.contains(source)) {//) || svg.map.contains(target)) {
    //      svg.addEdge(source, target)
    //      res = true
    //    }
    //    else if(source.nodeType == SourceNode || source.nodeType == SinkNode) {
    //      svg.addEdge(source, target)
    //      res = true
    //    }
    //    else if(target.nodeType == SourceNode || target.nodeType == SinkNode) {
    //      svg.addEdge(source, target)
    //      res = true
    //    }
    return res
  }
}
