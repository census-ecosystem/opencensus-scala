package io.opencensus.scala.doobie

import cats.effect.{ContextShift, IO}
import io.opencensus.scala.Tracing
import io.opencensus.scala.http.testSuite.MockTracing
import io.opencensus.trace.{BlankSpan, Status}
import org.scalatest.{Matchers, OptionValues, Outcome, fixture}

import scala.concurrent.ExecutionContext.global
import scala.util.Try

class FTracingSpec extends fixture.FlatSpec with Matchers with OptionValues {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  case class TestInput(fTracing: FTracing[IO], mock: MockTracing)
  override protected def withFixture(test: OneArgTest): Outcome =
    test(clientTracingWithMock())

  override type FixtureParam = TestInput

  behavior of "FTracingSpec"

  it should "start with the correct name" in { f =>
    f.fTracing.traceF(IO(()), "testSpan", None).unsafeRunSync()
    f.mock.startedSpans should have size 1
    f.mock.startedSpans.head.name shouldBe "testSpan"
  }

  it should "trace with parent Span" in { f =>
    val parentSpan = BlankSpan.INSTANCE

    f.fTracing.traceF(IO(()), "testSpan", Some(parentSpan)).unsafeRunSync()
    f.mock.startedSpans should have size 1
    f.mock.startedSpans.head.parentContext.value shouldBe parentSpan.getContext
  }

  it should "stop after normal exit" in { f =>
    f.fTracing.traceF(IO(()), "testSpan", None).unsafeRunSync()
    f.mock.endedSpans should have size 1
    f.mock.endedSpans.head._2.value.getCanonicalCode shouldBe Status.OK.getCanonicalCode
  }

  it should "stop after error" in { f =>
    Try(
      f.fTracing
        .traceF(IO.raiseError(new Exception("TEST")), "testSpan", None)
        .unsafeRunSync()
    )
    f.mock.endedSpans should have size 1
    f.mock.endedSpans.head._2.value.getCanonicalCode shouldBe Status.INTERNAL.getCanonicalCode
  }

  def clientTracingWithMock() = {
    val mockTracing = new MockTracing
    val fTracing = new FTracing[IO] {
      override protected val tracing: Tracing = mockTracing
    }
    TestInput(fTracing, mockTracing)
  }
}
