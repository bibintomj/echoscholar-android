import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class CheckoutWebViewDialog(
    private val url: String,
    private val onSuccess: () -> Unit
) : DialogFragment() {

    private lateinit var webView: WebView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val wv = WebView(requireContext()).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true


            // Required for Stripe Checkout
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            requestFocus(View.FOCUS_DOWN)

            // Lint-friendly touch handler: request focus + performClick
            setOnTouchListener { v, ev ->
                if (!v.hasFocus()) {
                    v.requestFocus()
                    requestFocusFromTouch()
                }
                if (ev.action == MotionEvent.ACTION_UP) v.performClick()
                false
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.toString()?.let { checkSuccess(it) }
                    return false
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let { checkSuccess(it) }
                }
            }

            loadUrl(this@CheckoutWebViewDialog.url)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(wv)
            .setNegativeButton("Close") { d, _ -> d.dismiss() }
            .create()

        // 🔧 KEYBOARD FIX: allow IME in dialog & make it resize and show
        dialog.setOnShowListener {
            dialog.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                )
            }
            wv.post { wv.requestFocus(); wv.requestFocusFromTouch() }
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Let the keyboard resize the dialog content
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
    }

    private fun checkSuccess(currentUrl: String) {
        if (currentUrl.contains("/payment-success")) {
            onSuccess.invoke()
            dismissAllowingStateLoss()
        }
    }
}
