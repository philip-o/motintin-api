package com.ogunleye.motintin.models

import akka.actor.ActorRef

case class SearchByItemNameRequest(name: String, ref: ActorRef)

case class SearchByItemNameResponse(item: Option[Item], ref: ActorRef)

case class ListingsByItemIdRequest(id: String, ref: ActorRef)

case class ListingsByItemIdResponse(listings: List[Listing], ref: ActorRef)

case class SearchByProductIdRequest(id: String, ref: ActorRef)

case class VendorListingSearchRequest(vendorId: String, ref: ActorRef)

case class VendorListingSearchResponse(vendor: Option[Vendor], ref: ActorRef)

case class SearchListing(vendor: String, productCode: Option[String], webAddress: String, price : Double = 0)

case class SearchResult(name: String, listings: List[SearchListing])
