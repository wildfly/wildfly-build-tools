
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


File provisionedServerDir = new File(basedir, "dist/target/it-type-classifier-dist-1.0-SNAPSHOT")
assert provisionedServerDir.exists()

assert new File(provisionedServerDir, "copied-artifacts/base-1.jar").exists()
//assert new File(provisionedServerDir, "copied-artifacts/base-2.jar").exists()
//assert new File(provisionedServerDir, "copied-artifacts/it-type-classifier-jar-1.0-SNAPSHOT.jar").exists()
//assert new File(provisionedServerDir, "copied-artifacts/it-type-classifier-jar-explicitly-typed.jar").exists()
//assert new File(provisionedServerDir, "copied-artifacts/it-type-classifier-jar-explicitly-typed-myclassifier.jar").exists()
assert new File(provisionedServerDir, "copied-artifacts/it-type-classifier-jar-myclassifier.jar").exists()
//assert new File(provisionedServerDir, "copied-artifacts/it-type-classifier-war-1.0-SNAPSHOT.war").exists()
//assert new File(provisionedServerDir, "copied-artifacts/it-type-classifier-war-myclassifier.war").exists()



assert new File(provisionedServerDir, "modules/org/wildfly/build/it-type-classifier-jar/main/it-type-classifier-jar-1.0-SNAPSHOT-myclassifier.jar").exists()
assert new File(provisionedServerDir, "modules/org/wildfly/build/it-type-classifier-jar/main/it-type-classifier-jar-1.0-SNAPSHOT.jar").exists()
assert new File(provisionedServerDir, "modules/org/wildfly/build/it-type-classifier-jar/main/module.xml").exists()
assert new File(provisionedServerDir, "modules/org/wildfly/build/it-type-classifier-jar-hardcoded-version/main/it-type-classifier-jar-1.0-SNAPSHOT-myclassifier.jar").exists()
assert new File(provisionedServerDir, "modules/org/wildfly/build/it-type-classifier-jar-hardcoded-version/main/it-type-classifier-jar-1.0-SNAPSHOT.jar").exists()
assert new File(provisionedServerDir, "modules/org/wildfly/build/it-type-classifier-jar-hardcoded-version/main/module.xml").exists()
