package com.kentech.actors.service

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.kentech.app.MainApp.logWithSleep

case class OrderGrocery (orderId: String,order: grocery)


object CasherActor {

  //Create a pool of casher, the amount is set in the application.conf.
  //Can use random Routing to make it more random.
  def create(context: ActorSystem, Poolsize:Int , storeId: String): ActorRef = {
    context.actorOf(RoundRobinPool(Poolsize).props(Props[CasherActor]),s"casher${storeId}")
  }
}


class CasherActor extends Actor{

  def receive ={
    case "CreateOrder" =>{
      logWithSleep(s"casher ${self.path.name} is making order")
      sender() ! OrderGrocery(java.util.UUID.randomUUID().toString(),null)
    }
  }
}
