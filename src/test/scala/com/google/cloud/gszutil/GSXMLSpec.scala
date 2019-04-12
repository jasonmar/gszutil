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

import java.io.{File, FileInputStream}

import org.scalatest.FlatSpec

import scala.collection.JavaConverters._

class GSXMLSpec extends FlatSpec {
  "GSXML" should "upload" in {
    val bucket = "mybucket"
    val file = "GSZUTIL.JCL"
    val key = "xmltest"

    val gcs = GSXML.getClient(GSXML.DefaultCredentialProvider)
    val list = gcs.listBucketRecursive(bucket, maxKeys = 2)
    val keys = list.flatMap(_.Contents.asScala.map(_.Key))
    assert(keys.nonEmpty)

    val data = new FileInputStream(new File(file))
    val putRequest = gcs.putObject(bucket, key, data)
    val resp2 = putRequest.execute()

    assert(keys.mkString(", ") == "cse1, cse2, cse3, csek, testfile, xmltest")
    assert(resp2.getStatusCode == 200)
  }
}
