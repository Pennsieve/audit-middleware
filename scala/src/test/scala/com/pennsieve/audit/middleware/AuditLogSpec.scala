// Copyright (c) 2020 Pennsieve, Inc. All Rights Reserved.

package com.pennsieve.audit.middleware

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.{ BeforeAndAfterEach }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues._

import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.{ Map => MutMap }
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Try

object TestConstants {
  val GATEWAY_HOST: String = "test-gateway-host"
  val ENHANCE_LOG_URI = GATEWAY_HOST + "/logs/enhance"
  val UNAUTHORIZED_TRACE_ID: String = "9999-9999"
}

trait MockRequestBody {
  def getRequestBody(traceId: TraceId): Option[String]
}

class MockAuditLogger(
  gatewayHost: GatewayHost
)(implicit
  override val system: ActorSystem,
  override val executionContext: ExecutionContext
) extends AuditLogger(gatewayHost)(system, executionContext)
    with MockHttpResponder
    with MockRequestBody {

  var tracerIdToRequest: MutMap[TraceId, String] = MutMap.empty

  def getRequestBody(traceId: TraceId): Option[String] =
    tracerIdToRequest.get(traceId)

  def extractTraceId(uri: Uri): Option[TraceId] = {
    val parts: Array[String] = uri.toString().split("/")
    parts.length match {
      case 4 => Some(TraceId(uri.toString().split("/").last.trim))
      case _ => None
    }
  }

  override def responder: HttpRequest => Future[HttpResponse] = {
    (req: HttpRequest) =>
      {
        // Track the trace ID header and request payload:
        val traceId: Option[TraceId] = extractTraceId(req.uri)
        for {
          body <- Unmarshal(req.entity).to[String]
          _ <- Future.successful {
            traceId match {
              case Some(traceId) => {
                tracerIdToRequest += traceId -> body
              }
              case _ => {}
            }
          }
          res <- super.responder(req)
        } yield res
      }
  }

  override def mock = {
    case (HttpMethods.POST, uri)
        if (uri.contains(TestConstants.UNAUTHORIZED_TRACE_ID)) => {
      (401, Json.fromString("Not authorized"))
    }
    case (HttpMethods.POST, uri)
        if (uri.startsWith(TestConstants.ENHANCE_LOG_URI) && uri
          .length() > TestConstants.ENHANCE_LOG_URI.length) =>
      (200, Json.Null)
    case (_, _) => (500, Json.fromString("Not valid"))
  }
}

class AuditLogSpec extends AnyWordSpec with BeforeAndAfterEach with Matchers {

  implicit val system: ActorSystem = ActorSystem("test-audit-logger")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val gatewayHost: GatewayHost = GatewayHost(TestConstants.GATEWAY_HOST)

  val testTraceId = TraceId("1234-5678")

  val unauthorizedTraceId = TraceId(TestConstants.UNAUTHORIZED_TRACE_ID)

  val otherTraceId = TraceId("4444-2222")

  "AuditLogger" should {
    "succeed when calling enhance" in {
      val mockLogger = new MockAuditLogger(gatewayHost)
      val ports = Ports(gatewayHost, mockLogger)
      val f = ports.auditLogger.enhance(testTraceId, "payload")
      val result = Await.result(f, 10.seconds)
      result should be(())
      val body = mockLogger.getRequestBody(testTraceId).get
      val expected = parse("\"payload\"").toOption.get.noSpaces
      body should be(expected)
      mockLogger.getRequestBody(otherTraceId) should be(None)
    }

    "succeed with the log message builder" in {
      val mockLogger = new MockAuditLogger(gatewayHost)
      val ports = Ports(gatewayHost, mockLogger)
      val f = ports.auditLogger
        .message()
        .append("key-1", "foo")
        .append("key-2", "bar")
        .append("key-1", 99.0)
        .append("key-2", 5)
        .append("list-items", "a", "b", "c")
        .append("records", List("123", "456", "789"): _*)
        .append("list-items", List("d", "e"): _*)
        .log(testTraceId)
      val result = Await.result(f, 10.seconds)
      result should be(())
      val body = mockLogger.getRequestBody(testTraceId).get
      val expected =
        parse(s"""
        {
          "key-1": ["foo", 99.0],
          "key-2": ["bar", 5],
          "list-items": ["a", "b", "c", "d", "e"],
          "records": ["123", "456", "789"]
        }
        """).toOption.get.noSpaces
      body should be(expected)
    }

    "fail gracefully" in {
      // Not authorized to make a request with the given trace ID:
      val mockLogger = new MockAuditLogger(gatewayHost)
      val ports = Ports(gatewayHost, mockLogger)
      val f = ports.auditLogger
        .message()
        .append("key-1", "foo")
        .append("key-2", "bar")
        .log(unauthorizedTraceId)
      val result = Try(Await.result(f, 10.seconds)).toEither
      result.left.value should be("\"Not authorized\"")
    }
  }
}
