package com.taskmaster.ai.ui.pomodoro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.PomodoroRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.data.repository.UserSettingsRepository
import com.taskmaster.ai.databinding.FragmentPomodoroBinding
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 番茄钟Fragment
 */
class PomodoroFragment : Fragment() {

    private var _binding: FragmentPomodoroBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PomodoroViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        PomodoroViewModel.PomodoroViewModelFactory(
            PomodoroRepository(database.pomodoroRecordDao()),
            TaskRepository(database.taskDao()),
            UserSettingsRepository(database.userSettingsDao())
        )
    }
    
    private var tasks: List<Task> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPomodoroBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTaskSpinner()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupTaskSpinner() {
        viewModel.allTasks.observe(viewLifecycleOwner) { taskList ->
            tasks = taskList
            
            // 创建任务名称列表，添加"无任务"选项
            val taskNames = mutableListOf("无任务")
            taskNames.addAll(taskList.map { it.title })
            
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                taskNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerTask.adapter = adapter
            
            binding.spinnerTask.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position == 0) {
                        // 选择"无任务"
                        viewModel.setSelectedTask(null)
                    } else {
                        // 选择具体任务
                        viewModel.setSelectedTask(tasks[position - 1])
                    }
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.setSelectedTask(null)
                }
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnStart.setOnClickListener {
            viewModel.startPomodoro()
        }
        
        binding.btnPause.setOnClickListener {
            viewModel.pausePomodoro()
        }
        
        binding.btnStop.setOnClickListener {
            viewModel.stopPomodoro()
        }
    }
    
    private fun observeViewModel() {
        // 观察番茄钟状态
        viewModel.pomodoroState.observe(viewLifecycleOwner) { state ->
            updateTimerState(state)
            updateButtonState(state)
        }
        
        // 观察剩余时间
        viewModel.remainingTime.observe(viewLifecycleOwner) { remainingSeconds ->
            updateTimerDisplay(remainingSeconds)
            updateProgressBar(remainingSeconds)
        }
        
        // 观察当前周期
        viewModel.currentCycle.observe(viewLifecycleOwner) { cycle ->
            val settings = viewModel.userSettings.value
            val totalCycles = settings?.pomodoroCyclesBeforeLongBreak ?: 4
            binding.tvCycleInfo.text = "周期 $cycle/$totalCycles"
        }
        
        // 观察完成的番茄钟数量
        viewModel.completedPomodoros.observe(viewLifecycleOwner) { count ->
            binding.tvCompletedPomodoros.text = count.toString()
            
            // 计算总专注时间（假设每个番茄钟25分钟）
            val settings = viewModel.userSettings.value
            val pomodoroMinutes = settings?.pomodoroWorkDuration ?: 25
            val totalMinutes = count * pomodoroMinutes
            binding.tvTotalTime.text = "${totalMinutes}分钟"
        }
        
        // 观察用户设置
        viewModel.userSettings.observe(viewLifecycleOwner) { settings ->
            // 如果处于空闲状态，更新计时器显示为工作时间
            if (viewModel.pomodoroState.value == PomodoroViewModel.POMODORO_STATE_IDLE) {
                val workSeconds = settings.pomodoroWorkDuration * 60
                updateTimerDisplay(workSeconds)
                updateProgressBar(workSeconds, workSeconds)
            }
        }
    }
    
    private fun updateTimerState(state: Int) {
        binding.tvTimerState.text = when (state) {
            PomodoroViewModel.POMODORO_STATE_WORKING -> getString(R.string.pomodoro_work)
            PomodoroViewModel.POMODORO_STATE_SHORT_BREAK -> getString(R.string.pomodoro_short_break)
            PomodoroViewModel.POMODORO_STATE_LONG_BREAK -> getString(R.string.pomodoro_long_break)
            PomodoroViewModel.POMODORO_STATE_PAUSED -> "已暂停"
            else -> getString(R.string.pomodoro_work)
        }
    }
    
    private fun updateButtonState(state: Int) {
        when (state) {
            PomodoroViewModel.POMODORO_STATE_IDLE -> {
                binding.btnStart.isEnabled = true
                binding.btnStart.text = getString(R.string.pomodoro_start)
                binding.btnPause.isEnabled = false
                binding.btnStop.isEnabled = false
                binding.spinnerTask.isEnabled = true
            }
            PomodoroViewModel.POMODORO_STATE_WORKING, PomodoroViewModel.POMODORO_STATE_SHORT_BREAK, PomodoroViewModel.POMODORO_STATE_LONG_BREAK -> {
                binding.btnStart.isEnabled = false
                binding.btnPause.isEnabled = true
                binding.btnPause.text = getString(R.string.pomodoro_pause)
                binding.btnStop.isEnabled = true
                binding.spinnerTask.isEnabled = false
            }
            PomodoroViewModel.POMODORO_STATE_PAUSED -> {
                binding.btnStart.isEnabled = true
                binding.btnStart.text = getString(R.string.pomodoro_resume)
                binding.btnPause.isEnabled = false
                binding.btnStop.isEnabled = true
                binding.spinnerTask.isEnabled = false
            }
        }
    }
    
    private fun updateTimerDisplay(seconds: Int) {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
        val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun updateProgressBar(seconds: Int, totalSeconds: Int? = null) {
        val settings = viewModel.userSettings.value ?: return
        val state = viewModel.pomodoroState.value ?: PomodoroViewModel.POMODORO_STATE_IDLE
        
        val total = totalSeconds ?: when (state) {
            PomodoroViewModel.POMODORO_STATE_WORKING -> settings.pomodoroWorkDuration * 60
            PomodoroViewModel.POMODORO_STATE_SHORT_BREAK -> settings.pomodoroShortBreakDuration * 60
            PomodoroViewModel.POMODORO_STATE_LONG_BREAK -> settings.pomodoroLongBreakDuration * 60
            else -> settings.pomodoroWorkDuration * 60
        }
        
        val progress = (seconds.toFloat() / total.toFloat() * 100).toInt()
        binding.progressTimer.progress = progress
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
