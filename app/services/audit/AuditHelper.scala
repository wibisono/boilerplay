package services.audit

import java.util.UUID

import models.audit._
import models.auth.Credentials
import models.result.data.DataField
import util.tracing.TraceData
import util.{Logging, NullUtils}

object AuditHelper extends Logging {
  private[this] var inst: Option[AuditService] = None
  private[this] def getInst = inst.getOrElse(throw new IllegalStateException("Not initialized."))

  def init(service: AuditService) = {
    inst.foreach(_ => throw new IllegalStateException("Double init."))
    inst = Some(service)
  }

  def onAudit(audit: Audit)(implicit trace: TraceData) = getInst.callback(audit)

  def onStart(creds: Credentials, id: UUID, msg: AuditStart)(implicit traceData: TraceData) = getInst.cache.onStart(creds, id, msg)
  def onComplete(creds: Credentials, msg: AuditComplete)(implicit traceData: TraceData) = getInst.cache.onComplete(creds, msg)

  def onInsert(t: String, pk: Seq[String], fields: Seq[DataField], creds: Credentials)(implicit trace: TraceData) = {
    val msg = s"Inserted new [$t] with [${fields.size}] fields:"
    val auditId = UUID.randomUUID
    val records = Seq(AuditRecord(auditId = auditId, t = t, pk = pk, changes = fields.map(f => AuditField(f.k, None, f.v))))
    onAudit(Audit(id = auditId, act = "insert", client = creds.remoteAddress, userId = creds.user.id, msg = msg, records = records))
  }

  def onUpdate(
    t: String, pk: Seq[DataField], originalFields: Seq[DataField], newFields: Seq[DataField], creds: Credentials
  )(implicit trace: TraceData) = {
    def changeFor(f: DataField) = originalFields.find(_.k == f.k).flatMap {
      case o if f.v != o.v => Some(AuditField(f.k, o.v, f.v))
      case _ => None
    }
    val changes = newFields.flatMap(changeFor)
    val msg = s"Updated [${changes.size}] fields of $t[${pk.map(id => id.k + ": " + id.v.getOrElse(NullUtils.str)).mkString(", ")}]:\n"
    val auditId = UUID.randomUUID
    val records = Seq(AuditRecord(auditId = auditId, t = t, changes = changes))
    onAudit(Audit(id = auditId, act = "update", client = creds.remoteAddress, userId = creds.user.id, msg = msg, records = records))
  }

  def onRemove(t: String, pk: Seq[String], fields: Seq[DataField], creds: Credentials)(implicit trace: TraceData) = {
    val msg = s"Removed [$t] with [${fields.size}] fields:"
    val auditId = UUID.randomUUID
    val records = Seq(AuditRecord(auditId = auditId, t = t, pk = pk, changes = fields.map(f => AuditField(f.k, None, f.v))))
    onAudit(Audit(id = auditId, act = "remove", client = creds.remoteAddress, userId = creds.user.id, msg = msg, records = records))
  }

  def pk(t: String, v: Any*) = AuditModelPk(t, v.map(_.toString))
  def auditStart(models: AuditModelPk*)(creds: Credentials, act: String = "development", tags: Map[String, String] = Map.empty) = {
    AuditStart(action = act, app = Some(util.Config.projectId), client = creds.remoteAddress, tags = tags, models = models)
  }
}
