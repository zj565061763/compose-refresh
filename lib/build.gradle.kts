plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  `maven-publish`
}

val libGroupId = "com.sd.lib.android"
val libArtifactId = "compose-refresh"
val libVersionName = "1.1.0-beta02"

android {
  namespace = "com.sd.lib.compose.refresh"
  compileSdk = libs.versions.androidCompileSdk.get().toInt()
  defaultConfig {
    minSdk = 21
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs += "-module-name=$libGroupId.$libArtifactId"
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
  }

  publishing {
    singleVariant("release") {
      withSourcesJar()
    }
  }
}

dependencies {
  implementation(libs.androidx.compose.material3)
}

publishing {
  publications {
    create<MavenPublication>("release") {
      groupId = libGroupId
      artifactId = libArtifactId
      version = libVersionName
      afterEvaluate {
        from(components["release"])
      }
    }
  }
}