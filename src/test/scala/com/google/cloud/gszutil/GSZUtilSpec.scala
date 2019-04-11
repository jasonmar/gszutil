/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.gszutil

import org.scalatest.FlatSpec

class GSZUtilSpec extends FlatSpec {
  "GSZUtil" should "parse gs path" in {
    val gsUri = "gs://bucket/path/to/object"
    val (bucket,path) = GSZUtil.parseUri(gsUri)
    assert(bucket == "bucket")
    assert(path == "path/to/object")
  }

  it should "parse args" in {
    val args = "cp DATASET.RECORD gs://bucket/DATASET.RECORD".split(" ")
    val parsed = GSZUtil.Parser.parse(args, GSZUtil.Config())
    assert(parsed.isDefined)
    assert(parsed.get.dest == "gs://bucket/DATASET.RECORD")
    assert(parsed.get.dsn == "DATASET.RECORD")
    assert(parsed.get.mode == "cp")
  }
}
