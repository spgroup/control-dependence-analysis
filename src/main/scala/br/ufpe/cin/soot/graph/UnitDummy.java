package br.ufpe.cin.soot.graph;

import soot.Unit;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.baf.Baf;
import soot.jimple.*;
import soot.jimple.internal.AbstractOpStmt;
import soot.util.Switch;

import java.util.List;

public class UnitDummy extends AbstractOpStmt implements ExitMonitorStmt {
     public UnitDummy(Value op) {
          this(Jimple.v(). newRValueBox(op));
     }

     protected UnitDummy(ValueBox opBox) {
          super(opBox);
     }

     public Object clone() {
          return new UnitDummy(Jimple.cloneIfNecessary(getOp()));
     }

     public String toString() {
          return Jimple.NULL;
     }

     public void toString(UnitPrinter up) {
          up.literal(Jimple.NULL);
          up.literal("UnitDummy");
     }

     public void apply(Switch sw) {
          ((StmtSwitch) sw).caseExitMonitorStmt(this);
     }

     public void convertToBaf(JimpleToBafContext context, List<Unit> out) {
          ((ConvertToBaf) (getOp())).convertToBaf(context, out);

          Unit u = Baf.v().newExitMonitorInst();
          u.addAllTagsOf(this);
          out.add(u);
     }

     public boolean fallsThrough() {
          return true;
     }

     public boolean branches() {
          return false;
     }

}