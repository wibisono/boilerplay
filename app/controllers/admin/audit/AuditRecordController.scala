/* Generated File */
package controllers.admin.audit

import controllers.BaseController
import io.circe.syntax._
import java.util.UUID
import models.Application
import models.audit.AuditRecordResult
import models.result.data.DataSummary._
import models.result.orderBy.OrderBy
import scala.concurrent.Future
import services.audit.AuditRecordService
import util.FutureUtils.defaultContext
import util.web.ControllerUtils.acceptsCsv

@javax.inject.Singleton
class AuditRecordController @javax.inject.Inject() (
    override val app: Application, svc: AuditRecordService
) extends BaseController("auditRecord") {
  def createForm = withSession("create.form", admin = true) { implicit request => implicit td =>
    val cancel = controllers.admin.audit.routes.AuditRecordController.list()
    val call = controllers.admin.audit.routes.AuditRecordController.create()
    Future.successful(Ok(views.html.admin.audit.auditRecordForm(
      request.identity, models.audit.AuditRecord.empty, "New Audit Record", cancel, call, isNew = true, debug = app.config.debug
    )))
  }

  def create = withSession("create", admin = true) { implicit request => implicit td =>
    svc.create(request, modelForm(request.body.asFormUrlEncoded)) match {
      case Some(model) => Future.successful(Redirect(controllers.admin.audit.routes.AuditRecordController.view(model.id)))
      case None => Future.successful(Redirect(controllers.admin.audit.routes.AuditRecordController.list()))
    }
  }

  def list(q: Option[String], orderBy: Option[String], orderAsc: Boolean, limit: Option[Int], offset: Option[Int]) = {
    withSession("list", admin = true) { implicit request => implicit td =>
      val startMs = util.DateUtils.nowMillis
      val orderBys = OrderBy.forVals(orderBy, orderAsc).toSeq
      val r = q match {
        case Some(query) if query.nonEmpty => svc.searchWithCount(request, query, Nil, orderBys, limit.orElse(Some(100)), offset)
        case _ => svc.getAllWithCount(request, Nil, orderBys, limit.orElse(Some(100)), offset)
      }
      Future.successful(render {
        case Accepts.Html() => Ok(views.html.admin.audit.auditRecordList(
          request.identity, Some(r._1), r._2, q, orderBy, orderAsc, limit.getOrElse(100), offset.getOrElse(0)
        ))
        case Accepts.Json() => Ok(AuditRecordResult.fromRecords(q, Nil, orderBys, limit, offset, startMs, r._1, r._2).asJson.spaces2).as(JSON)
        case acceptsCsv() => Ok(svc.csvFor("AuditRecord", r._1, r._2)).as("text/csv")
      })
    }
  }

  def autocomplete(q: Option[String], orderBy: Option[String], orderAsc: Boolean, limit: Option[Int]) = {
    withSession("autocomplete", admin = true) { implicit request => implicit td =>
      val orderBys = OrderBy.forVals(orderBy, orderAsc).toSeq
      val r = q match {
        case Some(query) if query.nonEmpty => svc.search(request, query, Nil, orderBys, limit.orElse(Some(5)), None)
        case _ => svc.getAll(request, Nil, orderBys, limit.orElse(Some(5)))
      }
      Future.successful(Ok(r.map(_.toSummary).asJson.spaces2).as(JSON))
    }
  }

  def byAuditId(auditId: UUID, orderBy: Option[String], orderAsc: Boolean, limit: Option[Int], offset: Option[Int]) = {
    withSession("get.by.auditId", admin = true) { implicit request => implicit td =>
      val orderBys = OrderBy.forVals(orderBy, orderAsc).toSeq
      val models = svc.getByAuditId(request, auditId, orderBys, limit, offset)
      Future.successful(render {
        case Accepts.Html() => Ok(views.html.admin.audit.auditRecordByAuditId(
          request.identity, auditId, models, orderBy, orderAsc, limit.getOrElse(5), offset.getOrElse(0)
        ))
        case Accepts.Json() => Ok(models.asJson.spaces2).as(JSON)
        case acceptsCsv() => Ok(svc.csvFor("AuditRecord by auditId", 0, models)).as("text/csv")
      })
    }
  }

  def view(id: java.util.UUID) = withSession("view", admin = true) { implicit request => implicit td =>
    val notes = app.noteService.getFor("auditRecord", id)
    svc.getByPrimaryKey(request, id) match {
      case Some(model) => Future.successful(render {
        case Accepts.Html() => Ok(views.html.admin.audit.auditRecordView(request.identity, model, notes, app.config.debug))
        case Accepts.Json() => Ok(model.asJson.spaces2).as(JSON)
      })
      case None => Future.successful(NotFound(s"No AuditRecord found with id [$id]."))
    }
  }

  def editForm(id: java.util.UUID) = withSession("edit.form", admin = true) { implicit request => implicit td =>
    val cancel = controllers.admin.audit.routes.AuditRecordController.view(id)
    val call = controllers.admin.audit.routes.AuditRecordController.edit(id)
    svc.getByPrimaryKey(request, id) match {
      case Some(model) => Future.successful(Ok(
        views.html.admin.audit.auditRecordForm(request.identity, model, s"Audit Record [$id]", cancel, call, debug = app.config.debug)
      ))
      case None => Future.successful(NotFound(s"No AuditRecord found with id [$id]."))
    }
  }

  def edit(id: java.util.UUID) = withSession("edit", admin = true) { implicit request => implicit td =>
    val fields = modelForm(request.body.asFormUrlEncoded)
    val res = svc.update(request, id = id, fields = fields)
    Future.successful(render {
      case Accepts.Html() => Redirect(controllers.admin.audit.routes.AuditRecordController.view(res._1.id)).flashing("success" -> res._2)
      case Accepts.Json() => Ok(res.asJson.spaces2).as(JSON)
    })
  }

  def remove(id: java.util.UUID) = withSession("remove", admin = true) { implicit request => implicit td =>
    svc.remove(request, id = id)
    Future.successful(render {
      case Accepts.Html() => Redirect(controllers.admin.audit.routes.AuditRecordController.list())
      case Accepts.Json() => Ok("{ \"status\": \"removed\" }").as(JSON)
    })
  }
}
