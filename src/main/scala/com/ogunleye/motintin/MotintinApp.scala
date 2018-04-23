package com.ogunleye.motintin

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.pattern.CircuitBreaker
import akka.stream.ActorMaterializer
import com.ogunleye.motintin.routes._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

object MotintinApp extends App with LazyLogging with RouteConcatenation {

  val applicationName = "motintin"

  implicit val system = ActorSystem(applicationName)
  implicit val mat = ActorMaterializer()

  logger.info(s"Number of processors visible to $applicationName service: ${Runtime.getRuntime.availableProcessors()}")

  implicit val breaker = CircuitBreaker(system.scheduler, 20, 100 seconds, 12 seconds)

  // routes
  //val swaggerDocRoute = new SwaggerDocRoute().routes
  val vendorRoute = new VendorRoute().route
  val itemRoute = new ItemRoute().route
  val listingRoute = new ListingRoute().route
  val dealRoute = new DealRoute().route
  val searchRoute = new SearchRoute().route

  val routes = vendorRoute ~ itemRoute ~ listingRoute ~ dealRoute ~ searchRoute //buildInfoRoute.route ~ healthRoute ~ swaggerDocRoute ~ swaggerSiteRoute ~ prebookingRoute ~ vendorRoute

  Http().bindAndHandle(routes, "localhost", 8080) //Settings.httpHost, Settings.httpPort)
  logger.info(s"$applicationName application started")

}
