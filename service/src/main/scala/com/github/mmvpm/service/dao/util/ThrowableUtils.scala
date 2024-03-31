package com.github.mmvpm.service.dao.util

import org.postgresql.util.PSQLException

object ThrowableUtils {

  object DuplicateKeyException {

    def unapply(t: Throwable): Option[Throwable] = {
      val isPSQLException = t.isInstanceOf[PSQLException]
      val isDuplicateKey = t.getMessage.contains("duplicate key value violates unique constraint")
      if (isPSQLException && isDuplicateKey) Some(t) else None
    }
  }
}
