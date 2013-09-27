package com.foursquare.spindle.codegen.binary

import com.foursquare.spindle.codegen.runtime.{CodegenException, ScalaProgram}
import java.io.File
import org.junit.Assert._
import org.junit.Test

class ThriftCodegenTest {
  val base = "src/test/thrift/com/foursquare/spindle/parser"

  @Test
  def testParseDuplicateWireName(): Unit = {
    try {
      val (sources, typeDeclarations, enhancedTypes) = ThriftCodegen.inputInfoForCompiler(
        Seq(new File(base + "/parse_duplicate_wire_name.thrift")), Seq.empty)
      val program = ScalaProgram(sources.head, typeDeclarations, enhancedTypes)
      program.structs.foreach(println _)
      fail("Parsing duplicate field wire_names should fail.")
    } catch {
      case e: CodegenException => ()
    }
  }

}
