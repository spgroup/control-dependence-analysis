package br.ufpe.cin.soot.graph;

import soot.jimple.internal.AbstractStmt;
import java.util.List;
import java.util.ArrayList;

import soot.Value;
import soot.ValueBox;

@SuppressWarnings("serial")
public abstract class AbstractDummyStmt extends AbstractStmt {

     final ValueBox opBox;

     protected AbstractDummyStmt(ValueBox opBox) {
          this.opBox = opBox;
     }

     final public Value getOp() {
          return opBox.getValue();
     }

     final public void setOp(Value op) {
          opBox.setValue(op);
     }

     final public ValueBox getOpBox() {
          return opBox;
     }

}