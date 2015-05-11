package suggestions



import scala.collection._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}
import scala.swing.event.Event
import scala.swing.Reactions.Reaction
import rx.lang.scala._
import org.scalatest._
import gui._
import rx.lang.scala.Observable._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SwingApiTest extends FunSuite {

  object swingApi extends SwingApi {
    class ValueChanged(val textField: TextField) extends Event

    object ValueChanged {
      def unapply(x: Event) = x match {
        case vc: ValueChanged => Some(vc.textField)
        case _ => None
      }
    }

    class ButtonClicked(val source: Button) extends Event

    object ButtonClicked {
      def unapply(x: Event) = x match {
        case bc: ButtonClicked => Some(bc.source)
        case _ => None
      }
    }

    class Component {
      private val subscriptions = mutable.Set[Reaction]()
      def subscribe(r: Reaction) {
        subscriptions add r
      }
      def unsubscribe(r: Reaction) {
        subscriptions remove r
      }
      def publish(e: Event) {
        for (r <- subscriptions) r(e)
      }
    }

    class TextField extends Component {
      private var _text = ""
      def text = _text
      def text_=(t: String) {
        _text = t
        publish(new ValueChanged(this))
      }
    }

    class Button extends Component {
      def click() {
        publish(new ButtonClicked(this))
      }
    }
  }

  import swingApi._
  
  test("SwingApi should emit text field values to the observable") {
    val textField = new swingApi.TextField
    val values = textField.textValues

    val observed = mutable.Buffer[String]()
    val sub = values subscribe {
      observed += _
    }

    // write some text now
    textField.text = "T"
    textField.text = "Tu"
    textField.text = "Tur"
    textField.text = "Turi"
    textField.text = "Turin"
    textField.text = "Turing"

    assert(observed == Seq("T", "Tu", "Tur", "Turi", "Turin", "Turing"), observed)
  }

  test("SwingApi should stop emitting text values after unsubscribe") {
    val textField = new swingApi.TextField
    val values = textField.textValues

    val observed = mutable.Buffer[String]()
    val sub = values subscribe {
      observed += _
    }

    // write some text now
    textField.text = "T"
    textField.text = "Tu"
    textField.text = "Tur"
    textField.text = "Turi"

    assert(!sub.isUnsubscribed)
    sub.unsubscribe()
    assert(sub.isUnsubscribed)

    textField.text = "Turin"
    textField.text = "Turing"

    assert(observed == Seq("T", "Tu", "Tur", "Turi"), observed)
  }

  test("SwingApi should emit clicks to the observable") {
    val btn = new swingApi.Button
    val clicks = btn.clicks

    val observed = mutable.Buffer[swingApi.Button]()
    val sub = clicks subscribe {
      observed += _
    }

    // write some text now
    val count = 3
    for (i <- 1 to count)
      btn.click()

    assert(observed == (1 to count).map(_ => btn), observed)
  }

  test("SwingApi should stop emitting clicks after unsubscribe") {
    val btn = new swingApi.Button
    val clicks = btn.clicks

    val observed = mutable.Buffer[swingApi.Button]()
    val sub = clicks subscribe {
      observed += _
    }

    // write some text now
    val count = 3
    for (i <- 0 to count)
      btn.click()

    assert(!sub.isUnsubscribed)
    sub.unsubscribe()
    assert(sub.isUnsubscribed)

    btn.click()

    assert(observed == (0 to count).map(_ => btn), observed)
  }

}
