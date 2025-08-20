package com.chat.security.ui

import android.app.Activity
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.chat.base.ui.Theme
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKToastUtils
import com.chat.security.R
import com.chat.security.service.GuardianPasswordManager

/**
 * 监护人密码对话框工具类
 * 处理密码设置和验证的UI交互
 */
class GuardianPasswordDialog {

    companion object {
        
        /**
         * 显示密码设置对话框（首次开启未成年模式时）
         */
        fun showSetPasswordDialog(activity: Activity, onSuccess: () -> Unit) {
            val passwordManager = GuardianPasswordManager.getInstance()
            
            // 创建密码输入布局
            val layout = createPasswordInputLayout(activity, true)
            val passwordEt = layout.getChildAt(0) as EditText
            val confirmPasswordEt = layout.getChildAt(1) as EditText
            
            WKDialogUtils.getInstance().showInputDialog(
                activity,
                activity.getString(R.string.set_guardian_password_title),
                activity.getString(R.string.set_guardian_password_desc),
                layout,
                activity.getString(R.string.minor_mode_cancel),
                activity.getString(R.string.minor_mode_confirm)
            ) { index ->
                if (index == 1) {
                    val password = passwordEt.text?.toString()?.trim() ?: ""
                    val confirmPassword = confirmPasswordEt.text?.toString()?.trim() ?: ""
                    
                    if (validatePasswordInput(activity, password, confirmPassword)) {
                        if (passwordManager.setGuardianPassword(password)) {
                            WKToastUtils.getInstance().showToastNormal(
                                activity.getString(R.string.guardian_password_set_success)
                            )
                            onSuccess()
                        } else {
                            WKToastUtils.getInstance().showToastNormal(
                                activity.getString(R.string.guardian_password_set_failed)
                            )
                        }
                    }
                }
            }
        }
        
        /**
         * 显示密码验证对话框（关闭未成年模式时）
         */
        fun showVerifyPasswordDialog(activity: Activity, onSuccess: () -> Unit) {
            val passwordManager = GuardianPasswordManager.getInstance()
            
            // 检查是否被锁定
            if (passwordManager.isLockedOut()) {
                val remainingTime = passwordManager.getRemainingLockoutTime()
                WKToastUtils.getInstance().showToastNormal(
                    activity.getString(R.string.password_locked_out, remainingTime)
                )
                return
            }
            
            // 创建密码输入布局
            val layout = createPasswordInputLayout(activity, false)
            val passwordEt = layout.getChildAt(0) as EditText
            val hintTv = layout.getChildAt(1) as TextView
            
            // 显示剩余尝试次数
            val remainingAttempts = passwordManager.getRemainingAttempts()
            hintTv.text = activity.getString(R.string.verify_guardian_password_hint, remainingAttempts)
            
            WKDialogUtils.getInstance().showInputDialog(
                activity,
                activity.getString(R.string.verify_guardian_password_title),
                activity.getString(R.string.verify_guardian_password_desc),
                layout,
                activity.getString(R.string.minor_mode_cancel),
                activity.getString(R.string.verify_password_confirm)
            ) { index ->
                if (index == 1) {
                    val password = passwordEt.text?.toString()?.trim() ?: ""
                    
                    if (password.isEmpty()) {
                        WKToastUtils.getInstance().showToastNormal(
                            activity.getString(R.string.password_cannot_empty)
                        )
                        return@showInputDialog
                    }
                    
                    if (passwordManager.verifyGuardianPassword(password)) {
                        WKToastUtils.getInstance().showToastNormal(
                            activity.getString(R.string.password_verify_success)
                        )
                        onSuccess()
                    } else {
                        if (passwordManager.isLockedOut()) {
                            val lockoutTime = passwordManager.getRemainingLockoutTime()
                            WKToastUtils.getInstance().showToastNormal(
                                activity.getString(R.string.password_locked_out, lockoutTime)
                            )
                        } else {
                            val remaining = passwordManager.getRemainingAttempts()
                            WKToastUtils.getInstance().showToastNormal(
                                activity.getString(R.string.password_verify_failed, remaining)
                            )
                        }
                    }
                }
            }
        }
        
        /**
         * 显示密码重置确认对话框
         */
        fun showResetPasswordDialog(activity: Activity, onSuccess: () -> Unit) {
            WKDialogUtils.getInstance().showDialog(
                activity,
                activity.getString(R.string.reset_guardian_password_title),
                activity.getString(R.string.reset_guardian_password_desc),
                true,
                activity.getString(R.string.minor_mode_cancel),
                activity.getString(R.string.confirm_reset),
                0,
                0
            ) { index ->
                if (index == 1) {
                    GuardianPasswordManager.getInstance().resetGuardianPassword()
                    WKToastUtils.getInstance().showToastNormal(
                        activity.getString(R.string.guardian_password_reset_success)
                    )
                    onSuccess()
                }
            }
        }
        
        /**
         * 创建密码输入布局
         */
        private fun createPasswordInputLayout(activity: Activity, isSetPassword: Boolean): LinearLayout {
            val layout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 16, 32, 16)
            }
            
