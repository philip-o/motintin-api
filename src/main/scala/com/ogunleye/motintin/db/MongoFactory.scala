package com.ogunleye.motintin.db

import java.util.Date

import com.mongodb.casbah.Imports._
import com.typesafe.scalalogging.LazyLogging

object MongoFactory {
  private val SERVER = "localhost"
  private val PORT = 27017
  private val DATABASE = "motintin"
  val uri = MongoClientURI("mongodb://localhost:27017/")
  //  val c = MongoClient(uri)

  val mongoClient = MongoClient(SERVER, PORT)
  val db = mongoClient(DATABASE)

  def getCollection(collection: String) = db(collection)
}

trait ObjectMongoConnection[A] extends LazyLogging {

  val collection: String

  def save(obj: A) = {
    val coll = retrieveCollection
    val toPersist = transform(obj)
    coll.save(toPersist)
  }

  protected def retrieveCollection = {
    MongoFactory.getCollection(collection)
  }

  protected def findByProperty(name: String, value: String, error: String) = {
    retrieveCollection.findOne(MongoDBObject(name -> value)) match {
      case Some(data) => Some(revert(data))
      case _ =>
        logger.error(error)
        None
    }
  }

  protected def findByObjectId(value: String, error: String) = {
    retrieveCollection.findOne(MongoDBObject("_id" -> new ObjectId(value))) match {
      case Some(data) => Some(revert(data))
      case _ =>
        logger.error(error)
        None
    }
  }

  protected def findByProperty(name: String, value: Int, error: String) = {
    retrieveCollection.findOne(MongoDBObject(name -> value)) match {
      case Some(data) => Some(revert(data))
      case _ =>
        logger.error(error)
        None
    }
  }

  protected def findAllByProperty(name: String, value: String, error: String) = {
    val cursor = retrieveCollection.find(MongoDBObject(name -> value))
    if (cursor.isEmpty) {
      logger.error(error)
      Nil
    } else {
      cursor.map(c => revert(c)).toList
    }
  }

  protected def transform(obj: A): MongoDBObject

  protected def revert(obj: MongoDBObject): A

  protected def getInt(obj: MongoDBObject, name: String) = getVar(obj, name).getOrElse(0).asInstanceOf[Int]

  protected def getString(obj: MongoDBObject, name: String) = getVar(obj, name).getOrElse("").asInstanceOf[String]

  protected def getOptional[B](obj: MongoDBObject, name: String) = getVar(obj, name).asInstanceOf[Option[B]]

  protected def getObjectId[B](obj: MongoDBObject, name: String) = getVar(obj, name).asInstanceOf[Option[ObjectId]].get.toString

  protected def getBigDecimal(obj: MongoDBObject, name: String) = getVar(obj, name).getOrElse(BigDecimal(0)).asInstanceOf[BigDecimal]

  protected def getDouble(obj: MongoDBObject, name: String) = getVar(obj, name).getOrElse(0).asInstanceOf[Double]

  protected def getDate(obj: MongoDBObject, name: String) = getVar(obj, name).getOrElse(new Date).asInstanceOf[Date]

  protected def getList[B](obj: MongoDBObject, name: String) = {
    getVar(obj, name).getOrElse(Nil).asInstanceOf[BasicDBList].toList.asInstanceOf[List[B]]
  }

  private def getVar(obj: MongoDBObject, name: String) = obj.get(name)
}
