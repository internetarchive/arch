package org.archive.webservices.ars

import org.archive.webservices.ars.model.LocalArchConf

class LocalArchConfSpec extends UnitSpec {
  setEnv("ARCH_BASE_PATH", "/")
  setEnv("ARCH_PROTO", "http")
  setEnv("ARCH_HOST", "arch.archive-it.org")
  setEnv("ARCH_EXTERNAL_PORT", "80")

  def conf = {
    mock[LocalArchConf]
  }

  test("baseUrl excludes port when proto=http and port=80") {
    setEnv("ARCH_PROTO", "http")
    setEnv("ARCH_EXTERNAL_PORT", "80")
    conf.baseUrl should be ("http://arch.archive-it.org")
  }

  test("baseUrl excludes port when proto=https and port=443") {
    setEnv("ARCH_PROTO", "https")
    setEnv("ARCH_EXTERNAL_PORT", "443")
    conf.baseUrl should be ("https://arch.archive-it.org")
  }

  test("baseUrl includes port when proto=http and port!=80") {
    setEnv("ARCH_PROTO", "http")
    setEnv("ARCH_EXTERNAL_PORT", "81")
    conf.baseUrl should be ("http://arch.archive-it.org:81")
  }

  test("baseUrl includes port when proto=https and port!=443") {
    setEnv("ARCH_PROTO", "https")
    setEnv("ARCH_EXTERNAL_PORT", "444")
    conf.baseUrl should be ("https://arch.archive-it.org:444")
  }
}
