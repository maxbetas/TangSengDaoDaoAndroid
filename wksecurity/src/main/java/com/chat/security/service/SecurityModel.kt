package com.chat.security.service

import com.chat.base.base.WKBaseModel
import com.chat.base.net.HttpResponseCode
import com.chat.base.net.ICommonListener
import com.chat.base.net.IRequestResultListener
import com.chat.base.net.entity.CommonResponse

class SecurityModel private constructor() : WKBaseModel() {

    companion object {
        private val HOLDER = SecurityModel()
        fun getInstance(): SecurityModel = HOLDER
    }

    fun sendDestroySms(listener: ICommonListener) {
        request(createService(SecurityService::class.java).sendDestroySms(), object : IRequestResultListener<CommonResponse> {
            override fun onSuccess(result: CommonResponse) {
                listener.onResult(HttpResponseCode.success, "")
            }

            override fun onFail(code: Int, msg: String) {
                listener.onResult(code, msg)
            }
        })
    }

    fun destroyAccount(code: String, listener: ICommonListener) {
        request(createService(SecurityService::class.java).destroy(code), object : IRequestResultListener<CommonResponse> {
            override fun onSuccess(result: CommonResponse) {
                listener.onResult(HttpResponseCode.success, "")
            }

            override fun onFail(code: Int, msg: String) {
                listener.onResult(code, msg)
            }
        })
    }
}


