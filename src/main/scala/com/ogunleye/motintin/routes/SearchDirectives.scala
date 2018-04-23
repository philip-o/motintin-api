package com.ogunleye.motintin.routes

import akka.http.scaladsl.server.{ Directive1, Directives }

trait SearchDirectives extends Directives {

  val nameDirective: Directive1[String] = parameter("name")
  val websiteDirective: Directive1[String] = parameter("website")
  val idDirective: Directive1[String] = parameter("id")
  val iitemIdDirective: Directive1[String] = parameter("itemId")
}
