package com.github.mmvpm.stub.dao.table

import com.github.mmvpm.model.Stub

import java.util.UUID

case class StubEntry(id: UUID, data: String) {
  def toStub: Stub = Stub(id, data)
}
