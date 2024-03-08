plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}