package com.example.billbro.screens.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billbro.R
import com.example.billbro.data.module.Group
import com.example.billbro.data.module.User
import com.example.billbro.databinding.ActivityGroupBinding
import com.example.billbro.screens.adapter.GroupAdapter
import com.example.billbro.screens.viewmodel.GroupViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupBinding
    private val viewModel: GroupViewModel by viewModels()
    private lateinit var groupAdapter: GroupAdapter

    private var selectedGroupId: String? = null

    private var users : List<User>?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtonClickListener()
        observeViewModel()

        binding.ivBell.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manage_users -> {
                selectedGroupId?.let { groupId ->
                    showManageUsersDialog(groupId)
                } ?: run {
                    Toast.makeText(this, "Please select a group first", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(
            onItemClick = { group ->
                lifecycleScope.launch {
                    val groupWithUsers = viewModel.getGroupWithUsers(group.groupId)
                    val usersInGroup = groupWithUsers?.getMembers() ?: emptyList()
                    if (usersInGroup.size > 1) {
                        selectedGroupId = group.groupId
                        val intent = Intent(this@GroupActivity, MainActivity::class.java)
                        intent.putExtra("groupId", group.groupId)
                        intent.putExtra("groupName", group.name)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@GroupActivity, "Please add at least 2 members to the group", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDeleteClick = { group ->
                showDeleteGroupDialog(group)
            },
            onManageUsersClick = { group ->
                selectedGroupId = group.groupId
                showManageUsersDialog(group.groupId)
            }
        )
        binding.groupRecycler.apply {
            layoutManager = LinearLayoutManager(this@GroupActivity)
            adapter = groupAdapter
        }
    }

    private fun setupButtonClickListener() {
        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString().trim()
            if (groupName.isNotEmpty()) {
                viewModel.createGroup(groupName)
                binding.etGroupName.text?.clear()
            } else {
                Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.groups.collectLatest { groups ->
                groupAdapter.submitList(groups)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.btnCreateGroup.isEnabled = !isLoading
                binding.btnCreateGroup.text = if (isLoading) "Creating..." else "Create Group"
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@GroupActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearMessages()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.successMessage.collectLatest { success ->
                success?.let {
                    Toast.makeText(this@GroupActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessages()
                }
            }
        }
    }

    private fun showDeleteGroupDialog(group: Group) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Do you want to delete the group '${group.name}'?")
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.deleteGroup(group.groupId)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showManageUsersDialog(groupId: String) {
        lifecycleScope.launch {
            viewModel.getGroupWithUsers(groupId)?.let { group ->
                val group = viewModel.getGroupWithUsers(groupId)
                users = group?.getMembers()
                val userNames = users?.map { it.name }

                androidx.appcompat.app.AlertDialog.Builder(this@GroupActivity)
                    .setTitle("Manage Users in '${group?.name}'")
                    .setMessage("Current users: ${if (userNames!!.isEmpty()) "None" else userNames.joinToString(", ")}")
                    .setPositiveButton("Add User") { dialog, _ ->
                        showAddUserDialog(groupId)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Remove User") { dialog, _ ->
                        if (users!!.isNotEmpty()) {
                            showRemoveUserDialog(groupId, users!!)
                        } else {
                            Toast.makeText(this@GroupActivity, "No users to remove", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNeutralButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } ?: run {
                Toast.makeText(this@GroupActivity, "Group not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddUserDialog(groupId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add User to Group")
            .setView(dialogView)
            .setPositiveButton("Add") { dialogInterface, _ ->
                val etUserName = dialogView.findViewById<android.widget.EditText>(R.id.etUserName)
                val etUserEmail = dialogView.findViewById<android.widget.EditText>(R.id.etUserEmail)

                val userName = etUserName.text.toString().trim()
                val userEmail = etUserEmail.text.toString().trim()

                if (userName.isNotEmpty() && userEmail.isNotEmpty()) {
                    viewModel.addUserToGroup(groupId, userName, userEmail)
                } else {
                    Toast.makeText(this, "Please enter both name and email", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showRemoveUserDialog(groupId: String, users: List<com.example.billbro.data.module.User>) {
        val userNames = users.map { it.name }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove User from Group")
            .setItems(userNames) { dialog, which ->
                val userToRemove = users[which]
                viewModel.removeUserFromGroup(groupId, userToRemove.userId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}