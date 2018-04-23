package com.ogunleye.motintin.routes

import javax.ws.rs.Path

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.{ CircuitBreaker, ask }
import akka.util.Timeout
import com.ogunleye.motintin.actors.DealActor
import com.ogunleye.motintin.models.{ DealResponse, PercentageOffRequest, PercentageOffResponse }
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@Path("/deals")
class DealRoute(implicit system: ActorSystem, breaker: CircuitBreaker) extends SearchDirectives with LazyLogging {

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(20 seconds)
  protected val dealActor = system.actorOf(Props(classOf[DealActor]))

  def route = pathPrefix("deals") {
    newDeal ~ findById ~ findByVendorName
  }

  def newDeal =
    post {
      entity(as[PercentageOffRequest]) { request =>
        logger.info(s"POST /deals - $request")
        onCompleteWithBreaker(breaker)(dealActor ? request) {
          case Success(msg: DealResponse) => complete(StatusCodes.Created -> msg.asInstanceOf[PercentageOffResponse])
          case Failure(t) => failWith(t)
        }
      }
    }

  def findById =
    get {
      idDirective { id =>
        logger.info(s"GET /deals - $id")
        onCompleteWithBreaker(breaker)(dealActor ? ("id", id)) {
          case Success(msg: DealResponse) => complete(msg.asInstanceOf[PercentageOffResponse])
          case Failure(t) => failWith(t)
        }
      }
    }

  def findByVendorName =
    get {
      nameDirective { name =>
        logger.info(s"GET /deals - $name")
        onCompleteWithBreaker(breaker)(dealActor ? ("vendor", name)) {
          case Success(msg: List[DealResponse]) => complete(msg.asInstanceOf[List[PercentageOffResponse]])
          case Failure(t) => failWith(t)
        }
      }
    }
}
