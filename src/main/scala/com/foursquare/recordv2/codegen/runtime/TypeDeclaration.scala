package com.foursquare.recordv2.codegen.runtime

import com.foursquare.recordv2.Annotations

sealed trait TypeDeclaration {
  def name: String
  def annotations: Annotations
}

object TypeDeclaration {
  def transform(f: String => String): TypeDeclaration => TypeDeclaration = _ match {
    case EnumDecl(name, annotations) => EnumDecl(f(name), annotations)
    case StructDecl(name, annotations) => StructDecl(f(name), annotations)
    case UnionDecl(name, annotations) => UnionDecl(f(name), annotations)
    case ExceptionDecl(name, annotations) => ExceptionDecl(f(name), annotations)
    case TypedefDecl(name, newType, ref, annotations) => TypedefDecl(f(name), newType, ref, annotations)
  }
}

case class EnumDecl(name: String, annotations: Annotations) extends TypeDeclaration
case class StructDecl(name: String, annotations: Annotations) extends TypeDeclaration
case class UnionDecl(name: String, annotations: Annotations) extends TypeDeclaration
case class ExceptionDecl(name: String, annotations: Annotations) extends TypeDeclaration
case class TypedefDecl(
    name: String,
    newType: Boolean,
    ref: TypeReference,
    annotations: Annotations
) extends TypeDeclaration
