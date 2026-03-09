package com.example.watertracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class LogFragment : Fragment() {

    private lateinit var waterAdapter: WaterAdapter
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        waterAdapter = WaterAdapter()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = waterAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val editTextAmount = view.findViewById<EditText>(R.id.editTextAmount)
        val spinnerDrinkType = view.findViewById<Spinner>(R.id.spinnerDrinkType)
        val buttonAdd = view.findViewById<Button>(R.id.buttonAdd)

        val drinkTypes = arrayOf("Water", "Juice", "Alcohol", "Milk", "Tea/Coffee", "Soda", "Other")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, drinkTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDrinkType.adapter = spinnerAdapter

        buttonAdd.setOnClickListener {
            val amountText = editTextAmount.text.toString()
            if (amountText.isNotEmpty()) {
                val amount = amountText.toInt()
                val drinkType = spinnerDrinkType.selectedItem.toString()
                val entry = WaterEntry(amount = amount, drinkType = drinkType)
                
                viewLifecycleOwner.lifecycleScope.launch {
                    database.waterDao().insert(entry)
                    editTextAmount.text.clear()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            database.waterDao().getAllEntries().collect { entries ->
                waterAdapter.submitList(entries)
            }
        }
    }
}
