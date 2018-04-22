package com.ogunleye.motintin.actors

import akka.actor.{Actor, ActorRef, Props}
import com.ogunleye.motintin.models._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.{Map => MyMap}


class SearchActor extends Actor with LazyLogging {

  private val itemActor = context.actorOf(Props[ItemActor])
  private val listingActor = context.actorOf(Props[ListingActor])
  private val vendorActor = context.actorOf(Props[VendorActor])
  private val dealActor = context.actorOf(Props[DealActor])
  private val itemMap = MyMap[ActorRef, Item]()
  private val listingsMap = MyMap[ActorRef, List[Listing]]()
  private val vendorMap = MyMap[ActorRef, List[Vendor]]()
  private val vendorCountMap = MyMap[ActorRef, Int]()
  private val dealVendorMap = MyMap[ActorRef, MyMap[String, List[Deal]]]()

  override def receive: Receive = {
    case name: String => logger.info(s"Looking for item by name: $name")
      itemActor ! SearchByItemNameRequest(name, sender())
    case response: SearchByItemNameResponse => response.item match {
      case None => logger.error(s"No Item found for name")
        response.ref ! NotFoundException(s"No item found")
      case Some(item) => listingActor ! ListingsByItemIdRequest(item._id.get, response.ref)
        itemMap(response.ref) = item
    }
    case response: ListingsByItemIdResponse =>
      if(response.listings.isEmpty) {
        response.ref ! NoListingsSearchException(s"No listings for search")
        itemMap -= response.ref
      }
      else {
        response.listings.foreach(listing => vendorActor ! VendorListingSearchRequest(listing.vendorId, response.ref))
        listingsMap(response.ref) = response.listings
        vendorCountMap(response.ref) = 0
      }
    case response: VendorListingSearchResponse =>
      vendorCountMap(response.ref) = vendorCountMap(response.ref) + 1
      response.vendor match {
        case None => logger.error("Missing vendor entry")
        case Some(vendor) => vendorMap(response.ref) = vendor :: vendorMap.getOrElse(response.ref, Nil)
      }
      checkAndRequest(response.ref)

    case response: VendorIdDealsResponse =>
      val map = dealVendorMap(response.actorRef)
      map(response.id) = response.deals
      dealVendorMap(response.actorRef) = map
      if(map.size == vendorMap(response.actorRef).size) {
        val results = new mutable.ListBuffer[SearchListing]
        val ref = response.actorRef
        vendorCountMap -= ref
        val vendors = vendorMap(ref)
        vendorMap -= ref
        val lm = listingsMap(ref)
        listingsMap -= ref
        val item = itemMap(ref)
        itemMap -= ref
        val deals = dealVendorMap(ref)
        dealVendorMap -= ref
        lm.foreach(li => results += buildSearchListingWithCheapestPriceForVendor(li, deals(li.vendorId), vendors.map(v=> (v._id.get, v)).toMap))
        ref ! SearchResult(item.name, results.toList)
      }
  }

  def buildSearchListingWithCheapestPriceForVendor(li: Listing, deals: List[Deal], vendors: Map[String, Vendor]) : SearchListing = {

    def calculateCheapest(dealList: List[Deal]) = {
      BigDecimal(li.price).*(dealList.map(_.asInstanceOf[PercentageOff]).filter(_.vendorId.equalsIgnoreCase(li.vendorId)).map(po => BigDecimal(100 - po.amount)./(100)).min).setScale(2)
    }
    SearchListing(vendors(li.vendorId).name, li.productCode, li.webAddress, calculateCheapest(deals).doubleValue())
  }

  def checkAndRequest(ref: ActorRef) : Unit = {
    val count = vendorCountMap(ref)
    if(listingsMap(ref).lengthCompare(count) == 0) {
      val vendors = vendorMap(ref)
      if(vendors.nonEmpty) {
        vendors.foreach(vendor => dealActor ! VendorIdDealsRequest(vendor._id.get, ref))
        dealVendorMap(ref) = MyMap[String, List[Deal]]()
      }
      else {
        ref ! NotFoundException("No vendors found")
        vendorCountMap -= ref
        vendorMap -= ref
        listingsMap -= ref
        itemMap -= ref
      }
    }
  }
}
