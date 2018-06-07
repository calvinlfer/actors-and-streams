package com.calvin.experiments.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Timers}
import com.calvin.experiments.actors.MessageReceiver._

import scala.concurrent.duration._

class MessageReceiver extends Actor with ActorLogging with Timers {
  var buffer = Vector.empty[ExampleMessage]
  val MaxBufferSize = 10

  def bufferIsFull: Boolean = buffer.length >= MaxBufferSize

  def scheduleDelayedMessage(message: ExampleMessage, replyTo: ActorRef, delayDuration: FiniteDuration = 2.seconds): Unit =
    timers.startSingleTimer(s"message-${message.id}", DelayedMessage(message, delayDuration, replyTo), delayDuration)

  override def preStart(): Unit =
    timers.startPeriodicTimer(s"flush-timer-${self.path.name}", WriteBuffer, 10.seconds)

  override def receive: Receive = {
    case StreamStarted =>
      log.info(s"MessageReceiver: ${self.path.name} has started")
      sender() ! Ack

    case StreamFinished =>
      log.info(s"MessageReceiver: ${self.path.name} has completed, shutting down")
      context stop self

    case StreamFailure(throwable) =>
      log.error(throwable, s"MessageReceiver: ${self.path.name} has failed")
      throw throwable

    case m: ExampleMessage =>
      log.debug(s"Received message: $m")
      val originalSender = sender()

      // delay the message if the buffer is full
      if (bufferIsFull) {
        log.warning(s"MessageReceiver: ${self.path.name} buffer is full, delaying message")
        scheduleDelayedMessage(m, originalSender)
      } else {
        buffer = buffer :+ m
        originalSender ! Ack
      }

    case dm @ DelayedMessage(m, delayDuration, replyTo) =>
      log.debug(s"Received delayed message: $dm")

      if (bufferIsFull) {
        // We apply exponential back-off
        log.warning(s"MessageReceiver: ${self.path.name} buffer is still full, delaying message again")
        val twiceAsLongDelay = delayDuration * 2
        scheduleDelayedMessage(m, replyTo, twiceAsLongDelay)
      } else {
        buffer = buffer :+ m
        replyTo ! Ack
      }

    case WriteBuffer =>
      log.info(s"MessageReceiver: ${self.path.name} is writing buffer of ${buffer.length} elements, Range[${buffer.head.id}, ${buffer.last.id}]")
      buffer = Vector.empty
  }
}

object MessageReceiver {
  sealed trait Message
  case class ExampleMessage(id: Int, payload: String) extends Message
  private case class DelayedMessage(m: ExampleMessage, delayedFor: FiniteDuration, replyTo: ActorRef) extends Message

  // Lifecycle
  sealed trait Lifecycle
  case object Ack extends Lifecycle
  case object StreamStarted extends Lifecycle
  case object StreamFinished extends Lifecycle
  case class StreamFailure(t: Throwable) extends Lifecycle

  // Timer
  private case object WriteBuffer
}