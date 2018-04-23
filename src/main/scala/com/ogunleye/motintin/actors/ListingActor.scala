package com.ogunleye.motintin.actors

import java.util.UUID

import akka.actor.{ Actor, ActorRef, Props }
import com.ogunleye.motintin.db.ListingConnection
import com.ogunleye.motintin.models._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{ Failure, Success }

class ListingActor extends Actor with LazyLogging {

  private val vendorActor = context.actorOf(Props[VendorActor])
  private val itemActor = context.actorOf(Props[ItemActor])
  private val listingMap = mutable.Map[ListingRequest, Listing]()
  private val requestMap = mutable.Map[ListingRequest, ActorRef]()
  private val listingsMap = mutable.Map[ListingRequest, List[Listing]]()
  private val vendorMap = mutable.Map[String, List[VendorDetail]]()
  private val vendorResponseMap = mutable.Map[ListingRequest, List[Option[Vendor]]]()
  val UUID_CONST = "uuid"
  private val listingConnection = new ListingConnection

  override def receive: Receive = {
    case response: ItemListingResponse =>
      response.item match {
        case None =>
          requestMap(response.request) ! NotFoundException(s"No item found for request: ${response.request}")
          requestMap -= response.request
        case Some(item) =>
          val request = response.request
          listingMap(request) = Listing(_id = None, itemId = item._id.get, vendorId = "", productCode = request.productCode, request.webAddress, request.price)
          vendorActor ! VendorNameRequest(request.vendorName, request)
      }
    case response: VendorNameResponse =>
      response.vendor match {
        case None =>
          requestMap(response.request) ! NotFoundException(s"No vendor found for request: ${response.request}")
          requestMap -= response.request
          listingMap -= response.request
        case Some(vendor) =>
          val listing = listingMap(response.request).copy(vendorId = vendor._id.get)
          listingMap -= response.request
          val ref = requestMap(response.request)
          requestMap -= response.request
          Future {
            listingConnection.save(listing)
          } onComplete {
            case Success(result) =>
              val saved = listingConnection.findByItemId(listing.itemId).filter(i => i.vendorId == response.vendor.get._id.get).head
              ref ! saved
            case Failure(t) => ref ! new Exception("Failed to save", t)
          }
      }
    case response: VendorListingResponse =>
      vendorResponseMap(response.request.asInstanceOf[ListingRequest]) = response.vendor :: vendorResponseMap(response.request.asInstanceOf[ListingRequest])
      response.vendor match {
        case None => logger.error(s"No vendor entry found for vendor id: ${response.id} on request: ${response.request}")
        case Some(_) =>
      }
    case ("itemId", id: String) =>
      val ref = sender()
      Future {
        findByItemId(id)
      } onComplete {
        case Success(list) => ref ! list
        case Failure(_) => ref ! new Exception(s"Listing with itemId $id does not exist")
      }
    case ListingsByItemIdRequest(itemId: String, parent: ActorRef) =>
      val ref = sender()
      Future {
        findByItemId(itemId)
      } onComplete {
        case Success(list) => ref ! ListingsByItemIdResponse(list, parent)
        case Failure(_) => ref ! ListingsByItemIdResponse(Nil, parent)
      }
    case ("id", id: String) => findById(id, sender())
    case request: ListingRequest =>
      requestMap(request) = sender()
      itemActor ! request
  }

  private def findByItemId(id: String) = {
    listingConnection.findByItemId(id)
  }

  private def findById(id: String, ref: ActorRef): Unit = {
    Future {
      listingConnection.findById(id)
    } onComplete {
      case Failure(_) => ref ! new Exception(s"Listing $id does not exist")
      case Success(listing) => ref ! listing
    }
  }
}
