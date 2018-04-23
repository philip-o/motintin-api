package com.ogunleye.motintin.routes

import javax.ws.rs.Path

import akka.pattern.{ CircuitBreaker, ask }
import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.util.Timeout
import com.ogunleye.motintin.actors.VendorActor
import com.ogunleye.motintin.models.{ Vendor, VendorRequest }
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@Path("/vendors")
class VendorRoute(implicit system: ActorSystem, breaker: CircuitBreaker) extends SearchDirectives with LazyLogging {

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(20 seconds)
  protected val vendorActor = system.actorOf(Props(classOf[VendorActor]))

  def route = pathPrefix("vendors") {
    newVendor ~ findById
  }

  def newVendor =
    post {
      entity(as[VendorRequest]) { request =>
        logger.info(s"POST /vendors - $request")
        onCompleteWithBreaker(breaker)(vendorActor ? request) {
          case Success(msg: Vendor) => complete(StatusCodes.Created -> msg)
          case Failure(t) => failWith(t)
        }
      }
    }

  def findById = get {
    idDirective { id =>
      logger.info(s"GET /vendors - $id")
      onCompleteWithBreaker(breaker)(vendorActor ? ("id", id)) {
        case Success(msg: Vendor) => complete(msg)
        case Failure(t) => failWith(t)
      }
    }
  }
}
