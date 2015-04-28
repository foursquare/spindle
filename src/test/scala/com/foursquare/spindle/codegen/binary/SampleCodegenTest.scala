// Copyright 2014 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.codegen.binary

import org.junit.Assert._
import org.junit.Test
import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Arrays

class CodegenSampleTest {
  val SampleFolder = "src/main/resources/sample"
  val OutFolder = "com/twitter/thrift/descriptors"
  val Filenames = Vector("java_thrift_descriptors.java", "thrift_descriptors.scala")
  val Message = "The thrift_descriptor samples didn't match. %s has been overwritten with the expected value."

  def tempFolder(): File = {
    val ret = File.createTempFile("spindle", "")
    ret.delete()
    ret.mkdir()
    ret
  }
  def recursiveDelete(file: File): Boolean = {
    Option(file.listFiles).map(_.toVector).getOrElse(Nil).forall(f => {
      recursiveDelete(f)
    }) && file.delete()
  }

  @Test
  def testSampleMatchesActualCodegen(): Unit = {
    val outDir = tempFolder()
    val workingDir = tempFolder()

    try {
      ThriftCodegen.main(Array(
        "--template", "scala/record.ssp",
        "--java_template", "javagen/record.ssp",
        "--extension", "scala",
        "--namespace_out", outDir.getAbsolutePath,
        "--working_dir", workingDir.getAbsolutePath,
        "src/main/thrift/com/twitter/thrift/descriptors/thrift_descriptors.thrift"
      ))

      val noMatchFiles = Filenames.filterNot(filename => {
        val expected = Files.readAllBytes(Paths.get(outDir.getAbsolutePath, OutFolder, filename))
        val actualPath = Paths.get(SampleFolder, filename)
        val matches = try {
          Arrays.equals(expected, Files.readAllBytes(actualPath))
        } catch {
          case _: Exception => false
        }
        if (!matches) {
          // Be helpful and overwrite the sample with the expected value.
          Files.write(actualPath, expected)
        }
        matches
      })
      assertTrue(
        "The thrift_descriptor samples didn't match. They have been overwritten with the expected values.",
        noMatchFiles.isEmpty
      )
    } finally {
      recursiveDelete(outDir)
      recursiveDelete(workingDir)
    }
  }
}
