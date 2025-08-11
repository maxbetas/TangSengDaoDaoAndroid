package com.chat.security

import android.app.Application
import android.content.Intent
import com.chat.base.WKBaseApplication
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.entity.PersonalInfoMenu
import com.chat.base.entity.AppModule
import com.chat.security.ui.DestroyAccountActivity

class WKSecurityApplication private constructor() {

    private var application: Application? = null

    companion object {
        private val HOLDER: WKSecurityApplication by lazy { WKSecurityApplication() }
        @JvmStatic
        fun getInstance(): WKSecurityApplication = HOLDER
    }

    fun init(app: Application) {
        this.application = app
        val appModule: AppModule? = WKBaseApplication.getInstance().getAppModuleWithSid("security")
        if (!WKBaseApplication.getInstance().appModuleIsInjection(appModule)) {
            return
        }
        registerPersonalCenter()
    }

    private fun registerPersonalCenter() {
        EndpointManager.getInstance().setMethod(
            "personal_center_security",
            EndpointCategory.personalCenter,
            10
        ) { _ ->
            val ctx = application ?: return@setMethod null
            PersonalInfoMenu(
                "security",
                com.chat.security.R.drawable.ic_privacy_shield,
                ctx.getString(com.chat.security.R.string.security_and_privacy)
            ) {
                val intent = Intent(ctx, com.chat.security.ui.SecurityHomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
        }
    }
}


