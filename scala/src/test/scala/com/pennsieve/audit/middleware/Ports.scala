// Copyright (c) 2020 Pennsieve, Inc. All Rights Reserved.

package com.pennsieve.audit.middleware

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

case class Ports(gateway: GatewayHost, auditLogger: Auditor)

object Ports {
  def apply(
    gateway: GatewayHost
  )(implicit
    system: ActorSystem,
    executionContext: ExecutionContext
  ): Ports = {
    val auditLogger = new AuditLogger(gateway)
    Ports(gateway, auditLogger)
  }
}
