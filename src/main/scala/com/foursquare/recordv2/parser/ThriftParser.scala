// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.recordv2.parser

import com.twitter.thrift.descriptors.Program
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileNotFoundException}
import java.lang.{ProcessBuilder => JProcessBuilder}
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport
import scalaj.collection.Imports._

/** Parses a Thrift file and turns it into a Thrift Program descriptor.
  *
  * Calls out to a Python process that does the parsing. Eventually we should
  * reimplement the parser in Java/Scala. An ANTLR grammar exists for Thrift,
  * but the tree walker that turns that into Thrift descriptors only exists for
  * Python at the moment.
  *
  * This could also be more efficient at parsing multiple programs.
  */
object ThriftParser {
  def parseProgram(name: String): Program = {
    parseProgram(new File(name))
  }

  def parseProgram(file: File): Program = {
    parsePrograms(Seq(file)).head
  }

  def parsePrograms(files: Seq[File]): Seq[Program] = {
    for (file <- files if !file.exists)
      throw new FileNotFoundException(file.getAbsolutePath)

    val paths = files.map(_.getAbsolutePath)
    val args = Seq("./build-support/parser.pex") ++ paths

    try {
      val processBuilder = new JProcessBuilder(args.asJava)
      val parser = new ProcessBuilder(processBuilder)
      val baos = new ByteArrayOutputStream
      val exitCode = parser.runWithOutputStream(baos)

      if (exitCode != 0) {
        throw new RuntimeException("Thrift parser returned exit code %d" format exitCode)
      }

      val bytes = baos.toByteArray
      val bais = new ByteArrayInputStream(bytes)
      val transport = new TIOStreamTransport(bais)
      val protocolFactory = new TBinaryProtocol.Factory
      val iprot = protocolFactory.getProtocol(transport)

      val numPrograms = iprot.readI32()
      val builder = Vector.newBuilder[Program]
      builder.sizeHint(numPrograms)

      for (_ <- 0 until numPrograms) {
        val program = Program.createRawRecord
        program.read(iprot)
        builder += program
      }

      builder.result()
    } catch {
      case t: Exception => throw new RuntimeException("Error Parsing " + paths, t)
    }
  }
}
