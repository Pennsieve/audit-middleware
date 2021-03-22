// Copyright (c) 2020 Blackfynn, Inc. All Rights Reserved.

package com.pennsieve.audit.middleware

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, _ }
import akka.util.ByteString
import io.circe._

import scala.concurrent.Future

/**
  * Borrowed from service-utilities so that a hard dependency isn't introduced
  */
trait MockHttpResponder extends HttpResponder {
  /*
   * Create a Future[HttpResponse] from a given StatusCode and some JSON
   */
  protected def jsonResponse(
    statusCode: StatusCode,
    payload: Json
  ): Future[HttpResponse] = {
    Future.successful {
      HttpResponse(
        entity = HttpEntity
          .Strict(
            ContentTypes.`application/json`,
            ByteString(payload.noSpaces)
          ),
        status = statusCode
      )
    }
  }

  /*
   * A partial function that can be used to respond to different request
   * methods against different uri values
   */
  def mock: PartialFunction[(HttpMethod, String), (StatusCode, Json)]

  override def responder = (req: HttpRequest) => {
    if (mock.isDefinedAt((req.method, req.uri.toString))) {
      val (statusCode, payload) = mock((req.method, req.uri.toString))
      jsonResponse(statusCode, payload)
    } else {
      throw new Exception(s"Route not supported: ${req.method} ${req.uri}")
    }
  }
}
