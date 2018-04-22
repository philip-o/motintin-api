package com.ogunleye.motintin.actors

import akka.actor.{Actor, ActorRef, Props}
import com.ogunleye.motintin.db.DealConnection
import com.ogunleye.motintin.models._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class DealActor extends Actor with LazyLogging {

  private val vendorActor = context.actorOf(Props[VendorActor])
  private val dealConnection = new DealConnection
  private val requestMap = mutable.Map[DealRequest, ActorRef]()

  override def receive: Receive = {
    case request: DealRequest => vendorActor ! DealVendorRequest(request.asInstanceOf[PercentageOffRequest].vendor, request)
      requestMap(request) = sender()
    case response: DealVendorResponse =>
      val ref = requestMap(response.request)
      requestMap -= response.request
      response.vendor match {
        case Some(vendor) =>
          response.request match {
            case request: PercentageOffRequest =>
              Future {
                dealConnection.save(PercentageOff(amount = request.amount, expiry = request.expiry, vendorId = vendor._id.get))
              } onComplete {
                case Success(result) => result.getN
                  ref ! PercentageOffResponse(_id  = None, request.amount, expiry = request.expiry, vendorId = vendor._id.get)
                case Failure(t) => ref ! t
              }
          }
      }
    case ("id", id: String) => val ref = sender()
      Future {
        dealConnection.findById(id)
      } onComplete {
        case Success(option) => option match {
          case Some(deal) => ref ! convertToResponse(deal)
          case None => ref ! new Exception(s"Deal id $id does not exist")
        }
        case Failure(t) => ref ! t
      }
    case ("vendor", name: String) => val ref = sender()
      logger.info(s"Looking for vendor: $name")
      vendorActor ! VendorDealsRequest(name, ref)

    case response: VendorDealsResponse => response.vendor match {
      case Some(vendor) => Future {
        dealConnection.findByVendorId(vendor._id.get)
      } onComplete {
        case Failure(t) => logger.error(s"Unable to find deals for vendor ${vendor.name}")
        case Success(list) => response.actorRef ! convertListToResponse(list)
      }
      case None => logger.error("Vendor does not exist")
    }
    case request: VendorIdDealsRequest => val ref = sender()
      Future {
      dealConnection.findByVendorId(request.id)
    } onComplete {
      case Failure(t) => ref ! VendorIdDealsResponse(request.id, Nil, request.actorRef)
      case Success(list) => ref ! VendorIdDealsResponse(request.id, list, request.actorRef)
    }
    case other => logger.error(s"Unknown message received: $other")
  }

  def convertListToResponse(deals: List[Deal]): List[DealResponse] = {
    deals match {
      case Nil => Nil
      case head :: tail => convertToResponse(head) :: convertListToResponse(tail)
    }
  }

  def convertToResponse(deal: Deal) : DealResponse = {
    //if(deal.isInstanceOf[PercentageOff]) {
    val p = deal.asInstanceOf[PercentageOff]
      PercentageOffResponse(p._id, p.amount, expiry = p.expiry, vendorId = p.vendorId)
    //}
  }
}
