package org.archive.webservices.ars.model.collections.inputspecs

import java.io.InputStream

trait OneTimeAccess { this: FileRecord =>
  private var accessed = false

  def in: InputStream

  override def access: InputStream = {
    if (!accessed) {
      accessed = true
      in
    } else throw new UnsupportedOperationException("InputStream can only be accessed once.")
  }
}
