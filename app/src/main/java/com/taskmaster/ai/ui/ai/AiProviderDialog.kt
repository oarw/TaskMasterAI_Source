package com.taskmaster.ai.ui.ai

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.taskmaster.ai.R
import com.taskmaster.ai.data.AiProvider

/**
 * AI提供商对话框
 * 用于添加或编辑AI提供商
 */
class AiProviderDialog(
    private val provider: AiProvider? = null,
    private val onSave: (name: String, apiUrl: String, apiKey: String) -> Unit
) : DialogFragment() {

    private lateinit var tilName: TextInputLayout
    private lateinit var tilApiUrl: TextInputLayout
    private lateinit var tilApiKey: TextInputLayout
    
    private lateinit var etName: EditText
    private lateinit var etApiUrl: EditText
    private lateinit var etApiKey: EditText
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_ai_provider, null)
        
        // 初始化视图
        tilName = view.findViewById(R.id.til_provider_name)
        tilApiUrl = view.findViewById(R.id.til_api_url)
        tilApiKey = view.findViewById(R.id.til_api_key)
        
        etName = view.findViewById(R.id.et_provider_name)
        etApiUrl = view.findViewById(R.id.et_api_url)
        etApiKey = view.findViewById(R.id.et_api_key)
        
        // 如果是编辑模式，填充现有数据
        provider?.let {
            etName.setText(it.name)
            etApiUrl.setText(it.apiUrl)
            etApiKey.setText(it.apiKey)
        }
        
        // 创建对话框
        val title = if (provider == null) R.string.add_ai_provider else R.string.edit_ai_provider
        
        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.save, null) // 在onStart中设置监听器，防止自动关闭
            .setNegativeButton(R.string.cancel, null)
            .create()
    }
    
    override fun onStart() {
        super.onStart()
        
        // 获取对话框按钮并设置点击监听器
        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) as Button
        
        positiveButton.setOnClickListener {
            if (validateInputs()) {
                val name = etName.text.toString().trim()
                val apiUrl = etApiUrl.text.toString().trim()
                val apiKey = etApiKey.text.toString().trim()
                
                onSave(name, apiUrl, apiKey)
                dismiss()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // 验证名称
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            tilName.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            tilName.error = null
        }
        
        // 验证API URL
        val apiUrl = etApiUrl.text.toString().trim()
        if (apiUrl.isEmpty()) {
            tilApiUrl.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
            tilApiUrl.error = getString(R.string.error_invalid_url)
            isValid = false
        } else {
            tilApiUrl.error = null
        }
        
        // 验证API密钥
        val apiKey = etApiKey.text.toString().trim()
        if (apiKey.isEmpty()) {
            tilApiKey.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            tilApiKey.error = null
        }
        
        return isValid
    }
}
