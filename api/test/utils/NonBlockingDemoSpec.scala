package utils

import java.util.concurrent.Executors

import io.flow.test.utils.FlowPlaySpec
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Futures

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


class NonBlockingDemoSpec extends FlowPlaySpec {

  private val f = init[Futures]

  private def printlnTime(s: String) = println(ISODateTimeFormat.basicTime().print(DateTime.now) + " - " + s)

  "blocks" ignore {
    printlnTime("==============")
    printlnTime("BLOCKS")

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

    val start = System.currentTimeMillis()
    val f1 = Future {
      printlnTime("Started 1")
      Await.result(f.delay(5.seconds), 10.seconds)
      printlnTime("Done 1")
    }(ec)

    val f2 = Future {
      printlnTime("Started 2")
      Await.result(f.delay(5.seconds), 10.seconds)
      printlnTime("Done 2")
    }(ec)

    printlnTime("Waiting for f1...")
    Await.result(f1, 15.seconds)
    printlnTime("f1 done...")

    printlnTime("Waiting for f2...")
    Await.result(f2, 15.seconds)
    printlnTime("f2 done...")

    val end = System.currentTimeMillis()

    printlnTime("took " + (end - start) + " ms")
    succeed
  }

  "does not blocks" ignore {
    printlnTime("==============")
    printlnTime("DOES NOT BLOCK")

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

    val start = System.currentTimeMillis()

    val f1 = Future.delegate {
      printlnTime("Started 1")
      val d = f.delay(5.seconds)
      d.foreach(_ => printlnTime("Done 1"))
      d
    }(ec)

    val f2 = Future.delegate {
      printlnTime("Started 2")
      val d = f.delay(5.seconds)
      d.foreach(_ => printlnTime("Done 2"))
      d
    }(ec)

    printlnTime("Waiting for f1...")
    Await.result(f1, 15.seconds)
    printlnTime("f1 done...")

    printlnTime("Waiting for f2...")
    Await.result(f2, 15.seconds)
    printlnTime("f2 done...")

    val end = System.currentTimeMillis()

    printlnTime("took " + (end - start) + " ms")
    succeed
  }

}
