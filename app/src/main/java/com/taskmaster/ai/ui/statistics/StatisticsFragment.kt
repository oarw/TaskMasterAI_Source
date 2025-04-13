package com.taskmaster.ai.ui.statistics

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.taskmaster.ai.R
import com.taskmaster.ai.TaskMasterApplication
import com.taskmaster.ai.data.Task
import com.taskmaster.ai.data.repository.PomodoroRepository
import com.taskmaster.ai.data.repository.TaskRepository
import com.taskmaster.ai.databinding.FragmentStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 时间统计Fragment
 */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: StatisticsViewModel by viewModels {
        val application = requireActivity().application as TaskMasterApplication
        val database = application.database
        StatisticsViewModel.StatisticsViewModelFactory(
            PomodoroRepository(database.pomodoroRecordDao()),
            TaskRepository(database.taskDao())
        )
    }
    
    private var taskPieChart: PieChart? = null
    private var dateBarChart: BarChart? = null
    
    private var tasks: List<Task> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTimeRangeRadioGroup()
        setupCustomDatePickers()
        setupCharts()
        observeViewModel()
    }
    
    private fun setupTimeRangeRadioGroup() {
        binding.rgTimeRange.setOnCheckedChangeListener { _, checkedId ->
            val timeRange = when (checkedId) {
                R.id.rb_today -> StatisticsViewModel.TIME_RANGE_TODAY
                R.id.rb_this_week -> StatisticsViewModel.TIME_RANGE_THIS_WEEK
                R.id.rb_this_month -> StatisticsViewModel.TIME_RANGE_THIS_MONTH
                R.id.rb_custom -> StatisticsViewModel.TIME_RANGE_CUSTOM
                else -> StatisticsViewModel.TIME_RANGE_TODAY
            }
            
            viewModel.setTimeRange(timeRange)
            
            // 显示/隐藏自定义日期选择器
            binding.layoutCustomDate.visibility = 
                if (timeRange == StatisticsViewModel.TIME_RANGE_CUSTOM) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupCustomDatePickers() {
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        // 如果已有日期，使用已有日期
        val currentDate = if (isStartDate) {
            viewModel.startDate.value
        } else {
            viewModel.endDate.value
        }
        
        currentDate?.let {
            calendar.time = it
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            
            if (isStartDate) {
                // 设置开始时间为当天的开始
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val startDate = calendar.time
                viewModel.setCustomStartDate(startDate)
                
                // 更新按钮文本
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.btnStartDate.text = dateFormat.format(startDate)
            } else {
                // 设置结束时间为当天的结束
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                
                val endDate = calendar.time
                viewModel.setCustomEndDate(endDate)
                
                // 更新按钮文本
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.btnEndDate.text = dateFormat.format(endDate)
            }
        }, year, month, day).show()
    }
    
    private fun setupCharts() {
        // 创建任务饼图
        taskPieChart = PieChart(requireContext()).apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }
        
        binding.chartContainerTask.addView(taskPieChart)
        
        // 创建日期柱状图
        dateBarChart = BarChart(requireContext()).apply {
            description.isEnabled = false
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            
            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f
            
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            
            animateY(1400)
        }
        
        binding.chartContainerDate.addView(dateBarChart)
    }
    
    private fun observeViewModel() {
        // 观察总专注时间
        viewModel.totalFocusTime.observe(viewLifecycleOwner) { totalMillis ->
            val hours = TimeUnit.MILLISECONDS.toHours(totalMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalMillis) - TimeUnit.HOURS.toMinutes(hours)
            binding.tvFocusTime.text = "${hours}小时${minutes}分钟"
        }
        
        // 观察完成的番茄钟数量
        viewModel.completedPomodoros.observe(viewLifecycleOwner) { count ->
            binding.tvCompletedPomodoros.text = count.toString()
        }
        
        // 观察完成的任务数量
        viewModel.completedTaskCount.observe(viewLifecycleOwner) { count ->
            binding.tvCompletedTasks.text = count.toString()
        }
        
        // 观察番茄钟记录
        viewModel.pomodoroRecords.observe(viewLifecycleOwner) { records ->
            // 更新任务饼图
            updateTaskPieChart()
            
            // 更新日期柱状图
            updateDateBarChart()
        }
        
        // 观察时间范围
        viewModel.timeRange.observe(viewLifecycleOwner) { range ->
            // 更新时间范围单选按钮
            val radioButtonId = when (range) {
                StatisticsViewModel.TIME_RANGE_TODAY -> R.id.rb_today
                StatisticsViewModel.TIME_RANGE_THIS_WEEK -> R.id.rb_this_week
                StatisticsViewModel.TIME_RANGE_THIS_MONTH -> R.id.rb_this_month
                StatisticsViewModel.TIME_RANGE_CUSTOM -> R.id.rb_custom
                else -> R.id.rb_today
            }
            
            val radioButton = binding.root.findViewById<RadioButton>(radioButtonId)
            radioButton.isChecked = true
        }
        
        // 观察开始日期
        viewModel.startDate.observe(viewLifecycleOwner) { date ->
            if (viewModel.timeRange.value == StatisticsViewModel.TIME_RANGE_CUSTOM) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.btnStartDate.text = dateFormat.format(date)
            }
        }
        
        // 观察结束日期
        viewModel.endDate.observe(viewLifecycleOwner) { date ->
            if (viewModel.timeRange.value == StatisticsViewModel.TIME_RANGE_CUSTOM) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.btnEndDate.text = dateFormat.format(date)
            }
        }
    }
    
    private fun updateTaskPieChart() {
        val focusTimeByTask = viewModel.getFocusTimeByTask()
        
        if (focusTimeByTask.isEmpty()) {
            binding.tvNoTaskData.visibility = View.VISIBLE
            taskPieChart?.visibility = View.GONE
            return
        }
        
        binding.tvNoTaskData.visibility = View.GONE
        taskPieChart?.visibility = View.VISIBLE
        
        val entries = ArrayList<PieEntry>()
        
        // 获取任务名称
        val taskRepository = (requireActivity().application as TaskMasterApplication).database.taskDao()
        
        focusTimeByTask.forEach { (taskId, duration) ->
            val taskName = if (taskId == null) {
                "无任务"
            } else {
                // 简化起见，这里使用任务ID
                // 实际应用中应该通过Repository获取任务名称
                "任务 #$taskId"
            }
            
            // 转换为小时
            val hours = duration / (1000f * 60f * 60f)
            entries.add(PieEntry(hours, taskName))
        }
        
        val dataSet = PieDataSet(entries, "任务分布").apply {
            setColors(*ColorTemplate.MATERIAL_COLORS)
            setDrawIcons(false)
            sliceSpace = 3f
            selectionShift = 5f
        }
        
        val data = PieData(dataSet).apply {
            setValueTextSize(11f)
            setValueTextColor(Color.BLACK)
            setValueFormatter { value, _, _, _ ->
                String.format("%.1f小时", value)
            }
        }
        
        taskPieChart?.data = data
        taskPieChart?.centerText = "任务分布"
        taskPieChart?.invalidate()
    }
    
    private fun updateDateBarChart() {
        val focusTimeByDate = viewModel.getFocusTimeByDate()
        
        if (focusTimeByDate.isEmpty()) {
            binding.tvNoDateData.visibility = View.VISIBLE
            dateBarChart?.visibility = View.GONE
            return
        }
        
        binding.tvNoDateData.visibility = View.GONE
        dateBarChart?.visibility = View.VISIBLE
        
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        // 按日期排序
        val sortedDates = focusTimeByDate.keys.sorted()
        
        sortedDates.forEachIndexed { index, date ->
            val duration = focusTimeByDate[date] ?: 0L
            // 转换为小时
            val hours = duration / (1000f * 60f * 60f)
            entries.add(BarEntry(index.toFloat(), hours))
            labels.add(date)
        }
        
        val dataSet = BarDataSet(entries, "每日专注时间").apply {
            setColors(*ColorTemplate.MATERIAL_COLORS)
            setDrawValues(true)
            valueTextSize = 10f
            valueFormatter = { value, _, _, _ ->
                String.format("%.1f小时", value)
            }
        }
        
        val data = BarData(dataSet).apply {
            barWidth = 0.6f
        }
        
        dateBarChart?.data = data
        dateBarChart?.xAxis?.valueFormatter = IndexAxisValueFormatter(labels)
        dateBarChart?.xAxis?.labelRotationAngle = 45f
        dateBarChart?.invalidate()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
