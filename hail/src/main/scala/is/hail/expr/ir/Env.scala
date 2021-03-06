package is.hail.expr.ir

object Env {
  type K = String
  def empty[V]: Env[V] = new Env()
  def apply[V](bindings: (String, V)*): Env[V] = fromSeq(bindings)
  def fromSeq[V](bindings: Iterable[(String, V)]): Env[V] = empty[V].bindIterable(bindings)
}


object BindingEnv {
  def empty[T]: BindingEnv[T] = BindingEnv(Env.empty[T], None, None)
}

case class BindingEnv[V](
  eval: Env[V],
  agg: Option[Env[V]] = None,
  scan: Option[Env[V]] = None
) {
  def promoteAgg: BindingEnv[V] = {
    assert(agg.isDefined)
    BindingEnv(agg.get)
  }

  def promoteScan: BindingEnv[V] = {
    assert(scan.isDefined)
    BindingEnv(scan.get)
  }

  def bindEval(name: String, v: V): BindingEnv[V] =
    copy(eval = eval.bind(name, v))

  def bindEval(bindings: (String, V)*): BindingEnv[V] =
    copy(eval = eval.bindIterable(bindings))

  def bindAgg(name: String, v: V): BindingEnv[V] =
    copy(agg = Some(agg.get.bind(name, v)))

  def bindAgg(bindings: (String, V)*): BindingEnv[V] =
    copy(agg = Some(agg.get.bindIterable(bindings)))

  def bindScan(name: String, v: V): BindingEnv[V] =
    copy(scan = Some(scan.get.bind(name, v)))

  def bindScan(bindings: (String, V)*): BindingEnv[V] =
    copy(scan = Some(scan.get.bindIterable(bindings)))
}

class Env[V] private(val m: Map[Env.K, V]) {
  def this() {
    this(Map())
  }

  def isEmpty: Boolean = m.isEmpty

  def lookup(name: String): V =
    m.get(name)
      .getOrElse(throw new RuntimeException(s"Cannot find $name in $m"))

  def lookupOption(name: String): Option[V] = m.get(name)

  def delete(name: String): Env[V] = new Env(m - name)

  def lookup(r: Ref): V =
    lookup(r.name)

  def bind(name: String, v: V): Env[V] =
    new Env(m + (name -> v))

  def bind(bindings: (String, V)*): Env[V] = bindIterable(bindings)

  def bindIterable(bindings: Iterable[(String, V)]): Env[V] = if (bindings.isEmpty) this else new Env(m ++ bindings)

  def freshName(prefix: String): String = {
    var i = 0
    var name = prefix
    while (m.keySet.contains(name)) {
      name = prefix + i
      i += 1
    }
    name
  }

  def freshNames(prefixes: String*): Array[String] =
    (prefixes map freshName).toArray

  def bindFresh(prefix: String, v: V): (String, Env[V]) = {
    val name = freshName(prefix)
    (name, new Env(m + (name -> v)))
  }

  def bindFresh(bindings: (String, V)*): (Array[String], Env[V]) = {
    val names = new Array[String](bindings.length)
    var i = 0
    var e = this
    while (i < bindings.length) {
      val (prefix, v) = bindings(i)
      val (name, e2) = e.bindFresh(prefix, v)
      names(i) = name
      e = e2
      i += 1
    }
    (names, e)
  }

  def mapValues[U](f: (V) => U): Env[U] =
    new Env(m.mapValues(f))

  override def toString: String = m.map { case (k,v) => s"$k -> $v" }.mkString("(", ",", ")")
}
