package com.taskmaster.ai.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.repository.UserSettingsRepository
import com.taskmaster.ai.data.sync.WebDavSyncManager
import com.taskmaster.ai.databinding.FragmentSyncSettingsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 同步设置Fragment
 */
class SyncSettingsFragment : Fragment() {

    private var _binding: FragmentSyncSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SyncSettingsViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        SyncSettingsViewModel.SyncSettingsViewModelFactory(
            UserSettingsRepository(database.userSettingsDao()),
            WebDavSyncManager(
                requireContext(),
                application.taskRepository,
                UserSettingsRepository(database.userSettingsDao())
            )
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupWebDavSettingsForm()
        setupSyncButton()
        setupOfflineSwitch()
        observeViewModel()
    }
    
    private fun setupWebDavSettingsForm() {
        // 加载现有设置
        viewModel.userSettings.observe(viewLifecycleOwner) { settings ->
            settings?.let {
                binding.etWebdavUrl.setText(it.webDavUrl ?: "")
                binding.etWebdavUsername.setText(it.webDavUsername ?: "")
                binding.etWebdavPassword.setText(it.webDavPassword ?: "")
                binding.switchOfflineMode.isChecked = it.offlineModeEnabled
            }
        }
        
        // 保存按钮点击事件
        binding.btnSaveWebdavSettings.setOnClickListener {
            val url = binding.etWebdavUrl.text.toString().trim()
            val username = binding.etWebdavUsername.text.toString().trim()
            val password = binding.etWebdavPassword.text.toString().trim()
            
            viewModel.saveWebDavSettings(url, username, password)
        }
    }
    
    private fun setupSyncButton() {
        binding.btnSyncNow.setOnClickListener {
            viewModel.startSync()
        }
    }
    
    private fun setupOfflineSwitch() {
        binding.switchOfflineMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.userSettings.value?.let {
                val userSettingsRepository = (requireActivity().application as TaskMasterApplication).userSettingsRepository
                userSettingsRepository.updateOfflineMode(isChecked)
            }
        }
    }
    
    private fun observeViewModel() {
        // 观察操作状态
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNotEmpty()) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
            }
        }
        
        // 观察同步状态
        viewModel.syncStatus.observe(viewLifecycleOwner) { status ->
            val statusText = when (status) {
                WebDavSyncManager.SyncStatus.IDLE -> "空闲"
                WebDavSyncManager.SyncStatus.SYNCING -> "同步中..."
                WebDavSyncManager.SyncStatus.SUCCESS -> "同步成功"
                WebDavSyncManager.SyncStatus.ERROR_NO_NETWORK -> "错误：无网络连接"
                WebDavSyncManager.SyncStatus.ERROR_NO_CREDENTIALS -> "错误：未设置WebDAV凭据"
                WebDavSyncManager.SyncStatus.ERROR_SYNC_FAILED -> "错误：同步失败"
            }
            
            binding.tvSyncStatus.text = statusText
            
            // 显示/隐藏进度条
            binding.progressBar.visibility = if (status == WebDavSyncManager.SyncStatus.SYNCING) View.VISIBLE else View.GONE
            
            // 禁用/启用同步按钮
            binding.btnSyncNow.isEnabled = status != WebDavSyncManager.SyncStatus.SYNCING
        }
        
        // 观察上次同步时间
        viewModel.lastSyncTime.observe(viewLifecycleOwner) { time ->
            binding.tvLastSyncTime.text = time?.let { formatDateTime(it) } ?: "从未同步"
        }
    }
    
    private fun formatDateTime(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
