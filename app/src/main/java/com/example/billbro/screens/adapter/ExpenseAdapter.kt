package com.example.billbro.screens.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billbro.R
import com.example.billbro.data.module.Expense
import com.example.billbro.data.module.User
import com.example.billbro.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ExpenseAdapter( private val onDeleteClick: (Expense) -> Unit)
    : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    private var userMap: Map<String, User> = emptyMap()

    fun setUsers(users: List<User>) {
        userMap = users.associateBy { it.userId }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense, userMap)
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense, userMap: Map<String, User>) {
            binding.tvDescription.text = expense.description
            binding.tvAmount.text = "₹${String.format("%.2f", expense.amount)}"

            val paidUser = userMap[expense.paidUserId]
            val paidByName = paidUser?.name ?: "Unknown User"
            binding.tvPaidBy.text = paidByName

            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(expense.date)

            binding.ivMore.setOnClickListener {
                onDeleteClick(expense)
            }

            binding.imageView.setImageResource(
                getExpenseAvatar(expense.expenseId)
            )
        }

        private fun getExpenseAvatar(expenseId: String): Int {
            val avatars = listOf(
                R.drawable.ic_expense,
                R.drawable.ic_expense1,
                R.drawable.ic_expense2,
                R.drawable.ic_expense3
            )
            val index = kotlin.math.abs(expenseId.hashCode()) % avatars.size
            return avatars[index]
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.expenseId == newItem.expenseId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}