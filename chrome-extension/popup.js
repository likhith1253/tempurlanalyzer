// DOM Elements
const blockedTab = document.getElementById('blocked-tab');
const settingsTab = document.getElementById('settings-tab');
const tabButtons = document.querySelectorAll('.tab-button');
const blockedList = document.getElementById('blocked-list');
const whitelistInput = document.getElementById('whitelist-input');
const blacklistInput = document.getElementById('blacklist-input');
const addWhitelistBtn = document.getElementById('add-whitelist');
const addBlacklistBtn = document.getElementById('add-blacklist');
const clearCacheBtn = document.getElementById('clear-cache');
const whitelistItems = document.getElementById('whitelist-items');
const blacklistItems = document.getElementById('blacklist-items');

// State
let whitelist = [];
let blacklist = [];

// Initialize the popup
document.addEventListener('DOMContentLoaded', () => {
  // Load data from storage
  loadData();
  
  // Set up tab switching
  setupTabs();
  
  // Set up event listeners
  setupEventListeners();
});

// Load data from Chrome storage
function loadData() {
  // Load whitelist and blacklist
  chrome.storage.local.get(['whitelist', 'blacklist'], (data) => {
    whitelist = data.whitelist || [];
    blacklist = data.blacklist || [];
    renderLists();
  });
  
  // Load blocked history
  chrome.runtime.sendMessage({ action: 'getBlockedHistory' }, (response) => {
    renderBlockedHistory(response?.blockedHistory || []);
  });
}

// Set up tab switching
function setupTabs() {
  tabButtons.forEach(button => {
    button.addEventListener('click', () => {
      // Update active tab button
      tabButtons.forEach(btn => btn.classList.remove('active'));
      button.classList.add('active');
      
      // Show the corresponding tab content
      const tabId = button.getAttribute('data-tab');
      document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
      });
      document.getElementById(`${tabId}-tab`).classList.add('active');
    });
  });
}

// Set up event listeners
function setupEventListeners() {
  // Add to whitelist
  addWhitelistBtn.addEventListener('click', () => {
    const pattern = whitelistInput.value.trim();
    if (pattern && !whitelist.includes(pattern)) {
      whitelist.push(pattern);
      chrome.storage.local.set({ whitelist }, () => {
        whitelistInput.value = '';
        renderLists();
      });
    }
  });
  
  // Add to blacklist
  addBlacklistBtn.addEventListener('click', () => {
    const pattern = blacklistInput.value.trim();
    if (pattern && !blacklist.includes(pattern)) {
      blacklist.push(pattern);
      chrome.storage.local.set({ blacklist }, () => {
        blacklistInput.value = '';
        renderLists();
      });
    }
  });
  
  // Clear cache
  clearCacheBtn.addEventListener('click', () => {
    chrome.runtime.sendMessage({ action: 'clearCache' }, () => {
      // Show a confirmation message
      const originalText = clearCacheBtn.textContent;
      clearCacheBtn.textContent = 'Cache Cleared!';
      clearCacheBtn.disabled = true;
      
      setTimeout(() => {
        clearCacheBtn.textContent = originalText;
        clearCacheBtn.disabled = false;
      }, 2000);
    });
  });
  
  // Handle Enter key in input fields
  whitelistInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') addWhitelistBtn.click();
  });
  
  blacklistInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') addBlacklistBtn.click();
  });
}

