# Copyright [2017] [B2W Digital]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

.PHONY: help package test clean

SBT_VERSION?=0.13.9

help:
	@echo "    package"
	@echo "        Builds a uber-jar with all engine-executor code and it's dependencies."
	@echo "    test"
	@echo "        Run all marvin engine-executor unit tests."
	@echo "    clean"
	@echo "        Clean the build artifacts."

package:
		sbt/sbt assembly

test:
		sbt/sbt coverage test coverageReport

clean:
		sbt/sbt clean

sonatype-publish:
    sbt/sbt publishSigned

sonatype-createfile:
  	echo "credentials += Credentials(\"Sonatype Nexus Repository Manager\",\"oss.sonatype.org\",\"(Sonatype user name)\",\"(Sonatype password)\")" >> $HOME/.sbt/$(SBT_VERSION)/sonatype.sbt

