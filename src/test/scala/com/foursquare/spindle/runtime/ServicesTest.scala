// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package com.foursquare.spindle.test

import com.foursquare.spindle.test.gen.AService
import com.twitter.finagle.Thrift
import org.junit.Assert.{assertEquals, assertFalse, assertTrue, fail}
import org.junit.Test
import com.twitter.util.{Await, Future, Promise}
import java.net.InetSocketAddress
import com.twitter.conversions.time._

class ServicesTest {
  
  @Test
  def testServices() {
    var voidMethodCounter = 0
    var oneWayVoidMethodCounter = 0

    val firstBarrierPromise = new Promise[Unit]
    val secondBarrierPromise = new Promise[Unit]

  	val server = Thrift.serveIface(":*", new AService.ServiceIface {
      def voidMethod(): com.twitter.util.Future[Unit] = {
        voidMethodCounter += 1
        Future.Done
      }

      def oneWayVoidMethod(): com.twitter.util.Future[Unit] = {
        Await.ready(firstBarrierPromise, 2.seconds)
        oneWayVoidMethodCounter += 1
        secondBarrierPromise.setValue(())
        Future.Done
      }

      def add(x: Int, y: Int): com.twitter.util.Future[Int] = {
        Future.value(x + y)
      }
  	})

    try {
      val client = Thrift.newIface[AService.ServiceIface](server)

      // Test that we can pass parameters.
      for ((x, y) <- Seq((60, 40), (-60, 40), (-60, -40), (60, 0), (-60, 0), (0, 40), (0, -40))) {
        val result = Await.result(client.add(x, y), 2.seconds)
        assertEquals(result, x + y)
      }

      // Test that a void method does not return until it has performed its work.
      Await.result(client.voidMethod(), 2.seconds)
      assertEquals(voidMethodCounter, 1)

      // Test that a one-way function returns immediately and that the function completes asynchronously.
      // This fact is tested using two barriers to make sure that oneWayVoidMethod cannot perform its
      // work until released by the main test thread.
      Await.result(client.oneWayVoidMethod(), 2.seconds)
      assertEquals(oneWayVoidMethodCounter, 0)
      firstBarrierPromise.setValue(())
      Await.ready(secondBarrierPromise, 2.seconds)
      assertEquals(oneWayVoidMethodCounter, 1)
    } finally {
      Await.ready(server.close(), 2.seconds)
    }
  }
}      
