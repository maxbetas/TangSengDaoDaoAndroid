package com.chat.security.service

import android.content.Context
import android.text.TextUtils
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.utils.WKCommonUtils
import com.chat.base.utils.WKDialogUtils
import com.chat.base.utils.WKToastUtils
import com.chat.base.views.pwdview.NumPwdDialog
import com.chat.security.R

/**
 * 监护人密码管理器
 * 负责未成年模式的密码设置、验证和管理
 */
class GuardianPasswordManager private constructor() {
    
    private val GUARDIAN_PWD_KEY = "guardian_password_hash"
    private val PWD_SETUP_COMPLETED_KEY = "guardian_pwd_setup_completed"
    
    companion object {
        private val HOLDER = GuardianPasswordManager()
        fun getInstance(): GuardianPasswordManager = HOLDER
    }
    
    /**
     * 检查是否已设置监护人密码
     */
    fun isPasswordSet(): Boolean {
        return WKSharedPreferencesUtil.getInstance().getBoolean(PWD_SETUP_COMPLETED_KEY, false)
    }
    
    /**
     * 首次设置密码流程
     */
    fun setupPassword(context: Context, onSuccess: () -> Unit, onCancel: () -> Unit) {
        showPasswordSetupDialog(context, onSuccess, onCancel)
    }
    
    /**
     * 验证密码
     */
    fun verifyPassword(context: Context, title: String, onSuccess: () -> Unit, onCancel: () -> Unit) {
        if (!isPasswordSet()) {
            // 如果没有设置密码，先设置
            setupPassword(context, onSuccess, onCancel)
            return
        }
        
        NumPwdDialog.getInstance().showNumPwdDialog(
            context,
            title,
            context.getString(R.string.guardian_pwd_verify_desc),
            context.getString(R.string.guardian_pwd_remark),
            object : NumPwdDialog.IPwdInputResult {
                override fun onResult(numPwd: String) {
                    if (verifyPasswordHash(numPwd)) {
                        onSuccess.invoke()
                    } else {
                        WKToastUtils.getInstance().showToastNormal(context.getString(R.string.guardian_pwd_error))
                        onCancel.invoke()
                    }
                }
                
                override fun forgetPwd() {
                    showForgetPasswordDialog(context)
                }
            }
        )
    }
    
    /**
     * 显示密码设置对话框
     */
    private fun showPasswordSetupDialog(context: Context, onSuccess: () -> Unit, onCancel: () -> Unit) {
        WKDialogUtils.getInstance().showInputDialog(
            context,
            context.getString(R.string.guardian_pwd_setup_title),
            context.getString(R.string.guardian_pwd_setup_desc),
            "",
            context.getString(R.string.guardian_pwd_setup_hint),
            6
        ) { firstPwd ->
            if (TextUtils.isEmpty(firstPwd) || firstPwd.length != 6) {
                WKToastUtils.getInstance().showToastNormal(context.getString(R.string.guardian_pwd_length_error))
                onCancel.invoke()
                return@showInputDialog
            }
            
            // 确认密码
            WKDialogUtils.getInstance().showInputDialog(
                context,
                context.getString(R.string.guardian_pwd_confirm_title),
                context.getString(R.string.guardian_pwd_confirm_desc),
                "",
                context.getString(R.string.guardian_pwd_setup_hint),
                6
            ) { confirmPwd ->
                if (firstPwd == confirmPwd) {
                    // 密码一致，保存
                    savePassword(firstPwd)
                    WKToastUtils.getInstance().showToastNormal(context.getString(R.string.guardian_pwd_setup_success))
                    onSuccess.invoke()
                } else {
                    WKToastUtils.getInstance().showToastNormal(context.getString(R.string.guardian_pwd_not_match))
                    onCancel.invoke()
                }
            }
        }
    }
    
    /**
     * 显示忘记密码对话框
     */
    private fun showForgetPasswordDialog(context: Context) {
        WKDialogUtils.getInstance().showSingleBtnDialog(
            context,
            context.getString(R.string.guardian_pwd_forget_title),
            context.getString(R.string.guardian_pwd_forget_desc),
context.getString(com.chat.base.R.string.sure)
        ) {
            // 仅提示用户联系客服，不提供重置功能
        }
    }
    
    /**
     * 保存密码（加密存储）
     */
    private fun savePassword(password: String) {
        val hashedPwd = WKCommonUtils.digest(password)
        WKSharedPreferencesUtil.getInstance().putSP(GUARDIAN_PWD_KEY, hashedPwd)
        WKSharedPreferencesUtil.getInstance().putBoolean(PWD_SETUP_COMPLETED_KEY, true)
    }
    
    /**
     * 验证密码哈希
     */
    private fun verifyPasswordHash(password: String): Boolean {
        val savedHash = WKSharedPreferencesUtil.getInstance().getSP(GUARDIAN_PWD_KEY)
        val inputHash = WKCommonUtils.digest(password)
        return savedHash == inputHash
    }
    
    /**
     * 重置密码（清除所有相关数据）
     */
    fun resetPassword() {
        WKSharedPreferencesUtil.getInstance().putSP(GUARDIAN_PWD_KEY, "")
        WKSharedPreferencesUtil.getInstance().putBoolean(PWD_SETUP_COMPLETED_KEY, false)
    }
}
