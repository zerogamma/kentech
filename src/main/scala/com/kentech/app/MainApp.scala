package com.kentech.app

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.kentech.actors.MainActor
import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.Await
import scala.concurrent.duration._


object MainApp extends App {

  implicit val timeout = Timeout(15 seconds)

  val actorSystem = ActorSystem("SupplyChain")

  val conf: Config = ConfigFactory.load().getConfig("dev").resolve()

  val actor = MainActor.create(actorSystem)

  def logWithSleep(msg:String){
    //Not advised to block an actor. No better option to display the println of steps.
    Thread.sleep(1000)
    println(msg)
  }

  def createPurchase(amount:Int) {
    for (i <- 0 to amount){
      val result = actor ? "buy"
      val myorder = Await.result(result, 1600 seconds)
      println(myorder)
    }
  }

  createPurchase(20)

  actor ! "restock"

  createPurchase(10)
}

