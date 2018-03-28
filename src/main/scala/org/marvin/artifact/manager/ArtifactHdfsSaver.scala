/*
 * Copyright [2017] [B2W Digital]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.marvin.artifact.manager

import java.io.{File, FileInputStream}

import akka.Done
import akka.actor.{Actor, ActorLogging}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.marvin.artifact.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.model.EngineMetadata

class ArtifactHdfsSaver(metadata: EngineMetadata) extends Actor with ActorLogging {
  var conf: Configuration = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    conf = new Configuration()

    if (sys.env.get("HADOOP_CONF_DIR") != None){
      val confFiles:List[File] = getListOfFiles(sys.env.get("HADOOP_CONF_DIR").mkString)

      for(file <- confFiles){
        log.info(s"Loading ${file.getAbsolutePath} file to hdfs client configuration ..")
        conf.addResource(new FileInputStream(file))
      }
    }

    conf.set("fs.defaultFS", metadata.hdfsHost)
  }

  def generatePaths(artifactName: String, protocol: String): Map[String, Path] = {
    Map(
      "localPath" -> new Path(s"${metadata.artifactsLocalPath}/${metadata.name}/$artifactName"),
      "remotePath" -> new Path(s"${metadata.artifactsRemotePath}/${metadata.name}/${metadata.version}/$artifactName/$protocol")
    )
  }

  def getListOfFiles(path: String): List[File] = {
    val dir = new File(path)
    val extensions = List("xml")
    dir.listFiles.filter(_.isFile).toList.filter { file =>
      extensions.exists(file.getName.endsWith(_))
    }
  }

  def validProtocol(protocol: Path): Boolean = {
    new java.io.File(protocol.toString).exists
  }

  override def receive: Receive = {
    case SaveToLocal(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val fs = FileSystem.get(conf)
      val uris = generatePaths(artifactName, protocol)

      if (!fs.exists(uris("remotePath"))) {
        log.error(s"Invalid protocol: ${protocol}, reload action canceled!")
      }
      else {
        log.info(s"Copying files from ${uris("remotePath")} to ${uris("localPath")}")
        fs.copyToLocalFile(false, uris("remotePath"), uris("localPath"), false)
        fs.close()
        log.info(s"File ${uris("localPath")} saved!")
      }

      sender ! Done

    case SaveToRemote(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val fs = FileSystem.get(conf)
      val uris = generatePaths(artifactName, protocol)

      if (!validProtocol(uris("localPath"))) {
        log.error(s"Invalid protocol: ${protocol}, reload action canceled!")
      }
      else {
        log.info(s"Copying files from ${uris("localPath")} to ${uris("remotePath")}")
        fs.copyFromLocalFile(uris("localPath"), uris("remotePath"))
        fs.close()
        log.info(s"File ${uris("localPath")} saved!")
      }

      sender ! Done

    case _ =>
      log.warning("Received a bad format message...")
  }
}
