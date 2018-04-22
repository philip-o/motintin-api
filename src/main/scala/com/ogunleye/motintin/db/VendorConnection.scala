package com.ogunleye.motintin.db

import com.mongodb.casbah.Imports._
import com.ogunleye.motintin.models.Vendor
import com.typesafe.scalalogging.LazyLogging

class VendorConnection extends ObjectMongoConnection[Vendor] with LazyLogging {

  val collection = "vendors"

  override def transform(obj: Vendor) : MongoDBObject = {
    MongoDBObject("_id" -> obj._id, "name" -> obj.name, "website" -> obj.website)
  }

  def findByName(name : String) : Option[Vendor] = {
    findByProperty("name", name, s"No vendor found with name: $name")
  }

  def findById(id: String) : Option[Vendor] =
    findByObjectId(id,s"No vendor found with id: $id")




  override def revert(obj : MongoDBObject) : Vendor = {
    Vendor(Some(getObjectId(obj, "_id")),
      getString(obj, "name"),
      getString(obj, "website"))
  }
}
