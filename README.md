# kentech

To make a Jar of the app use sbt assembly.

The App can customize by modifying the MainApp.scala

To start the app you can use:
The function createPurchase(n) will try to simulate the transaction. (n is the number of transanctions.)
Or actor ! "restock" this will refresh the stock of the groceries.

Other thing that can be change is the amount of actors (provider, store, cashiers, groceries) 
this settings are in the application.conf

Note: Most of the stuff can escalate using DB, messageQueue, 
and more complex case class (I mean for the groceries made a single List of object can be done with a List[List[groceries]])
