package com.chat.security.service

import com.chat.base.net.entity.CommonResponse
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface SecurityService {
    @GET("user/sms/destroy")
    fun sendDestroySms(): Observable<CommonResponse>

    @DELETE("user/destroy/{code}")
    fun destroy(@Path("code") code: String): Observable<CommonResponse>
}


