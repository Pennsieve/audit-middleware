// Copyright (c) 2020 Blackfynn, Inc. All Rights Reserved.

package com.pennsieve.audit.middleware

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

case class Ports(gateway: GatewayHost, auditLogger: Auditor)

object Ports {
  def apply(
    gateway: GatewayHost
  )(implicit
    system: ActorSystem,
    executionContext: ExecutionContext,
    materializer: ActorMaterializer
  ): Ports = {
    val auditLogger = new AuditLogger(gateway)
    Ports(gateway, auditLogger)
  }
}
