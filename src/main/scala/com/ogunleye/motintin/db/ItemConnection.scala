package com.ogunleye.motintin.db

import com.mongodb.casbah.Imports._
import com.ogunleye.motintin.models.Item
import com.typesafe.scalalogging.LazyLogging

class ItemConnection extends ObjectMongoConnection[Item] with LazyLogging {

  val collection = "items"

  override def transform(obj: Item): MongoDBObject = {
    MongoDBObject("name" -> obj.name, "description" -> obj.description, "_id" -> obj._id)
  }

  def findByName(name: String): Option[Item] = {
    findByProperty("name", name, s"No item found with name: $name")
  }

  def findById(id: String): Option[Item] =
    findByObjectId(id, s"No item found with id: $id")

  override def revert(obj: MongoDBObject): Item = Item(Some(getObjectId(obj, "_id")), getString(obj, "name"), getOptional[String](obj, "description"))

  def persist(item: Item) = {
    if (findByName(item.name).isEmpty) {
      save(item)
      findByName(item.name)
    } else {
      logger.error(s"Item already exists: $item")
      None
    }
  }
}
