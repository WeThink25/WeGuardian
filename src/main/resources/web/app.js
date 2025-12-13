const API_BASE = window.location.origin;


let config = null;



document.addEventListener('DOMContentLoaded', async () => {
    await loadConfig();
    setupEventListeners();
    loadRecentPunishments();
});

async function loadConfig() {
    try {
        const response = await fetch(`${API_BASE}/api/config`);
        if (!response.ok) throw new Error('Config fetch failed');
        config = await response.json();

        if (config.serverName) {
            document.getElementById('server-name').textContent = config.serverName;
            document.title = `${config.serverName} - Punishment Panel`;
        }

        if (config.accentColor) {
            document.documentElement.style.setProperty('--accent', config.accentColor);
            const glowColor = config.accentColor + '4d'; // 30% opacity
            document.documentElement.style.setProperty('--accent-glow', glowColor);
        }

        if (config.footerText) {
            document.getElementById('footer-text').textContent = config.footerText;
        }
    } catch (error) {
        console.error('Failed to load config:', error);
    }
}

function setupEventListeners() {
    const searchBtn = document.getElementById('search-btn');
    const usernameInput = document.getElementById('username-input');

    if (searchBtn) {
        searchBtn.addEventListener('click', performSearch);
    }

    if (usernameInput) {
        usernameInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
    }
}



