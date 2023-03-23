import akka.actor._

object Prob2 extends App {

  case class take_deck_shuffleNum_shuffleType(deck: List[Int], shuffleNum: Int, shuffleType: Boolean, requesterActor: ActorRef)
  case class shuffler_take_deck_shuffleNum_shuffleType_shuffleActor(deck: List[Int], shuffleNum: Int, shuffleType: Boolean, shuffleActor: ActorRef, requesterActor: ActorRef)
  case class take_deck_faroShufflerActor(deck: List[Int], faroShufflerActor: ActorRef)
  case class take_shuffleType_shuffleActor(shuffleType: Boolean, shuffleActor: ActorRef)
  case class faroShuffler_take_deck(deck: List[Int])
  case class cardCollector_take_card(card: Int)
  case class cardCollector_take_shufflerName_numCards(shuffleActor: ActorRef, numCards: Int)
  case class shuffler_take_deck(deck: List[Int])
  case class requester_take_deck(deck: List[Int])

  class CardCollector extends Actor {

    var numOfCards = -1
    var shuffleActorRef: ActorRef = null
    var combinedList: List[Int] = Nil
    var numCardsCount = 0

    def receive: PartialFunction[Any, Unit] = {

      case cardCollector_take_card(card) => {
        combinedList = combinedList ::: List(card)
        numCardsCount += 1
//        println(card)

        if (numCardsCount == numOfCards) {
          println("got here 5")

          if (shuffleActorRef == null) println("error")
          shuffleActorRef ! shuffler_take_deck(combinedList)
        }
      }

      case cardCollector_take_shufflerName_numCards(shuffleActor, numCards) => {
        println("got here 2")
        numOfCards = numCards
        shuffleActorRef = shuffleActor
      }
    }
  }

  class Splitter extends Actor {

    def receive: PartialFunction[Any, Unit] = {
      case take_deck_faroShufflerActor(deck, faroShufflerActor) =>  {
        var deckLength = deck.length
        var l1 = deck.take(deckLength/2)
        var l2 = deck.drop(deckLength/2)
        faroShufflerActor ! faroShuffler_take_deck(l1)
        faroShufflerActor ! faroShuffler_take_deck(l2)
      }
    }
  }

  class FaroShuffler extends Actor {

    var l1: List[Int] = Nil
    var l2: List[Int] = Nil
    var shuffleTypeRef = true
    var shuffleTypeInitialized = false
    var shuffleActorRef: ActorRef = null
    val cardCollectorActor = system.actorOf(Props[CardCollector], "cardCollectorActor")
    var combinedList: List[Int] = Nil

    def receive: PartialFunction[Any, Unit] = {
      case take_shuffleType_shuffleActor(shuffleType, shuffleActor) => {
        shuffleTypeRef = shuffleType
        shuffleActorRef = shuffleActor
      }

      case faroShuffler_take_deck(deck) => {
        println("got here 1")
        if (l1.isEmpty) l1 = deck
        else if (l2.isEmpty) l2 = deck


        if (!l1.isEmpty && !l2.isEmpty) {
//          println(l1)
//          println(l2)
          cardCollectorActor ! cardCollector_take_shufflerName_numCards(shuffleActorRef, l1.length + l2.length)

          for( a <- 0 until l1.length){
//            combinedList = combinedList ::: List(l1(a))
//            combinedList = combinedList ::: List(l2(a))
            cardCollectorActor ! cardCollector_take_card(l1(a))
            cardCollectorActor ! cardCollector_take_card(l2(a))
          }
        }
      }
    }
  }

  class Shuffler extends Actor {

    var shuffleActorRef: ActorRef = null
    val splitter = system.actorOf(Props[Splitter], "Splitter")
    val faroShuffler = system.actorOf(Props[FaroShuffler], "FaroShuffler")
    var requester: ActorRef = null
    var maxShuffleCount = -1

    def receive: PartialFunction[Any, Unit] = {
      case shuffler_take_deck_shuffleNum_shuffleType_shuffleActor(deck, shuffleNum, shuffleType, shuffleActor, requesterActor)=> {
        shuffleActorRef = shuffleActor
        maxShuffleCount = shuffleNum
        splitter ! take_deck_faroShufflerActor(deck, faroShuffler)
        faroShuffler ! take_shuffleType_shuffleActor(shuffleType, shuffleActorRef)
        requester = requesterActor
        maxShuffleCount = shuffleNum
      }

      case shuffler_take_deck(deck) => {
        println(deck)

        requester ! requester_take_deck(deck)
      }
    }
  }

  class Requester extends Actor {
    val shuffler = system.actorOf(Props[Shuffler], "Shuffler")

    def receive: PartialFunction[Any, Unit] = {

      case take_deck_shuffleNum_shuffleType(deck, shuffleNum, shuffleType, requester) => {
        shuffler ! shuffler_take_deck_shuffleNum_shuffleType_shuffleActor(deck, shuffleNum, shuffleType, shuffler, requester)
      }

      case requester_take_deck(deck) => {
        println("got here")
        println(deck)
        system.terminate()
      }
    }
  }

  val system = ActorSystem("ActorShuffling")
  val requester = system.actorOf(Props[Requester], "requester")
  var l1 = List(1, 2, 3, 4)
  requester ! take_deck_shuffleNum_shuffleType(l1, 1, true, requester)
}
