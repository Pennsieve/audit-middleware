// Copyright (c) 2020 Pennsieve, Inc. All Rights Reserved.

package com.pennsieve.audit.middleware

import collection.immutable.{ Seq => ISeq }
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpRequest,
  HttpResponse
}
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import io.circe.{ Encoder, Json }
import io.circe.syntax._

import scala.concurrent.{ ExecutionContext, Future }

trait HttpResponder {
  implicit val system: ActorSystem
  implicit val executionContext: ExecutionContext

  def responder: HttpResponder.Responder
}

object HttpResponder {
  type Responder = HttpRequest => Future[HttpResponse]
}

trait ToMessage[T] {
  def format(s: T): Json
}

object ToMessage {
  implicit object BooleanToMessage extends ToMessage[Boolean] {
    def format(s: Boolean) = Json.fromBoolean(s)
  }
  implicit object DoubleToMessage extends ToMessage[Double] {
    def format(s: Double) = Json.fromDouble(s).getOrElse(Json.fromString("NaN"))
  }
  implicit object StringToMessage extends ToMessage[String] {
    def format(s: String) = Json.fromString(s)
  }
  implicit object IntToMessage extends ToMessage[Int] {
    def format(s: Int) = Json.fromInt(s)
  }
  implicit object LongToMessage extends ToMessage[Long] {
    def format(s: Long) = Json.fromLong(s)
  }
  implicit object JsonToMessage extends ToMessage[Json] {
    def format(s: Json) = s
  }
}

final case class TraceId(id: String) extends AnyVal
object TraceId {
  implicit val encoder: Encoder[TraceId] = new Encoder[TraceId] {
    final def apply(traceId: TraceId): Json = Json.fromString(traceId.id)
  }
}

final case class GatewayHost(uri: String) extends AnyVal

trait Auditor {

  /**
    * Calling `enhance()` will add an "enhancement" message to the audit log.
    *
    * @param traceId
    * @param payload
    * @param converter
    * @tparam T
    * @return
    */
  def enhance[T](
    traceId: TraceId,
    payload: T
  )(implicit
    converter: ToMessage[T]
  ): Future[Unit]

  /**
    * A method to introduce a builder pattern for constructing a log enhancement message.
    *
    *  Example:
    *
    *  <code>
    *     val f: Future[Unit] = ports
    *       .auditLogger
    *       .message()
    *       .append("key-1", "foo")
    *       .append("key-2", "bar")
    *       .append("key-1", "baz")
    *       .log(traceId)
    *  </code>
    *
    * @return
    */
  def message(): MessageBuilder = {
    new MessageBuilder(this)
  }
}

private[middleware] case class MessageBuilder(
  logger: Auditor,
  contents: Map[String, ISeq[Json]] = Map.empty
) {
  def append[T](
    key: String,
    message: T*
  )(implicit
    converter: ToMessage[T]
  ): MessageBuilder = {
    val e: ISeq[Json] = ISeq(message.map(converter.format(_)): _*)
    contents.get(key) match {
      case Some(entries: ISeq[Json]) =>
        MessageBuilder(logger, contents + (key -> (entries ++ e)))
      case None => MessageBuilder(logger, contents + (key -> e))
    }
  }

  def log[T](traceId: TraceId): Future[Unit] = {
    logger.enhance(traceId, MessageBuilder.MessageBuilderToMessage.format(this))
  }
}

object MessageBuilder {
  implicit object MessageBuilderToMessage extends ToMessage[MessageBuilder] {
    def format(s: MessageBuilder): Json = s.contents.asJson
  }
}

object AuditLogger {

  /**
    * The audit enhance endpoint, relative to the gateway host
    */
  val ENHANCE_ENDPOINT: String = "/logs/enhance"

  /**
    * A custom header containing an identifier used to track a request originating
    * in the gateway such that service events can be correlated back to the original
    * request.
    */
  val TRACE_ID_HEADER: String = "X-Bf-Trace-Id"
}

class AuditLogger(
  gateway: GatewayHost
)(implicit
  val system: ActorSystem,
  val executionContext: ExecutionContext
) extends HttpResponder
    with Auditor {

  val http = Http()

  override def responder: (HttpRequest) => Future[HttpResponse] =
    (req) => http.singleRequest(req)

  override def enhance[T](
    traceId: TraceId,
    payload: T
  )(implicit
    converter: ToMessage[T]
  ): Future[Unit] =
    enhance(traceId, converter.format(payload))

  def enhance(traceId: TraceId, payload: Json): Future[Unit] = {
    val req = HttpRequest(
      method = POST,
      uri = s"${gateway.uri}${AuditLogger.ENHANCE_ENDPOINT}/${traceId.id}",
      entity = HttpEntity(ContentTypes.`application/json`, payload.noSpaces)
    )
    for {
      res <- responder(req)
      _ <- (if (res.status.isSuccess()) {
              Future.successful(())
            } else {
              Unmarshal(res.entity)
                .to[String]
                .flatMap((body: String) => Future.failed(new Exception(body)))
            })
    } yield ()
  }
}
