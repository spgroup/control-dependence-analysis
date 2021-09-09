package br.ufpe.cin.soot.graph;

import soot.*;
import soot.baf.*;
import soot.jimple.*;


/**
 * <p>
 * Represents a control flow graph for a {@link Body} instance where the nodes are {@link Unit} instances, and where control
 * flow associated with exceptions is taken into account.
 * </p>
 *
 * <p>
 * To describe precisely the circumstances under which exceptional edges are added to the graph, we need to distinguish the
 * exceptions thrown explicitly by a <code>throw</code> instruction from the exceptions which are thrown implicitly by the VM
 * to signal an error it encounters in the course of executing an instruction, which need not be a <code>throw</code>.
 * </p>
 *
 * <p>
 * For every {@link ThrowInst} or {@link ThrowStmt} <code>Unit</code> which may explicitly throw an exception that would be
 * caught by a {@link Trap} in the <code>Body</code>, there will be an edge from the <code>throw</code> <code>Unit</code> to
 * the <code>Trap</code> handler's first <code>Unit</code>.
 * </p>
 *
 * <p>
 * For every <code>Unit</code> which may implicitly throw an exception that could be caught by a <code>Trap</code> in the
 * <code>Body</code>, there will be an edge from each of the excepting <code>Unit</code>'s predecessors to the
 * <code>Trap</code> handler's first <code>Unit</code> (since any of those predecessors may have been the last
 * <code>Unit</code> to complete execution before the handler starts execution). If the excepting <code>Unit</code> might
 * have the side effect of changing some field, then there will definitely be an edge from the excepting <code>Unit</code>
 * itself to its handlers, since the side effect might occur before the exception is raised. If the excepting
 * <code>Unit</code> has no side effects, then parameters passed to the <code>ExceptionalUnitGraph</code> constructor
 * determine whether or not there is an edge from the excepting <code>Unit</code> itself to the handler <code>Unit</code>.
 * </p>
 */

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1999 Patrick Lam
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.Collections;
import java.util.List;

import soot.Local;
import soot.Scene;
import soot.Type;
import soot.Unit;
import soot.UnitPrinter;
import soot.ValueBox;
import soot.baf.Baf;
import soot.jimple.ConvertToBaf;
import soot.jimple.JimpleToBafContext;
import soot.jimple.JimpleValueSwitch;
import soot.util.Numberer;
import soot.util.Switch;

public class LocalDummy implements Local, ConvertToBaf {
     protected String name;
     Type type;

     public LocalDummy(String name, Type type) {
          setName(name);
          setType(type);
          Numberer<Local> numberer = Scene.v().getLocalNumberer();
          if (numberer != null) {
               numberer.add(this);
          }
     }

     /** Returns true if the given object is structurally equal to this one. */
     @Override
     public boolean equivTo(Object o) {
          return this.equals(o);
     }

     /**
      * Returns a hash code for this object, consistent with structural equality.
      */
     @Override
     public int equivHashCode() {
          final int prime = 31;
          int result = 1;
          result = prime * result + ((name == null) ? 0 : name.hashCode());
          result = prime * result + ((type == null) ? 0 : type.hashCode());
          return result;
     }

     /** Returns a clone of the current JimpleLocal. */
     @Override
     public Object clone() {
          // do not intern the name again
          LocalDummy local = new LocalDummy(null, type);
          local.name = name;
          return local;
     }

     /** Returns the name of this object. */
     @Override
     public String getName() {
          return name;
     }

     /** Sets the name of this object as given. */
     @Override
     public void setName(String name) {
          this.name = (name == null) ? null : name.intern();
     }

     /** Returns the type of this local. */
     @Override
     public Type getType() {
          return type;
     }

     /** Sets the type of this local. */
     @Override
     public void setType(Type t) {
          this.type = t;
     }

     @Override
     public String toString() {
          return getName();
     }

     @Override
     public void toString(UnitPrinter up) {
          up.local(this);
     }

     @Override
     public final List<ValueBox> getUseBoxes() {
          return Collections.emptyList();
     }

     @Override
     public void apply(Switch sw) {
          ((JimpleValueSwitch) sw).caseLocal(this);
     }

     @Override
     public void convertToBaf(JimpleToBafContext context, List<Unit> out) {
          Unit u = Baf.v().newLoadInst(getType(), context.getBafLocalOfJimpleLocal(this));
          u.addAllTagsOf(context.getCurrentUnit());
          out.add(u);
     }

     @Override
     public final int getNumber() {
          return number;
     }

     @Override
     public void setNumber(int number) {
          this.number = number;
     }

     private volatile int number = 0;
}