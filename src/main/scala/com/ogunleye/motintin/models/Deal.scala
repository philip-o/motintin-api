package com.ogunleye.motintin.models

import akka.actor.ActorRef

abstract class Deal(expiry: String, vendorId: String)

abstract class DealRequest(expiry: String, vendor: String)

case class PercentageOff(_id: Option[String] = None, amount: Int, dealType: String = "PercentageOff", expiry: String, vendorId: String) extends Deal(expiry, vendorId)

case class PercentageOffRequest(amount: Int, dealType: String = "PercentageOff", expiry: String, vendor: String) extends DealRequest(expiry, vendor)

abstract class DealResponse(_id: Option[String] = None, expiry: String, vendorId: String, dealType: String)

case class PercentageOffResponse(_id: Option[String] = None, amount: Int, dealType: String = "PercentageOff", expiry: String, vendorId: String) extends DealResponse(_id, expiry, vendorId, dealType)

case class DealVendorRequest(vendorId: String, request: DealRequest)

case class VendorDealsRequest(name: String, actorRef: ActorRef)

case class VendorDealsResponse(vendor: Option[Vendor], actorRef: ActorRef)

case class VendorIdDealsRequest(id: String, actorRef: ActorRef)

case class VendorIdDealsResponse(id: String, deals: List[Deal], actorRef: ActorRef)