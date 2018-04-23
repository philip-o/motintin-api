package com.ogunleye.motintin.db

import com.mongodb.casbah.Imports._
import com.ogunleye.motintin.models.Listing
import com.typesafe.scalalogging.LazyLogging

class ListingConnection extends ObjectMongoConnection[Listing] with LazyLogging {

  val collection = "listings"

  override def revert(obj: MongoDBObject): Listing = Listing(Some(getObjectId(obj, "_id")), getString(obj, "itemId"), getString(obj, "vendorId"), getOptional(obj, "productCode"), getString(obj, "webAddress"), getDouble(obj, "price"))

  override def transform(obj: Listing): MongoDBObject = {
    MongoDBObject("_id" -> obj._id, "itemId" -> obj.itemId, "vendorId" -> obj.vendorId, "productCode" -> obj.productCode, "webAddress" -> obj.webAddress, "price" -> obj.price)
  }

  def findByItemId(id: String): List[Listing] = {
    findAllByProperty("itemId", id, s"No listings found with item id: $id")
  }

  def findById(id: String): Option[Listing] =
    findByObjectId(id, s"No listing found with id: $id")
}
