package zukaufen.http

import com.twitter.finagle.http.path.{/, Path, Root}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Service, http}
import com.twitter.util.Future
import com.typesafe.config.Config
import org.json4s.native.Serialization.write
import zukaufen.core.ShoppingListManagerComponent
import zukaufen.dao.Item
import zukaufen.dto.{DiffListDto, ListDto}
import zukaufen.errors.ClientError
import zukaufen.utils.HttpUtils
import zukaufen.utils.HttpUtils._


/** Finagle HTTP service for operations with shopping lists */
class ListsService(config: Config) extends Service[http.Request, http.Response]
  with ShoppingListManagerComponent {

  override def apply(request: Request): Future[Response] = {
    val uid = HttpUtils.getUid(request).get

    Path(request.path) match {
      case Root / "api" / "lists" / name =>
        request.method match {
          case Method.Post => createList(uid, name)
          case _ => throw new ClientError("Unsupported method")
        }

      case Root / "api" / "lists" =>
        request.method match {
          case Method.Get => getLists(uid)
          case Method.Post => mergeLists(uid, request)
          case _ => throw new ClientError("Unsupported method")
        }
    }
  }

  /**
    * Retrieves lists of a user.
    *
    * @param uid user ID
    * @return HTTP response with the following body:
    *         [
    *         {
    *         "id": (string),
    *         "name": (string),
    *         "items": {
    *         "id": (string),
    *         "name": (string),
    *         "qty": (string, optional),
    *         "checked": (boolean),
    *         "ts": (timestamp)>,
    *         }
    *         "ts": (timestamp)
    *         }
    *         ]
    */
  private def getLists(uid: String): Future[Response] = {
    Future.apply {
      val lists = shoppingListManager.getLists(uid)
      val items: String => Option[Item] = shoppingListManager.getItem(_)

      val response = new Response.Ok()
      response.setContentTypeJson()
      response.setContentString(write(lists.map { list =>
        ListDto(list, items)
      })(formats = defaultFormats))

      response
    }
  }

  /**
    * Merges shopping lists diff to backend.
    *
    * @param uid     user ID
    * @param request HTTP request with shopping list diff as JSON
    * @return HTTP 200
    */
  private def mergeLists(uid: String, request: Request): Future[Response] = {
    Future.apply {
      val diffListDto = HttpUtils.parseJsonFromHttpMessage[List[DiffListDto]](request)
      diffListDto.foreach(shoppingListManager.mergeList(uid, _))

      new Response.Ok()
    }
  }

  /**
    * Creates a new list.
    *
    * @param uid  user ID
    * @param name list name
    * @return list ID
    */
  private def createList(uid: String, name: String): Future[Response] = {
    Future.apply {
      val listId = shoppingListManager.createList(name, uid)

      val response = new Response.Ok()
      response.setContentString(listId)
      response
    }
  }
}