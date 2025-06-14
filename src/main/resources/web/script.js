// Global variables
let isLoading = false;

// DOM Elements
const elements = {
    spoofMode: null,
    spoofX: null,
    spoofY: null,
    spoofZ: null,
    setSpoofCoords: null,
    animateCoords: null,
    obscureRotations: null,
    rapidChangeMode: null,
    rapidHudMode: null,
    textReplaceMode: null,
    replacementTextInput: null,
    setReplacementText: null,
    textReplacementControls: null,
    biomeSpoofInput: null,
    setBiomeSpoof: null,
    fovSlider: null,
    renderSlider: null,
    brightnessSlider: null,
    volumeSlider: null,
    fovValue: null,
    renderValue: null,
    brightnessValue: null,
    volumeValue: null,
    serverAddress: null,
    joinServer: null,
    leaveServer: null,
    toggleStatsAutoUpdate: null,
    autoUpdateText: null,
    toggleCoordsVisibility: null,
    coordsVisibilityText: null,
    playerPosition: null,
    playerBiome: null,
    gameTime: null,
    gameFPS: null,
    toast: null,
    toastMessage: null,
    toastIcon: null,
    loadingOverlay: null,
    connectionStatus: null,
    statusText: null,
    visualRange: null,
    visualRangeInput: null,
    setVisualRange: null,
    visualRangeControls: null,
    playersInRange: null,
    panicQuit: null
};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    initializeElements();
    setupEventListeners();
    loadInitialSettings();
    startStatusCheck();
    updateCoordsVisibilityButton(true); // Initialize coords as hidden by default

    // Load initial data first, then start auto-update
    loadInitialSettings().then(() => {
        // Hide coordinates after data is loaded
        hideCoordinates();
        // Start auto-updating after initial load
        startStatsAutoUpdate();

        // Ensure coordinate input fields are always editable
        ensureCoordInputsEditable();
    });
});

// Initialize DOM elements
function initializeElements() {
    Object.keys(elements).forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            elements[key] = element;
        }
    });
}

// Setup all event listeners
function setupEventListeners() {
    // Coordinate Spoofer
    if (elements.spoofMode) {
        elements.spoofMode.addEventListener('change', handleSpoofModeChange);
    }
    
    if (elements.setSpoofCoords) {
        elements.setSpoofCoords.addEventListener('click', handleSetSpoofCoords);
    }
    
    if (elements.animateCoords) {
        elements.animateCoords.addEventListener('change', handleAnimateCoords);
    }
    
    if (elements.obscureRotations) {
        elements.obscureRotations.addEventListener('change', handleObscureRotations);
    }
    
    if (elements.setBiomeSpoof) {
        elements.setBiomeSpoof.addEventListener('click', handleSetBiomeSpoof);
    }

    // Extreme Protection
    if (elements.rapidChangeMode) {
        elements.rapidChangeMode.addEventListener('change', handleRapidChangeMode);
    }
    
    if (elements.rapidHudMode) {
        elements.rapidHudMode.addEventListener('change', handleRapidHudMode);
    }
    
    if (elements.textReplaceMode) {
        elements.textReplaceMode.addEventListener('change', handleTextReplaceMode);
    }
    
    if (elements.setReplacementText) {
        elements.setReplacementText.addEventListener('click', handleSetReplacementText);
    }

    // Visual Range
    if (elements.visualRange) {
        elements.visualRange.addEventListener('change', handleVisualRangeToggle);
    }

    if (elements.setVisualRange) {
        elements.setVisualRange.addEventListener('click', handleSetVisualRange);
    }

    // Panic Quit
    if (elements.panicQuit) {
        elements.panicQuit.addEventListener('change', handlePanicQuitToggle);
    }

    // Game Settings Sliders
    setupSlider('fov', 'fovSlider', 'fovValue', '/api/settings/fov');
    setupSlider('render', 'renderSlider', 'renderValue', '/api/settings/render');
    setupSlider('brightness', 'brightnessSlider', 'brightnessValue', '/api/settings/brightness');
    setupSlider('volume', 'volumeSlider', 'volumeValue', '/api/settings/volume');

    // Server Controls
    if (elements.joinServer) {
        elements.joinServer.addEventListener('click', handleJoinServer);
    }
    
    if (elements.leaveServer) {
        elements.leaveServer.addEventListener('click', handleLeaveServer);
    }
    
    if (elements.toggleStatsAutoUpdate) {
        elements.toggleStatsAutoUpdate.addEventListener('click', handleToggleStatsAutoUpdate);
    }

    if (elements.toggleCoordsVisibility) {
        elements.toggleCoordsVisibility.addEventListener('click', handleToggleCoordsVisibility);
    }

    // Preset buttons
    document.querySelectorAll('.preset-btn[data-preset]').forEach(btn => {
        btn.addEventListener('click', () => handlePreset(btn.dataset.preset));
    });

    // Biome preset buttons
    document.querySelectorAll('.preset-btn[data-biome]').forEach(btn => {
        btn.addEventListener('click', () => handleBiomePreset(btn.dataset.biome));
    });

    // Server preset buttons
    document.querySelectorAll('.server-btn[data-server]').forEach(btn => {
        btn.addEventListener('click', () => handleServerPreset(btn.dataset.server));
    });

    // Text replacement preset buttons
    document.querySelectorAll('.preset-btn[data-text]').forEach(btn => {
        btn.addEventListener('click', () => handleTextPreset(btn.dataset.text));
    });

    // Quick Actions
    setupQuickAction('toggleF3', '/api/actions/f3');
    setupQuickAction('toggleFullscreen', '/api/actions/fullscreen');
    setupQuickAction('takeScreenshot', '/api/actions/screenshot');
    setupQuickAction('togglePause', '/api/actions/pause');
}

