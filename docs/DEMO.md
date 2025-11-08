# 🎬 SentinelAI Guard - Demo Guide

This guide will help you demonstrate SentinelAI Guard's capabilities effectively.

## 🚀 Quick Demo Setup

1. **Start the system** (if not already running):
   ```bash
   docker-compose up -d
   ```

2. **Configure your browser** to use the proxy:
   - **Chrome/Edge**:
     1. Go to `chrome://settings/system`
     2. Enable "Use a proxy server"
     3. Set address: `localhost` and port: `8081`
   - **Firefox**:
     1. Go to `about:preferences#general`
     2. Scroll to Network Settings > Settings
     3. Select "Manual proxy configuration"
     4. Set HTTP Proxy: `localhost` Port: `8081`

## 🎯 Demo Scenarios

### 1. Basic URL Analysis
1. Visit: `http://example.com` (should be allowed)
2. Visit a known malicious URL (will be blocked)
3. Check the dashboard to see the analysis results

### 2. Real-time Protection
1. Open the dashboard at `http://localhost:3000`
2. In another tab, visit various websites
3. Watch the dashboard update in real-time

### 3. Threat Simulation
1. Try accessing these test URLs:
   - `http://malware.wicar.org/` (known malicious)
   - `http://phishing.example.com` (test phishing)
   - `http://malware.testing.google.test` (test malware)

2. Observe the blocking behavior and dashboard alerts

## 📊 Expected Outcomes

| Action | Expected Result |
|--------|-----------------|
| Visit safe site | Page loads normally |
| Visit malicious site | Block page with warning |
| Check dashboard | Real-time updates of blocked requests |
| View analytics | Threat statistics and patterns |

## 🎥 Recording Tips

1. **Start with the problem**:
   - Show a regular browser without protection
   - Demonstrate visiting a malicious site (in a sandbox)

2. **Introduce SentinelAI Guard**:
   - Show the dashboard
   - Explain the protection in simple terms

3. **Demonstrate protection**:
   - Try accessing the same malicious site
   - Show the block page
   - Highlight the real-time dashboard updates

4. **Showcase features**:
   - Threat analysis details
   - Historical data
   - Configuration options

## 🧹 Cleanup

After the demo, reset the proxy settings in your browser to "Use system proxy settings" or "No proxy".
