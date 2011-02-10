package xt.vc.controller

import java.lang.reflect.Method
import scala.collection.mutable.ArrayBuffer

/**
 * Although the inputs for beforeFilter are Strings, they are converted to
 * java.lang.reflect.Method, thus any type error can be caught early.
 */
trait Filter {
  type ActionName = String

  type OnlyActions   = ArrayBuffer[Method]
  type ExceptActions = ArrayBuffer[Method]

  case class Only  (actionNames: ActionName*)
  case class Except(actionNames: ActionName*)

  type Filter = (Method, OnlyActions, ExceptActions)

  val beforeFilters = ArrayBuffer[Filter]()

  //----------------------------------------------------------------------------

  /** f: method name, the method should be public or protected */
  def beforeFilter(f: String) {
    val m = getClass.getMethod(f)
    val filter = (m, ArrayBuffer[Method](), ArrayBuffer[Method]())
    beforeFilters.append(filter)
  }

  /** f: method name, the method should be public or protected */
  def beforeFilter(f: String, filterOption: Only) {
    val m = getClass.getMethod(f)
    val onlyActions = ArrayBuffer[Method]()
    onlyActions.appendAll(filterOption.actionNames.map(name => getClass.getMethod(name)))
    val filter = (m, onlyActions, ArrayBuffer[Method]())
    beforeFilters.append(filter)
  }

  /** f: method name, the method should be public or protected */
  def beforeFilter(f: String, filterOption: Except) {
    val m = getClass.getMethod(f)
    val exceptActions = ArrayBuffer[Method]()
    exceptActions.appendAll(filterOption.actionNames.map(name => getClass.getMethod(name)))
    val filter = (m, ArrayBuffer[Method](), exceptActions)
    beforeFilters.append(filter)
  }

  def skipBeforeFilter(f: String, filterOption: FilterOption) {
    beforeFilters.find(tuple => tuple._1 == filterName) match {
      case None => throw new Exception("Filter named " + filterName + " not found")

      case Some(filter) =>
        val onlyActionNames   = filter._3
        val exceptActionNames = filter._4

        filterOption match {
          case Only(actionNames) =>
            onlyActionNames.remove(0, onlyActionNames.size)
            exceptActionNames

          case Except(actionNames) =>
        }
    }
  }

  /** Called by Dispatcher */
  def callBeforeFilters(action: Method): Boolean = {
    beforeFilters.forall(filter => {
      val onlyActions = filter._2
      if (onlyActions.isEmpty) {
        val exceptActions = filter._3
        if (!exceptActions.contains(action)) {
          val method = filter._1
          method.invoke(this).asInstanceOf[Boolean]
        } else true
      } else {
        if (onlyActions.contains(action)) {
          val method = filter._1
          method.invoke(this).asInstanceOf[Boolean]
        } else true
      }
    })
  }
}
