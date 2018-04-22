package com.ogunleye.motintin.actors

import akka.actor.{Actor, ActorRef}
import com.ogunleye.motintin.db.ItemConnection
import com.ogunleye.motintin.models._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ItemActor extends Actor with LazyLogging {

  private val itemConnection = new ItemConnection

  override def receive: Receive = {
    case ("id", id: String) =>  val ref = sender()
      Future {
      itemConnection.findById(id)
    } onComplete {
        case Success(option) => option match {
          case Some(item) => ref ! item
          case None => ref ! new Exception(s"Item id $id does not exist")
        }
        case Failure(t) => ref ! t
      }
    case ItemNameRequest(name) => findByName(name, sender())
    case request: SearchByItemNameRequest => val ref = sender()
      Future {
      itemConnection.findByName(request.name)
    } onComplete {
      case Success(option) => ref ! SearchByItemNameResponse(option, request.ref)
      case Failure(t) => ref ! t
    }
    case request: ItemRequest => val send = sender()
      Future {
        itemConnection.save(Item(name = request.name, description = Some(request.description)))
      } onComplete {
        case Success(result) => val upserted = result
          logger.info(s"Id is $upserted")
          findByName(request.name, send)
        case Failure(t) => send ! t
      }
    case request: ListingRequest => val ref = sender()
      Future {
        itemConnection.findByName(request.itemName)
      } onComplete {
        case Success(item) => ref ! ItemListingResponse(item, request)
        case Failure(t) => logger.error(s"Failed to find item for $request due to $t")
          ref ! ItemListingResponse(None, request)
      }
  }

  private def findByName(name: String, ref: ActorRef) : Unit = {
    Future {
      itemConnection.findByName(name)
    } onComplete {
      case Success(option) => option match {
        case Some(item) => ref ! item
        case None => ref ! new Exception(s"Item $name does not exist")
      }
      case Failure(t) => ref ! t
    }
  }
}
