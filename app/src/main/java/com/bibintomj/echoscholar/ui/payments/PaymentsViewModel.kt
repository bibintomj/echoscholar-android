package com.bibintomj.echoscholar.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibintomj.echoscholar.data.repository.PaymentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val isLoading: Boolean = false,
    val checkoutUrl: String? = null,
    val error: String? = null,
    val isPro: Boolean = false
)

class PaymentsViewModel(
    private val repo: PaymentsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentsUiState())
    val state: StateFlow<PaymentsUiState> = _state

    fun startCheckout() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = repo.beginCheckout()
            _state.value = result.fold(
                onSuccess = { url -> _state.value.copy(isLoading = false, checkoutUrl = url) },
                onFailure = { err -> _state.value.copy(isLoading = false, error = err.message ?: "Checkout failed") }
            )
        }
    }

    fun onPaymentSuccess() {
        _state.value = _state.value.copy(isPro = true, checkoutUrl = null)
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun clearCheckoutUrl() { _state.value = _state.value.copy(checkoutUrl = null) }
}
