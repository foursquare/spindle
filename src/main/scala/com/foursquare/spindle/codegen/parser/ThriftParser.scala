// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.parser

import com.twitter.thrift.descriptors
import java.io.File
import java.nio.charset.Charset
import org.apache.commons.io.FileUtils
import org.parboiled.errors.ErrorUtils
import org.parboiled.matchers.{Matcher, ProxyMatcher}
import org.parboiled.scala.{ANY, EOI, MemoMismatches, Parser, ParseRunner, ReportingParseRunner, Rule, Rule0, Rule1,
    Rule2, RuleOption, SkipNode, SuppressNode, SuppressSubnodes, TracingParseRunner}
import scala.collection.mutable
import scala.util.DynamicVariable

class ThriftParser extends Parser {
  lazy val Letter = rule("Letter")("a" - "z" | "A" - "Z")
  lazy val Sign: Rule0 = rule("Sign") { optional(anyOf("+-")) }
  lazy val Digit: Rule0 = rule("Digit") { "0" - "9" }
  lazy val Digits: Rule0 = rule("Digits") { oneOrMore(Digit) }
  lazy val HexDigit: Rule0 = rule("HexDigit") { Digit | "a" - "f" | "A" - "F" }

  lazy val HexConstant: Rule1[String] = rule("HexConstant") { "0x" ~ oneOrMore(HexDigit) } ~> identity
  lazy val IntConstant: Rule1[String] = rule("IntConstant") { Sign ~ Digits } ~> identity
  lazy val IdConstant: Rule1[Int] = rule("IdConstant") (
    /* HexConstant must come first to avoid ambiguities in the grammar */
    HexConstant ~~> (hexString => Integer.parseInt(hexString.stripPrefix("0x"), 16)) |
    IntConstant ~~> (intString => Integer.parseInt(intString, 10))
  )

  lazy val Frac: Rule0 = rule("Frac") { "." ~ Digits }
  lazy val Exp: Rule0 = rule("Exp") { ignoreCase("e") ~ Sign ~ Digits }
  lazy val DoubleConstant: Rule1[String] = rule("DoubleConstant") (
    Sign ~ zeroOrMore(Digit) ~ Frac ~ optional(Exp) |
    Sign ~ zeroOrMore(Digit) ~ optional(Frac) ~ Exp
  ) ~> identity

  lazy val Identifier: Rule1[String] = rule("Identifier") {
    (Letter | "_") ~ zeroOrMore("." | Letter | "_" | Digit)
  } ~> identity

  lazy val StIdentifier: Rule1[String] = rule("StIdentifier") {
    (Letter | "_") ~ zeroOrMore("." | Letter | "_" | Digit | "-")
  } ~> identity

  lazy val MultiComment: Rule0 = rule("MultiComment") { "/*" ~ zeroOrMore(!"*/" ~ ANY) ~ "*/" }
  lazy val EOLComment: Rule0 = rule("EOLComment") { "//" ~ zeroOrMore(!"\n" ~ ANY) }
  lazy val UnixComment: Rule0 = rule("UnixComment") { "#" ~ zeroOrMore(!"\n" ~ ANY) }
  lazy val Comment: Rule0 = rule("Comment")(MultiComment | EOLComment | UnixComment)

  lazy val Spaces: Rule0 = rule("Spaces") { oneOrMore(anyOf(" \t")) }
  lazy val Newline: Rule0 = rule("Newline") { ("\n" | "\r\n") }
  lazy val EOL: Rule0 = rule("EOL") { zeroOrMore(Spaces | Comment) ~ Newline }
  lazy val EOC: Rule0 = rule("EOC") { zeroOrMore(Spaces | Comment) ~ anyOf(",;") }
  lazy val Deadspace: Rule0 = rule("Deadspace") { zeroOrMore(Spaces | Newline | Comment) }
  lazy val Lines: Rule0 = rule("Lines") { EOL ~ Deadspace }

  lazy val LiteralChoice: Rule2[String, String] = rule("LiteralChoice") {
    ("'" ~ zeroOrMore(!"'" ~ ANY) ~> identity ~ "'") |
    ("\"" ~ zeroOrMore(!"\"" ~ ANY) ~> identity ~ "\"")
  } ~> identity
  lazy val Literal: Rule1[String] = rule("Literal") { LiteralChoice ~~> ((text, source) => text) }
  lazy val LiteralConstant: Rule1[String] = rule("LiteralConstant") { LiteralChoice ~~> ((text, source) => source) }