// Render whitelist and blacklist
function renderLists() {
  // Render whitelist
  whitelistItems.innerHTML = whitelist.length > 0 
    ? whitelist.map(item => createListItem(item, 'whitelist')).join('')
    : '<div class="empty-state">No whitelisted items</div>';
  
  // Add event listeners to whitelist remove buttons
  document.querySelectorAll('.remove-whitelist').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const pattern = e.target.closest('.list-item').getAttribute('data-pattern');
      whitelist = whitelist.filter(item => item !== pattern);
      chrome.storage.local.set({ whitelist }, renderLists);
    });
  });
  
  // Render blacklist
  blacklistItems.innerHTML = blacklist.length > 0 
    ? blacklist.map(item => createListItem(item, 'blacklist')).join('')
    : '<div class="empty-state">No blacklisted items</div>';
  
  // Add event listeners to blacklist remove buttons
  document.querySelectorAll('.remove-blacklist').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const pattern = e.target.closest('.list-item').getAttribute('data-pattern');
      blacklist = blacklist.filter(item => item !== pattern);
      chrome.storage.local.set({ blacklist }, renderLists);
    });
  });
}

// Create a list item for whitelist/blacklist
function createListItem(pattern, type) {
  return `
    <div class="list-item" data-pattern="${pattern}">
      <span>${pattern}</span>
      <button class="remove-${type}" title="Remove">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M19 6.41L17.59 5L12 10.59L6.41 5L5 6.41L10.59 12L5 17.59L6.41 19L12 13.41L17.59 19L19 17.59L13.41 12L19 6.41Z" fill="currentColor"/>
        </svg>
      </button>
    </div>
  `;
}

// Render blocked history
function renderBlockedHistory(history) {
  if (!history || history.length === 0) {
    blockedList.innerHTML = '<div class="empty-state">No blocked websites yet</div>';
    return;
  }
  
  blockedList.innerHTML = history.map(item => {
    const url = new URL(item.originalUrl || item.url);
    const timestamp = new Date(item.timestamp).toLocaleString();
    
    return `
      <div class="blocked-item">
        <div class="blocked-url" title="${url}">
          ${url.hostname}
        </div>
        <div class="blocked-meta">
          <span>${timestamp}</span>
          <span>Source: ${item.source || 'unknown'}</span>
        </div>
      </div>
    `;
  }).join('');
  
  // Add click handlers to blocked items
  document.querySelectorAll('.blocked-item').forEach((item, index) => {
    item.addEventListener('click', () => {
      // Toggle details on click
      const details = item.querySelector('.blocked-details');
      if (details) {
        details.remove();
      } else {
        const entry = history[index];
        const detailsEl = document.createElement('div');
        detailsEl.className = 'blocked-details';
        detailsEl.innerHTML = `
          <p><strong>URL:</strong> ${entry.url}</p>
          <p><strong>Blocked by:</strong> ${entry.source || 'Unknown'}</p>
          <p><strong>Time:</strong> ${new Date(entry.timestamp).toLocaleString()}</p>
          <div class="actions">
            <button class="btn secondary whitelist-btn" data-url="${entry.url}">Whitelist Domain</button>
            <button class="btn secondary blacklist-btn" data-url="${entry.url}">Blacklist Domain</button>
          </div>
        `;
        item.appendChild(detailsEl);
        
        // Add event listeners to the new buttons
        const whitelistBtn = item.querySelector('.whitelist-btn');
        const blacklistBtn = item.querySelector('.blacklist-btn');
        
        if (whitelistBtn) {
          whitelistBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const domain = new URL(whitelistBtn.dataset.url).hostname;
            if (!whitelist.includes(domain)) {
              whitelist.push(domain);
              chrome.storage.local.set({ whitelist }, renderLists);
            }
          });
        }
        
        if (blacklistBtn) {
          blacklistBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const domain = new URL(blacklistBtn.dataset.url).hostname;
            if (!blacklist.includes(domain)) {
              blacklist.push(domain);
              chrome.storage.local.set({ blacklist }, renderLists);
            }
          });
        }
      }
    });
  });
}

// Request blocked history from background script
function updateBlockedHistory() {
  chrome.runtime.sendMessage({ action: 'getBlockedHistory' }, (response) => {
    renderBlockedHistory(response?.blockedHistory || []);
  });
}

// Update the UI every 5 seconds to show new blocked sites
setInterval(updateBlockedHistory, 5000);
