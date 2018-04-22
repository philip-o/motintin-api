package com.ogunleye.motintin.models

case class ItemRequest(name: String, description: String)

case class Item(_id: Option[String] = None, name: String, description: Option[String] = None)
