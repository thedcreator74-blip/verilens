package com.example.ui.screens.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.permissions.PermissionItem
import com.example.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionViewModel(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _permissions.value = permissionManager.getPermissionItems()
        }
    }

    fun getOverlayPermissionIntent() = permissionManager.getOverlayPermissionIntent()

    fun getApplicationSettingsIntent() = permissionManager.getApplicationSettingsIntent()
}
