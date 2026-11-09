package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.SavedLocation
import com.example.data.repository.LocationRepository
import com.example.location.GpsFix
import com.example.location.LocationHelper
import com.example.navigation.NavigationLaunchResult
import com.example.navigation.WazeLauncher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val locations: List<SavedLocation> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val selectedLocation: SavedLocation? = null,
    val sharingLocation: SavedLocation? = null,
    val isCapturingGps: Boolean = false,
    val currentGpsFix: GpsFix? = null,
    val showSaveDialog: Boolean = false,
    val editingLocation: SavedLocation? = null,
    val showBackupDialog: Boolean = false,
    val showWazeNotInstalledDialog: Boolean = false,
    val pendingNavigationTarget: SavedLocation? = null,
    val message: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LocationRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LocationRepository(database.locationDao())
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedLocation = MutableStateFlow<SavedLocation?>(null)
    private val _sharingLocation = MutableStateFlow<SavedLocation?>(null)
    private val _isCapturingGps = MutableStateFlow(false)
    private val _currentGpsFix = MutableStateFlow<GpsFix?>(null)
    private val _showSaveDialog = MutableStateFlow(false)
    private val _editingLocation = MutableStateFlow<SavedLocation?>(null)
    private val _showBackupDialog = MutableStateFlow(false)
    private val _showWazeDialog = MutableStateFlow(false)
    private val _pendingNavTarget = MutableStateFlow<SavedLocation?>(null)
    private val _message = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val locationsList: StateFlow<List<SavedLocation>> = _searchQuery
        .flatMapLatest { query ->
            repository.getLocations(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> = repository.getTotalCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val uiState: StateFlow<MainUiState> = combine(
        locationsList,
        totalCount,
        _searchQuery,
        _selectedLocation,
        _sharingLocation,
        _isCapturingGps,
        _currentGpsFix,
        _showSaveDialog,
        _editingLocation,
        _showBackupDialog,
        _showWazeDialog,
        _pendingNavTarget,
        _message
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            locations = args[0] as List<SavedLocation>,
            totalCount = args[1] as Int,
            searchQuery = args[2] as String,
            selectedLocation = args[3] as SavedLocation?,
            sharingLocation = args[4] as SavedLocation?,
            isCapturingGps = args[5] as Boolean,
            currentGpsFix = args[6] as GpsFix?,
            showSaveDialog = args[7] as Boolean,
            editingLocation = args[8] as SavedLocation?,
            showBackupDialog = args[9] as Boolean,
            showWazeNotInstalledDialog = args[10] as Boolean,
            pendingNavigationTarget = args[11] as SavedLocation?,
            message = args[12] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSelectLocation(location: SavedLocation?) {
        _selectedLocation.value = location
    }

    fun onOpenShareQr(location: SavedLocation) {
        _selectedLocation.value = null
        _sharingLocation.value = location
    }

    fun onDismissShareQr() {
        _sharingLocation.value = null
    }

    fun onOpenBackupDialog() {
        _showBackupDialog.value = true
    }

    fun onDismissBackupDialog() {
        _showBackupDialog.value = false
    }

    fun restoreBackup(locations: List<SavedLocation>, clearExisting: Boolean) {
        viewModelScope.launch {
            repository.restoreLocations(locations, clearExisting)
            _message.value = if (clearExisting) "Copia restaurada exitosamente" else "Destinos agregados a la lista"
        }
    }

    fun importSingleLocation(
        name: String,
        fare: Double,
        notes: String,
        lat: Double,
        lng: Double,
        snippet: String
    ) {
        viewModelScope.launch {
            val newLoc = SavedLocation(
                name = name.trim(),
                fare = fare,
                notes = notes.trim(),
                latitude = lat,
                longitude = lng,
                accuracyMeters = 5f,
                addressSnippet = snippet,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                tripCount = 0
            )
            repository.saveLocation(newLoc)
            _message.value = "Destino agregado desde código"
        }
    }

    fun openSaveDialogWithCurrentGps(context: Context) {
        _editingLocation.value = null
        _isCapturingGps.value = true
        _currentGpsFix.value = null
        _showSaveDialog.value = true

        viewModelScope.launch {
            val fix = LocationHelper.getCurrentLocation(context)
            _isCapturingGps.value = false
            if (fix != null) {
                _currentGpsFix.value = fix
            } else {
                _currentGpsFix.value = GpsFix(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracyMeters = 0f,
                    addressSnippet = "Coordenadas por definir (activa GPS)"
                )
                _message.value = "GPS no disponible o buscando satélites"
            }
        }
    }

    fun openEditDialog(location: SavedLocation) {
        _editingLocation.value = location
        _currentGpsFix.value = GpsFix(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracyMeters,
            addressSnippet = location.addressSnippet
        )
        _showSaveDialog.value = true
    }

    fun dismissSaveDialog() {
        _showSaveDialog.value = false
        _editingLocation.value = null
        _currentGpsFix.value = null
        _isCapturingGps.value = false
    }

    fun saveLocation(
        name: String,
        fare: Double,
        notes: String,
        lat: Double,
        lng: Double,
        accuracy: Float,
        addressSnippet: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val editing = _editingLocation.value
            if (editing != null) {
                val updated = editing.copy(
                    name = name.trim(),
                    fare = fare,
                    notes = notes.trim(),
                    latitude = if (lat != 0.0) lat else editing.latitude,
                    longitude = if (lng != 0.0) lng else editing.longitude,
                    accuracyMeters = if (accuracy > 0) accuracy else editing.accuracyMeters,
                    addressSnippet = addressSnippet.ifBlank { editing.addressSnippet },
                    photoUri = photoUri ?: editing.photoUri
                )
                repository.updateLocation(updated)
                _message.value = "Ubicación actualizada"
                if (_selectedLocation.value?.id == updated.id) {
                    _selectedLocation.value = updated
                }
            } else {
                val newLoc = SavedLocation(
                    name = name.trim(),
                    fare = fare,
                    notes = notes.trim(),
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = accuracy,
                    addressSnippet = addressSnippet,
                    photoUri = photoUri,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis(),
                    tripCount = 0
                )
                repository.saveLocation(newLoc)
                _message.value = "Ubicación guardada con éxito"
            }
            dismissSaveDialog()
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.deleteLocation(location)
            if (_selectedLocation.value?.id == location.id) {
                _selectedLocation.value = null
            }
            _message.value = "Destino eliminado"
        }
    }

    fun navigateTo(context: Context, location: SavedLocation) {
        viewModelScope.launch {
            // Record trip usage (updates lastUsedAt so it stays in recent list)
            repository.recordTrip(location.id)
            _selectedLocation.value = null // Close sheet

            when (val result = WazeLauncher.navigateWithWaze(
                context = context,
                latitude = location.latitude,
                longitude = location.longitude,
                name = location.name,
                fare = location.fare
            )) {
                is NavigationLaunchResult.LaunchedWaze -> {
                    _message.value = "Iniciando navegación..."
                }
                is NavigationLaunchResult.WazeNotInstalled -> {
                    _pendingNavTarget.value = location
                    _showWazeDialog.value = true
                }
                is NavigationLaunchResult.Error -> {
                    _message.value = result.message
                }
            }
        }
    }

    fun openFallbackMaps(context: Context) {
        _pendingNavTarget.value?.let { loc ->
            WazeLauncher.openFallbackNavigation(
                context = context,
                latitude = loc.latitude,
                longitude = loc.longitude,
                name = loc.name,
                fare = loc.fare
            )
        }
        dismissWazeDialog()
    }

    fun installWaze(context: Context) {
        WazeLauncher.openPlayStoreForWaze(context)
        dismissWazeDialog()
    }

    fun dismissWazeDialog() {
        _showWazeDialog.value = false
        _pendingNavTarget.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
}
