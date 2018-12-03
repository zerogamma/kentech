package com.kentech.actors.service

import java.util.{Timer, TimerTask}

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.kentech.app.MainApp.logWithSleep

import scala.concurrent.Await
import scala.concurrent.duration._



object ProviderActor {

  //Create a pool of providers, the amount is set in the application.conf.
  //Can use random Routing to make it more random.
  def create(context: ActorSystem, Poolsize:Int, storageActorRef:ActorRef): ActorRef = {
    context.actorOf(RoundRobinPool(Poolsize).props(Props(new ProviderActor(storageActorRef))),"providers")
  }
}


class ProviderActor (storageActorRef: ActorRef) extends Actor{
  implicit val timeout = Timeout(15 seconds)
  val uuid = java.util.UUID.randomUUID().toString()

  val t = new Timer()

  //old school semaphore
  var queue = 3;

  var enable = true

  ///Timer to disable the Actor.
  val disableTask = new TimerTask {
    override def run(): Unit = {
      enable = Math.random() > 0.25
      //if (!enable) println(s"provider ${uuid} is Blocked")
    }
  }

  ///Timer to enable the Actor if disable.
  val enableTask = new TimerTask {
    override def run(): Unit = {
      if (!enable)
        enable = true
    }
  }

  t.schedule(disableTask, 0 , 10000)
  t.schedule(enableTask, 0 , 5000)

  def receive = {
    case (groceryQueue:OrderGrocery)=>{

      println(s"Checking Provider ${uuid} availability")
      if (!enable || queue ==0){
        sender() ! Failure
      }
      else{
        //Check if there are stock available.
        logWithSleep(s"Available, Checking Grocery stock")

        val value = storageActorRef ? "getGroceryAmount"
        if (Await.result(value,timeout.duration) == 0) {
          sender() ! "Out Of Stock"
        }
        else {
          queue -= 1

          logWithSleep(s"With Stock, Preparing Order")
          val preparedOrder = storageActorRef ? (groceryQueue, uuid)
          val preparedOrderGrocery = Await.result(preparedOrder, timeout.duration)

          queue += 1

          sender() ! groceryQueue.copy( order = preparedOrderGrocery.asInstanceOf[grocery])
        }
      }
    }
  }
}