  lazy val BlockSeparator: Rule0 = rule("BlockSeparator") { zeroOrMore(Spaces | Comment) ~ ((anyOf(",;") ~ Newline) | Newline | anyOf(",;")) ~ Deadspace }
  lazy val ArgSeparator: Rule0 = rule("ArgSeparator") { EOC ~ Deadspace }
  def block[T](rule: Rule1[T]): Rule1[List[T]] = {
    Deadspace ~ "{" ~ Deadspace ~ zeroOrMore(rule, BlockSeparator) ~ optional(BlockSeparator) ~ Deadspace ~ "}"
  }
  def argList[T](rule: Rule1[T], allowNewlineSeparator: Boolean = false): Rule1[List[T]] = {
    val separator = if (allowNewlineSeparator) BlockSeparator else ArgSeparator
    Deadspace ~ "(" ~ Deadspace ~ zeroOrMore(rule, separator) ~ optional(separator) ~ Deadspace ~ ")"
  }

  lazy val Program: Rule1[descriptors.Program] = rule("Program")(
    (Deadspace ~ zeroOrMore(Header ~ optional(EOC), Lines) ~ Deadspace ~ zeroOrMore(Definition ~ optional(EOC), Lines) ~ Deadspace ~ EOI) ~~> ((headers, definitions) => {
      val result = descriptors.Program.createRawRecord
      headers.foreach(header => result.merge(header))
      definitions.foreach(definition => result.merge(definition))
      val aliasToTypeId = Map() ++ result.typedefs.map(td => td.typeAlias -> td.typeId)
      val typeRegistry = descriptors.TypeRegistry(idToTypeBuilder.result, aliasToTypeId)
      result.typeRegistry_=(typeRegistry)
      if (!result.namespacesIsSet) result.namespaces_=(Nil)
      if (!result.includesIsSet) result.includes_=(Nil)
      if (!result.constantsIsSet) result.constants_=(Nil)
      if (!result.enumsIsSet) result.enums_=(Nil)
      if (!result.typedefsIsSet) result.typedefs_=(Nil)
      if (!result.structsIsSet) result.structs_=(Nil)
      if (!result.unionsIsSet) result.unions_=(Nil)
      if (!result.exceptionsIsSet) result.exceptions_=(Nil)
      if (!result.servicesIsSet) result.services_=(Nil)
      result
    })
  )

  lazy val Header: Rule1[descriptors.Program] = rule("Header")(
    Include ~~> (include => descriptors.Program.newBuilder.includes(List(include)).typeRegistry(null).result()) |
    Namespace ~~> (namespace => descriptors.Program.newBuilder.namespaces(List(namespace)).typeRegistry(null).result()))

  lazy val Include: Rule1[descriptors.Include] = rule("Include")(
    ("include" ~ Spaces ~ Literal) ~~> (include => descriptors.Include(include)) |
    ("cpp_include" ~ Spaces ~ Literal) ~~> (include => descriptors.Include(include)))

  lazy val Namespace: Rule1[descriptors.Namespace] = rule("Namespace")(
    ("namespace" ~ Spaces ~ Identifier ~ Spaces ~ Identifier) ~~> ((lang, path) => descriptors.Namespace(lang, path)) |
    ("namespace" ~ Spaces ~ "*" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("*", path)) |
    ("cpp_namespace" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("cpp", path)) |
    ("php_namespace" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("php", path)) |
    ("py_module" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("py", path)) |
    ("perl_package" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("perl", path)) |
    ("ruby_namespace" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("ruby", path)) |
    ("smalltalk_category" ~ Spaces ~ StIdentifier) ~~> (path => descriptors.Namespace("smalltalk_category", path)) |
    ("smalltalk_prefix" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("smalltalk_prefix", path)) |
    ("java_package" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("java", path)) |
    ("scala_package" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("scala", path)) | 
    ("cocoa_package" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("cocoa", path)) |
    ("xsd_namespace" ~ Spaces ~ Literal) ~~> (path => descriptors.Namespace("xsd", path)) |
    ("csharp_namespace" ~ Spaces ~ Identifier) ~~> (path => descriptors.Namespace("csharp", path))
  )

