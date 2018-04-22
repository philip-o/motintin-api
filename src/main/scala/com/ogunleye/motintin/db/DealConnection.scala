package com.ogunleye.motintin.db

import com.mongodb.casbah.Imports._
import com.ogunleye.motintin.models.{Deal, PercentageOff}
import com.typesafe.scalalogging.LazyLogging

class DealConnection extends ObjectMongoConnection[Deal] with LazyLogging {

  val collection: String = "deals"

  override def revert(obj : MongoDBObject) : Deal = {
    getString(obj, "dealType") match {
      case "PercentageOff" => PercentageOff(Some(getObjectId(obj, "_id")), getInt(obj, "amount"), expiry = getString(obj, "expiry"), vendorId = getString(obj, "vendorId"))
    }
  }

  override def transform(obj: Deal) : MongoDBObject = {
    obj match {
      case p : PercentageOff => MongoDBObject("amount" -> p.amount, "dealType" -> p.dealType, "expiry" -> p.expiry, "vendorId" -> p.vendorId)
    }
  }

  def findById(id: String) : Option[Deal] = findByObjectId(id,s"No item found with id: $id")

  def findByVendorId(id : String) : List[Deal] = findAllByProperty("vendorId", id, s"No deals found with vendor id: $id")
}
