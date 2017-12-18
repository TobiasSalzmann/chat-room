import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}

object ChatRoom {

  sealed trait ChatCommand
  final case class GetSession(screenName: String, replyTo: ActorRef[ChatEvent]) extends ChatCommand
  final case class CloseSession(screenName: String) extends ChatCommand
  final case class PostMessage(screenName: String, message: String) extends ChatCommand

  sealed trait ChatEvent
  final case class SessionGranted(screenName: String) extends ChatEvent
  final case class SessionClosed(screenName: String) extends ChatEvent
  final case class MessagePosted(screenName: String, message: String) extends ChatEvent

  def behavior(sessions: Map[String, ActorRef[ChatEvent]], events: Seq[ChatEvent] = Nil): Behavior[ChatCommand] = {

    def broadcast(event: ChatEvent): Unit = {
      sessions.values.foreach(_ ! event)
    }

    Actor.immutable[ChatCommand] { (_, command) ⇒
      command match {
        case GetSession(screenName, clientRef) ⇒
          val granted = SessionGranted(screenName)
          broadcast(granted)
          events.foreach(it => clientRef ! it)
          behavior(sessions + (screenName -> clientRef), events :+ granted)
        case CloseSession(screenName) ⇒
          val closed = SessionClosed(screenName)
          broadcast(closed)
          behavior(sessions - screenName, events :+ closed)
        case PostMessage(screenName, message) ⇒
          val posted = MessagePosted(screenName, message)
          broadcast(posted)
          behavior(sessions, events :+ posted)
      }
    }
  }
}