// Setup slider with real-time updates
function setupSlider(name, sliderId, valueId, endpoint) {
    const slider = elements[sliderId];
    const valueDisplay = elements[valueId];
    
    if (!slider || !valueDisplay) return;
    
    slider.addEventListener('input', () => {
        valueDisplay.textContent = slider.value;
    });
    
    slider.addEventListener('change', async () => {
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ value: parseInt(slider.value) })
            });
            
            const data = await response.json();
            if (!data.success) {
                showToast(`Failed to update ${name}`, 'error');
            } else {
                saveSettingsToStorage(); // Auto-save settings
            }
        } catch (error) {
            showToast(`Failed to update ${name}`, 'error');
        }
    });
}

// Setup quick action button
function setupQuickAction(buttonId, endpoint) {
    const button = document.getElementById(buttonId);
    if (!button) return;
    
    button.addEventListener('click', async () => {
        try {
            showLoading(true);
            const response = await fetch(endpoint, { method: 'POST' });
            const data = await response.json();
            showToast(data.message || 'Action completed', 'success');
        } catch (error) {
            showToast('Action failed', 'error');
        } finally {
            showLoading(false);
        }
    });
}

// Event Handlers
async function handleSpoofModeChange() {
    const mode = elements.spoofMode.value;
    
    try {
        showLoading(true);
        const response = await fetch('/api/spoof/mode', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Spoofing mode set to: ${mode}`, 'success');
            updateCoordInputsVisibility(mode);
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to set spoofing mode', 'error');
        }
    } catch (error) {
        showToast('Failed to update spoofing mode', 'error');
    } finally {
        showLoading(false);
    }
}

async function handleSetSpoofCoords() {
    const x = parseFloat(elements.spoofX.value) || 0;
    const y = parseFloat(elements.spoofY.value) || 0;
    const z = parseFloat(elements.spoofZ.value) || 0;

    console.log('Setting spoof coordinates:', { x, y, z });
    console.log('Input values:', {
        spoofX: elements.spoofX.value,
        spoofY: elements.spoofY.value,
        spoofZ: elements.spoofZ.value
    });

    try {
        showLoading(true);
        const response = await fetch('/api/spoof/set', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ x, y, z })
        });

        console.log('API response status:', response.status);
        const data = await response.json();
        console.log('API response data:', data);

        if (data.status === 'success') {
            showToast('Coordinates applied successfully', 'success');
            saveSettingsToStorage(); // Auto-save settings
            // Reload spoof status to update the interface
            loadSpoofStatus();
        } else {
            showToast(data.error || 'Failed to set coordinates', 'error');
        }
    } catch (error) {
        console.error('Error setting coordinates:', error);
        showToast('Failed to set coordinates', 'error');
    } finally {
        showLoading(false);
    }
}

async function handleAnimateCoords() {
    try {
        const response = await fetch('/api/spoof/animate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled: elements.animateCoords.checked })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Animation ${elements.animateCoords.checked ? 'enabled' : 'disabled'}`, 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to update animation', 'error');
        }
    } catch (error) {
        showToast('Failed to update animation', 'error');
    }
}

