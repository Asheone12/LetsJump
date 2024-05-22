package com.muen.letsjump.widget

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.muen.letsjump.R


class CommonTwoSelectionDialog constructor(context: Context) : CenterPopupView(context) {

    constructor(context: Context, bgRes:Int) : this(context) {
        bgResourse=bgRes
    }

    var bgResourse:Int?=null
    var title: String = ""
    var contentText:String=""
    var sureTitle: String? = null
    var content: TextView?=null
    var contentGravity:Int?= Gravity.CENTER
    var clickListener: (() -> Unit)? = null
    var cancelTitle:String=""
    var cancelClickListener: (() -> Unit)? = null

    var tv_title: TextView? = null
    var cancel: Button? = null
    var sure: Button? = null


    override fun getImplLayoutId(): Int {
        return R.layout.dialog_common_two_selection
    }

    override fun onCreate() {
        super.onCreate()
        tv_title = findViewById(R.id.tv_title)
        sure = findViewById(R.id.sure)
        content=findViewById(R.id.content)
        cancel = findViewById(R.id.cancel)
        tv_title?.text = title
        content?.text=contentText
        content?.gravity=contentGravity!!
        sure?.text = sureTitle ?: resources.getString(R.string.sure)
        if (!cancelTitle.isNullOrEmpty()){
            cancel?.text=cancelTitle
            cancel?.visibility = View.VISIBLE
        }else{
            cancel?.visibility = View.GONE
        }

        cancel?.setOnClickListener {
            cancelClickListener?.invoke()
            dismiss()
        }
        sure?.setOnClickListener {
            clickListener?.invoke()
            dismiss()
        }

        if (bgResourse!=null){
            sure?.setBackgroundResource(bgResourse!!)
        }
    }

    fun setOKBtnText(txt:String){
        sure.let {
            sure?.setText(txt)
        }

    }


    companion object {
        fun show(
            context: Context,
            title: String,
            content: String,
            sureTitle: String? = null,
            listener: (() -> Unit)? = null,
            cancelListener: (() -> Unit)? = null
        ) {
            val normalDialogView = CommonTwoSelectionDialog(context)
            normalDialogView.title = title
            normalDialogView.clickListener = listener
            normalDialogView.cancelClickListener = cancelListener
            normalDialogView.contentText = content
            normalDialogView.sureTitle = sureTitle
            XPopup.Builder(context).asCustom(normalDialogView).show()
        }

        fun show(
            context: Context,
            title: String,
            content: String,
            sureTitle: String? = null,
            cancelText:String="取消",
            listener: (() -> Unit)? = null,
            cancelListener: (() -> Unit)? = null
        ) {
            val normalDialogView = CommonTwoSelectionDialog(context)
            normalDialogView.title = title
            normalDialogView.clickListener = listener
            normalDialogView.cancelTitle=cancelText
            normalDialogView.cancelClickListener = cancelListener
            normalDialogView.contentText = content
            normalDialogView.sureTitle = sureTitle
            normalDialogView.cancel?.visibility = View.VISIBLE
            XPopup.Builder(context).asCustom(normalDialogView).show()
        }
    }
}