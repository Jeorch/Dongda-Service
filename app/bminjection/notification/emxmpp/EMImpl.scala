package bminjection.notification.emxmpp

import akka.actor.{ActorSystem, Props}
import bminjection.notification.DDNTrait
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await

trait EMImpl extends DDNTrait {

    implicit val time_out = Timeout(3 second)

    initDDN
    def initDDN = EMNotification.getAuthTokenForEM

    def notifyAsync(parameters : (String, JsValue)*)(implicit as : ActorSystem) = {
        val a = as.actorOf(Props[EMActor])
        a ! DDNNotifyUsers(parameters.toList)
        as.stop(a)
    }

    def registerForDDN(user_id : String)(implicit as : ActorSystem) : JsValue = {
        val a = as.actorOf(Props[EMActor])
        val f = a ? DDNRegisterUser("username" -> toJson(user_id),
                                    "password" -> toJson(EMNotification.notification_password),
                                    "nickname" -> toJson(user_id))

        Await.result(f.mapTo[JsValue], time_out.duration)
    }
}
