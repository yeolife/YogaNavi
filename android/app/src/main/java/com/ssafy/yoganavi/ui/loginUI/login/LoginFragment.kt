package com.ssafy.yoganavi.ui.loginUI.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ssafy.yoganavi.R
import com.ssafy.yoganavi.databinding.FragmentLoginBinding
import com.ssafy.yoganavi.ui.core.BaseFragment
import com.ssafy.yoganavi.ui.core.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(FragmentLoginBinding::inflate) {
    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCollect()
        initListener()
    }

    private fun initListener() {
        binding.btnLogin.setOnClickListener {
            val email = binding.tieId.text.toString()
            val password = binding.tiePassword.text.toString()
            viewModel.login(email, password)
        }

        binding.tvForgetPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_findFragment)
        }

        binding.tvJoin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_joinFragment)
        }
    }

    private fun initCollect() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.loginEvent.collectLatest {
                when (it) {
                    is LogInEvent.LoginSuccess -> loginSuccess(it)
                    is LogInEvent.LoginError -> loginError(it.message)
                }
            }
        }
    }

    private fun loginSuccess(data: LogInEvent) {
        showSnackBar(data.message)
        moveMainActivity()
    }

    private fun loginError(message: String) = showSnackBar(message)

    private fun moveMainActivity() {
        val intent = Intent(requireActivity(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}
