package com.taskmaster.ai.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.AiProvider
import com.taskmaster.ai.data.repository.AiProviderRepository
import com.taskmaster.ai.databinding.FragmentAiProviderSettingsBinding

/**
 * AI提供商设置Fragment
 * 用于管理AI提供商配置
 */
class AiProviderSettingsFragment : Fragment() {

    private var _binding: FragmentAiProviderSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AiProviderSettingsViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        AiProviderSettingsViewModel.AiProviderSettingsViewModelFactory(
            AiProviderRepository(database.aiProviderDao())
        )
    }
    
    private lateinit var aiProviderAdapter: AiProviderAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiProviderSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupAddButton()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        aiProviderAdapter = AiProviderAdapter(
            onEditClick = { provider ->
                showEditDialog(provider)
            },
            onDeleteClick = { provider ->
                viewModel.deleteAiProvider(provider)
            },
            onSetDefaultClick = { provider ->
                viewModel.setDefaultProvider(provider.id)
            }
        )
        
        binding.rvAiProviders.apply {
            adapter = aiProviderAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }
    
    private fun setupAddButton() {
        binding.fabAddProvider.setOnClickListener {
            showAddDialog()
        }
    }
    
    private fun showAddDialog() {
        val dialog = AiProviderDialog(
            onSave = { name, apiUrl, apiKey ->
                val newProvider = AiProvider(
                    name = name,
                    apiUrl = apiUrl,
                    apiKey = apiKey,
                    isDefault = false
                )
                viewModel.addAiProvider(newProvider)
            }
        )
        dialog.show(childFragmentManager, "add_provider")
    }
    
    private fun showEditDialog(provider: AiProvider) {
        val dialog = AiProviderDialog(
            provider = provider,
            onSave = { name, apiUrl, apiKey ->
                val updatedProvider = provider.copy(
                    name = name,
                    apiUrl = apiUrl,
                    apiKey = apiKey
                )
                viewModel.updateAiProvider(updatedProvider)
            }
        )
        dialog.show(childFragmentManager, "edit_provider")
    }
    
    private fun observeViewModel() {
        viewModel.aiProviders.observe(viewLifecycleOwner) { providers ->
            aiProviderAdapter.submitList(providers)
            
            // 显示或隐藏空状态视图
            if (providers.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvAiProviders.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvAiProviders.visibility = View.VISIBLE
            }
        }
        
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNotEmpty()) {
                Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
