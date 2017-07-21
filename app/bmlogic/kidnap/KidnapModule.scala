package bmlogic.kidnap

import bminjection.db.DBTrait
import bmlogic.kidnap.KidnapData._
import bmlogic.kidnap.KidnapMessage._
import bmmessages.{CommonModules, MessageDefines}
import bmpattern.ModuleTrait
import bmutil.errorcode.ErrorCode
import com.mongodb.casbah.Imports._
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson

object KidnapModule extends ModuleTrait {
    def dispatchMsg(msg : MessageDefines)(pr : Option[Map[String, JsValue]])(implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = msg match {
        case msg_KidnapCanPush(data) => canPushService(data)(pr)
        case msg_KidnapPush(data) => pushService(data)
        case msg_KidnapCanPop(data) => canPopService(data)(pr)
        case msg_KidnapPop(data) => popService(data)
        case msg_KidnapDetail(data) => detailService(data)
        case msg_KidnapMultiQuery(data) => multiQueryService(data)
        case msg_KidnapSearch(data) => searchService(data)
        case msg_KidnapUpdate(data) => updateService(data)
        case msg_KidnapCanUpdate(data) => canUpdateService(data)(pr)
        case _ => ???
    }

    object inner_traits extends KidnapConditions
                           with KidnapDetailConditions
                           with KidnapSearchConditions
                           with KidnapMultiConditions
                           with KidnapResults

    def canPushService(data : JsValue)
                      (pr : Option[Map[String, JsValue]])
                      (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {

            val user = pr.get.get("profile").get
            val user_id = (user \ "user_id").asOpt[String].get
            val is_service_provider = (user \ "is_service_provider").asOpt[Int].get
            val owner_id = (data \ "service" \ "owner_id").asOpt[String].map (x => x).getOrElse(throw new Exception("push service input error"))

            if (user_id != owner_id) throw new Exception("only can push own service")
            else if (is_service_provider == 0) throw new Exception("only service provider can push service")
            else (pr, None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }

    def pushService(data : JsValue)
                   (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {
            val db = cm.modules.get.get("db").map (x => x.asInstanceOf[DBTrait]).getOrElse(throw new Exception("no db connection"))

            import inner_traits.pc
            val o : DBObject = data
            db.insertObject(o, "kidnap", "service_id")

            import inner_traits.dr
            val reVal = toJson(o - "date" - "update_date")
            (Some(Map("service" -> reVal)), None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }

    def canPopService(data : JsValue)
                     (pr : Option[Map[String, JsValue]])
                     (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {

            val user = pr.get.get("profile").get
            val user_id = (user \ "user_id").asOpt[String].get
            val owner_id = (data \ "condition" \ "owner_id").asOpt[String].map (x => x).getOrElse(throw new Exception("push service input error"))

            if (user_id != owner_id) throw new Exception("only can pop own service")
            else (pr, None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }

    def popService(data : JsValue)
                  (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {
            val db = cm.modules.get.get("db").map (x => x.asInstanceOf[DBTrait]).getOrElse(throw new Exception("no db connection"))

            import inner_traits.dc
            val o : DBObject = data
            db.deleteObject(o, "kidnap", "service_id")
            (Some(Map("pop" -> toJson("success"))), None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }

    }

    def canUpdateService(data : JsValue)
                        (pr : Option[Map[String, JsValue]])
                        (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {
        try {

            val user = pr.get.get("profile").get
            val user_id = (user \ "user_id").asOpt[String].get
            val owner_id = (data \ "condition" \ "owner_id").asOpt[String].map (x => x).getOrElse(throw new Exception("push service input error"))

            if (user_id != owner_id) throw new Exception("only can update own service")
            else (pr, None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }

    def updateService(data : JsValue)
                     (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {
            val db = cm.modules.get.get("db").map (x => x.asInstanceOf[DBTrait]).getOrElse(throw new Exception("no db connection"))

            import inner_traits.dc
            val o : DBObject = data
            val reVal = db.queryObject(o, "kidnap") { obj =>

                // TODO: 修改流程

                import inner_traits.dr
                obj - "date" - "update_date"
            }

            if (reVal.isEmpty) throw new Exception("service not exist")
            else (Some(Map("service" -> toJson(reVal))), None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }

    }

    def searchService(data : JsValue)
                     (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {
            val db = cm.modules.get.get("db").map (x => x.asInstanceOf[DBTrait]).getOrElse(throw new Exception("no db connection"))

            import inner_traits.sc
            import inner_traits.sr

            val skip = (data \ "skip").asOpt[Int].map (x => x).getOrElse(0)
            val take = (data \ "take").asOpt[Int].map (x => x).getOrElse(20)

            val o : DBObject = data
            val reVal = db.queryMultipleObject(o, "kidnap", skip = skip, take = take)

            (Some(Map("services" -> toJson(reVal))), None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }

    def detailService(data : JsValue)
                     (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {
            val db = cm.modules.get.get("db").map (x => x.asInstanceOf[DBTrait]).getOrElse(throw new Exception("no db connection"))

            import inner_traits.dc
            import inner_traits.dr
            val o : DBObject = data
            val reVal = db.queryObject(o, "kidnap")

            (Some(Map("service" -> toJson(reVal))), None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }

    def multiQueryService(data: JsValue)
                         (implicit cm : CommonModules) : (Option[Map[String, JsValue]], Option[JsValue]) = {

        try {
            val db = cm.modules.get.get("db").map (x => x.asInstanceOf[DBTrait]).getOrElse(throw new Exception("no db connection"))

            import inner_traits.mc
            import inner_traits.sr

            val o : DBObject = data
            val reVal = db.queryMultipleObject(o, "kidnap")

            (Some(Map("services" -> toJson(reVal))), None)

        } catch {
            case ex : Exception => (None, Some(ErrorCode.errorToJson(ex.getMessage)))
        }
    }
}