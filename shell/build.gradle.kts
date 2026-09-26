import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.dev.shell"
    compileSdk = 36 

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 
        targetCompatibility = JavaVersion.VERSION_17 
    }

    buildFeatures {
        viewBinding = true
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

dependencies {
    /*
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.material)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.interpolator)
    */
}