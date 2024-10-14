package com.retameur.dartsconnect

import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

//    init {
//        UIApplication.sharedApplication.idleTimerDisabled = true
//    }
}

actual fun getPlatform(): Platform = IOSPlatform()