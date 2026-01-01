package com.example.billbro.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billbro.data.module.Group
import com.example.billbro.data.module.User
import com.example.billbro.data.repository.BillBro
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val billBro: BillBro
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _groups.value = billBro.getAllGroups()
        }
    }

    fun createGroup(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Group name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val group = billBro.createGroup(name)
                _successMessage.value = "Group '${group.name}' created successfully!"
                loadGroups()
            } catch (e: IllegalArgumentException) {
                _errorMessage.value = e.message ?: "Group name already exists"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                billBro.deleteGroup(groupId)
                loadGroups()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getGroupWithUsers(groupId: String): Group? {
        return billBro.getGroup(groupId)
    }

    suspend fun getUser(groupId: String): Group? {
        return billBro.getGroup(groupId)
    }

    fun addUserToGroup(groupId: String, userName: String, userEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // First create or get the user
                val existingUser = billBro.getUserByEmail(userEmail)
                val userId = if (existingUser != null) {
                    existingUser.userId
                } else {
                    // Create new user
                    val newUser = billBro.createUser(userName, userEmail)
                    newUser.userId
                }

                // Add user to group
                val success = billBro.addUserToGroup(groupId, userId)
                if (success) {
                    _successMessage.value = "User '$userName' added to group!"
                    // Force reload groups to update member counts
                    loadGroups()
                } else {
                    _errorMessage.value = "Failed to add user to group"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeUserFromGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val success = billBro.removeUserFromGroup(groupId, userId)
                if (success) {
                    _successMessage.value = "User removed from group!"
                    loadGroups()
                } else {
                    _errorMessage.value = "Failed to remove user from group"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}