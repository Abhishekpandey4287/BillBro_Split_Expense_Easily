package com.example.billbro.data.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billbro.data.module.Expense
import com.example.billbro.data.module.Group
import com.example.billbro.data.module.SplitType
import com.example.billbro.data.module.User
import com.example.billbro.data.repository.BillBro
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BillBroViewModel @Inject constructor(
    private val billBro: BillBro
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _simplifiedDebts = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val simplifiedDebts: StateFlow<Map<String, Map<String, Double>>> = _simplifiedDebts.asStateFlow()

    fun createUser(name: String, email: String) = viewModelScope.launch {
        val user = billBro.createUser(name, email)
        loadUsers()
    }

    fun createGroup(name: String) = viewModelScope.launch {
        val group = billBro.createGroup(name)
        loadGroups()
    }

    fun addUserToGroup(groupId: String, userId: String) = viewModelScope.launch {
        billBro.addUserToGroup(groupId, userId)
        loadGroups()
    }

    fun addExpense(
        description: String,
        amount: Double,
        paidUserId: String,
        groupId: String?,
        splitType: SplitType,
        values: List<Double>? = null
    ) = viewModelScope.launch {
        billBro.addExpense(description, amount, paidUserId, groupId, splitType, values)
        loadExpenses()
        loadGroups()
    }

    fun settlePayment(groupId: String, fromUserId: String, toUserId: String, amount: Double) = viewModelScope.launch {
        billBro.settlePayment(groupId, fromUserId, toUserId, amount)
        loadGroups()
    }

    fun simplifyDebts(groupId: String) = viewModelScope.launch {
        val simplified = billBro.simplifyGroupDebts(groupId)
        _simplifiedDebts.value = simplified
    }

    private suspend fun loadUsers() {
        _users.value = billBro.getAllUsers()
    }

    private suspend fun loadGroups() {
        _groups.value = billBro.getAllGroups()
    }

    private suspend fun loadExpenses() {
        _expenses.value = billBro.getAllExpenses()
    }

    init {
        viewModelScope.launch {
            loadUsers()
            loadGroups()
            loadExpenses()
        }
    }
}