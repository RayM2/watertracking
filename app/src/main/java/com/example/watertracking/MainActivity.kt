package com.example.watertracking

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var waterAdapter: WaterAdapter
    private lateinit var database: AppDatabase
    private var hydrationGoal = 2000

    private lateinit var textViewGoalStatus: TextView
    private lateinit var progressBarGoal: ProgressBar
    private lateinit var textViewAverage: TextView
    private lateinit var textViewTotalEntries: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("WaterTrackingPrefs", Context.MODE_PRIVATE)
        hydrationGoal = prefs.getInt("hydration_goal", 2000)

        database = AppDatabase.getDatabase(this)
        waterAdapter = WaterAdapter()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = waterAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val editTextAmount = findViewById<EditText>(R.id.editTextAmount)
        val spinnerDrinkType = findViewById<Spinner>(R.id.spinnerDrinkType)
        val buttonAdd = findViewById<Button>(R.id.buttonAdd)
        textViewGoalStatus = findViewById(R.id.textViewGoalStatus)
        progressBarGoal = findViewById(R.id.progressBarGoal)
        val buttonEditGoal = findViewById<Button>(R.id.buttonEditGoal)
        textViewAverage = findViewById(R.id.textViewAverage)
        textViewTotalEntries = findViewById(R.id.textViewTotalEntries)

        val drinkTypes = arrayOf("Water", "Juice", "Alcohol", "Milk", "Tea/Coffee", "Soda", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, drinkTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDrinkType.adapter = spinnerAdapter

        buttonAdd.setOnClickListener {
            val amountText = editTextAmount.text.toString()
            if (amountText.isNotEmpty()) {
                val amount = amountText.toInt()
                val drinkType = spinnerDrinkType.selectedItem.toString()
                val entry = WaterEntry(amount = amount, drinkType = drinkType)
                
                lifecycleScope.launch {
                    database.waterDao().insert(entry)
                    editTextAmount.text.clear()
                }
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            }
        }

        buttonEditGoal.setOnClickListener {
            showEditGoalDialog()
        }

        lifecycleScope.launch {
            database.waterDao().getAllEntries().collect { entries ->
                waterAdapter.submitList(entries)
                updateGoalProgress(entries)
                updateTrends(entries)
            }
        }
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

        // Calculate average per day for days with entries
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
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Hydration Goal (ml)")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(hydrationGoal.toString())
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val goalText = input.text.toString()
            if (goalText.isNotEmpty()) {
                hydrationGoal = goalText.toInt()
                val prefs = getSharedPreferences("WaterTrackingPrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("hydration_goal", hydrationGoal).apply()

                lifecycleScope.launch {
                    val entries = database.waterDao().getAllEntries().first()
                    updateGoalProgress(entries)
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}
