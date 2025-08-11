package com.chat.security.ui

import android.content.Intent
import android.widget.TextView
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.base.base.WKBaseActivity
import com.chat.base.entity.AppModule
import com.chat.base.ui.Theme
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.singleclick.SingleClickUtil
import com.chat.security.R
import com.chat.security.databinding.ActSecurityHomeBinding
import com.chat.uikit.R as UiR
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class SecurityHomeActivity : WKBaseActivity<ActSecurityHomeBinding>() {

    override fun getViewBinding(): ActSecurityHomeBinding {
        return ActSecurityHomeBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv?.setText(R.string.security_and_privacy)
    }

    override fun initView() {
        wkVBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        wkVBinding.recyclerView.setHasFixedSize(true)
        val adapter = SecurityMenuAdapter()
        initAdapter(wkVBinding.recyclerView, adapter)
        val items = mutableListOf<MenuItem>()
        items.add(MenuItem(getString(R.string.destroy_account_title), getString(R.string.destroy_account_desc)) {
            startActivity(Intent(this, DestroyAccountActivity::class.java))
        })
        adapter.setList(items)
    }

    data class MenuItem(val title: String, val desc: String, val onClick: () -> Unit)

    inner class SecurityMenuAdapter : BaseQuickAdapter<MenuItem, BaseViewHolder>(UiR.layout.item_app_module_layout) {
        override fun convert(holder: BaseViewHolder, item: MenuItem) {
            holder.setText(UiR.id.nameTv, item.title)
            holder.setText(UiR.id.descTv, item.desc)
            // 隐藏模块选择用的控件，保持为普通功能项样式
            holder.getView<View>(UiR.id.errorIV)?.visibility = View.GONE
            holder.getView<View>(UiR.id.checkBox)?.visibility = View.GONE
            SingleClickUtil.onSingleClick(holder.itemView) {
                item.onClick.invoke()
            }
        }
    }
}


