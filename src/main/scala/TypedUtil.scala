import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._

object TypedUtil {
  def actorRefToFlow[In, Out](
                               actorRef: ActorRef[In],
                               onInit: ActorRef[Out] => In,
                               onComplete: In,
                               queueSize: Int = 10,
                               overflowStrategy: OverflowStrategy = OverflowStrategy.fail
                             ): Flow[In, Out, NotUsed] = {
    val source: Source[Out, akka.NotUsed] = Source.actorRef(queueSize, OverflowStrategy.dropBuffer)
      .mapMaterializedValue { ref =>
        actorRef ! onInit(actorRefAdapter[Out](ref))
        akka.NotUsed
      }
    val sink = Sink.actorRef[In](actorRef.toUntyped, onComplete)

    Flow.fromSinkAndSource(sink, source)
  }
}
