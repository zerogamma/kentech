package com.kentech.actors.service

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.kentech.app.MainApp.{actorSystem, conf,logWithSleep}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._


object StoreActor {
  //Create a pool of store, the amount is set in the application.conf.
  //Can use random Routing to make it more random.
  def create(context: ActorSystem, Poolsize: Int): ActorRef = {
    context.actorOf(RoundRobinPool(Poolsize).props(Props[storeActor]), "stores")
  }
}

class storeActor extends Actor{
  val uuid = java.util.UUID.randomUUID().toString()
  implicit val timeout = Timeout(15 seconds)

  val casherActor = CasherActor.create(actorSystem, conf.getInt("casher"), uuid)

  def receive ={
    case "makeOrder" =>{
      //asign a cashier to make a order.
      logWithSleep(s"creating order from store :${self.path.name}")

      val generateOrder = casherActor ? "CreateOrder"
      val providerOrderGrocery = Await.result(generateOrder,timeout.duration)
      sender() ! providerOrderGrocery
    }

  }
}
