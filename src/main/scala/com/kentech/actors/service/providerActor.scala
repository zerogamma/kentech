package com.kentech.actors.service

import java.util.{Timer, TimerTask}

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.kentech.app.MainApp.conf

import scala.concurrent.Await
import scala.concurrent.duration._



object ProviderActor {

  //Create a pool of providers, the amount is set in the application.conf.
  def create(context: ActorSystem, Poolsize:Int, storageActorRef:ActorRef): ActorRef = {
    context.actorOf(RoundRobinPool(Poolsize).props(Props(new providerActor(storageActorRef))),"providers")
  }
}


class providerActor (storageActorRef: ActorRef) extends Actor{
  implicit val timeout = Timeout(15 seconds)
  val uuid = java.util.UUID.randomUUID().toString()

  val t = new Timer()

  //old school semaphore
  var queue = 3;

  var enable = true

  ///Random Timer to enable or disable the Actor.
  val task = new TimerTask {
    override def run(): Unit = {
      enable = Math.random() > 0.25
      //if (!enable) println(s"provider ${uuid} is Blocked")
    }
  }

  t.schedule(task, 0 , 10000)

  def receive = {
    case (groceryQueue:OrderGrocery)=>{
      if (!enable || queue ==0){
        //use to re-enable the actor or wait another 10 second to be evaluate if is enable or not.
        if (conf.getBoolean("re-enable"))
          enable = true
        sender() ! Failure
      }
      else{
        //Check if there are stock available.
        val value = storageActorRef ? "getGroceryAmount"
        if (Await.result(value,timeout.duration) == 0) {
          sender() ! "Out Of Stock"
        }
        else {
          println(s"id:${uuid} - queue: ${queue}")
          queue -= 1

          val preparedOrder = storageActorRef ? (groceryQueue, uuid)
          val preparedOrderGrocery = Await.result(preparedOrder, timeout.duration)

          queue += 1

          sender() ! groceryQueue.copy( order = preparedOrderGrocery.asInstanceOf[grocery])
        }
      }
    }
  }
}
