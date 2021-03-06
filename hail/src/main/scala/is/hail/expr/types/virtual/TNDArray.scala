package is.hail.expr.types.virtual

import is.hail.annotations.ExtendedOrdering
import is.hail.expr.types.physical.PNDArray
import org.apache.spark.sql.Row

import scala.reflect.{ClassTag, classTag}

final case class TNDArray(elementType: Type, override val required: Boolean = false) extends Type {
  lazy val physicalType: PNDArray = PNDArray(elementType.physicalType, required)

  override def pyString(sb: StringBuilder): Unit = {
    sb.append("ndarray<")
    elementType.pyString(sb)
    sb.append('>')
  }
  
  def _toPretty = s"NDArray[$elementType]"

  override def scalaClassTag: ClassTag[Row] = classTag[Row]

  def _typeCheck(a: Any): Boolean = throw new UnsupportedOperationException

  val ordering: ExtendedOrdering = null
}
