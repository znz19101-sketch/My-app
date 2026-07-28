
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.guardexa.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.guardexa.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        versionCode = 1
        versionName = "1.0.0-alpha01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            type = "String",
            name = "PRIVACY_POLICY_VERSION",
            value = "\"1.0\""
        )
        buildConfigField(
            type = "int",
            name = "MAX_SECURITY_IMAGES",
            value = "30"
        )
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.gradleProperty("GUARDEXA_KEYSTORE_PATH")
            val keystorePassword = providers.gradleProperty("GUARDEXA_KEYSTORE_PASSWORD")
            val keyAliasValue = providers.gradleProperty("GUARDEXA_KEY_ALIAS")
            val keyPasswordValue = providers.gradleProperty("GUARDEXA_KEY_PASSWORD")

            if (
                keystorePath.isPresent &&
                keystorePassword.isPresent &&
                keyAliasValue.isPresent &&
                keyPasswordValue.isPresent
            ) {
                storeFile = file(keystorePath.get())
                storePassword = keystorePassword.get()
                keyAlias = keyAliasValue.get()
                keyPassword = keyPasswordValue.get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_DEVELOPER_TOOLS", "true")
        }

        create("internal") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            matchingFallbacks += listOf("debug")
            buildConfigField("boolean", "ENABLE_DEVELOPER_TOOLS", "true")
        }

        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            matchingFallbacks += listOf("release")
            isMinifyEnabled = true
            buildConfigField("boolean", "ENABLE_DEVELOPER_TOOLS", "false")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("boolean", "ENABLE_DEVELOPER_TOOLS", "false")
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("boolean", "DEVICE_OWNER_FEATURES", "false")
        }

        create("managed") {
            dimension = "distribution"
            buildConfigField("boolean", "DEVICE_OWNER_FEATURES", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable += setOf(
            "MissingTranslation"
        )
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-model"))
    implementation(project(":core-domain"))
    implementation(project(":core-data"))
    implementation(project(":core-database"))
    implementation(project(":core-security"))
    implementation(project(":core-policy"))
    implementation(project(":core-time"))
    implementation(project(":core-ui"))
    implementation(project(":core-resilience"))
    implementation(project(":ai-core"))
    implementation(project(":ai-camera-face"))
    implementation(project(":ai-glasses-mediapipe"))
    implementation(project(":ai-liveness-evidence"))
    implementation(project(":device-platform"))
    implementation(project(":feature-apps"))
    implementation(project(":feature-usage-schedules"))
    implementation(project(":feature-ui-core"))
    implementation(project(":feature-reports-notifications"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.camera)
    implementation(libs.mediapipe.tasks.vision)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
