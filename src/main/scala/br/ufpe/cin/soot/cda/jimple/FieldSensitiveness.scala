package br.ufpe.cin.soot.cda.jimple

trait FieldSensitiveness {
  def isFieldSensitiveAnalysis(): Boolean
}

trait FieldSenstive extends FieldSensitiveness {
  override def isFieldSensitiveAnalysis(): Boolean = true
}

trait FieldInsenstive extends FieldSensitiveness {
  override def isFieldSensitiveAnalysis(): Boolean = false
}
