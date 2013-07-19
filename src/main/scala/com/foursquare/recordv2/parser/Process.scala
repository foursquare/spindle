// Copyright 2011 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.recordv2.parser

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.lang.{ProcessBuilder => JProcessBuilder}

class ProcessBuilder(p: JProcessBuilder) {
  def runWithOutputStream(out: OutputStream): Int = {
    val process = p.start()
    val outThread = Process.spawn(Process.pipeTo(process.getInputStream, out))
    val errThread = Process.spawn(Process.pipeTo(process.getErrorStream, System.err))
    val exitValue = try {
      process.waitFor()
    } finally Thread.interrupted
    List(outThread, errThread).foreach(_.join)
    process.getOutputStream.close()
    process.destroy()
    exitValue
  }

  def runWithOutput(): (Int, String) = {
    val baos = new ByteArrayOutputStream
    val exitCode = runWithOutputStream(baos)
    val output = baos.toString
    (exitCode, output)
  }

  def run(): Int = {
    runWithOutputStream(System.out)
  }
}

object Process {
  def apply(cmd: String): ProcessBuilder = {
    val args = cmd.split("""\s+""")
    val p = new JProcessBuilder(args: _*)
    new ProcessBuilder(p)
  }
  def apply(x: scala.xml.Elem): ProcessBuilder = Process(x.text.trim)

  def spawn(f: => Unit): Thread = {
    val thread = new Thread() { override def run() = { f } }
    thread.setDaemon(false)
    thread.start()
    thread
  }

  def pipeTo(in: InputStream, out: OutputStream): Unit = try {
    val continueCount = 1
    val buffer = new Array[Byte](8192)
    def read {
      val byteCount = in.read(buffer)
      if (byteCount >= continueCount) {
        out.write(buffer, 0, byteCount)
        out.flush()
        read
      }
    }
    try {
      read
    } finally {
      in.close
    }
  } catch { case  _: InterruptedException => () }
}
