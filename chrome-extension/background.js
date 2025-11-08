// Configuration
const CONFIG = {
  API_ENDPOINT: 'http://localhost:8080/analyze/url',
  CACHE_TTL: 5 * 60 * 1000, // 5 minutes
  MAX_CACHE_ENTRIES: 1000,
  MAX_BLOCKED_HISTORY: 100
};

// Cache for API responses
let urlCache = new Map();
let blockedHistory = [];

// Load data from storage
async function loadStorage() {
  try {
    const data = await chrome.storage.local.get(['urlCache', 'blockedHistory', 'whitelist', 'blacklist']);
    if (data.urlCache) {
      // Filter out expired cache entries
      const now = Date.now();
      urlCache = new Map(
        Object.entries(data.urlCache).filter(([_, entry]) => entry.expires > now)
      );
    }
    if (data.blockedHistory) {
      blockedHistory = data.blockedHistory;
    }
    if (!data.whitelist) {
      await chrome.storage.local.set({ whitelist: [] });
    }
    if (!data.blacklist) {
      await chrome.storage.local.set({ blacklist: [] });
    }
  } catch (error) {
    console.error('Error loading storage:', error);
  }
}

// Save data to storage
async function saveToStorage() {
  try {
    // Clean up cache before saving
    const now = Date.now();
    for (const [url, entry] of urlCache.entries()) {
      if (entry.expires <= now) {
        urlCache.delete(url);
      }
    }
    
    // Limit blocked history size
    if (blockedHistory.length > CONFIG.MAX_BLOCKED_HISTORY) {
      blockedHistory = blockedHistory.slice(-CONFIG.MAX_BLOCKED_HISTORY);
    }
    
    await chrome.storage.local.set({
      urlCache: Object.fromEntries(urlCache),
      blockedHistory
    });
  } catch (error) {
    console.error('Error saving to storage:', error);
  }
}

// Check URL against local whitelist/blacklist
async function checkLocalLists(url) {
  try {
    const { whitelist, blacklist } = await chrome.storage.local.get(['whitelist', 'blacklist']);
    
    // Check whitelist first
    if (whitelist && whitelist.some(pattern => 
      new RegExp(pattern.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g='[^ ]*') + '$').test(url)
    )) {
      return { decision: 'ALLOW', source: 'whitelist' };
    }
    
    // Check blacklist
    if (blacklist && blacklist.some(pattern => 
      new RegExp(pattern.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g='[^ ]*') + '$').test(url)
    )) {
      return { decision: 'BLOCK', source: 'blacklist' };
    }
    
    return null;
  } catch (error) {
    console.error('Error checking local lists:', error);
    return null;
  }
}

// Analyze URL using the API
async function analyzeUrl(url) {
  // Check local lists first
  const localResult = await checkLocalLists(url);
  if (localResult) {
    return localResult;
  }
  
  // Check cache
  const cached = urlCache.get(url);
  if (cached && cached.expires > Date.now()) {
    return { decision: cached.decision, source: 'cache' };
  }
  
  try {
    const response = await fetch(CONFIG.API_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ url })
    });
    
    if (!response.ok) {
      throw new Error(`API request failed: ${response.status}`);
    }
    
    const data = await response.json();
    const decision = data.decision?.toUpperCase() || 'ALLOW';
    
    // Cache the result
    urlCache.set(url, {
      decision,
      expires: Date.now() + CONFIG.CACHE_TTL,
      timestamp: Date.now()
    });
    
    // Save to storage in the background
    saveToStorage();
    
    return { decision, source: 'api' };
    
  } catch (error) {
    console.error('Error analyzing URL:', error);
    return { decision: 'ALLOW', source: 'error', error: error.message };
  }
}

// Handle web requests
chrome.webRequest.onBeforeRequest.addListener(
  async (details) => {
    // Skip non-HTTP/HTTPS requests
    if (!details.url.startsWith('http')) {
      return { cancel: false };
    }
    
    // Get the main frame URL for top-level navigation
    const isMainFrame = details.type === 'main_frame';
    const url = isMainFrame ? details.url : details.documentUrl || details.url;
    
    try {
      const { decision, source } = await analyzeUrl(url);
      
      if (decision === 'BLOCK') {
        // Add to blocked history for the popup
        blockedHistory.unshift({
          url,
          timestamp: Date.now(),
          source,
          originalUrl: details.url
        });
        await saveToStorage();
        
        if (isMainFrame) {
          // For main frame, redirect to blocked page
          const blockedUrl = chrome.runtime.getURL(
            `blocked.html?url=${encodeURIComponent(url)}&source=${encodeURIComponent(source)}`
          );
          return { redirectUrl: blockedUrl };
        }
        
        // For sub-resources, just block them
        return { cancel: true };
      }
    } catch (error) {
      console.error('Error processing request:', error);
    }
    
    return { cancel: false };
  },
  { urls: ['<all_urls>'] },
  ['blocking']
);

// Listen for messages from the popup
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'getBlockedHistory') {
    sendResponse({ blockedHistory: blockedHistory.slice(0, 10) });
  } else if (request.action === 'addToWhitelist') {
    chrome.storage.local.get(['whitelist'], (data) => {
      const whitelist = data.whitelist || [];
      if (!whitelist.includes(request.pattern)) {
        whitelist.push(request.pattern);
        chrome.storage.local.set({ whitelist });
      }
    });
  } else if (request.action === 'addToBlacklist') {
    chrome.storage.local.get(['blacklist'], (data) => {
      const blacklist = data.blacklist || [];
      if (!blacklist.includes(request.pattern)) {
        blacklist.push(request.pattern);
        chrome.storage.local.set({ blacklist });
      }
    });
  } else if (request.action === 'clearCache') {
    urlCache.clear();
    saveToStorage();
  }
});

// Initialize
loadStorage();
