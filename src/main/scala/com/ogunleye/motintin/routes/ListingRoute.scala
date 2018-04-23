package com.ogunleye.motintin.routes

import javax.ws.rs.Path

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.{ CircuitBreaker, ask }
import akka.util.Timeout
import com.ogunleye.motintin.actors.ListingActor
import com.ogunleye.motintin.models.{ Listing, ListingRequest }
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

@Path("/listings")
class ListingRoute(implicit system: ActorSystem, breaker: CircuitBreaker) extends SearchDirectives with LazyLogging {

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(20 seconds)
  protected val listingActor: ActorRef = system.actorOf(Props(classOf[ListingActor]))

  def route: Route = pathPrefix("listings") {
    newListing ~ findById ~ findByItemId
  }

  def newListing: Route =
    post {
      entity(as[ListingRequest]) { request =>
        logger.info(s"POST /listings - $request")
        onCompleteWithBreaker(breaker)(listingActor ? request) {
          case Success(msg: Listing) => complete(StatusCodes.Created -> msg)
          case Failure(t) => failWith(t)
        }
      }
    }

  def findById: Route = get {
    idDirective { id =>
      logger.info(s"GET /listings - $id")
      onCompleteWithBreaker(breaker)(listingActor ? ("id", id)) {
        case Success(msg: Some[Listing]) => complete(msg)
        case Success(_) => complete(StatusCodes.InternalServerError)
        case Failure(t) => failWith(t)
      }
    }
  }

  def findByItemId: Route = get {
    iitemIdDirective { id =>
      logger.info(s"GET /listings - $id")
      onCompleteWithBreaker(breaker)(listingActor ? ("itemId", id)) {
        case Success(msg: List[Listing]) => complete(msg)
        case Failure(t) => failWith(t)
      }
    }
  }
}
