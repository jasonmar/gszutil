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

import java.io.{ByteArrayInputStream, InputStream, StringReader}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{PrivateKey, Security}
import java.util.Collections
import java.util.logging.{ConsoleHandler, Level, Logger}

import com.google.api.client.auth.oauth2.{BearerToken, Credential}
import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential, GoogleOAuthConstants}
import com.google.api.client.googleapis.util.Utils
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonObjectParser
import com.google.api.client.util.{PemReader, SecurityUtils}
import com.google.cloud.gszutil.GSXML.CredentialProvider
import com.google.cloud.gszutil.KeyFileProto.KeyFile

import scala.util.{Failure, Try}

object Util {
  def parseUri(gsUri: String): (String,String) = {
    if (gsUri.substring(0, 5) != "gs://") {
      ("", "")
    } else {
      val dest = gsUri.substring(5, gsUri.length)
      val bucket = dest.substring(0, dest.indexOf('/'))
      val path = dest.substring(dest.indexOf('/')+1, dest.length)
      (bucket, path)
    }
  }

  def printDebugInformation(): Unit = {
    import scala.collection.JavaConverters._
    System.out.println("\n\nSystem Properties:")
    System.getProperties.list(System.out)
    System.out.println("\n\nEnvironment Variables:")
    System.getenv.asScala.toMap.foreach{x =>
      System.out.println(s"${x._1}=${x._2}")
    }
  }

  def configureBouncyCastleProvider(): Unit = {
    Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1)
  }

  def configureLogging(level: Level = Level.ALL): Unit = {
    val logHandler = new ConsoleHandler
    logHandler.setLevel(level)
    val httpLogger = Logger.getLogger(classOf[HttpTransport].getName)
    httpLogger.setLevel(level)
    httpLogger.addHandler(logHandler)
  }

  val StorageScope: java.util.Collection[String] = Collections.singleton("https://www.googleapis.com/auth/devstorage.read_write")

  def readNio(path: String): Array[Byte] = {
    Files.readAllBytes(Paths.get(path))
  }

  def readCredentials(json: InputStream): GoogleCredential = {
    val parsed = new JsonObjectParser(Utils.getDefaultJsonFactory).parseAndClose(json, StandardCharsets.UTF_8, classOf[ServiceAccountCredential])
    new GoogleCredential.Builder()
      .setTransport(Utils.getDefaultTransport)
      .setJsonFactory(Utils.getDefaultJsonFactory)
      .setServiceAccountId(parsed.getClientEmail)
      .setServiceAccountScopes(StorageScope)
      .setServiceAccountPrivateKey(privateKey(parsed.getPrivateKeyPem))
      .setServiceAccountPrivateKeyId(parsed.getPrivateKeyId)
      .setTokenServerEncodedUrl(parsed.getTokenUri)
      .setServiceAccountProjectId(parsed.getProjectId)
      .build()
  }

  def convertJson(json: InputStream): KeyFile = {
    val parsed = new JsonObjectParser(Utils.getDefaultJsonFactory).parseAndClose(json, StandardCharsets.UTF_8, classOf[ServiceAccountCredential])
    KeyFile.newBuilder()
      .setType(parsed.getKeyType)
      .setProjectId(parsed.getProjectId)
      .setPrivateKeyId(parsed.getPrivateKeyId)
      .setPrivateKey(parsed.getPrivateKeyPem)
      .setClientEmail(parsed.getClientEmail)
      .setClientId(parsed.getClientId)
      .setAuthUri(parsed.getAuthUri)
      .setTokenUri(parsed.getTokenUri)
      .setAuthProviderX509CertUrl(parsed.getAuthProviderX509CertUrl)
      .setClientX509CertUrl(parsed.getClientX509CertUrl)
      .build()
  }

  def readPbCredentials(bytes: Array[Byte]): GoogleCredential = {
    val keyFile = KeyFile.parseFrom(bytes)
    new GoogleCredential.Builder()
      .setTransport(Utils.getDefaultTransport)
      .setJsonFactory(Utils.getDefaultJsonFactory)
      .setServiceAccountId(keyFile.getClientEmail)
      .setServiceAccountScopes(StorageScope)
      .setServiceAccountPrivateKey(privateKey(keyFile.getPrivateKey))
      .setServiceAccountPrivateKeyId(keyFile.getPrivateKeyId)
      .setTokenServerEncodedUrl(keyFile.getTokenUri)
      .setServiceAccountProjectId(keyFile.getProjectId)
      .build()
  }

  def accessTokenCredentials(token: String): Credential = {
    val cred = new Credential(BearerToken.authorizationHeaderAccessMethod())
    cred.setAccessToken(token)
    cred
  }

  def privateKey(privateKeyPem: String): PrivateKey = {
    SecurityUtils.getRsaKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(PemReader
      .readFirstSectionAndClose(new StringReader(privateKeyPem), "PRIVATE KEY")
      .getBase64DecodedBytes))
  }

  def printException[T](x: Try[T]): Unit = {
    x match {
      case Failure(exception) =>
        System.err.println(exception.getMessage)
        exception.printStackTrace(System.err)
      case _ =>
    }
  }

  case class PBCredentialProvider(pb: Array[Byte]) extends CredentialProvider {
    override def getCredential: Credential =
      readPbCredentials(pb)
  }

  case class AccessTokenCredentialProvider(token: String) extends CredentialProvider {
    override def getCredential: Credential =
      accessTokenCredentials(token)
  }
}
