/*
 * Copyright (C) 2013 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
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
import aQute.bnd.gradle.BundleTaskExtension
import org.gradle.process.CommandLineArgumentProvider

plugins {
  // java-library (not plain java) so slf4j-api can be an `api` dependency: it is
  // part of the public API (ProcessExecutor.info(org.slf4j.Logger), etc.) and
  // must land at compile scope in the published POM.
  `java-library`
  alias(libs.plugins.maven.publish)
  alias(libs.plugins.bnd)
}

group = "org.zeroturnaround"
version = "1.13-SNAPSHOT"
description = "A lightweight library to execute external processes from Java."

val moduleName = "org.zeroturnaround.exec"

repositories {
  mavenCentral()
}

java {
  // The main classes target Java 8 bytecode (slf4j 1.7 supports Java 8 and the
  // library uses no newer API), keeping pre-9 consumers working. The JPMS
  // module-info is shipped separately as a Java 9 multi-release entry (see below).
  // A toolchain is pinned (rather than the Java 8 floor) because compiling
  // module-info requires a JDK 9+ compiler; it is auto-provisioned via the foojay
  // resolver in settings.gradle.kts when not already installed.
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

dependencies {
  // slf4j-api is part of the public API, so it is an `api` dependency: it appears
  // at compile scope in the POM and on consumers' compile classpath.
  api(libs.slf4j.api)

  testImplementation(libs.junit)
  testImplementation(libs.logback.classic)
  testImplementation(libs.commons.lang3)
  testImplementation(libs.commons.io)
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

tasks.named<JavaCompile>("compileJava") {
  options.release.set(8)
}

// The JPMS descriptor lives in its own source set and is compiled as Java 9
// bytecode. It exports packages that live in the main source set, so the main
// classes are patched into the module and the API dependencies are put on the
// module path for the `requires` clauses to resolve.
sourceSets {
  create("moduleInfo") {
    java.srcDir("src/moduleInfo/java")
  }
}

val main = sourceSets.main.get()

// Supplies the module-info compile with the main classes (patched into the module
// so the exported packages resolve) and the API dependencies (on the module path so
// `requires` resolves). A managed type rather than a doFirst/closure so the inputs
// are tracked and the task stays configuration-cache compatible.
abstract class ModuleInfoCompileArgs : CommandLineArgumentProvider {
  @get:Input
  abstract val moduleName: Property<String>

  @get:CompileClasspath
  abstract val modulePath: ConfigurableFileCollection

  @get:Classpath
  abstract val patchModuleClasses: ConfigurableFileCollection

  override fun asArguments(): Iterable<String> = listOf(
    "--module-path", modulePath.asPath,
    "--patch-module", "${moduleName.get()}=${patchModuleClasses.asPath}",
  )
}

val execModuleName = moduleName
tasks.named<JavaCompile>("compileModuleInfoJava") {
  dependsOn(tasks.named("compileJava"))
  options.release.set(9)
  classpath = files()
  options.compilerArgumentProviders.add(
    objects.newInstance(ModuleInfoCompileArgs::class).apply {
      moduleName.set(execModuleName)
      modulePath.from(main.compileClasspath)
      patchModuleClasses.from(main.output.classesDirs)
    },
  )
}

tasks.jar {
  // Match the historical Maven build: no generated Maven descriptor in the jar.
  manifest {
    attributes(
      "Implementation-Title" to project.name,
      "Implementation-Version" to project.version,
      "Multi-Release" to "true",
    )
  }
  // Ship module-info.class as a Java 9 multi-release entry so the jar stays a
  // plain classpath jar on Java 8 and becomes a named module on Java 9+.
  into("META-INF/versions/9") {
    from(sourceSets["moduleInfo"].output)
  }
  // Produce the OSGi bundle manifest (Bundle-SymbolicName, Export-Package, and
  // the computed Import-Package) with bnd, reproducing the bnd-maven-plugin setup.
  val bundle = extensions.getByType<BundleTaskExtension>()
  bundle.setBnd(
    """
    Bundle-SymbolicName: org.zeroturnaround.zt-exec
    Export-Package: org.zeroturnaround.exec.*
    -exportcontents: org.zeroturnaround.exec.*
    """.trimIndent(),
  )
  bundle.classpath(main.compileClasspath)
}

// The Javadoc predates the strict doclint in JDK 8+; don't fail the build on it.
tasks.withType<Javadoc>().configureEach {
  (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// Several tests fork `java -cp target/test-classes <HelperClass>` and one runs in
// `new File("target")`, i.e. the layout the Maven build produced. Mirror the
// compiled test classes (and resources) there so the tests run unmodified.
val syncForkedProcessClasspath by tasks.registering(Sync::class) {
  from(sourceSets.test.get().output)
  into(layout.projectDirectory.dir("target/test-classes"))
}

tasks.withType<Test>().configureEach {
  dependsOn(syncForkedProcessClasspath)
  // The test sources are JUnit 4; Gradle picks JUnit 4 up automatically from the
  // test classpath, so no useJUnitPlatform() here.
  testLogging {
    events("passed", "skipped", "failed")
  }
}

// Sign only when in-memory keys are provided (CI release). Local builds skip signing.
tasks.withType<Sign>().configureEach {
  enabled = project.findProperty("signingInMemoryKey") != null
}

mavenPublishing {
  // The release workflow invokes `publishAndReleaseToMavenCentral`, which both
  // uploads and releases; no need to also auto-release from the plain task.
  publishToMavenCentral()
  signAllPublications()

  coordinates(group.toString(), "zt-exec", version.toString())

  pom {
    name.set("ZT Process Executor")
    description.set(project.description)
    url.set("https://github.com/zeroturnaround/zt-exec")
    organization {
      name.set("ZeroTurnaround")
      url.set("https://zeroturnaround.com/")
    }
    licenses {
      license {
        name.set("The Apache Software License, Version 2.0")
        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        comments.set("A business-friendly OSS license")
      }
    }
    developers {
      developer {
        id.set("nemecec")
        name.set("Neeme Praks")
        email.set("neeme@praks.net")
        url.set("https://github.com/nemecec")
      }
      developer {
        id.set("rein")
        name.set("Rein")
        email.set("rein@zeroturnaround.com")
        organization.set("ZeroTurnaround")
        organizationUrl.set("https://zeroturnaround.com")
      }
      developer {
        id.set("toomasr")
        name.set("Toomas")
        email.set("toomas@zeroturnaround.com")
        organization.set("ZeroTurnaround")
        organizationUrl.set("https://zeroturnaround.com")
      }
    }
    scm {
      url.set("https://github.com/zeroturnaround/zt-exec")
      connection.set("scm:git:git://github.com/zeroturnaround/zt-exec.git")
      developerConnection.set("scm:git:ssh://git@github.com/zeroturnaround/zt-exec.git")
    }
  }
}