  lazy val Definition: Rule1[descriptors.Program] = rule("Definition")(
    TypeDefinition |
    Const ~~> (const => descriptors.Program.newBuilder.constants(List(const)).typeRegistry(null).result()) |
    Service ~~> (service => descriptors.Program.newBuilder.services(List(service)).typeRegistry(null).result())
  )

  lazy val TypeDefinition: Rule1[descriptors.Program] = rule("TypeDefinition")(
    Struct ~~> (struct => descriptors.Program.newBuilder.structs(List(struct)).typeRegistry(null).result()) |
    Typedef ~~> (typedef => descriptors.Program.newBuilder.typedefs(List(typedef)).typeRegistry(null).result()) |
    Enum ~~> (enum => descriptors.Program.newBuilder.enums(List(enum)).typeRegistry(null).result()) |
    // Senums are parsed but not represented in the AST. Return an empty Program.
    Senum ~~> (senum => descriptors.Program.newBuilder.typeRegistry(null).result()) |
    Union ~~> (union => descriptors.Program.newBuilder.unions(List(union)).typeRegistry(null).result()) |
    Xception ~~> (exception => descriptors.Program.newBuilder.exceptions(List(exception)).typeRegistry(null).result())
  )

  lazy val Typedef: Rule1[descriptors.Typedef] = rule("Typedef")(
    ("typedef" ~ Spaces ~ FieldType ~ optional(Spaces) ~ Identifier ~ optional(TypeAnnotations)) ~~> ((tpe, name, annots) => {
      (descriptors.Typedef.newBuilder
        .typeId(tpe)
        .typeAlias(name)
        .__annotations(annots)
        .result())
    })
  )

