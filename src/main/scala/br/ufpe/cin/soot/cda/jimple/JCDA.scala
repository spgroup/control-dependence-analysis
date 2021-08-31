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

//    try {

      traversedMethods.add(method)

      val body = method.retrieveActiveBody()
      val blocks = new ExceptionalBlockGraph(body)
/*
    //Create EntryPoint Node
    val EntryPoint = createEntryPointNode(method)
    //Create Start Node
    val Start = createStartNode(method)

    //Add edge from Entry Point to Start with label True
    addEdgeControlDependence(EntryPoint, Start, true)
    //Create Stop Node
    val Stop = createStopNode(method)

    //Add edge From Entry Point to Stop with label False
    addEdgeControlDependence(EntryPoint, Stop, false)

    //Add edge from Start to first unit from block[0]
    addEdgeFromBlockToStart(Start, blocks, 0, method)

    //Add edge from into blocks
    for (i <-  0 to blocks.getBlocks.size()-1) {
      addEdgeinBlock(blocks, i, method)
    }

    blocks.getBlocks.forEach(block => {
        if (block.getSuccs.size()==2){ //True and False edge
          //Add edge from tail block from head succ 1 and succ 2
          addEdgeFromBlockToSuccs(block, block.getSuccs.get(0), block.getSuccs.get(1), method)
        }else if (block.getSuccs.size()==1){ //True edge
          //Add edge from tail block from head succ 1
          addEdgeFromBlockToSucc(block, block.getSuccs.get(0), method)
        }else if (block.getSuccs.size == 0){ //Edge to Stop Node
          addEdgeFromBlockToStop(Stop, blocks, block.getIndexInMethod, method)
        }
      })
    */

    val cfg = new BriefBlockGraph(body)

//      BlockGraphConverter.addStartStopNodesTo(cfg)

//      BlockGraphConverter.reverse(cfg)

    var unitGraph2 = new BriefUnitGraph(body)
    val initialGraph = new BriefUnitGraph(body)

    //Eric's PDG Implementation

    val graph2 = new UnitGraphNodes(body)

    val analysis = new MHGPostDominatorsFinder(graph2)
    /*
    println("\ndigraph { ")
    val it = body.getUnits.iterator
    while (it.hasNext()) {
      val s = it.next()
      val dominators = analysis.getDominators(s)
      val dIt = dominators.iterator
      while (dIt.hasNext()) {
        val ds = dIt.next()

        //        addEdgeGraph(ds, s, method)

        if (ds!=s){
          if (s.isInstanceOf[UnitDummy] && ds.isInstanceOf[UnitDummy]){
            val info = "\"" +ds.getUseAndDefBoxes.get(0).getValue +"\"" + " -> " + "\""+s.getUseAndDefBoxes.get(0).getValue+"\""
            println(info)
          }else if (s.isInstanceOf[UnitDummy]){
            val info = "\"" +ds +"\"" + " -> " + "\""+s.getUseAndDefBoxes.get(0).getValue+"\""
            println(info)
          }else if (ds.isInstanceOf[UnitDummy]){
            val info = "\"" +ds.getUseAndDefBoxes.get(0).getValue +"\"" + " -> " + "\""+s+"\""
            println(info)
          }else {
            val info = "\"" +ds +"\"" + " -> " + "\""+s+"\""
            println(info)
          }
        }
      }
    }
    println("}")
*/

    graph2.forEach(unit => {
      var edges = graph2.getSuccsOf(unit)

      if (unit.getUseAndDefBoxes.size()>0){
        if ((unit.branches()) || unit.getUseAndDefBoxes.get(0).getValue.toString().contains("EntryPoint")){ //It's a conditional statement: True e False Edge
          var ADominators = analysis.getDominators(unit)

          //Find a path with from unit to edges, using the post-dominator graph, excluding the LCA node
          //Add True and False edge

          var BDominators = analysis.getDominators(edges.get(0))
//          println("\n("+unit+", "+edges.get(0)+") -> ")
          var dItB = BDominators.iterator
          while (dItB.hasNext()) {
            val dsB = dItB.next()
            if (!ADominators.contains(dsB)){
              addControlDependenceEdge(unit, dsB, true, method)
//              print(dsB+", ")
            }
          }

          BDominators = analysis.getDominators(edges.get(1))
//          println("\n("+unit+", "+edges.get(1)+") -> ")
          dItB = BDominators.iterator
          while (dItB.hasNext()) {
            val dsB = dItB.next()
            if (!ADominators.contains(dsB)){
              addControlDependenceEdge(unit, dsB, false, method)
//              print(dsB+", ")
            }
          }

        }else{
          //Find a path with from unit to edges, using the post-dominator graph, excluding the LCA node
          var ADominators = analysis.getDominators(unit)
          var BDominators = analysis.getDominators(edges.get(0))
//          println("\n("+unit+", "+edges.get(0)+") -> ")
          var dItB = BDominators.iterator
          while (dItB.hasNext()) {
            val dsB = dItB.next()
            if (!ADominators.contains(dsB)){
              addControlDependenceEdge(unit, edges.get(0), true, method)
//              print(dsB+", ")
            }
          }

        }
      }
    })

    var xxx = graph2.getHeads.get(0).getUseAndDefBoxes

    val analysis2 = new MHGPostDominatorsFinder(cfg)

