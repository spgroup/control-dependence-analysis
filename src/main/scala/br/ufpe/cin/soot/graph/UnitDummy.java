package br.ufpe.cin.soot.graph;

import soot.Unit;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import java.util.List;

public class UnitDummy extends AbstractDummyStmt implements soot.jimple.Stmt {
     private String dummyNode;

     public UnitDummy(Value op, String node) {
          this(Jimple.v().newVariableBox(op));
          this.dummyNode = node;
     }

     public String getDummyNode(){
          return this.dummyNode;
     }

     public void setDummyNode(String dummyNode){
          this.dummyNode = dummyNode;
     }

     protected UnitDummy(ValueBox opBox) {
          super(opBox);
     }

     public Object clone() {
          return null;
     }

     public String toString() {
          return this.dummyNode;
     }

     public void toString(UnitPrinter up) {
          up.literal(this.dummyNode);
     }

     public void convertToBaf(JimpleToBafContext context, List<Unit> out) {}

     public boolean fallsThrough() {
          return true;
     }

     public boolean branches() {
          return false;
     }

}