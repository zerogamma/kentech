package com.kentech.actors

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.kentech.actors.service._
import com.kentech.app.MainApp.{actorSystem, conf}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random


///Main father Actor to coordinate the actors
object MainActor {
  def create(context: ActorSystem): ActorRef = {
    context.actorOf(Props[MainActor])
  }
}


class MainActor extends Actor{

  //Mutable variable not the best option. But is more clear than using Immutable and make a copy every time to change the value.
  //This is better to be in a DB, mongoDB, redis, etc.
  var groceryList = new ListBuffer[grocery]()

  val debug = conf.getBoolean("debug")
  val storageActor = initialize(actorSystem, 0 , "storage")
  val storeActor = initialize(actorSystem, conf.getInt("store"), "store")
  val providerActor = initialize(actorSystem, conf.getInt("provider"), "provider")
  implicit val timeout = Timeout(15 seconds)


  //in memory setting the grocery. better to use a BD like mongoBD
  def prepareGroceries: ListBuffer[grocery] = {
    for( i <- 1 to conf.getInt("groceries"))
      groceryList += grocery("", Random.nextInt(10) )
    groceryList
  }

  //Create n Actors for store, providers. the Storage Actor is only one for everybody.
  def initialize(context: ActorSystem, i: Int, typeInstance: String) : ActorRef = {
    typeInstance match {
      case "store" =>
        StoreActor.create(context, i)
      case "provider" =>
        ProviderActor.create(context, i, storageActor)
      case "storage" =>{
        val storageAct= StorageActor.create(actorSystem)
        StorageActor.initilizeStock(prepareGroceries)
        storageAct
      }
    }
  }

  def receive ={
    //Main function to buy a grocery.
    case "buy" => {
      println("taking Order:")

      //Extra println to get amount of grocery available
      if (debug) {
        println("Listing available grocery:")
        storageActor ! "getGroceryAmount"
      }

      //Can use message Queue like RabbitMQ or Kafka to deal with all the call between the actors for better management and control.
      val order = storeActor ? "makeOrder"
      val orderGrocery = Await.result(order,timeout.duration)

      println(s"Creating Order: ${orderGrocery.asInstanceOf[OrderGrocery].orderId}")

      val providerOrder = providerActor ? orderGrocery
      //Using Await and not onComplete/onSuccess/onFailure because need to give response to the sender.
      val providerOrderGrocery = Await.result(providerOrder,timeout.duration)

      providerOrderGrocery match {
        case Failure => {
          sender() ! "Failed"
        }
        case _ =>{
          providerOrder pipeTo sender()
        }
      }

    }

    //Call for Filling the ListBuffer.
    case "restock" => {
      println("re-stocking")
      StorageActor.stockGrocery(prepareGroceries)
    }
  }

}
