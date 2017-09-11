package bmlogic.auth

import play.api.libs.json.JsValue
import com.pharbers.bmmessages.CommonMessage

abstract class msg_AuthCommand extends CommonMessage(cat ="auth", mt = AuthModule)

object AuthMessage {
	case class msg_AuthLogin(data : JsValue) extends msg_AuthCommand
	case class msg_AuthQuery(data : JsValue) extends msg_AuthCommand
	case class msg_AuthTokenParser(data : JsValue) extends msg_AuthCommand
	case class msg_CheckTokenExpire(data : JsValue) extends msg_AuthCommand
	case class msg_GenerateToken() extends msg_AuthCommand
}
