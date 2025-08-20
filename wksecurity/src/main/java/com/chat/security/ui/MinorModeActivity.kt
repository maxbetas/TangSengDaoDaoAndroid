package com.chat.security.ui

import android.widget.TextView
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.ui.Theme
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKToastUtils
import com.chat.security.R
import com.chat.security.databinding.ActMinorModeBinding
import com.chat.security.service.GuardianPasswordManager

class MinorModeActivity : WKBaseActivity<ActMinorModeBinding>() {
    
    private val MINOR_MODE_KEY = "minor_mode_enabled"

    override fun getViewBinding(): ActMinorModeBinding {
        return ActMinorModeBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv?.setText(R.string.minor_mode_title)
    }

    override fun initView() {
        // 获取当前状态
        val isMinorModeEnabled = WKSharedPreferencesUtil.getInstance().getBoolean(MINOR_MODE_KEY, false)
        wkVBinding.minorModeSwitch.isChecked = isMinorModeEnabled
        
        // 设置开关颜色和状态文字
        updateSwitchState(isMinorModeEnabled)
    }
    
    private fun updateSwitchState(isEnabled: Boolean) {
        if (isEnabled) {
            // 开启状态 - 使用主题色
            wkVBinding.minorModeSwitch.trackTintList = android.content.res.ColorStateList.valueOf(Theme.colorAccount)
            wkVBinding.minorModeSwitch.thumbTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            wkVBinding.statusTv.text = getString(R.string.minor_mode_status_on)
            wkVBinding.statusTv.setTextColor(Theme.colorAccount)
        } else {
            // 关闭状态 - 使用灰色
            wkVBinding.minorModeSwitch.trackTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, com.chat.base.R.color.color999)
            )
            wkVBinding.minorModeSwitch.thumbTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            wkVBinding.statusTv.text = getString(R.string.minor_mode_status_off)
            wkVBinding.statusTv.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, com.chat.base.R.color.color999)
            )
        }
    }

    override fun initListener() {
        wkVBinding.minorModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 启用未成年模式 - 需要监护人密码验证
                verifyGuardianPassword(getString(R.string.minor_mode_warning_title)) {
                    showEnableConfirmDialog()
                }
            } else {
                // 关闭未成年模式 - 需要监护人密码验证
                verifyGuardianPassword(getString(R.string.minor_mode_title)) {
                    showDisableConfirmDialog()
                }
            }
        }
    }
    
    /**
     * 安全地设置开关状态，不触发监听器
     */
    private fun setSwitchCheckedSafely(checked: Boolean) {
        wkVBinding.minorModeSwitch.setOnCheckedChangeListener(null)
        wkVBinding.minorModeSwitch.isChecked = checked
        wkVBinding.minorModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showEnableConfirmDialog()
            } else {
                showDisableConfirmDialog()
            }
        }
    }

    private fun showEnableConfirmDialog() {
        WKDialogUtils.getInstance().showDialog(
            this,
            getString(R.string.minor_mode_warning_title),
            getString(R.string.minor_mode_warning_content),
            true,
            getString(R.string.minor_mode_cancel),
            getString(R.string.minor_mode_confirm),
            0,
            0
        ) { index ->
            if (index == 1) {
                // 确认启用
                saveMinorModeState(true)
                updateSwitchState(true)
                WKToastUtils.getInstance().showToastNormal(getString(R.string.minor_mode_enabled))
            } else {
                // 取消，安全地重置开关状态，避免死循环
                setSwitchCheckedSafely(false)
                updateSwitchState(false)
            }
        }
    }

    private fun showDisableConfirmDialog() {
        WKDialogUtils.getInstance().showDialog(
            this,
            getString(R.string.minor_mode_title),
            "确认关闭未成年模式？\n\n关闭后将移除所有使用限制。",
            true,
            getString(R.string.minor_mode_cancel),
            "确认关闭",
            0,
            0
        ) { index ->
            if (index == 1) {
                // 确认关闭
                saveMinorModeState(false)
                updateSwitchState(false)
                WKToastUtils.getInstance().showToastNormal(getString(R.string.minor_mode_disabled))
            } else {
                // 取消，安全地重置开关状态，避免死循环
                setSwitchCheckedSafely(true)
                updateSwitchState(true)
            }
        }
    }

    private fun saveMinorModeState(enabled: Boolean) {
        WKSharedPreferencesUtil.getInstance().putBoolean(MINOR_MODE_KEY, enabled)
    }
    
    /**
     * 验证监护人密码
     */
    private fun verifyGuardianPassword(title: String, onSuccess: () -> Unit) {
        GuardianPasswordManager.getInstance().verifyPassword(
            this,
            title,
            onSuccess = onSuccess,
            onCancel = {
                // 密码验证失败，重置开关状态
                val currentState = WKSharedPreferencesUtil.getInstance().getBoolean(MINOR_MODE_KEY, false)
                setSwitchCheckedSafely(currentState)
                updateSwitchState(currentState)
            }
        )
    }
}
