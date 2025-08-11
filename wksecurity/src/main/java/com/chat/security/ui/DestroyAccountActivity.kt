package com.chat.security.ui

import android.text.TextUtils
import com.chat.base.base.WKBaseActivity
import com.chat.base.net.HttpResponseCode
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKToastUtils
import com.chat.security.R
import com.chat.security.databinding.ActDestroyAccountBinding
import com.chat.security.service.SecurityModel

class DestroyAccountActivity : WKBaseActivity<ActDestroyAccountBinding>() {

    override fun getViewBinding(): ActDestroyAccountBinding {
        return ActDestroyAccountBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: android.widget.TextView?) {
        titleTv?.setText(R.string.destroy_account_title)
    }

    override fun initListener() {
        wkVBinding.sendSmsBtn.setOnClickListener {
            SecurityModel.getInstance().sendDestroySms { code, msg ->
                if (code == HttpResponseCode.success) {
                    WKToastUtils.getInstance().showToastNormal(getString(R.string.send_sms))
                } else {
                    WKToastUtils.getInstance().showToastNormal(msg)
                }
            }
        }

        wkVBinding.confirmBtn.setOnClickListener {
            val code = wkVBinding.codeEt.text?.toString()?.trim()
            if (TextUtils.isEmpty(code)) {
                WKToastUtils.getInstance().showToastNormal(getString(R.string.input_code_hint))
                return@setOnClickListener
            }
            WKDialogUtils.getInstance().showDialog(
                this,
                getString(R.string.destroy_account_title),
                getString(R.string.destroy_account_desc),
                true,
                "",
                getString(R.string.confirm_destroy),
                0,
                0
            ) { index ->
                if (index == 1) {
                    SecurityModel.getInstance().destroyAccount(code!!) { code1, msg ->
                        if (code1 == HttpResponseCode.success) {
                            WKToastUtils.getInstance().showToastNormal(getString(R.string.destroy_account_title))
                            com.chat.uikit.WKUIKitApplication.getInstance().exitLogin(0)
                            finish()
                        } else {
                            WKToastUtils.getInstance().showToastNormal(msg)
                        }
                    }
                }
            }
        }
    }
}


