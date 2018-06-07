# Example: Integrating Akka Actors with Akka Streams #

The purpose of this example is to show how to integrate a stateful actor which uses batching behavior and needs to 
back-pressure upstream if it receives too many elements whilst it waits to deposit those elements elsewhere. The act of
depositing elements elsewhere is done based on a timer. We send elements to this Actor and require back-pressure so that
no elements are lost or elements are not sent too quickly. The idea behind this is that the Actor may have to poll a 
data store periodically and store these elements in that data store. There could be a constraint on that data-store 
which only allows you to push X elements and no more. So we want to respect those constraints and signal demand
appropriately upstream so that we don't overwhelm the data-store.

The `MessageReceiver` actor is responsible for modelling the interaction point between the theoretical data store and 
the stream. You can imagine that we could be pulling data off a message-queue and feeding those elements downstream. 
This actor hold an internal buffer and attempts to batch up 10 elements (and no more) in the internal buffer and writes 
the content of the buffer out every 10 seconds. Note that when the internal buffer is full, we do not send the 
acknowledge message back to the sender (which causes the back-pressure to happen). We set a timer and delay the message
in the hopes that when the message is received again, the buffer will be cleared and only then do we add it to the
internal buffer .

If you run the application, it will set up a Graph and run it where the upstream emits elements at a far greater pace 
than the Actor Sink can handle. However, due to back-pressure, the Actor Sink is able to properly signal demand to the 
Source without getting overwhelmed and the entire stream conforms to the pace of the slowest moving part (in this case, 
the `MessageReceiver`).
