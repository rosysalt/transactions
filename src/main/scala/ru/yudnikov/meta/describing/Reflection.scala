package ru.yudnikov.meta.describing

import java.util.UUID

import ru.yudnikov.core.modeling.{Model, Manager}
import ru.yudnikov.core.storing.Storable

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

/**
  * Created by Don on 29.07.2017.
  */
object Reflection {
  
  val scalaToJava: Map[Class[_], Class[_]] = Map(
    Class.forName("scala.Boolean") -> Class.forName("java.lang.Boolean"),
    Class.forName("scala.Char") -> Class.forName("java.lang.Character"),
    Class.forName("scala.Short") -> Class.forName("java.lang.Short"),
    Class.forName("scala.Int") -> Class.forName("java.lang.Integer"),
    Class.forName("scala.Long") -> Class.forName("java.lang.Long"),
    Class.forName("scala.Double") -> Class.forName("java.lang.Double"),
    Class.forName("scala.Float") -> Class.forName("java.lang.Float")
    //Class.forName("scala.math.BigDecimal") -> Class.forName("java.math.BigDecimal")
  )
  
  
  def runtimeMirror(aClass: Class[_]): RuntimeMirror = universe.runtimeMirror(aClass.getClassLoader)
  
  
  def typeByClass(aClass: Class[_]): Type = runtimeMirror(aClass).classSymbol(aClass).toType
  
  def classByTag[T: ClassTag]: Class[T] = implicitly[reflect.ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
  
  def classByType(aType: Type): Class[_] = Class.forName(aType.typeSymbol.fullName)
  
  
  def getTerms(aType: Type, names: List[String]): Map[String, TermSymbol] = {
    aType.decls.collect {
      case ts: TermSymbol if ts.isPublic & !ts.isConstructor && (names == Nil || names.contains(ts.name.toString)) =>
        ts.name.toString -> ts
    }.toMap
  }
  
  def getTerms(aClass: Class[_], names: List[String]): Map[String, TermSymbol] = getTerms(typeByClass(aClass), names)
  
  def getTerms(aClass: Class[_]): Map[String, TermSymbol] = getTerms(aClass, Nil)
  
  
  def getPrimaryConstructor(aType: Type): MethodSymbol = {
    aType.decl(termNames.CONSTRUCTOR).asTerm.alternatives.collectFirst {
      case ms: MethodSymbol if ms.isPrimaryConstructor => ms
    }.get
  }
  
  def getPrimaryConstructor(aClass: Class[_]): MethodSymbol = getPrimaryConstructor(typeByClass(aClass))
  
  
  def getArgs(primaryConstructor: MethodSymbol, names: List[String]): List[String] = {
    val args = primaryConstructor.paramLists.flatten.collect {
      case ts: TermSymbol if ts.isPublic => ts.name.toString
    }
    if (names.nonEmpty)
      args.intersect(names)
    else
      args
  }
  
  def getArgs(primaryConstructor: MethodSymbol): List[String] = getArgs(primaryConstructor, Nil)
  
  
  def companionObject(aClass: Class[_]): Any = {
    val rm = runtimeMirror(aClass)
    val classMirror = rm.reflectClass(rm.classSymbol(aClass))
    rm.reflectModule(classMirror.symbol.companion.asModule).instance
  }
  
  def modelManager[T <: Model](aClass: Class[T]): Manager[T] =
    companionObject(aClass).asInstanceOf[Manager[T]]
  
  
  def unapplyStorableInstance(storable: Storable[_]): ListMap[String, (Type, Any)] = {
    val aClass = storable.getClass
    val instanceMirror = runtimeMirror(aClass).reflect(storable)
    getClassTerms(aClass).map(tuple => tuple._1 -> (tuple._2.typeSignature.finalResultType -> instanceMirror.reflectField(tuple._2).get))
  }
  
  def unapplyCaseInstance(instance: Any): ListMap[String, (Type, Any)] = {
    val aClass = instance.getClass
    val instanceMirror = runtimeMirror(aClass).reflect(instance)
    getClassTerms(aClass).map(tuple => tuple._1 -> (tuple._2.typeSignature.finalResultType -> instanceMirror.reflectField(tuple._2).get))
  }
  
  def unapplyStorableClass(aClass: Class[_ <: Storable[_]]): ListMap[String, Type] =
    getClassTerms(aClass).map(tuple => tuple._1 -> tuple._2.typeSignature.finalResultType)
  
  def unapplyCaseClass(aClass: Class[_]): ListMap[String, Type] =
    getClassTerms(aClass).map(tuple => tuple._1 -> tuple._2.typeSignature.finalResultType)
  
  def getClassTerms(aClass: Class[_]): ListMap[String, TermSymbol] = {
    val aType = typeByClass(aClass)
    val pc = getPrimaryConstructor(aType)
    val args = getArgs(pc)
    val terms = getTerms(aClass, args)
    try {
      ListMap(args.map(arg => arg -> terms(arg)): _*)
    } catch {
      case e: Exception =>
        null
    }
  }
  
  
  def getStorableIdClass[T <: Storable[_]](aClass: Class[T]): Class[_] = {
    Reflection.classByType(Reflection.getClassTerms(aClass).find(_._1 == "id").get._2.typeSignature)
  }
  
  
  def isCaseClass(aClass: Class[_]): Boolean =
    runtimeMirror(aClass).reflectClass(runtimeMirror(aClass).classSymbol(aClass)).symbol.isCaseClass
  
  def isCaseClass(aType: Type): Boolean =
    aType.typeSymbol.asClass.isCaseClass
  
}
