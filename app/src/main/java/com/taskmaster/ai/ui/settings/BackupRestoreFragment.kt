package com.taskmaster.ai.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.backup.BackupManager
import com.taskmaster.ai.databinding.FragmentBackupRestoreBinding

/**
 * 备份与恢复Fragment
 */
class BackupRestoreFragment : Fragment() {

    private var _binding: FragmentBackupRestoreBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BackupRestoreViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        BackupRestoreViewModel.BackupRestoreViewModelFactory(
            BackupManager(
                requireContext(),
                application.database,
                application.taskRepository,
                application.userSettingsRepository
            )
        )
    }
    
    private lateinit var backupAdapter: BackupAdapter
    
    // 文件选择结果处理
    private val restoreFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.restoreBackupFromUri(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupButtons()
        observeViewModel()
        
        // 加载备份文件列表
        viewModel.loadBackupFiles()
    }
    
    private fun setupRecyclerView() {
        backupAdapter = BackupAdapter(
            onRestoreClick = { backupFile ->
                viewModel.restoreBackup(backupFile.absolutePath)
            },
            onDeleteClick = { backupFile ->
                viewModel.deleteBackup(backupFile.absolutePath)
            }
        )
        
        binding.rvBackups.apply {
            adapter = backupAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun setupButtons() {
        // 创建备份按钮
        binding.btnCreateBackup.setOnClickListener {
            viewModel.createBackup()
        }
        
        // 从文件恢复按钮
        binding.btnRestoreFromFile.setOnClickListener {
            openFilePicker()
        }
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip"))
        }
        
        restoreFileLauncher.launch(intent)
    }
    
    private fun observeViewModel() {
        // 观察备份文件列表
        viewModel.backupFiles.observe(viewLifecycleOwner) { files ->
            backupAdapter.submitList(files)
            
            // 显示或隐藏空状态视图
            if (files.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvBackups.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvBackups.visibility = View.VISIBLE
            }
        }
        
        // 观察操作状态
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNotEmpty()) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
                
                // 操作完成后刷新备份文件列表
                viewModel.loadBackupFiles()
            }
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnCreateBackup.isEnabled = !isLoading
            binding.btnRestoreFromFile.isEnabled = !isLoading
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