async function handleObscureRotations() {
    try {
        const response = await fetch('/api/spoof/rotations', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled: elements.obscureRotations.checked })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Rotation obscuring ${elements.obscureRotations.checked ? 'enabled' : 'disabled'}`, 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to update rotation setting', 'error');
        }
    } catch (error) {
        showToast('Failed to update rotation setting', 'error');
    }
}

async function handleSetBiomeSpoof() {
    const biome = elements.biomeSpoofInput.value.trim();
    
    try {
        const response = await fetch('/api/spoof/biome', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ biome })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(biome ? `Biome set to: ${biome}` : 'Biome spoofing disabled', 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to set biome', 'error');
        }
    } catch (error) {
        showToast('Failed to set biome', 'error');
    }
}

async function handlePreset(preset) {
    try {
        showLoading(true);
        const response = await fetch('/api/spoof/preset', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ preset })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Applied ${preset} preset`, 'success');
            await loadSpoofStatus(); // Refresh coordinate inputs
        } else {
            showToast(data.error || 'Failed to apply preset', 'error');
        }
    } catch (error) {
        showToast('Failed to apply preset', 'error');
    } finally {
        showLoading(false);
    }
}

function handleBiomePreset(biome) {
    elements.biomeSpoofInput.value = biome;
    handleSetBiomeSpoof();
}

function handleServerPreset(server) {
    elements.serverAddress.value = server;
    saveSettingsToStorage(); // Auto-save settings
}

// Extreme Protection Handlers
async function handleRapidChangeMode() {
    try {
        const response = await fetch('/api/spoof/rapid', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled: elements.rapidChangeMode.checked })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Rapid F3 changes ${elements.rapidChangeMode.checked ? 'enabled' : 'disabled'}`, 
                elements.rapidChangeMode.checked ? 'warning' : 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to update rapid change mode', 'error');
        }
    } catch (error) {
        showToast('Failed to update rapid change mode', 'error');
    }
}

async function handleRapidHudMode() {
    try {
        const response = await fetch('/api/spoof/rapidhud', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled: elements.rapidHudMode.checked })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Rapid HUD changes ${elements.rapidHudMode.checked ? 'enabled' : 'disabled'}`, 
                elements.rapidHudMode.checked ? 'warning' : 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to update rapid HUD mode', 'error');
        }
    } catch (error) {
        showToast('Failed to update rapid HUD mode', 'error');
    }
}

