import ChatRoom._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter._


object Server extends App {
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()

  private val chatRoomRef: ActorRef[ChatCommand] = system.spawn(ChatRoom.behavior(Map.empty), "chatroom")

  def chatController(name: String, chatRoomRef: ActorRef[ChatCommand]): Flow[Message, Message, Any] = {
    Flow.fromFunction[Message, ChatCommand](message => PostMessage(name, message.asTextMessage.getStrictText))
      .via(TypedUtil.actorRefToFlow[ChatCommand, ChatEvent](
        actorRef = chatRoomRef,
        onInit = (clientRef) => GetSession(name, clientRef),
        onComplete = CloseSession(name),
        queueSize = 100
      ))
      .via(Flow.fromFunction(event => TextMessage(event.toString)))
  }

  val websocketRoute: Route = pathPrefix("chat" / Segment) {
    (name) => handleWebSocketMessages(chatController(name, chatRoomRef))
  }

  val bindingFuture = Http().bindAndHandle(websocketRoute, "localhost", 5678)
  val bindingFuture2 = Http().bindAndHandle(websocketRoute, "localhost", 5679)
}
