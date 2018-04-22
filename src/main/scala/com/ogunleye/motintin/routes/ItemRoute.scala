package com.ogunleye.motintin.routes

import javax.ws.rs.Path

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.{CircuitBreaker, ask}
import akka.util.Timeout
import com.ogunleye.motintin.actors.ItemActor
import com.ogunleye.motintin.models.{Item, ItemNameRequest, ItemRequest}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@Path("/items")
class ItemRoute (implicit system: ActorSystem, breaker: CircuitBreaker) extends SearchDirectives with LazyLogging {

  implicit val executor: ExecutionContext = system.dispatcher
  implicit val timeout : Timeout = Timeout(20 seconds)
  protected val itemActor : ActorRef = system.actorOf(Props(classOf[ItemActor]))

  def route : Route = pathPrefix("items") {
    newItem ~ findById ~ findByName
  }

  def newItem : Route =
    post {
      entity(as[ItemRequest]) { request =>
        logger.info(s"POST /items - $request")
        onCompleteWithBreaker(breaker)(itemActor ? request) {
          case Success(msg: Item) => complete(StatusCodes.Created -> msg)
          case Failure(t) => failWith(t)
        }
      }
    }

  def findById : Route = get {
    idDirective { id =>
      logger.info(s"GET /items - $id")
      onCompleteWithBreaker(breaker)(itemActor ? ("id", id)) {
        case Success(msg: Item) => complete(msg)
        case Failure(t) => failWith(t)
      }
    }
  }

  def findByName : Route = get {
    nameDirective { name =>
      logger.info(s"GET /items - $name")
      onCompleteWithBreaker(breaker)(itemActor ? ItemNameRequest(name)) {
        case Success(msg: Item) => complete(msg)
        case Failure(t) => failWith(t)
      }
    }
  }
}
