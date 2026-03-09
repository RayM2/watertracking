package com.example.watertracking

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var database: AppDatabase
    private var hydrationGoal = 2000

    private lateinit var textViewGoalStatus: TextView
    private lateinit var progressBarGoal: ProgressBar
    private lateinit var textViewAverage: TextView
    private lateinit var textViewTotalEntries: TextView
    private lateinit var barChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("WaterTrackingPrefs", Context.MODE_PRIVATE)
        hydrationGoal = prefs.getInt("hydration_goal", 2000)

        database = AppDatabase.getDatabase(requireContext())

        textViewGoalStatus = view.findViewById(R.id.textViewGoalStatus)
        progressBarGoal = view.findViewById(R.id.progressBarGoal)
        textViewAverage = view.findViewById(R.id.textViewAverage)
        textViewTotalEntries = view.findViewById(R.id.textViewTotalEntries)
        barChart = view.findViewById(R.id.barChart)
        val buttonEditGoal = view.findViewById<Button>(R.id.buttonEditGoal)
        val buttonClearAll = view.findViewById<Button>(R.id.buttonClearAll)

        setupChart()

        buttonEditGoal.setOnClickListener {
            showEditGoalDialog()
        }

        buttonClearAll.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to delete all entries? This cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        database.waterDao().deleteAll()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            database.waterDao().getAllEntries().collect { entries ->
                updateGoalProgress(entries)
                updateTrends(entries)
                updateChart(entries)
            }
        }
    }

    private fun setupChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.legend.isEnabled = false
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false
    }

    private fun updateChart(entries: List<WaterEntry>) {
        val barEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Get data for the last 7 days
        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            
            val startOfDay = dateCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val endOfDay = dateCal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            val dayTotal = entries.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.amount }
            
            barEntries.add(BarEntry((6 - i).toFloat(), dayTotal.toFloat()))
            labels.add(dateFormat.format(dateCal.time))
        }

        val dataSet = BarDataSet(barEntries, "Water Intake")
        dataSet.color = Color.parseColor("#2196F3")
        dataSet.valueTextSize = 10f
        dataSet.setDrawValues(true)

        val data = BarData(dataSet)
        barChart.data = data
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.invalidate() // refresh
    }

    private fun updateGoalProgress(entries: List<WaterEntry>) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val totalToday = entries.filter { it.timestamp >= startOfToday }.sumOf { it.amount }
        
        textViewGoalStatus.text = "$totalToday / $hydrationGoal ml today"
        progressBarGoal.max = hydrationGoal
        progressBarGoal.progress = totalToday
    }

    private fun updateTrends(entries: List<WaterEntry>) {
        if (entries.isEmpty()) {
            textViewAverage.text = "0 ml"
            textViewTotalEntries.text = "0"
            return
        }

        val entriesByDay = entries.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        val dailySums = entriesByDay.map { (_, dayEntries) -> dayEntries.sumOf { it.amount } }
        val average = dailySums.average().toInt()

        textViewAverage.text = "$average ml"
        textViewTotalEntries.text = entries.size.toString()
    }

    private fun showEditGoalDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Set Hydration Goal (ml)")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(hydrationGoal.toString())
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val goalText = input.text.toString()
            if (goalText.isNotEmpty()) {
                hydrationGoal = goalText.toInt()
                val prefs = requireContext().getSharedPreferences("WaterTrackingPrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("hydration_goal", hydrationGoal).apply()

                viewLifecycleOwner.lifecycleScope.launch {
                    val entries = database.waterDao().getAllEntries().first()
                    updateGoalProgress(entries)
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
