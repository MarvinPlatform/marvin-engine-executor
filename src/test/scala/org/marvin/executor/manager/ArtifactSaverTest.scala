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
package org.marvin.executor.manager

import org.marvin.manager.ArtifactSaver
import org.marvin.model.EngineMetadata
import org.scalatest.{Matchers, WordSpec}

class ArtifactSaverTest extends WordSpec with Matchers {

  "A engineMetadata with artifactsSaverType as HDFS" should {
    "return Props with actorClass ArtifactHdfsSaver" in {
      val props = ArtifactSaver.build(new EngineMetadata("name",
        "version", "engineType", null, "artifactsRemotePath", "HDFS", "marvin-artifact-bucket", null,
        3000, 3000, 3000, Option(3000), 3000, "testHost"))
      assert(props.actorClass().toString == "class org.marvin.manager.ArtifactHdfsSaver")
    }
  }

  "A engineMetadata with artifactsSaverType as S3" should {
    "return Props with actorClass ArtifactS3Saver" in {
      val props = ArtifactSaver.build(new EngineMetadata("name",
        "version", "engineType", null, "artifactsRemotePath", "S3", "marvin-artifact-bucket", null,
        3000, 3000, 3000, Option(3000), 3000, "testHost"))
      assert(props.actorClass().toString == "class org.marvin.manager.ArtifactS3Saver")
    }
  }

  "A engineMetadata with artifactsSaverType as FS" should {
    "return Props with actorClass ArtifactFSSaver" in {
      assert(props3.actorClass().toString == "class org.marvin.manager.ArtifactFSSaver")
    }
  }
}