/*
      val it = cfg.getBlocks.iterator
      println("digraph { ")
      while (it.hasNext()) {
        val s = it.next()
        val dominators = analysis2.getDominators(s)
        val dIt = dominators.iterator

//        l += dIt.next()
//        println("\""+s+"\"")
//        println(dominators)
        dominators.iterator.forEachRemaining(unit =>{
          if (unit.getSuccs().size()!=0) {
            println(unit)
          }
        })

        while (dIt.hasNext()) {
          val ds = dIt.next()
//          if (ds!=s){
//            val info = "\"" +ds +"\"" + " -> " + "\""+s+"\""
//            println(info)
//            println(new LinkTag(info, ds, body.getMethod.getDeclaringClass.getName, "Dominators"))
//          }
        }
      }
//      println(l)
      println("}")
*/
//      val pdg = new HashMutablePDG(graph2)
//
//      var y = pdg.getWeakRegions
//      var x = pdg.getStrongRegions

//      controlDependence(pdg, method)

//      val EntryPointNode = createNodeEntryPoint(method)
//
//      addCDEdges(method, EntryPointNode, ListBuffer[ValueNodeType](), blocks, 0, true, ListBuffer[Int]())

//    } catch {
//      case e: NullPointerException => {
//        println ("Error creating node, an invalid statement.")
//      }
//    }
  }

  def addControlDependenceEdge(s: soot.Unit, t: soot.Unit, typeEdge: Boolean, method: SootMethod): Unit = {
    if (s.isInstanceOf[GotoStmt] || t.isInstanceOf[GotoStmt]) return
    var source = createNode(method, s)
    var target = createNode(method, t)

    if (s.isInstanceOf[UnitDummy]) {
      if (s.getUseAndDefBoxes.get(0).getValue.toString().contains("EntryPoint")) {
        source = createEntryPointNode(method)
      } else if (s.getUseAndDefBoxes.get(0).getValue.toString().contains("Start")) {
        source = createStartNode(method)
      } else if (s.getUseAndDefBoxes.get(0).getValue.toString().contains("Stop")) {
        source = createStopNode(method)
      }
    }

    if (t.isInstanceOf[UnitDummy]){
      if (t.getUseAndDefBoxes.get(0).getValue.toString().contains("EntryPoint")) {
        target = createEntryPointNode(method)
      }else if (t.getUseAndDefBoxes.get(0).getValue.toString().contains("Start")){
        target = createStartNode(method)
      } else if (t.getUseAndDefBoxes.get(0).getValue.toString().contains("Stop")){
        target = createStopNode(method)
      }
    }

    addEdgeControlDependence(source, target, typeEdge)
  }

  def controlDependence(pdg: HashMutablePDG, method: SootMethod): Unit = {
    val regions = pdg.getStrongRegions

    var isNotEntryPoint = true
    var pos = -1
    while (isNotEntryPoint && pos < pdg.getStrongRegions.size()) {
      pos = pos + 1
      if (pdg.getStrongRegions.get(pos).getID==0){
        isNotEntryPoint = false
      }
    }


    var cont = 1
    if (pdg.getStrongRegions.get(pos).getID == 0){
      pdg.GetStartRegion.getUnits.forEach(unit => {
        addNodeEP(method, unit)
        if (unit.isInstanceOf[IfStmt]) {
          pdg.getStrongRegions.get(cont).getUnits.forEach(unitTo => {
            val source = createNode(method, unit)
            val target = createNode(method, unitTo)
            addEdgeControlDependence(source, target, true)
          })
          cont = cont + 1
        }

      })
    }
  }

    class ValueNodeType(valueIndex: Int, stmt: StmtNode) {
    val value: Int = valueIndex
    val nodeStmt: StmtNode = stmt

    def show(): String = stmt.stmt.toString
  }

  def addCDEdges(method: SootMethod, fromNode: StmtNode, visitedBlocks: ListBuffer[ValueNodeType], blocks: ExceptionalBlockGraph, value: Int, typeEdge: Boolean, exitList: ListBuffer[Int]): Unit = {

    visitedBlocks.foreach(visitedBlock =>{
      if (visitedBlock.value == value && visitedBlock.nodeStmt == fromNode) {
        return
      }
    })

    addEdgesToBlock(method, fromNode, blocks, value, typeEdge)

    var block = blocks.getBlocks.get(value)

    visitedBlocks += new ValueNodeType(value, fromNode)

    if (block.getSuccs.size() == 2) {

      var ifStmt = blockToIfstatement(blocks, value)
      var stmNodeX = createNode(method, ifStmt).stmt
      visitedBlocks.foreach(visitedBlock =>{
        if (visitedBlock.nodeStmt.stmt.stmt == stmNodeX.stmt) {
          return
        }
      })
      var succ = block.getSuccs
      var pred = block.getPreds

      var enterBlock = block.getSuccs.get(0).getIndexInMethod
      var exitBlock = block.getSuccs.get(1).getIndexInMethod

      var listContainsPathsLoop = ListBuffer[Boolean]()
      var blockListContainsPathsLoop = ListBuffer[Int]()

      hasAPathFromTo(blocks.getBlocks.get(enterBlock), block, listContainsPathsLoop, blockListContainsPathsLoop)
      var hasAPathLoop = false
      if (listContainsPathsLoop.contains(true)){
        hasAPathLoop = true
      }

      var hasReturnEdge = false
      pred.forEach(predElem =>{
        if (predElem.getIndexInMethod > block.getIndexInMethod){
          hasReturnEdge = true
        }
      })

      var valueHDC = HCD(blocks, value)

      var isAnIf = false

      var booleanListWithBlock = ListBuffer[Boolean]()
      var intListWithBlock = ListBuffer[Int]()
      intListWithBlock += block.getIndexInMethod
      hasAPathFromTo(block.getSuccs.get(0), block.getSuccs.get(1), booleanListWithBlock, intListWithBlock)
      if (booleanListWithBlock.contains(true)){
        isAnIf = true
      }

      var listContainsDoWhile = ListBuffer[Boolean]()
      var blockListContainsDoWhile = ListBuffer[Int]()

      blockListContainsDoWhile += exitBlock
      var itsDoWhile = true
      hasAPathFromToDoWhile(blocks.getBlocks.get(enterBlock), block, exitBlock, listContainsDoWhile, blockListContainsDoWhile)
      if (listContainsDoWhile.contains(true)){
        itsDoWhile = false
      }

      var isALoop = false
      var loopsList = new LoopNestTree(method.getActiveBody())

      loopsList.forEach(loopElem=> {
       var loopStatement = loopElem.getHead

        if (ifStmt.equals(loopStatement)){
          isALoop = true
        }
      })

      var itsWhile = false
      if (hasAPathLoop){
        itsWhile = true
      }

      var isAnIfElse = false

      if (enterBlock < valueHDC && exitBlock < valueHDC && valueHDC != -1 || (!isAnIf && !isALoop)) {
        isAnIfElse = true
      }

      if (isAnIf){
        println("It's an IF")
      }
      if (isAnIfElse){
        println("It's an IF-ELSE")
      }
      if (itsWhile){
        println("It's a WHILE")
      }
      if (itsDoWhile && isALoop){
        println("It's a DO-WHILE")
      }

      if (valueHDC == -1 || isAnIf) { //It's an IF, WHILE, DO-WHILE, FOR and others
        var exitNotVisited = true
        if (isAnIf && !itsDoWhile) {
          exitList.foreach(exitElem =>{
            if (exitElem == exitBlock) {
              exitNotVisited = false
            }
          })
          if (hasReturnEdge){
            exitNotVisited = true
          }

          exitList += exitBlock
        }
        if (itsDoWhile){ //swap enter and exit blocks
          var aux = exitBlock
          exitBlock = enterBlock
          enterBlock = aux
        }

        if (exitBlock > value+1 && exitNotVisited || itsDoWhile) {
          addCDEdges(method, fromNode, visitedBlocks, blocks, exitBlock, typeEdge, exitList)
        }

        addCDEdges(method, createNode(method, ifStmt), visitedBlocks, blocks, enterBlock, false, exitList)

      }else{ //It's an IF-ELSE

        addCDEdges(method, fromNode, visitedBlocks, blocks, valueHDC, false, exitList)

        visitedBlocks += new ValueNodeType(valueHDC, createNode(method, ifStmt))
        exitList += valueHDC

        addCDEdges(method, createNode(method, ifStmt), visitedBlocks, blocks, exitBlock, false, exitList)
        addCDEdges(method, createNode(method, ifStmt), visitedBlocks, blocks, enterBlock, true, exitList)

      }

    }else if (block.getSuccs.size() == 1){
      exitList.foreach(exitElem =>{
        if (exitElem == block.getSuccs.get(0).getIndexInMethod) {
          return
        }
      })
      if (isNotContainsGoto(block.getSuccs.get(0)) && isNotContainsGoto(block)) {
        addCDEdges(method, fromNode, visitedBlocks, blocks, block.getSuccs.get(0).getIndexInMethod, typeEdge, exitList)
      }
    }
  }

  def isNotContainsGoto(block: Block): Boolean = {
    var contains = true
    block.forEach(blockElem => {
      if (blockElem.isInstanceOf[GotoStmt]) {
        contains = false
      }
    })
    return contains
  }

  def hasAPathFromToDoWhile(enter: Block, block: Block, exit: Int, visitedPaths: ListBuffer[Boolean], visitedBlock: ListBuffer[Int]): Unit = {

    if (visitedBlock.contains(enter.getIndexInMethod)) return

    visitedBlock += enter.getIndexInMethod

    val suc = enter.getSuccs

    if (suc.size() > 0){
      suc.forEach(actualSucc => {

        if (actualSucc.getIndexInMethod == block.getIndexInMethod || actualSucc.getIndexInMethod == exit) {
          visitedPaths += true
          return visitedPaths
        }
        if (!visitedBlock.contains(actualSucc.getIndexInMethod) && actualSucc.getIndexInMethod > block.getIndexInMethod){
          hasAPathFromToDoWhile(actualSucc, block, exit, visitedPaths, visitedBlock)
        }
      })
    }
  }

  def hasAPathFromTo(enter: Block, exit: Block, visitedPaths: ListBuffer[Boolean], visitedBlock: ListBuffer[Int]): Unit = {

    if (visitedBlock.contains(enter.getIndexInMethod)) return

    visitedBlock += enter.getIndexInMethod

    val suc = enter.getSuccs

    if (suc.size() > 0){
      suc.forEach(actualSucc => {

        if (actualSucc.getIndexInMethod == exit.getIndexInMethod) {
          visitedPaths += true
          return visitedPaths
        }
        if (!visitedBlock.contains(actualSucc.getIndexInMethod)){
          hasAPathFromTo(actualSucc, exit, visitedPaths, visitedBlock)
        }
      })
    }
  }

  def blockToIfstatement(blocks: ExceptionalBlockGraph, value: Int): soot.Unit ={
    blocks.getBlocks.get(value).forEach(unit => {
      if (unit.isInstanceOf[soot.jimple.IfStmt]){
        return unit
      }
    })
    return null
  }

  def HCD(blocks: ExceptionalBlockGraph, blockIndex: Int): Int = {
    var left =  ListBuffer[Int]()
    var right = ListBuffer[Int]()

    left += blockIndex
    right += blockIndex
    //Succs of left
    var lIndex =  blocks.getBlocks.get(blockIndex).getSuccs.get(0).getIndexInMethod
    getSuccsDFS(blocks, lIndex, left)

    //Succs of right
    var rIndex = blocks.getBlocks.get(blockIndex).getSuccs.get(1).getIndexInMethod
    getSuccsDFS(blocks, rIndex, right)

    //    TODO: to use Binary Search
    for (leftIndex <- left) {
      if (right.contains(leftIndex) && leftIndex != blockIndex){
        return leftIndex
      }
    }
    return -1
  }

  def getSuccsDFS(blocks: ExceptionalBlockGraph, actualIndex: Int, visited: ListBuffer[Int]): Unit = {
    if (visited.contains(actualIndex)) {
      return
    }else {
      visited += actualIndex
      var block = blocks.getBlocks.get(actualIndex).getSuccs
      for (i <- 0 to block.size()-1){
        var vActual = block.get(i).getIndexInMethod
        if (!visited.contains(vActual)) {
          getSuccsDFS(blocks, vActual, visited)
        }
      }
    }
  }

  def addEdgesToBlock(method: SootMethod, sourceStmt: StmtNode, blocks: ExceptionalBlockGraph, value: Int, typeEdge: Boolean): Unit = {
    blocks.getBlocks.get(value).forEach(unit => {
      if (sourceStmt==null){
        addNodeEP(method, unit)
      }else{
        addCDEdgeFromIf(sourceStmt, unit, method, typeEdge)
      }
    })
  }

  def addEPEdges(method: SootMethod, block: Block): Unit = {
    block.forEach(unit => {
      addNodeEP(method, unit)
    })
  }

  def addNodeEP(method: SootMethod, unit: soot.Unit): Boolean = {
    val source = createEntryPointNode(method)
    val target = createNode(method, unit)
    addEdgeControlDependence(source, target, true)
  }

  def addCDEdgeFromIf(sourceStmt: StmtNode, targetStmt: soot.Unit, method: SootMethod, typeEdge: Boolean): Unit ={
    val target = createNode(method, targetStmt)
    if (!targetStmt.isInstanceOf[GotoStmt]){
      addEdgeControlDependence(sourceStmt, target, typeEdge)
    }
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