async function handleTextReplaceMode() {
    try {
        const response = await fetch('/api/spoof/textreplace', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled: elements.textReplaceMode.checked })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Text replacement ${elements.textReplaceMode.checked ? 'enabled' : 'disabled'}`, 'success');
            updateTextReplacementControls(elements.textReplaceMode.checked);
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to update text replacement mode', 'error');
        }
    } catch (error) {
        showToast('Failed to update text replacement mode', 'error');
    }
}

async function handleSetReplacementText() {
    const text = elements.replacementTextInput.value.trim() || 'HIDDEN';
    
    try {
        const response = await fetch('/api/spoof/replacementtext', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text })
        });
        
        const data = await response.json();
        if (data.status === 'success') {
            showToast(`Replacement text set to: ${text}`, 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to set replacement text', 'error');
        }
    } catch (error) {
        showToast('Failed to set replacement text', 'error');
    }
}

function handleTextPreset(text) {
    elements.replacementTextInput.value = text;
    handleSetReplacementText();
}

function updateTextReplacementControls(enabled) {
    const controls = elements.textReplacementControls;
    if (controls) {
        if (enabled) {
            controls.classList.add('show');
        } else {
            controls.classList.remove('show');
        }
    }
}

// Visual Range Handlers
async function handleVisualRangeToggle() {
    try {
        const response = await fetch('/api/visualrange/toggle', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const data = await response.json();
        if (data.enabled !== undefined) {
            showToast(data.message, 'success');
            updateVisualRangeControls(data.enabled);
            if (data.enabled) {
                loadVisualRangeStatus(); // Load current status when enabled
            }
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to toggle visual range', 'error');
        }
    } catch (error) {
        showToast('Failed to toggle visual range', 'error');
    }
}

async function handleSetVisualRange() {
    const range = parseFloat(elements.visualRangeInput.value) || 100;

    try {
        const response = await fetch('/api/visualrange/range', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ range })
        });

        const data = await response.json();
        if (data.range !== undefined) {
            showToast(data.message, 'success');
            elements.visualRangeInput.value = data.range;
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to set visual range', 'error');
        }
    } catch (error) {
        showToast('Failed to set visual range', 'error');
    }
}

function updateVisualRangeControls(enabled) {
    const controls = elements.visualRangeControls;
    if (controls) {
        if (enabled) {
            controls.classList.add('show');
        } else {
            controls.classList.remove('show');
        }
    }
}

async function loadVisualRangeStatus() {
    try {
        const response = await fetch('/api/visualrange/status');
        const data = await response.json();

        if (data.enabled !== undefined) {
            elements.visualRange.checked = data.enabled;
            elements.visualRangeInput.value = data.range || 100;
            elements.playersInRange.textContent = data.playersInRange || 0;
            updateVisualRangeControls(data.enabled);
        }
    } catch (error) {
        console.error('Failed to load visual range status:', error);
    }
}

async function loadPanicQuitStatus() {
    try {
        const response = await fetch('/api/panicquit/status');
        const data = await response.json();

        if (data.enabled !== undefined) {
            if (elements.panicQuit) {
                elements.panicQuit.checked = data.enabled;
            }
        }
    } catch (error) {
        console.error('Failed to load panic quit status:', error);
    }
}

// Panic Quit Handlers
async function handlePanicQuitToggle() {
    try {
        const response = await fetch('/api/panicquit/toggle', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const data = await response.json();
        if (data.enabled !== undefined) {
            showToast(data.message || `Panic Quit ${data.enabled ? 'enabled' : 'disabled'}`, 'success');
            saveSettingsToStorage(); // Auto-save settings
        } else {
            showToast(data.error || 'Failed to toggle panic quit', 'error');
        }
    } catch (error) {
        showToast('Failed to toggle panic quit', 'error');
    }
}



async function handleJoinServer() {
    const address = elements.serverAddress.value.trim();
    if (!address) {
        showToast('Please enter a server address', 'warning');
        return;
    }
    
    try {
        showLoading(true);
        const response = await fetch('/api/server/join', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ address })
        });
        
        const data = await response.json();
        showToast(data.message, data.error ? 'error' : 'success');
    } catch (error) {
        showToast('Failed to join server', 'error');
    } finally {
        showLoading(false);
    }
}

async function handleLeaveServer() {
    try {
        showLoading(true);
        const response = await fetch('/api/server/leave', { method: 'POST' });
        const data = await response.json();
        showToast(data.message, data.error ? 'error' : 'success');
    } catch (error) {
        showToast('Failed to leave server', 'error');
    } finally {
        showLoading(false);
    }
}



// Utility Functions
function updateCoordInputsVisibility(mode) {
    const coordGrid = document.querySelector('.coord-grid');
    if (!coordGrid) return;
    
    if (mode === 'vanilla') {
        coordGrid.style.opacity = '0.5';
        coordGrid.style.pointerEvents = 'none';
    } else {
        coordGrid.style.opacity = '1';
        coordGrid.style.pointerEvents = 'auto';
    }
}

function showToast(message, type = 'info') {
    if (!elements.toast || !elements.toastMessage || !elements.toastIcon) return;
    
    // Set message
    elements.toastMessage.textContent = message;
    
    // Set icon based on type
    const icons = {
        success: 'fas fa-check-circle',
        error: 'fas fa-exclamation-circle',
        warning: 'fas fa-exclamation-triangle',
        info: 'fas fa-info-circle'
    };
    
    elements.toastIcon.className = icons[type] || icons.info;
    
    // Set toast class
    elements.toast.className = `toast ${type} show`;
    
    // Auto hide after 4 seconds
    setTimeout(() => {
        hideToast();
    }, 4000);
}

function hideToast() {
    if (elements.toast) {
        elements.toast.classList.remove('show');
    }
}

function showLoading(show) {
    if (!elements.loadingOverlay) return;
    
    isLoading = show;
    if (show) {
        elements.loadingOverlay.classList.add('show');
    } else {
        elements.loadingOverlay.classList.remove('show');
    }
}

// Local Storage Functions
function saveSettingsToStorage() {
    const settings = {
        spoofMode: elements.spoofMode?.value || 'vanilla',
        spoofX: elements.spoofX?.value || 0,
        spoofY: elements.spoofY?.value || 0,
        spoofZ: elements.spoofZ?.value || 0,
        animateCoords: elements.animateCoords?.checked || false,
        obscureRotations: elements.obscureRotations?.checked || false,
        rapidChangeMode: elements.rapidChangeMode?.checked || false,
        rapidHudMode: elements.rapidHudMode?.checked || false,
        textReplaceMode: elements.textReplaceMode?.checked || false,
        replacementText: elements.replacementTextInput?.value || 'HIDDEN',
        biomeSpoofInput: elements.biomeSpoofInput?.value || '',
        serverAddress: elements.serverAddress?.value || '',
        fovSlider: elements.fovSlider?.value || 70,
        renderSlider: elements.renderSlider?.value || 12,
        brightnessSlider: elements.brightnessSlider?.value || 50,
        volumeSlider: elements.volumeSlider?.value || 100
    };
    
    try {
        localStorage.setItem('webcontrol_settings', JSON.stringify(settings));
    } catch (error) {
        console.warn('Failed to save settings to localStorage:', error);
    }
}

function loadSettingsFromStorage() {
    try {
        const saved = localStorage.getItem('webcontrol_settings');
        if (!saved) return null;
        
        return JSON.parse(saved);
    } catch (error) {
        console.warn('Failed to load settings from localStorage:', error);
        return null;
    }
}

function applyStoredSettings(settings) {
    if (!settings) return;
    
    // Apply UI settings without triggering API calls
    if (elements.spoofMode && settings.spoofMode) {
        elements.spoofMode.value = settings.spoofMode;
        updateCoordInputsVisibility(settings.spoofMode);
    }
    
    if (elements.spoofX && settings.spoofX !== undefined) {
        elements.spoofX.value = settings.spoofX;
    }
    
    if (elements.spoofY && settings.spoofY !== undefined) {
        elements.spoofY.value = settings.spoofY;
    }
    
    if (elements.spoofZ && settings.spoofZ !== undefined) {
        elements.spoofZ.value = settings.spoofZ;
    }
    
    if (elements.animateCoords && settings.animateCoords !== undefined) {
        elements.animateCoords.checked = settings.animateCoords;
    }
    
    if (elements.obscureRotations && settings.obscureRotations !== undefined) {
        elements.obscureRotations.checked = settings.obscureRotations;
    }
    
    if (elements.rapidChangeMode && settings.rapidChangeMode !== undefined) {
        elements.rapidChangeMode.checked = settings.rapidChangeMode;
    }
    
    if (elements.rapidHudMode && settings.rapidHudMode !== undefined) {
        elements.rapidHudMode.checked = settings.rapidHudMode;
    }
    
    if (elements.textReplaceMode && settings.textReplaceMode !== undefined) {
        elements.textReplaceMode.checked = settings.textReplaceMode;
        updateTextReplacementControls(settings.textReplaceMode);
    }
    
    if (elements.replacementTextInput && settings.replacementText) {
        elements.replacementTextInput.value = settings.replacementText;
    }
    
    if (elements.biomeSpoofInput && settings.biomeSpoofInput !== undefined) {
        elements.biomeSpoofInput.value = settings.biomeSpoofInput;
    }
    
    if (elements.serverAddress && settings.serverAddress) {
        elements.serverAddress.value = settings.serverAddress;
    }
    
    // Apply slider settings
    if (elements.fovSlider && elements.fovValue && settings.fovSlider) {
        elements.fovSlider.value = settings.fovSlider;
        elements.fovValue.textContent = settings.fovSlider;
    }
    
    if (elements.renderSlider && elements.renderValue && settings.renderSlider) {
        elements.renderSlider.value = settings.renderSlider;
        elements.renderValue.textContent = settings.renderSlider;
    }
    
    if (elements.brightnessSlider && elements.brightnessValue && settings.brightnessSlider) {
        elements.brightnessSlider.value = settings.brightnessSlider;
        elements.brightnessValue.textContent = settings.brightnessSlider;
    }
    
    if (elements.volumeSlider && elements.volumeValue && settings.volumeSlider) {
        elements.volumeSlider.value = settings.volumeSlider;
        elements.volumeValue.textContent = settings.volumeSlider;
    }
}

// Data Loading Functions
async function loadInitialSettings() {
    try {
        // First load stored settings from localStorage
        const storedSettings = loadSettingsFromStorage();
        if (storedSettings) {
            applyStoredSettings(storedSettings);
            showToast('Settings restored from previous session', 'info');
        }
        
        // Then load current server state (this will override stored settings if server has different values)
        await Promise.all([
            loadGameSettings(),
            loadSpoofStatus(),
            loadGameStats(),
            loadVisualRangeStatus(),
            loadPanicQuitStatus()
        ]);
    } catch (error) {
        showToast('Failed to load initial settings', 'error');
    }
}

async function loadGameSettings() {
    try {
        const response = await fetch('/api/settings');
        const settings = await response.json();
        
        if (elements.fovSlider && elements.fovValue) {
            elements.fovSlider.value = settings.fov || 70;
            elements.fovValue.textContent = settings.fov || 70;
        }
        
        if (elements.renderSlider && elements.renderValue) {
            elements.renderSlider.value = settings.renderDistance || 12;
            elements.renderValue.textContent = settings.renderDistance || 12;
        }
    } catch (error) {
        console.error('Failed to load game settings:', error);
    }
}

async function loadSpoofStatus() {
    try {
        const response = await fetch('/api/spoof/status');
        const status = await response.json();
        
        if (elements.spoofMode) {
            elements.spoofMode.value = status.mode || 'vanilla';
            updateCoordInputsVisibility(status.mode || 'vanilla');
        }
        
        if (elements.spoofX) elements.spoofX.value = status.offsetX || 0;
        if (elements.spoofY) elements.spoofY.value = status.offsetY || 0;
        if (elements.spoofZ) elements.spoofZ.value = status.offsetZ || 0;
        
        if (elements.animateCoords) {
            elements.animateCoords.checked = status.animateCoords || false;
        }
        
        if (elements.obscureRotations) {
            elements.obscureRotations.checked = status.obscureRotations || false;
        }
        
        if (elements.biomeSpoofInput) {
            elements.biomeSpoofInput.value = status.spoofedBiome || '';
        }
        
        if (elements.rapidChangeMode) {
            elements.rapidChangeMode.checked = status.rapidChangeMode || false;
        }
        
        if (elements.rapidHudMode) {
            elements.rapidHudMode.checked = status.rapidHudMode || false;
        }
        
        if (elements.textReplaceMode) {
            elements.textReplaceMode.checked = status.textReplaceMode || false;
            updateTextReplacementControls(status.textReplaceMode || false);
        }
        
        if (elements.replacementTextInput) {
            elements.replacementTextInput.value = status.replacementText || 'HIDDEN';
        }
    } catch (error) {
        console.error('Failed to load spoof status:', error);
    }
}



// Status Check
function startStatusCheck() {
    // Check connection status every 5 seconds
    setInterval(checkConnectionStatus, 5000);
    checkConnectionStatus(); // Initial check
}

// Player Stats Auto-Update
let statsUpdateInterval;

function startStatsAutoUpdate() {
    // Clear any existing interval
    if (statsUpdateInterval) {
        clearInterval(statsUpdateInterval);
    }

    // Start new interval for maximum speed - every 100ms (10 times per second)
    statsUpdateInterval = setInterval(() => {
        loadGameStats();
    }, 100);

    console.log('Player stats auto-update started (every 100ms - real-time)');
    updateAutoUpdateButton(true);
}

function stopStatsAutoUpdate() {
    if (statsUpdateInterval) {
        clearInterval(statsUpdateInterval);
        statsUpdateInterval = null;
        console.log('Player stats auto-update stopped');
    }
}

// Toggle auto-update on/off
function handleToggleStatsAutoUpdate() {
    if (statsUpdateInterval) {
        // Currently running, stop it
        stopStatsAutoUpdate();
        updateAutoUpdateButton(false);
        showToast('Player stats auto-update disabled', 'info');
    } else {
        // Currently stopped, start it
        startStatsAutoUpdate();
        updateAutoUpdateButton(true);
        showToast('Player stats real-time update enabled (10x per second)', 'success');
    }
}

function updateAutoUpdateButton(isRunning) {
    if (elements.autoUpdateText) {
        elements.autoUpdateText.textContent = isRunning ? 'Real-time: ON' : 'Real-time: OFF';
    }

    if (elements.toggleStatsAutoUpdate) {
        const icon = elements.toggleStatsAutoUpdate.querySelector('i');
        if (icon) {
            icon.className = isRunning ? 'fas fa-pause' : 'fas fa-play';
        }

        // Update button style
        if (isRunning) {
            elements.toggleStatsAutoUpdate.classList.add('btn-success');
            elements.toggleStatsAutoUpdate.classList.remove('btn-secondary');
        } else {
            elements.toggleStatsAutoUpdate.classList.add('btn-secondary');
            elements.toggleStatsAutoUpdate.classList.remove('btn-success');
        }
    }
}

// Coordinates visibility toggle - hidden by default for privacy
let coordsHidden = true;

function handleToggleCoordsVisibility() {
    console.log('Toggle coords visibility clicked. Current state:', coordsHidden ? 'hidden' : 'visible');

    if (coordsHidden) {
        // Currently hidden, show them
        console.log('Showing coordinates...');
        showCoordinates();
        updateCoordsVisibilityButton(false);
        showToast('Coordinates now visible', 'success');
    } else {
        // Currently visible, hide them
        console.log('Hiding coordinates...');
        hideCoordinates();
        updateCoordsVisibilityButton(true);
        showToast('Coordinates hidden for privacy', 'info');
    }
}

function hideCoordinates() {
    // Set coordsHidden to true first
    coordsHidden = true;

    // Hide coordinates in Player Stats
    if (elements.playerPosition) {
        if (elements.playerPosition.textContent && elements.playerPosition.textContent !== 'Loading...' && elements.playerPosition.textContent !== '***HIDDEN***') {
            // Store the original text (might be formatted, but we'll reformat when showing)
            elements.playerPosition.dataset.originalText = elements.playerPosition.textContent;
        }
        elements.playerPosition.textContent = '***HIDDEN***';
    }

    // Hide coordinates in Coordinate Spoofing inputs - but keep them editable
    const coordInputs = document.querySelectorAll('#spoofX, #spoofY, #spoofZ');
    coordInputs.forEach(input => {
        if (input && input.value && input.value !== '***') {
            input.dataset.originalValue = input.value;
            input.value = '***';
            input.style.color = '#666';
            // Don't disable the inputs - keep them editable for setting new coordinates
            // input.disabled = true;
        }
    });

    // Hide any coordinate displays in the interface
    const coordDisplays = document.querySelectorAll('.coord-display, .position-display');
    coordDisplays.forEach(display => {
        if (display && display.textContent && display.textContent !== '***HIDDEN***') {
            display.dataset.originalText = display.textContent;
            display.textContent = '***HIDDEN***';
            display.style.color = '#666';
        }
    });
}

function showCoordinates() {
    // Set coordsHidden to false first
    coordsHidden = false;

    // Restore coordinates in Coordinate Spoofing inputs
    const coordInputs = document.querySelectorAll('#spoofX, #spoofY, #spoofZ');
    coordInputs.forEach(input => {
        if (input && input.dataset.originalValue !== undefined) {
            input.value = input.dataset.originalValue;
            input.style.color = '';
            // input.disabled = false; // Not needed since we don't disable them anymore
            delete input.dataset.originalValue;
        }
    });

    // Restore coordinate displays with formatting
    const coordDisplays = document.querySelectorAll('.coord-display, .position-display');
    coordDisplays.forEach(display => {
        if (display && display.dataset.originalText !== undefined) {
            const formattedText = formatCoordinates(display.dataset.originalText);
            display.textContent = formattedText;
            display.style.color = '';
            delete display.dataset.originalText;
        }
    });

    // Force reload data to show coordinates
    loadGameStats();
    loadSpoofStatus();
}

function updateCoordsVisibilityButton(hidden) {
    if (elements.coordsVisibilityText) {
        elements.coordsVisibilityText.textContent = hidden ? 'Show Coords' : 'Hide Coords';
    }

    if (elements.toggleCoordsVisibility) {
        const icon = elements.toggleCoordsVisibility.querySelector('i');
        if (icon) {
            icon.className = hidden ? 'fas fa-eye' : 'fas fa-eye-slash';
        }

        // Update button style - orange when hidden (default), green when visible
        if (hidden) {
            elements.toggleCoordsVisibility.classList.add('btn-warning');
            elements.toggleCoordsVisibility.classList.remove('btn-success');
        } else {
            elements.toggleCoordsVisibility.classList.add('btn-success');
            elements.toggleCoordsVisibility.classList.remove('btn-warning');
        }
    }
}

// Store original loadGameStats function
let originalLoadGameStats = null;

// Override loadGameStats to respect coordinates visibility
function setupLoadGameStatsOverride() {
    if (!originalLoadGameStats) {
        // Find the original function in the global scope
        originalLoadGameStats = window.loadGameStats || loadGameStats;
    }
}

// New loadGameStats that respects coordinate visibility
async function loadGameStats() {
    try {
        const response = await fetch('/api/stats', { method: 'POST' });
        const stats = await response.json();

        // Update position - respect coordinate visibility and format coordinates
        if (elements.playerPosition) {
            if (coordsHidden) {
                elements.playerPosition.textContent = '***HIDDEN***';
            } else {
                const formattedPosition = formatCoordinates(stats.position || 'Unknown');
                elements.playerPosition.textContent = formattedPosition;
            }
        }

        // Always update non-coordinate data
        if (elements.playerBiome) {
            elements.playerBiome.textContent = stats.biome || 'Unknown';
        }

        if (elements.gameTime) {
            elements.gameTime.textContent = stats.time || 'Unknown';
        }

        if (elements.gameFPS) {
            elements.gameFPS.textContent = stats.fps || 'Unknown';
        }
    } catch (error) {
        console.error('Failed to load game stats:', error);

        // Set fallback values
        if (elements.playerPosition) {
            elements.playerPosition.textContent = coordsHidden ? '***HIDDEN***' : 'Not available';
        }
        if (elements.playerBiome) elements.playerBiome.textContent = 'Not available';
        if (elements.gameTime) elements.gameTime.textContent = 'Not available';
        if (elements.gameFPS) elements.gameFPS.textContent = 'Not available';
    }
}

async function checkConnectionStatus() {
    try {
        const response = await fetch('/api/status', { 
            method: 'GET',
            timeout: 3000 
        });
        
        if (response.ok) {
            updateConnectionStatus(true);
        } else {
            updateConnectionStatus(false);
        }
    } catch (error) {
        updateConnectionStatus(false);
    }
}

function updateConnectionStatus(connected) {
    if (elements.connectionStatus && elements.statusText) {
        if (connected) {
            elements.connectionStatus.style.background = '#10B981'; // Green
            elements.statusText.textContent = 'Connected';
        } else {
            elements.connectionStatus.style.background = '#EF4444'; // Red
            elements.statusText.textContent = 'Disconnected';
        }
    }
}

// Utility function to format coordinates with thousand separators
function formatCoordinates(coordString) {
    if (!coordString || coordString === 'Unknown' || coordString === 'Not available' || coordString === '***HIDDEN***') {
        return coordString;
    }

    // Extract X, Y, Z coordinates from string like "X: 1234, Y: 64, Z: 5678"
    return coordString.replace(/([XYZ]: ?)(-?\d+)/g, (match, prefix, number) => {
        // Format number with dots as thousand separators
        const formattedNumber = formatNumber(parseInt(number));
        return prefix + formattedNumber;
    });
}

// Manual formatting function for better control
function formatNumber(num) {
    if (isNaN(num)) return num.toString();

    // Handle negative numbers
    const isNegative = num < 0;
    const absNum = Math.abs(num);

    // Convert to string and add dots every 3 digits from right to left
    const formatted = absNum.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');

    return isNegative ? '-' + formatted : formatted;
}

// Ensure coordinate input fields are always editable
function ensureCoordInputsEditable() {
    const coordInputs = document.querySelectorAll('#spoofX, #spoofY, #spoofZ');
    coordInputs.forEach(input => {
        if (input) {
            input.disabled = false;
            input.style.color = '';
            console.log('Ensured input is editable:', input.id, 'disabled:', input.disabled);
        }
    });
}

// Global function for toast close button
window.hideToast = hideToast;