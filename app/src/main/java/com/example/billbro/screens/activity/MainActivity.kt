package com.example.billbro.screens.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billbro.R
import com.example.billbro.data.module.Expense
import com.example.billbro.data.module.SplitType
import com.example.billbro.data.viewModel.BillBroViewModel
import com.example.billbro.databinding.ActivityMainBinding
import com.example.billbro.databinding.DialogAddExpenseBinding
import com.example.billbro.databinding.ItemSplitValueBinding
import com.example.billbro.screens.adapter.ExpenseAdapter
import com.example.billbro.screens.viewmodel.ExpenseViewModel
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var expenseAdapter: ExpenseAdapter

    private var selectedUserIds = mutableSetOf<String>()

    private var groupId: String? = null
    private var usersInGroup = emptyList<com.example.billbro.data.module.User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val groupName = intent.getStringExtra("groupName") ?: "Group"

        binding.tvGroupName.text = groupName

        binding.download.setOnClickListener {
            groupId?.let { id ->
                lifecycleScope.launch {
                    downloadExpensePdf(id)
                }
            }
        }

        binding.imageView.setImageResource(R.drawable.avatar1)

        binding.setting.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        binding.ivBack.setOnClickListener {
            val intent = Intent(this, GroupActivity::class.java)
            startActivity(intent)
        }

        groupId = intent.getStringExtra("groupId")

        setupRecyclerView()
        observeViewModel()
        loadData()

        binding.btnAdd.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter { expense ->
            showDeleteExpenseDialog(expense)
        }
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expenseAdapter
        }
    }

    private suspend fun downloadExpensePdf(groupId: String) {
        val netBalances = viewModel.calculateNetBalances(groupId)

        if (netBalances.isEmpty()) {
            Toast.makeText(this, "No expenses to export", Toast.LENGTH_SHORT).show()
            return
        }

        val groupName = binding.tvGroupName.text.toString()

        // Folder: /Android/data/.../files/Trip
        val folder = File(getExternalFilesDir(null), "Trip")
        if (!folder.exists()) folder.mkdirs()

        val fileName = "${groupName.replace(" ", "_")}_expenses.pdf"
        val file = File(folder, fileName)

        createPdf(file, groupName, netBalances)

        openPdf(file)
    }

    private fun openPdf(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No PDF viewer found on device",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createPdf(
        file: File,
        groupName: String,
        netBalances: Map<String, Double>
    ) {
        val pdfDocument = android.graphics.pdf.PdfDocument()

        val titlePaint = android.graphics.Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }

        val headerPaint = android.graphics.Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }

        val textPaint = android.graphics.Paint().apply {
            textSize = 14f
        }

        val borderPaint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val pageWidth = 595
        val pageHeight = 842

        val pageInfo =
            android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val margin = 24f // 16dp ≈ 24px
        val startX = margin
        val tableWidth = pageWidth - (margin * 2)
        val rowHeight = 32f

        var y = 50f
        canvas.drawText(groupName, startX, y, titlePaint)

        y += 24f

        val colWidths = listOf(
            tableWidth * 0.45f,
            tableWidth * 0.30f,
            tableWidth * 0.25f
        )

        val headers = listOf("Member", "Amount (₹)", "Status")

        var x = startX
        headers.forEachIndexed { index, header ->
            canvas.drawRect(
                x,
                y,
                x + colWidths[index],
                y + rowHeight,
                borderPaint
            )
            canvas.drawText(header, x + 10, y + 21, headerPaint)
            x += colWidths[index]
        }

        y += rowHeight
        var total = 0.0

        netBalances.forEach { (userId, amount) ->
            val userName =
                usersInGroup.find { it.userId == userId }?.name ?: "Unknown"

            val status = if (amount >= 0) "GETS" else "OWES"
            val displayAmount = String.format("%.2f", amount)

            val rowValues = listOf(userName, displayAmount, status)
            x = startX

            rowValues.forEachIndexed { index, value ->
                canvas.drawRect(
                    x,
                    y,
                    x + colWidths[index],
                    y + rowHeight,
                    borderPaint
                )
                canvas.drawText(value, x + 10, y + 21, textPaint)
                x += colWidths[index]
            }

            total += kotlin.math.abs(amount)
            y += rowHeight
        }

        x = startX

        canvas.drawRect(
            x,
            y,
            x + colWidths[0],
            y + rowHeight,
            borderPaint
        )
        canvas.drawText("Total Expense", x + 10, y + 21, headerPaint)
        x += colWidths[0]

        canvas.drawRect(
            x,
            y,
            x + colWidths[1],
            y + rowHeight,
            borderPaint
        )
        canvas.drawText(
            String.format("%.2f", total),
            x + 10,
            y + 21,
            headerPaint
        )

        x += colWidths[1]

        canvas.drawRect(
            x,
            y,
            x + colWidths[2],
            y + rowHeight,
            borderPaint
        )

        pdfDocument.finishPage(page)

        file.outputStream().use {
            pdfDocument.writeTo(it)
        }

        pdfDocument.close()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.expenses.collect { expenses ->
                val groupExpenses = groupId?.let { id ->
                    expenses.filter { it.groupId == id }
                } ?: expenses

                expenseAdapter.submitList(groupExpenses)
                groupId?.let { showBalanceSummary(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.usersInGroup.collect { users ->
                usersInGroup = users
                expenseAdapter.setUsers(users)
                binding.tvGroupMembers.text = "👥 ${users.size} people"
                showBalanceSummary(groupId!!)
            }
        }
    }

    private fun loadData() {
        groupId?.let {
            viewModel.loadGroupExpenses(it)
            viewModel.loadUsersInGroup(it)
            showBalanceSummary(it)
        }
    }

    private fun showDeleteExpenseDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Delete '${expense.description}'?")
            .setPositiveButton("Delete") { _, _ ->
                groupId?.let {
                    viewModel.deleteExpense(expense.expenseId, it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBalanceSummary(groupId: String) {
        lifecycleScope.launch {
            val netBalances = viewModel.calculateNetBalances(groupId)

            binding.layoutPaidBySummary.removeAllViews()

            val meaningful = netBalances.filterValues { kotlin.math.abs(it) > 0.01 }

            if (meaningful.isEmpty()) {
                binding.tvBalanceText.text = "All settled 🎉"
                return@launch
            }

            binding.tvBalanceText.text = "Balance Summary"

            meaningful.forEach { (userId, amount) ->
                val user = usersInGroup.find { it.userId == userId } ?: return@forEach

                val abs = kotlin.math.abs(amount)
                val amountText = "₹${"%.2f".format(abs)}"

                val fullText = if (amount < 0) {
                    "${user.name} owes $amountText"
                } else {
                    "${user.name} gets $amountText"
                }

                val spannable = android.text.SpannableString(fullText)

                val start = fullText.indexOf(amountText)
                val end = start + amountText.length

                val color = if (amount < 0)
                    getColor(R.color.red_500)
                else
                    getColor(R.color.green_500)

                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(color),
                    start,
                    end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    start,
                    end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                val tv = TextView(this@MainActivity).apply {
                    textSize = 15f
                    setLineSpacing(6f, 1f)
                    setPadding(0, 2, 0, 2)
                    text = spannable
                }

                binding.layoutPaidBySummary.addView(tv)
            }
        }
    }


    private fun showSplitBetweenDialog() {
        val userNames = usersInGroup.map { it.name }.toTypedArray()
        val checked = BooleanArray(usersInGroup.size) { true }

        selectedUserIds.clear()
        usersInGroup.forEach { selectedUserIds.add(it.userId) }

        AlertDialog.Builder(this)
            .setTitle("Split between")
            .setMultiChoiceItems(userNames, checked) { _, index, isChecked ->
                val userId = usersInGroup[index].userId
                if (isChecked) {
                    selectedUserIds.add(userId)
                } else {
                    selectedUserIds.remove(userId)
                }
            }
            .setPositiveButton("Done", null)
            .show()
    }



    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(this))

        val userNames = usersInGroup.map { it.name }
        val userAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, userNames)
        dialogBinding.autoCompletePaidBy.setAdapter(userAdapter)

        val splitTypes = SplitType.values().map { it.name }
        val splitTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, splitTypes)
        dialogBinding.autoCompleteSplitType.setAdapter(splitTypeAdapter)

        dialogBinding.autoCompleteSplitType.setOnItemClickListener { _, _, position, _ ->
            val selectedSplitType = SplitType.values()[position]
            updateValuesUI(dialogBinding, selectedSplitType)
            if (selectedSplitType == SplitType.BETWEEN) {
                showSplitBetweenDialog()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Expense")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { dialogInterface, _ ->
                val description = dialogBinding.etDescription.text.toString().trim()
                val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull()
                val paidByName = dialogBinding.autoCompletePaidBy.text.toString().trim()
                val splitTypeName = dialogBinding.autoCompleteSplitType.text.toString().trim()

                if (validateInput(description, amount, paidByName, splitTypeName)) {
                    val paidByUser = usersInGroup.find { it.name == paidByName }
                    val splitType = SplitType.valueOf(splitTypeName)
                    val values = getValuesFromUI(dialogBinding, splitType, amount!!)
                    if (splitType != SplitType.EQUAL && values == null) {
                        return@setPositiveButton
                    }
                    paidByUser?.let { user ->
                        groupId?.let { gId ->
                            lifecycleScope.launch {
                                viewModel.addExpense(
                                    description = description,
                                    amount = amount,
                                    paidUserId = user.userId,
                                    groupId = gId,
                                    splitType = splitType,
                                    values = values,
                                    splitBetweenUserIds = if (splitType == SplitType.BETWEEN)
                                        selectedUserIds.toList() else null
                                )
                                showBalanceSummary(groupId!!)
                                Toast.makeText(this@MainActivity, "Expense added!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun updateValuesUI(binding: DialogAddExpenseBinding, splitType: SplitType) {
        val containerValues = binding.containerValues
        val layoutValues = binding.layoutValues

        layoutValues.removeAllViews()

        when (splitType) {
            SplitType.EQUAL -> {
                containerValues.visibility = View.GONE
            }
            SplitType.PERCENTAGE -> {
                containerValues.visibility = View.VISIBLE
                addValueInputs(layoutValues, "", usersInGroup)
            }
            SplitType.EXACT -> {
                containerValues.visibility = View.VISIBLE
                addValueInputs(layoutValues, "", usersInGroup)
            }

            SplitType.BETWEEN ->{
                containerValues.visibility = View.GONE
            }
        }
    }

    private fun addValueInputs(layout: LinearLayout, hint: String, users: List<com.example.billbro.data.module.User>) {
        users.forEach { user ->
            val itemBinding = ItemSplitValueBinding.inflate(LayoutInflater.from(this))
            itemBinding.tvUserName.text = user.name
            itemBinding.etValue.hint = hint
            layout.addView(itemBinding.root)
        }
    }

    private fun getValuesFromUI(
        binding: DialogAddExpenseBinding,
        splitType: SplitType,
        totalAmount: Double
    ): List<Double>? {

        if (splitType == SplitType.EQUAL) return null

        val values = mutableListOf<Double>()

        for (i in 0 until binding.layoutValues.childCount) {
            val child = binding.layoutValues.getChildAt(i)
            val et = child.findViewById<TextInputEditText>(R.id.etValue)

            val text = et.text?.toString()?.trim()

            val value = if (text.isNullOrEmpty()) {
                0.0
            } else {
                text.toDoubleOrNull() ?: run {
                    Toast.makeText(this, "Invalid number entered", Toast.LENGTH_SHORT).show()
                    return null
                }
            }

            if (value < 0) {
                Toast.makeText(this, "Split value cannot be negative", Toast.LENGTH_SHORT).show()
                return null
            }

            values.add(value)
        }

        when (splitType) {
            SplitType.EXACT -> {
                val sum = values.sum()
                if (kotlin.math.abs(sum - totalAmount) > 0.01) {
                    Toast.makeText(
                        this,
                        "Exact split must total ₹$totalAmount",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
            }

            SplitType.PERCENTAGE -> {
                val sum = values.sum()
                if (kotlin.math.abs(sum - 100.0) > 0.01) {
                    Toast.makeText(
                        this,
                        "Percentages must total 100%",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
            }

            else -> {}
        }

        return values
    }

    private fun validateInput(
        description: String,
        amount: Double?,
        paidByName: String,
        splitTypeName: String
    ): Boolean {
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
            return false
        }

        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter valid amount", Toast.LENGTH_SHORT).show()
            return false
        }

        if (paidByName.isEmpty()) {
            Toast.makeText(this, "Please select who paid", Toast.LENGTH_SHORT).show()
            return false
        }

        if (splitTypeName.isEmpty()) {
            Toast.makeText(this, "Please select split type", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}