package controllers.admin.system

import java.util.UUID

import controllers.BaseController
import models.Application
import models.auth.Credentials
import play.twirl.api.Html
import services.ServiceRegistry
import util.tracing.TraceData

import scala.concurrent.Future

@javax.inject.Singleton
class SearchController @javax.inject.Inject() (override val app: Application, services: ServiceRegistry) extends BaseController("search") {
  import app.contexts.webContext

  def search(q: String) = withSession("admin.search", admin = true) { implicit request => implicit td =>
    val creds = Credentials.fromRequest(request)
    val results = try {
      searchInt(creds, q, q.toInt)
    } catch {
      case _: NumberFormatException => try {
        searchUuid(creds, q, UUID.fromString(q))
      } catch {
        case _: IllegalArgumentException => searchString(creds, q)
      }
    }
    Future.successful(Ok(views.html.admin.explore.searchResults(q, results, request.identity)))
  }

  private[this] def searchInt(creds: Credentials, q: String, id: Int)(implicit timing: TraceData) = {
    // Start int searches
    val intSearches = Seq.empty[Seq[Html]]
    // End int searches

    intSearches.flatten
  }

  private[this] def searchUuid(creds: Credentials, q: String, id: UUID)(implicit timing: TraceData) = {
    // Start uuid searches

    val auditRecord = services.auditServices.auditRecordService.getByPrimaryKey(creds, id).map { model =>
      views.html.admin.audit.auditRecordSearchResult(model, s"Audit Record [${model.id}] matched [$q].")
    }.toSeq
    val note = services.noteServices.noteService.getByPrimaryKey(creds, id).map { model =>
      views.html.admin.note.noteSearchResult(model, s"Note [${model.id}] matched [$q].")
    }.toSeq
    val user = services.userServices.userService.getByPrimaryKey(creds, id).map { model =>
      views.html.admin.user.userSearchResult(model, s"User [${model.id}] matched [$q].")
    }.toSeq

    val uuidSearches = Seq[Seq[Html]](auditRecord, note, user)

    // End uuid searches

    val auditR = app.auditService.getByPrimaryKey(creds, id).map { model =>
      views.html.admin.audit.auditSearchResult(model, s"Audit [${model.id}] matched [$q].")
    }.toSeq

    (auditR +: uuidSearches).flatten
  }

  private[this] def searchString(creds: Credentials, q: String)(implicit timing: TraceData) = {
    // Start string searches

    val auditRecord = services.auditServices.auditRecordService.searchExact(creds = creds, q = q, limit = Some(5)).map { model =>
      views.html.admin.audit.auditRecordSearchResult(model, s"Audit Record [${model.id}] matched [$q].")
    }
    val note = services.noteServices.noteService.searchExact(creds = creds, q = q, limit = Some(5)).map { model =>
      views.html.admin.note.noteSearchResult(model, s"Note [${model.id}] matched [$q].")
    }
    val user = services.userServices.userService.searchExact(creds = creds, q = q, limit = Some(5)).map { model =>
      views.html.admin.user.userSearchResult(model, s"User [${model.id}] matched [$q].")
    }

    val stringSearches = Seq[Seq[Html]](auditRecord, note, user)

    // End string searches

    val auditR = app.auditService.searchExact(creds = creds, q = q, limit = Some(5)).map { model =>
      views.html.admin.audit.auditSearchResult(model, s"Audit [${model.id}] matched [$q].")
    }

    (auditR +: stringSearches).flatten
  }
}
