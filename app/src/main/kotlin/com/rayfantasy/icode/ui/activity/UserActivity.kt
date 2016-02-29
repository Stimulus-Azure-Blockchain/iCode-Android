package com.rayfantasy.icode.ui.activity

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.android.volley.Request
import com.rayfantasy.icode.R
import com.rayfantasy.icode.databinding.ActivityUserBinding
import com.rayfantasy.icode.extra.PreloadLinearLayoutManager
import com.rayfantasy.icode.model.ICodeTheme
import com.rayfantasy.icode.postutil.PostUtil
import com.rayfantasy.icode.ui.adapter.UserListAdapter
import kotlinx.android.synthetic.main.content_user.*
import kotlinx.android.synthetic.main.nv_layout.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.support.v4.onRefresh
import java.util.*

class UserActivity : ActivityBase() {
    private lateinit var adapter: UserListAdapter
    private var isRefreshing: Boolean = false
    private lateinit var request: Request<out Any>
    private lateinit var username: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityUserBinding>(this, R.layout.activity_user).theme = ICodeTheme
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        username = intent.getSerializableExtra("username").toString()
        title = username
        initRecyclerView()
        user_swipe.onRefresh { refresh() }

    }

    private fun initRecyclerView() {
        user_recyclerview.layoutManager = PreloadLinearLayoutManager(this)
        adapter = UserListAdapter(this, username, ArrayList()) {}
        user_recyclerview.adapter = adapter
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        //回收资源
        PostUtil.cancel(request)
        isRefreshing = false

    }

    private fun refresh() {
        //如果正在刷新，则不再发起新的刷新请求
        if (isRefreshing)
            return
        isRefreshing = true
        val condition = "WHERE ${if (!isRefreshing && adapter.codeGoods.isNotEmpty()) "createat < ${adapter.codeGoods.last().createAt} AND " else ""}username=$username " +
                "ORDER BY createat DESC LIMIT 0,3"
        //按照username查找
        request = PostUtil.selectCodeGood(condition/*"WHERE username = '$username' ORDER BY updateat DESC"*/) {
            onSuccess {
                isRefreshing = false
                /* if (it.isEmpty()) {
                    adapter.setFooterState(UserListAdapter.FOOTER_STATE_NO_MORE)
                }*/
                if (isRefreshing) {
                    adapter.codeGoods.clear()
                }
                adapter.codeGoods.addAll(it)


                //重复利用原来的adapter，节省内存
                if (adapter != null) {
                    if (it.isEmpty()) {
                        /* adapter = UserListAdapter(this, username, it) {}
                        user_recyclerview.adapter = adapter*/
                        adapter.setFooterState(UserListAdapter.FOOTER_STATE_NO_MORE)
                    } else {
                        /* adapter.codeGoods = it
                         adapter.notifyDataSetChanged()*/
                        if (isRefreshing) adapter.notifyDataSetChanged()
                        else adapter.notifyItemRangeChanged(adapter.itemCount - 1 - it.size, it.size)
                    }
                }
            }
            onFailed { throwable, rc ->
                isRefreshing = false
                adapter.setFooterState(UserListAdapter.FOOTER_STATE_FAILED)
                throwable.printStackTrace()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (PostUtil.user?.username == username) {
            menuInflater.inflate(R.menu.user_menu, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.account_menu_out -> {
                alert(getString(R.string.exit_user_msg), getString(R.string.app_name)) {
                    positiveButton(getString(R.string.ok_btn)) {
                        PostUtil.logoutUser()
                        nv_user_icon.setImageDrawable(resources.getDrawable(R.mipmap.ic_nv_user))
                        nv_username.text = getText(R.string.not_Login)

                        finish()

                    }
                    negativeButton(getString(R.string.no_btn)) { }
                }.show()

                return true
            }
            R.id.account_setAccount -> {
                startActivity<AccountSettingActivity>()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
