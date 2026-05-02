import dev.scaffoldit.hytale.Patchline

rootProject.name = "trigger-camera-plugin"

plugins {
    id("dev.scaffoldit") version "0.2.14"
}

hytale {
    usePatchline(Patchline.PRE_RELEASE.name)
}