async function loadRecentPunishments() {
    const container = document.getElementById('recent-punishments');
    if (!container) return;

    try {
        const response = await fetch(`${API_BASE}/api/recent?limit=10`);
        if (!response.ok) throw new Error('Failed to fetch');

        const data = await response.json();

        if (!data.punishments || data.punishments.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <span class="empty-icon">✨</span>
                    <p>No punishments recorded yet</p>
                </div>
            `;
            return;
        }

        container.innerHTML = data.punishments.map(p => createPunishmentCard(p, p.active && !p.expired)).join('');
    } catch (error) {
        console.error('Failed to load recent punishments:', error);
        container.innerHTML = `
            <div class="empty-state">
                <span class="empty-icon">⚠️</span>
                <p>Could not load recent punishments</p>
            </div>
        `;
    }
}



async function performSearch() {
    const usernameInput = document.getElementById('username-input');
    if (!usernameInput) return;

    const username = usernameInput.value.trim();

    if (!username) {
        showError('Invalid Username', 'Please enter a valid Minecraft username.');
        return;
    }

    showLoading();

    try {
        const response = await fetch(`${API_BASE}/api/lookup/${encodeURIComponent(username)}`);
        const data = await response.json();

        if (data.error) {
            showError('Player Not Found', data.message || 'Could not find that player.');
            return;
        }

        showResults(data);
    } catch (error) {
        console.error('Search error:', error);
        showError('Connection Error', 'Failed to connect to the server. Please try again.');
    }
}



function showLoading() {
    hideAllSections();
    document.getElementById('loading-section').style.display = 'flex';
}

function showResults(data) {
    hideAllSections();

    const resultsSection = document.getElementById('results-section');
    resultsSection.style.display = 'block';

    document.getElementById('player-name').textContent = data.username;
    document.getElementById('player-uuid').textContent = data.uuid;
    document.getElementById('player-avatar').src =
        `https://mc-heads.net/avatar/${data.uuid}/100`;

    const statusContainer = document.getElementById('player-status');
    const hasActivePunishments = data.activePunishments && data.activePunishments.length > 0;

    if (hasActivePunishments) {
        statusContainer.innerHTML = `
            <span class="status-badge punished">⚠ ${data.activePunishments.length} Active Punishment(s)</span>
        `;
    } else {
        statusContainer.innerHTML = `
            <span class="status-badge clean">✓ Clean Record</span>
        `;
    }

    renderActivePunishments(data.activePunishments || []);

    renderPunishmentHistory(data.punishments || []);
}

function showError(title, message) {
    hideAllSections();

    document.getElementById('error-title').textContent = title;
    document.getElementById('error-message').textContent = message;
    document.getElementById('error-section').style.display = 'block';
}

function hideError() {
    document.getElementById('error-section').style.display = 'none';
    document.getElementById('recent-section').style.display = 'block';
}

function backToHome() {
    hideAllSections();
    document.getElementById('recent-section').style.display = 'block';
    document.getElementById('username-input').value = '';
}

function hideAllSections() {
    const sections = ['results-section', 'loading-section', 'error-section', 'recent-section'];
    sections.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
}



function renderActivePunishments(punishments) {
    const container = document.getElementById('active-punishments');

    if (!punishments || punishments.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <span class="empty-icon">✨</span>
                <p>No active punishments</p>
            </div>
        `;
        return;
    }

    container.innerHTML = punishments.map(p => createPunishmentCard(p, true)).join('');
}

function renderPunishmentHistory(punishments) {
    const container = document.getElementById('punishment-history');

    if (!punishments || punishments.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <span class="empty-icon">📭</span>
                <p>No punishment history</p>
            </div>
        `;
        return;
    }

    container.innerHTML = punishments.map(p => createPunishmentCard(p, false)).join('');
}

function createPunishmentCard(punishment, isActive) {
    const typeIcons = {
        'BAN': '🚫',
        'TEMPBAN': '🚫',
        'MUTE': '🔇',
        'TEMPMUTE': '🔇',
        'KICK': '⚡',
        'BANIP': '🌐',
        'TEMPBANIP': '🌐',
        'MUTEIP': '🔇',
        'TEMPMUTEIP': '🔇'
    };

    const typeLabels = {
        'BAN': 'Banned',
        'TEMPBAN': 'Temp Banned',
        'MUTE': 'Muted',
        'TEMPMUTE': 'Temp Muted',
        'KICK': 'Kicked',
        'BANIP': 'IP Banned',
        'TEMPBANIP': 'Temp IP Banned',
        'MUTEIP': 'IP Muted',
        'TEMPMUTEIP': 'Temp IP Muted'
    };

    const icon = typeIcons[punishment.type] || '⚠';
    const label = typeLabels[punishment.type] || punishment.type;
    const typeClass = punishment.type.toLowerCase().replace('_', '-').replace('temp', '');

    const createdDate = punishment.createdAt ? formatDate(punishment.createdAt) : 'Unknown';
    const expiresText = punishment.permanent ? 'Permanent' :
        (punishment.expiresAt ? formatDate(punishment.expiresAt) : 'Unknown');

    const isCurrentlyActive = punishment.active && !punishment.expired;
    const statusBadge = isCurrentlyActive
        ? '<span class="punishment-active">ACTIVE</span>'
        : '<span class="punishment-expired">EXPIRED</span>';

    return `
        <div class="punishment-card ${typeClass}">
            <div class="punishment-header">
                <div class="punishment-type">
                    <span class="punishment-icon">${icon}</span>
                    <span class="punishment-label">${label}</span>
                </div>
                ${statusBadge}
            </div>
            <div class="punishment-target">
                <strong>${punishment.targetName || 'Unknown'}</strong>
            </div>
            <div class="punishment-reason">"${punishment.reason || 'No reason specified'}"</div>
            <div class="punishment-meta">
                <div class="punishment-meta-item">
                    <span>👤</span>
                    <span>Staff: ${punishment.staffName || 'System'}</span>
                </div>
                <div class="punishment-meta-item">
                    <span>📅</span>
                    <span>Issued: ${createdDate}</span>
                </div>
                <div class="punishment-meta-item">
                    <span>⏰</span>
                    <span>Expires: ${expiresText}</span>
                </div>
            </div>
        </div>
    `;
}


function formatDate(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = date - now;

    if (diff > 0) {
        return formatTimeRemaining(diff);
    }

    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatTimeRemaining(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
        return `${days}d ${hours % 24}h remaining`;
    }
    if (hours > 0) {
        return `${hours}h ${minutes % 60}m remaining`;
    }
    if (minutes > 0) {
        return `${minutes}m remaining`;
    }
    return `${seconds}s remaining`;
}
