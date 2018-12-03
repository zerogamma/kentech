package com.kentech.actors.service

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable.ListBuffer


case class grocery( providerId:String, expiration: Int)

object StorageActor{
  var groceryStock : ListBuffer[grocery] = new ListBuffer[grocery]()

  def initilizeStock(groceryList: ListBuffer[grocery]) = {
    groceryStock = groceryList
  }

  def stockGrocery(groceryList: ListBuffer[grocery]) = {
    groceryStock ++ groceryList
  }

  def create(context: ActorSystem): ActorRef = {
    context.actorOf(Props[storageActor],"storage")
  }

}

class storageActor extends Actor{
import StorageActor._

  def receive ={
    case "getGroceryAmount" => {
      sender() ! groceryStock.length
    }
    case (order:OrderGrocery, providerID:String) =>{
        //sorting by expiration time to get the one with close to 0
        val takeOneGrocery = groceryStock.sortBy(_.expiration).head
        //getting a copy because the object in the ListBuffer is inmutable.
        var groceryOrdened = takeOneGrocery.copy(providerId = providerID)
        groceryStock -= takeOneGrocery

        sender() ! groceryOrdened
    }
  }
}
