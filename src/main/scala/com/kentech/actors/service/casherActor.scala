package com.kentech.actors.service

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool

case class OrderGrocery (orderId: String,order: grocery)


object CasherActor {

  //Create a pool of casher, the amount is set in the application.conf.
  def create(context: ActorSystem, Poolsize:Int , storeId: String): ActorRef = {
    context.actorOf(RoundRobinPool(Poolsize).props(Props[casherActor]),s"casher${storeId}")
  }
}


class casherActor extends Actor{

  def receive ={
    case "CreateOrder" =>{
      sender() ! OrderGrocery(java.util.UUID.randomUUID().toString(),null)
    }
  }
}
