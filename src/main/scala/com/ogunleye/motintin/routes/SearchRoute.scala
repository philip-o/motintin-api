package com.ogunleye.motintin.routes

import javax.ws.rs.Path

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{CircuitBreaker, ask}
import akka.util.Timeout
import com.ogunleye.motintin.actors.SearchActor
import com.ogunleye.motintin.models.SearchResult
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Path("/search")
class SearchRoute (implicit system: ActorSystem, breaker: CircuitBreaker) extends SearchDirectives with LazyLogging {

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(20 seconds)
  protected val searchActor : ActorRef = system.actorOf(Props(classOf[SearchActor]))

  def route = pathPrefix("search") {
    byName// ~ findById ~ findByVendorName
  }

  def byName =
    get {
      nameDirective { itemName =>
        logger.info(s"GET /search - $itemName")
        onCompleteWithBreaker(breaker)(searchActor ? itemName) {
          case Success(msg: SearchResult) => complete(msg)
          case Failure(t) => failWith(t)
        }
      }
    }
}
