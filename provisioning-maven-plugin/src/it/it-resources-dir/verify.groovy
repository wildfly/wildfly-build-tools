import org.apache.commons.io.FileUtils;

/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

String rootArtifactId = basedir.getName()

Assertions assertions = new Assertions(basedir, rootArtifactId, "feature-pack")
assertions.assertExpected("configuration/standalone/subsystems.xml")
assertions.assertExpected("configuration/standalone/template.xml")
assertions.assertExpected("subsystem-templates/example-subsystem.xml")
assertions.assertExpected("wildfly-feature-pack.xml")

assertions = new Assertions(basedir, rootArtifactId, "dist")
assertions.assertExpected("standalone/configuration/standalone.xml")

public class Assertions {
    File actualRootDir
    File expectedRootDir
    Assertions(File basedir, String artifactId, String suffix) {
        this.actualRootDir = new File(basedir, "${suffix}/target/${artifactId}-${suffix}-1.0-SNAPSHOT")
        this.expectedRootDir = new File(basedir, "${suffix}/it-expected")
    }
    void assertExpected(String path) {
        File expected = new File(expectedRootDir, path)
        File actual = new File(actualRootDir, path)
        assert expected.exists()
        assert actual.exists()
        assert org.codehaus.plexus.util.FileUtils.contentEquals(expected, actual)
    }
}
