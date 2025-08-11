package com.chat.security.ui

import android.widget.TextView
import com.chat.base.base.WKBaseActivity
import com.chat.base.utils.singleclick.SingleClickUtil
import com.chat.security.R
import com.chat.security.databinding.ActSecurityHomeBinding

class SecurityHomeActivity : WKBaseActivity<ActSecurityHomeBinding>() {

    override fun getViewBinding(): ActSecurityHomeBinding {
        return ActSecurityHomeBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv?.setText(R.string.security_and_privacy)
    }

    override fun initListener() {
        SingleClickUtil.onSingleClick(wkVBinding.destroyLayout) {
            startActivity(android.content.Intent(this, DestroyAccountActivity::class.java))
        }
    }
}


