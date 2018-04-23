package com.ogunleye.motintin.models

case class ListingRequest(itemName: String, vendorName: String, productCode: Option[String], webAddress: String, price: Double) extends Payload

case class Listing(_id: Option[String], itemId: String, vendorId: String, productCode: Option[String], webAddress: String, price: Double = 0)
