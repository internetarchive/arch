package org.archive.webservices.ars.util

import com.fasterxml.uuid.Generators

object UUID {
  def uuid7 = {
    // see https://github.com/cowtowncoder/java-uuid-generator
    Generators.timeBasedEpochGenerator().generate()
  }

  def uuid7str: String = uuid7.toString
}
