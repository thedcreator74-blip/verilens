package com.example.ui.screens.permissions

import androidx.lifecycle.ViewModel
import com.example.permissions.PermissionItem
import com.example.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionViewModel(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _permissions.value = permissionManager.getPermissionList()
    }
}
