package br.ufpe.cin.soot.cda

import br.ufpe.cin.soot.graph.NodeType

trait SourceSinkDef {
  this : CDA =>
  def analyze(unit: soot.Unit) : NodeType
}
