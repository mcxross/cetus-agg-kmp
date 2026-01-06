import com.android.build.api.dsl.androidLibrary
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.vanniktech.mavenPublish)
}

group = "xyz.mcxross.agg.cetus"

kotlin {
  jvm()
  androidLibrary {
    namespace = "xyz.mcxross.agg.cetus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    withJava()
    withHostTestBuilder {}.configure {}
    withDeviceTestBuilder { sourceSetTreeName = "test" }

    compilations.configureEach { compilerOptions.configure { jvmTarget.set(JvmTarget.JVM_17) } }
  }
  iosX64()
  iosArm64()
  iosSimulatorArm64()

  macosArm64()
  macosX64()
  tvosX64()
  tvosArm64()
  watchosArm32()
  watchosArm64()

  sourceSets {
    commonMain.dependencies {
      implementation(libs.ksui)
      implementation(libs.ktor.client.core)
      implementation(libs.kotlinx.serialization.json)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotest.assertions.core)
      implementation(libs.kotest.framework.engine)
      implementation(libs.kotlinx.coroutines.core)
    }

    jvmTest.dependencies { implementation(libs.kotest.runner.junit5) }
  }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

mavenPublishing {
  publishToMavenCentral(automaticRelease = true)

  signAllPublications()

  coordinates(group.toString(), "kmp", version.toString())

  pom {
    name = "Cetus Sui Aggregator SDK"
    description = "A library."
    inceptionYear = "2026"
    url = "https://github.com/mcxross/cetus-agg-kmp"
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("mcxross")
        name.set("Mcxross")
        email.set("oss@mcxross.xyz")
        url.set("https://mcxross.xyz/")
      }
    }
    scm {
      url.set("https://github.com/mcxross/cetus-agg-kmp")
      connection.set("scm:git:ssh://github.com/mcxross/cetus-agg-kmp.git")
      developerConnection.set("scm:git:ssh://github.com/mcxross/cetus-agg-kmp.git")
    }
  }
}
