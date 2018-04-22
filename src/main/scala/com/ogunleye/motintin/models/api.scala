package com.ogunleye.motintin.models

case class VendorDetail(name: String, productCode: String, webAddress: String, price : BigDecimal = BigDecimal(0))

case class ListingDetail(name: String, vendorDetail: List[VendorDetail] = Nil)

case class ItemNameRequest(name: String)

case class ItemListingResponse(item: Option[Item], request: ListingRequest)

case class VendorListingRequest(vendorId: String, request: ListingRequest)

case class VendorNameRequest(vendor: String, request: ListingRequest)

case class VendorNameResponse(vendor: Option[Vendor], request: ListingRequest)

case class VendorListingResponse(vendor: Option[Vendor], id: String, request: Payload)

case class NotFoundException(message: String) extends Exception

case class NoListingsSearchException(message: String) extends Exception

case class DealVendorResponse(vendor: Option[Vendor], request: DealRequest)

trait Payload