            // 密码输入框
            val passwordEt = EditText(activity).apply {
                hint = activity.getString(R.string.input_guardian_password_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                textSize = 16f
                setTextColor(Theme.colorDark)
                setHintTextColor(Theme.color999)
                setPadding(16, 16, 16, 16)
                background?.setTint(Theme.colorLine)
            }
            layout.addView(passwordEt, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            // 如果是设置密码，添加确认密码输入框
            if (isSetPassword) {
                val confirmPasswordEt = EditText(activity).apply {
                    hint = activity.getString(R.string.confirm_guardian_password_hint)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    textSize = 16f
                    setTextColor(Theme.colorDark)
                    setHintTextColor(Theme.color999)
                    setPadding(16, 16, 16, 16)
                    background?.setTint(Theme.colorLine)
                }
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.topMargin = 16
                layout.addView(confirmPasswordEt, params)
                
                // 密码要求提示
                val requirementTv = TextView(activity).apply {
                    text = activity.getString(R.string.password_requirement)
                    textSize = 12f
                    setTextColor(Theme.color999)
                }
                val requirementParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                requirementParams.topMargin = 8
                layout.addView(requirementTv, requirementParams)
            } else {
                // 验证密码时的提示文字
                val hintTv = TextView(activity).apply {
                    textSize = 12f
                    setTextColor(Theme.color999)
                }
                val hintParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                hintParams.topMargin = 8
                layout.addView(hintTv, hintParams)
                
                // 忘记密码链接
                val forgotTv = TextView(activity).apply {
                    text = activity.getString(R.string.forgot_guardian_password)
                    textSize = 14f
                    setTextColor(Theme.colorAccount)
                    setOnClickListener {
                        showResetPasswordDialog(activity) {
                            // 重置成功后，可以重新设置密码
                        }
                    }
                }
                val forgotParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                forgotParams.topMargin = 16
                layout.addView(forgotTv, forgotParams)
            }
            
            return layout
        }
        
        /**
         * 验证密码输入
         */
        private fun validatePasswordInput(activity: Activity, password: String, confirmPassword: String): Boolean {
            when {
                password.isEmpty() -> {
                    WKToastUtils.getInstance().showToastNormal(
                        activity.getString(R.string.password_cannot_empty)
                    )
                    return false
                }
                password.length < 4 -> {
                    WKToastUtils.getInstance().showToastNormal(
                        activity.getString(R.string.password_too_short)
                    )
                    return false
                }
                password.length > 20 -> {
                    WKToastUtils.getInstance().showToastNormal(
                        activity.getString(R.string.password_too_long)
                    )
                    return false
                }
                password != confirmPassword -> {
                    WKToastUtils.getInstance().showToastNormal(
                        activity.getString(R.string.password_not_match)
                    )
                    return false
                }
                else -> return true
            }
        }
    }
}