  lazy val EnumDefExplicitId: Rule1[descriptors.EnumElement] = rule("EnumDefExplicitId")(
    (Identifier ~ optional(Spaces) ~ "=" ~ optional(Spaces) ~ IdConstant ~ optional(TypeAnnotations)) ~~> {
      (name, id, annots) => {
        (descriptors.EnumElement
          .newBuilder
          .name(name)
          .value(id)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val EnumDefImplicitId: Rule1[descriptors.EnumElement] = rule("EnumDefImplicitId")(
    (Identifier ~ optional(TypeAnnotations)) ~~> {
      (name, annots) => {
        (descriptors.EnumElement
          .newBuilder
          .name(name)
          .value(-1)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val EnumDef: Rule1[descriptors.EnumElement] = rule("EnumDef")(EnumDefExplicitId | EnumDefImplicitId)

  lazy val Enum: Rule1[descriptors.Enum] = rule("Enum") {
    ("enum" ~ Spaces ~ Identifier ~ block(EnumDef) ~ optional(TypeAnnotations)) ~~> {
      (name, elems, annots) => {
        var _nextId = 0
        val numberedElems =
          for (elem <- elems) yield {
            val id = if (elem.value == -1) _nextId else elem.value
            _nextId = id + 1
            elem.copy(value = id)
          }

        val nums = numberedElems.map(_.value)
        assert(nums.size == nums.distinct.size)

        (descriptors.Enum
          .newBuilder
          .name(name)
          .elements(numberedElems)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  }

  lazy val Senum: Rule1[Unit] = rule("Senum") { 
    ("senum" ~ Spaces ~ Identifier ~ block(SenumDef) ~ optional(TypeAnnotations)) ~~> {
      (name, elems, annots) => {
        // Senums are parsed but not represented in the AST. Return Unit.
        ()
      }
    }
  }

  lazy val SenumDef: Rule1[String] = rule("SenumDef") { Literal }

  lazy val Const: Rule1[descriptors.Const] = rule("Const")(
    ("const" ~ Spaces ~ FieldType ~ optional(Spaces) ~ Identifier ~ optional(Spaces) ~ "=" ~ optional(Spaces) ~ ConstValue) ~~> { (tpe, name, value) =>
      descriptors.Const(tpe, name, value)
    }
  )

  lazy val ConstValue: Rule1[String] = rule("ConstValue")(
    DoubleConstant |
    HexConstant |
    IntConstant |
    LiteralConstant |
    Identifier |
    ConstList |
    ConstMap)

  lazy val ConstList: Rule1[String] = rule("ConstList") {
    "[" ~ optional(Deadspace) ~ zeroOrMore(ConstValue, optional(EOC ~ Deadspace)) ~ optional(Deadspace) ~ "]"
  } ~> identity ~~> ((_, s) => s)

  lazy val ConstMap: Rule1[String] = rule("ConstMap") {
    "{" ~ optional(Deadspace) ~ zeroOrMore(ConstValuePair, optional(EOC ~ Deadspace)) ~ optional(Deadspace) ~ "}"
  } ~> identity ~~> ((_, s) => s)

  lazy val ConstValuePair: Rule1[String] = rule("ConstValuePair") {
    optional(Spaces) ~ ConstValue ~ optional(Spaces) ~ ":" ~ optional(Spaces) ~ ConstValue
  } ~> identity ~~> ((_, _, s) => s)

  lazy val Struct: Rule1[descriptors.Struct] = rule("Struct")(
    "struct" ~ Spaces ~ Identifier ~ optional(Spaces) ~ optional("xsdAll") ~ block(Field) ~ optional(TypeAnnotations) ~~> {
      (name, fields, annots) => {
        (descriptors.Struct
          .newBuilder
          .name(name)
          .__fields(fields)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val Union: Rule1[descriptors.Union] = rule("Union")(
    "union" ~ Spaces ~ Identifier ~ optional(Spaces) ~ optional("xsdAll") ~ block(Field) ~ optional(TypeAnnotations) ~~> {
      (name, fields, annots) => {
        (descriptors.Union
          .newBuilder
          .name(name)
          .__fields(fields)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val Xception: Rule1[descriptors.Exception] = rule("Xception")(
    "exception" ~ Spaces ~ Identifier ~ block(Field) ~ optional(TypeAnnotations) ~~> {
      (name, fields, annots) => {
        (descriptors.Exception
          .newBuilder
          .name(name)
          .__fields(fields)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val Service: Rule1[descriptors.Service] = rule("Service")(
    "service" ~ Spaces ~ Identifier ~ optional(Extends) ~ block(Function) ~ optional(TypeAnnotations) ~~> {
      (name, extendz, functions, annots) => {
        (descriptors.Service
          .newBuilder
          .name(name)
          .extendz(extendz)
          .functions(functions)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val Extends: Rule1[String] = rule("Extends") { Deadspace ~ "extends" ~ Spaces ~ Identifier }

  lazy val Oneway: Rule1[Boolean] = rule("Oneway") { optional("oneway" ~ Spaces) ~> (_.startsWith("oneway")) }

  lazy val Function: Rule1[descriptors.Function] = rule("Function")(
    (Oneway ~ FunctionType ~ Spaces ~ Identifier ~ argList(Field) ~ optional(Throws) ~ optional(TypeAnnotations)) ~~> {
      (oneway, tpe, name, argz, throwz, annots) => {
        (descriptors.Function
          .newBuilder
          .name(name)
          .returnTypeId(tpe)
          .oneWay(oneway)
          .argz(argz)
          .throwz(throwz.getOrElse(Nil))
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val Throws: Rule1[List[descriptors.Field]] = rule("Throws") { Deadspace ~ "throws" ~ argList(Field) }

  lazy val Field: Rule1[descriptors.Field] = rule("Field")(
    FieldIdentifier ~ optional(Spaces) ~ optional(FieldRequiredness ~ Spaces) ~ FieldType ~ optional(Spaces) ~
    Identifier ~ optional(Spaces) ~ optional(FieldValue) ~ optional(Spaces) ~ optional("xsdOptional") ~
    optional(Spaces) ~ optional("xsdNillable") ~ optional(Spaces) ~ optional(XsdAttributes) ~
    optional(TypeAnnotations) ~~> {
      (id, reqd, tpe, name, value, _, annots) => {
        (descriptors.Field
          .newBuilder
          .identifier(id.toShort)
          .name(name)
          .typeId(tpe)
          .requiredness(reqd)
          .defaultValue(value)
          .__annotations(annots.getOrElse(Nil))
          .result())
      }
    }
  )

  lazy val XsdAttributes: Rule1[List[descriptors.Field]] = rule("XsdAttributes") { "xsd_attributes" ~ block(Field) }

  lazy val FieldIdentifier: Rule1[Int] = rule("FieldIdentifier") { IdConstant ~ optional(Spaces) ~ ":" }

  lazy val FieldRequiredness: Rule1[descriptors.Requiredness] = rule("FieldRequiredness")(
    "required" ~> (_ => descriptors.Requiredness.REQUIRED) |
    "optional" ~> (_ => descriptors.Requiredness.OPTIONAL)
  )

  lazy val FieldValue: Rule1[String] = rule("FieldValue") { "=" ~ optional(Spaces) ~ ConstValue }

  lazy val FunctionType: Rule1[Option[String]] = rule("FunctionType")(
    "void" ~> (_ => None) |
    FieldType ~~> (tpe => Some(tpe))
  )

  var _nextTypeId = 0

  val idToTypeBuilder = Map.newBuilder[String, descriptors.Type]
  val aliasToTypeIdBuilder = Map.newBuilder[String, String]

  def nextTypeId = {
    val id = _nextTypeId
    _nextTypeId += 1
    "T" + id
  }

  def addType(simpleType: descriptors.SimpleType): String = {
    val id = nextTypeId
    val tpe = descriptors.Type(id, simpleType)
    idToTypeBuilder += ((id, tpe))
    id
  }

  lazy val FieldType: Rule1[String] = rule("FieldType") {
    BaseType ~~> (baseType => {
      val simpleType = descriptors.SimpleType.newBuilder.baseType(baseType).result()
      addType(simpleType)
    }) |
    ContainerType ~~> (containerType => {
      val simpleType = descriptors.SimpleType.newBuilder.containerType(containerType).result()
      addType(simpleType)
    }) |
    Identifier ~~> (identifier => {
      val typeref = descriptors.Typeref(identifier)
      val simpleType = descriptors.SimpleType.newBuilder.typeref(typeref).result()
      addType(simpleType)
    })
  }

  lazy val BaseType: Rule1[descriptors.BaseType] = rule("BaseType") {
    SimpleBaseType ~ optional(TypeAnnotations)
  } ~~> ((sbt, annots) => descriptors.BaseType(sbt, annots.getOrElse(Nil)))

  lazy val SimpleBaseType: Rule1[descriptors.SimpleBaseType] = rule("SimpleBaseType")(
    "string" ~ push(descriptors.SimpleBaseType.STRING) |
    "binary" ~ push(descriptors.SimpleBaseType.BINARY) |
    "slist" ~> (_ => throw new RuntimeException("slist not a supported type")) |
    "bool" ~ push(descriptors.SimpleBaseType.BOOL) |
    "byte" ~ push(descriptors.SimpleBaseType.BYTE) |
    "i16" ~ push(descriptors.SimpleBaseType.I16) |
    "i32" ~ push(descriptors.SimpleBaseType.I32) |
    "i64" ~ push(descriptors.SimpleBaseType.I64) |
    "double" ~ push(descriptors.SimpleBaseType.DOUBLE)
  )

  lazy val ContainerType: Rule1[descriptors.ContainerType] = rule("ContainerType") {
    SimpleContainerType ~ optional(TypeAnnotations)
  } ~~> ((simpleContainerType, annots) => descriptors.ContainerType(simpleContainerType, annots.getOrElse(Nil)))

  lazy val SimpleContainerType: Rule1[descriptors.SimpleContainerType] = rule("SimpleContainerType") {
    MapType ~~> (mapType => descriptors.SimpleContainerType.newBuilder.mapType(mapType).result()) |
    SetType ~~> (setType => descriptors.SimpleContainerType.newBuilder.setType(setType).result()) |
    ListType ~~> (listType => descriptors.SimpleContainerType.newBuilder.listType(listType).result())
  }

  lazy val MapType: Rule1[descriptors.MapType] = rule("MapType") {
    ("map" ~ optional(CppType) ~ optional(Spaces) ~ "<" ~ optional(Spaces) ~ FieldType ~ optional(Spaces) ~
      "," ~ optional(Spaces) ~ FieldType ~ optional(Spaces) ~ ">")
  } ~~> ((_, keyTypeId, valueTypeId) => descriptors.MapType(keyTypeId, valueTypeId))

  lazy val SetType: Rule1[descriptors.SetType] = rule("SetType") {
    ("set" ~ optional(CppType) ~ optional(Spaces) ~ "<" ~ optional(Spaces) ~ FieldType ~ optional(Spaces) ~
      ">")
  } ~~> ((_, typeId) => descriptors.SetType(typeId))

  lazy val ListType: Rule1[descriptors.ListType] = rule("ListType") {
    ("list" ~ "<" ~ optional(Spaces) ~ FieldType ~ optional(Spaces) ~ ">" ~ optional(CppType))
  } ~~> ((typeId, _) => descriptors.ListType(typeId))

  lazy val CppType: Rule1[String] = rule("CppType") { Spaces ~ "cpp_type" ~ Spaces ~ Literal }

  lazy val TypeAnnotations: Rule1[List[descriptors.Annotation]] = rule("TypeAnnotations") {
    argList(TypeAnnotation, allowNewlineSeparator = true)
  }

  lazy val TypeAnnotation: Rule1[descriptors.Annotation] = rule("TypeAnnotation") {
    Identifier ~ optional(Spaces) ~ "=" ~ optional(Spaces) ~ Literal
  } ~~> ((key, value) => descriptors.Annotation(key, value))

  /**
   * The code below is copied from Parboiled's Parser.scala and re-written to avoid getting stack traces.
   * This gives us a ~20x speedup from the standard Parboiled parser setup.
   */
  override def rule[T <: Rule](label: String, options: RuleOption*)(block: => T)(implicit creator: Matcher => T): T = {
    myRule(label, label, options, block, creator)
  }

  private val cache = mutable.Map.empty[String, Rule]
  private val lock = new AnyRef()

  private def myRule[T <: Rule](label: String, key: String, options: Seq[RuleOption], block: => T, creator: Matcher => T): T =
    lock.synchronized {
      cache.get(key) match {
        case Some(rule) => rule.asInstanceOf[T]
        case None => {
          val proxy = new ProxyMatcher
          // protect block from infinite recursion by immediately caching a new Rule of type T wrapping the proxy creator
          cache += key -> creator(proxy)
          var rule = ThriftParser.withCurrentRuleLabel(label) { block.label(label) } // evaluate rule definition block
          if (!buildParseTree || options.contains(SuppressNode)) rule = rule.suppressNode
          if (options.contains(SuppressSubnodes)) rule = rule.suppressSubnodes
          if (options.contains(SkipNode)) rule = rule.skipNode
          if (options.contains(MemoMismatches)) rule = rule.memoMismatches
          proxy.arm(rule.matcher) // arm the proxy in case it is in use
          cache += key -> rule // replace the cache value with the actual rule (overwriting the proxy rule)
          rule
        }
      }
    }
}

class ParserException(val message: String, val file: File) extends RuntimeException(file.getPath + ": " + message)

object ThriftParser {
  private val currentRuleLabel = new DynamicVariable[String](null)
  private val currentActionIndex = new DynamicVariable[Int](0)

  private def withCurrentRuleLabel[A](s: String)(f: => A): A =
    currentRuleLabel.withValue(s) {
      currentActionIndex.withValue(0) {
        f
      }
    }

  def parseProgram(name: String): descriptors.Program = {
    parseProgram(new File(name))
  }

  def parsePrograms(files: Seq[File]): Seq[descriptors.Program] = {
    files.map(file => parseProgram(file))
  }

  def parseProgram(file: File, trace: Boolean = false): descriptors.Program = {
    val parser = new ThriftParser
    val runner: ParseRunner[descriptors.Program] =
      if (trace) TracingParseRunner(parser.Program) else ReportingParseRunner(parser.Program)
    val thrift = FileUtils.readFileToString(file, "UTF-8")
    val runResult = runner.run(thrift)
    val result = runResult.result.getOrElse(throw new ParserException(ErrorUtils.printParseErrors(runResult), file))
    val validator = new ThriftValidator(file)
    validator.validateProgram(result)
    result
  }
}
