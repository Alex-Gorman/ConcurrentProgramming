import akka.actor._

object Prob extends App {

  case class client_take_listUnsorted_actorRef(lst: List[Int], actor: ActorRef)
  case class child_take_val_actorRef(v: Int, actor: ActorRef, actor2: ActorRef)
  case class take_sorted(lst: List[Int])

  class Child extends Actor {
    var smallest = 0
    val child = system.actorOf(Props(classOf[Child]))
    var hasChild = false
    var parentRef: ActorRef = null

    def receive: PartialFunction[Any, Unit] = {

      case child_take_val_actorRef(v, actor, actor2) => {
        parentRef = actor

        if (v == 0) {
          if (hasChild) child ! child_take_val_actorRef(v, actor2, child)
          else {
            parentRef ! take_sorted(List(smallest))
          }
        } else if (smallest == 0) smallest = v
        else {
          hasChild = true
          if (smallest > v) {
            val tmp = smallest
            smallest = v
            child ! child_take_val_actorRef(tmp, actor2, child)
          } else {
            child ! child_take_val_actorRef(v, actor2, child)
          }
        }
      }

      case take_sorted(lst) => {
        parentRef ! take_sorted(smallest::lst)
      }
    }

  }

  class Client extends Actor {
    val child = system.actorOf(Props[Child])

    def receive: PartialFunction[Any, Unit] = {

      case client_take_listUnsorted_actorRef(lst, actor) => {
        var i = 0
        while (i < lst.length) {
          child ! child_take_val_actorRef(lst(i), actor, child)
          i += 1
        }
      }

      case take_sorted(lst) => {
        println("The sorted list is :"+lst)
        system.terminate()
      }
    }
  }

  val system = ActorSystem("ActorSorting")
  val client = system.actorOf(Props[Client], "client")
  val l1 = List(6, 3, 1, 0)
  client ! client_take_listUnsorted_actorRef(l1, client)
}