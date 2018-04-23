package com.ogunleye.motintin.actors

import akka.actor.{ Actor, ActorRef }
import com.ogunleye.motintin.db.VendorConnection
import com.ogunleye.motintin.models._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{ Failure, Success }

class VendorActor extends Actor with LazyLogging {

  private val vendorConnection = new VendorConnection

  override def receive: Receive = {
    case ("id", id: String) =>
      val send = sender()
      Future {
        vendorConnection.findById(id)
      } onComplete {
        case Success(data) => data match {
          case Some(vendor) => send ! vendor
          case None => send ! new Exception(s"Vendor id $id does not exist")
        }
        case Failure(t) => send ! t
      }
    case request: VendorListingRequest =>
      val send = sender()
      Future {
        vendorConnection.findById(request.vendorId)
      } onComplete {
        case Success(vendor) => send ! VendorListingResponse(vendor, request.vendorId, request.request)
        case Failure(t) => send ! VendorListingResponse(None, request.vendorId, request.request)
      }
    //    case ("uuid", uuid: String, vendorId: String) => Future {
    //      sender() ! (uuid, vendorConnection.findById(vendorId))
    //    }
    case ("name", name: String) => findByName(name, sender())
    case request: VendorRequest =>
      val send = sender()
      Future {
        vendorConnection.save(Vendor(name = request.name, website = request.website))
      } onComplete {
        case Success(result) =>
          result.getN
          findByName(request.name, send)
        case Failure(t) => send ! t
      }
    case request: VendorNameRequest =>
      val ref = sender()
      Future {
        vendorConnection.findByName(request.vendor)
      } onComplete {
        case Success(result) => ref ! VendorNameResponse(result, request.request)
        case Failure(t) => ref ! t
      }
    case request: DealVendorRequest =>
      val ref = sender()
      Future {
        vendorConnection.findByName(request.request.asInstanceOf[PercentageOffRequest].vendor)
      } onComplete {
        case Success(result) => ref ! DealVendorResponse(result, request.request)
        case Failure(t) => ref ! t
      }
    case request: VendorDealsRequest =>
      val ref = sender()
      Future {
        vendorConnection.findByName(request.name)
      } onComplete {
        case Success(result) => ref ! VendorDealsResponse(result, request.actorRef)
        case Failure(t) => ref ! t
      }

    case request: VendorListingSearchRequest =>
      val ref = sender()
      Future {
        vendorConnection.findById(request.vendorId)
      } onComplete {
        case Success(data) => ref ! VendorListingSearchResponse(data, request.ref)
        case Failure(t) =>
          logger.error(s"Exception thrown when searching by id for id: ${request.vendorId}")
          ref ! VendorListingSearchResponse(None, request.ref)
      }
  }

  private def findByName(name: String, sender: ActorRef): Unit = {
    Future {
      vendorConnection.findByName(name)
    } onComplete {
      case Success(result) => result match {
        case Some(vendor) => sender ! vendor
        case None => sender ! new Exception(s"Vendor $name does not exist")
      }
      case Failure(t) => sender ! t
    }
  }
}
