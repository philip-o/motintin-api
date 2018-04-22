package com.ogunleye.motintin.models

case class VendorRequest(name: String, website: String)

case class Vendor(_id: Option[String] = None, name: String, website: String) extends Payload
