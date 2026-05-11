rootProject.name = "inplay"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include(
    "modules:core",
    "modules:collector",
    "modules:ingest",
    "modules:ml-inference",
    "modules:decision",
    "modules:notify",
    "modules:journal",
    "modules:api",
)
