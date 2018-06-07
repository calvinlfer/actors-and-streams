package com.calvin.experiments

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.calvin.experiments.actors.MessageReceiver
import com.calvin.experiments.actors.MessageReceiver._

import scala.concurrent.duration._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("test-system")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  val receiverRef = system.actorOf(Props(new MessageReceiver), "message-receiver")

  val sink: Sink[ExampleMessage, NotUsed] = Sink.actorRefWithAck[ExampleMessage](
    ref = receiverRef,
    onInitMessage = StreamStarted,
    ackMessage = Ack,
    onCompleteMessage = StreamFinished,
    onFailureMessage = StreamFailure
  )

  val graph =
    Source(1 to 10000)
      .throttle(elements = 10, per = 1.second)
      .map(i => ExampleMessage(i, "example-payload"))
      .to(sink)

  graph.run()
}
