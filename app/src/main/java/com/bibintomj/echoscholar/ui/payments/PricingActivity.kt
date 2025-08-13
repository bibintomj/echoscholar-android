package com.bibintomj.echoscholar.ui.payments

import CheckoutWebViewDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bibintomj.echoscholar.data.repository.PaymentsRepository
import com.bibintomj.echoscholar.data.repository.SupabaseRepository
import com.bibintomj.echoscholar.databinding.ActivityPricingBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.OkHttpClient
import android.graphics.Paint
import android.util.Log

class PricingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPricingBinding

    private val viewModel: PaymentsViewModel by viewModels {
        val okHttp = OkHttpClient()
        val repo = PaymentsRepository(okHttp, SupabaseRepository)
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PaymentsViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPricingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvFreeAiAsk.paintFlags =
            binding.tvFreeAiAsk.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        // Click listener for Get Pro button
        binding.btnPro.setOnClickListener {
            viewModel.startCheckout()
        }

        // Observe VM state
        viewModel.state.onEach { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.error?.let {
                Log.e("PricingActivity", "Checkout error: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }

            state.checkoutUrl?.let { url ->
                CheckoutWebViewDialog(
                    url = url,
                    onSuccess = {
                        viewModel.onPaymentSuccess()
                        setResult(RESULT_OK)
                        Toast.makeText(this, "Payment successful! Pro unlocked.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                ).show(supportFragmentManager, "checkout")
                viewModel.clearCheckoutUrl()
            }
        }.launchIn(lifecycleScope)
    }
}
