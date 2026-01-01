package com.example.billbro.screens.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.billbro.R
import com.example.billbro.data.module.Group
import com.example.billbro.databinding.ItemGroupBinding

class GroupAdapter(
    private val onItemClick: (Group) -> Unit,
    private val onDeleteClick: (Group) -> Unit,
    private val onManageUsersClick: (Group) -> Unit
) : ListAdapter<Group, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group)
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.ivMore.setOnClickListener { view ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(view, getItem(position))
                }
            }

        }

        private fun getGroupAvatar(groupId: String): Int {
            val avatars = listOf(
                R.drawable.avatar1,
                R.drawable.avatar2,
                R.drawable.avatar3,
                R.drawable.avatar4,
                R.drawable.avatar5,
                R.drawable.avatar6
            )

            val index = kotlin.math.abs(groupId.hashCode()) % avatars.size
            return avatars[index]

        }


        private fun showPopupMenu(view: View, group: Group) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_group_item, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        onDeleteClick(group)
                        true
                    }
                    R.id.action_manage_users -> {
                        onManageUsersClick(group)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        fun bind(group: Group) {
            binding.tvGroupName.text = group.name

            binding.imageView.setImageResource(
                getGroupAvatar(group.groupId)
            )

            // Calculate and display balance summary
            val balanceSummary = calculateBalanceSummary(group)
            binding.tvBalanceSummary.text = balanceSummary

            // Display member count
            val memberCount = group.getMembers().size
            // You can add a TextView for member count or use an existing one
            // For now, we'll add it to the balance summary
            if (binding.tvBalanceSummary.text.isEmpty()) {
                binding.tvBalanceSummary.text = "$memberCount members"
            } else {
                binding.tvBalanceSummary.text = "$balanceSummary • $memberCount members"
            }
        }

        private fun calculateBalanceSummary(group: Group): String {
            val balances = group.getBalances()
            var totalOwed = 0.0
            var totalOwes = 0.0

            balances.forEach { (_, userBalances) ->
                userBalances.forEach { (_, amount) ->
                    if (amount > 0) {
                        totalOwed += amount
                    } else {
                        totalOwes += -amount
                    }
                }
            }

            return if (totalOwed == 0.0 && totalOwes == 0.0) {
                ""
            } else {
                "+₹${String.format("%.2f", totalOwed)}/-₹${String.format("%.2f", totalOwes)}"
            }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.groupId == newItem.groupId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }
}