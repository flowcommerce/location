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

  // Output:
  // 114856.755-0400 - ==============
  // 114856.785-0400 - BLOCKS
  // 114856.785-0400 - Started 1
  // 114856.786-0400 - Waiting for f1...
  // 114901.804-0400 - Done 1
  // 114901.804-0400 - Started 2
  // 114901.804-0400 - f1 done...
  // 114901.804-0400 - Waiting for f2...
  // 114906.825-0400 - Done 2
  // 114906.825-0400 - f2 done...
  // 114906.825-0400 - took 10040 ms
  // [info] - blocks
  // 114906.835-0400 - ==============
  // 114906.835-0400 - DOES NOT BLOCK
  // 114906.836-0400 - Started 1
  // 114906.836-0400 - Waiting for f1...
  // 114906.836-0400 - Started 2
  // 114911.854-0400 - Done 2
  // 114911.854-0400 - f1 done...
  // 114911.854-0400 - Done 1
  // 114911.854-0400 - Waiting for f2...
  // 114911.854-0400 - f2 done...
  // 114911.854-0400 - took 5019 ms9s

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
