package com.ssafy.yoganavi.ui.homeUI.myPage.notice

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.ssafy.yoganavi.R
import com.ssafy.yoganavi.databinding.FragmentNoticeBinding
import com.ssafy.yoganavi.ui.core.BaseFragment

class NoticeFragment : BaseFragment<FragmentNoticeBinding>(FragmentNoticeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListener()
    }

    private fun initListener() {
        with(binding) {
            floatingActionButton.setOnClickListener {
                findNavController().navigate(R.id.action_noticeFragment_to_registerNoticeFragment)
            }
        }
    }
}
