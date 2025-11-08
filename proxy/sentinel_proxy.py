#!/usr/bin/env python3
"""
Sentinel Proxy - A mitmproxy addon for URL analysis and content filtering.

This script intercepts HTTP/HTTPS requests, analyzes them against a backend API,
and takes actions based on the analysis results (BLOCK, WARN, or ALLOW).
"""

import json
import os
import time
import sqlite3
import logging
from pathlib import Path
from datetime import datetime
from typing import Dict, Optional, Tuple, Any

import requests
from mitmproxy import http, ctx

# Configuration
BACKEND_URL = "http://localhost:8080/analyze/url"
REQUEST_TIMEOUT = 5  # seconds
CACHE_TTL = 3600  # 1 hour in seconds
BLACKLIST_FILE = "blacklist.txt"
LOG_FILE = "sentinel_proxy.log"
DB_FILE = "sentinel_cache.db"

# Logging setup
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("SentinelProxy")


class URLCache:
    """Simple SQLite-based cache for URL analysis results."""
    
    def __init__(self, db_path: str = DB_FILE):
        self.db_path = db_path
        self._init_db()
    
    def _init_db(self):
        """Initialize the SQLite database."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS url_cache (
                    url TEXT PRIMARY KEY,
                    decision TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    expires INTEGER NOT NULL
                )
            """)
            conn.execute("""
                CREATE INDEX IF NOT EXISTS idx_url_cache_expires 
                ON url_cache(expires)
            """)
    
    def get(self, url: str) -> Optional[str]:
        """Get a cached decision for a URL."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(
                "SELECT decision FROM url_cache WHERE url = ? AND expires > ?",
                (url, int(time.time()))
            )
            result = cursor.fetchone()
            return result[0] if result else None
    
    def set(self, url: str, decision: str, ttl: int = CACHE_TTL) -> None:
        """Cache a URL decision."""
        timestamp = int(time.time())
        expires = timestamp + ttl
        
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO url_cache 
                (url, decision, timestamp, expires) 
                VALUES (?, ?, ?, ?)
                """,
                (url, decision, timestamp, expires)
            )
    
    def cleanup(self) -> None:
        """Remove expired cache entries."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                "DELETE FROM url_cache WHERE expires <= ?",
                (int(time.time()),)
            )


class RequestLogger:
    """Logs requests and their analysis results."""
    
    def __init__(self, log_file: str = LOG_FILE):
        self.log_file = log_file
        self._ensure_log_file()
    
    def _ensure_log_file(self) -> None:
        """Ensure the log file and directory exist."""
        log_dir = os.path.dirname(os.path.abspath(self.log_file))
        if not os.path.exists(log_dir):
            os.makedirs(log_dir, exist_ok=True)
        
        if not os.path.exists(self.log_file):
            with open(self.log_file, 'w') as f:
                f.write("timestamp,url,host,decision,action\n")
    
    def log_request(self, url: str, host: str, decision: str, action: str) -> None:
        """Log a request and its analysis result."""
        timestamp = datetime.utcnow().isoformat()
        log_entry = f'"{timestamp}","{url}","{host}","{decision}","{action}"\n'
        try:
            with open(self.log_file, 'a') as f:
                f.write(log_entry)
        except Exception as e:
            logger.error(f"Failed to write to log file: {e}")


class SentinelAddon:
    """Main mitmproxy addon for URL analysis and filtering."""
    
    def __init__(self):
        self.cache = URLCache()
        self.logger = RequestLogger()
        self.blacklist = self._load_blacklist()
        self.offline_mode = False
        self.offline_since = None
        
        # Clean up old cache entries on startup
        self.cache.cleanup()
    
    def _load_blacklist(self) -> set:
        """Load the local blacklist file."""
        try:
            if os.path.exists(BLACKLIST_FILE):
                with open(BLACKLIST_FILE, 'r') as f:
                    return {line.strip() for line in f if line.strip() and not line.startswith('#')}
        except Exception as e:
            logger.error(f"Failed to load blacklist: {e}")
        return set()
    
    def _check_backend_health(self) -> bool:
        """Check if the backend API is available."""
        if self.offline_mode:
            # If we've been offline for more than 5 minutes, try to reconnect
            if time.time() - self.offline_since > 300:  # 5 minutes
                try:
                    response = requests.head(BACKEND_URL, timeout=3)
                    if response.status_code < 500:
                        self.offline_mode = False
                        self.offline_since = None
                        logger.info("Backend is back online")
                        return True
                except:
                    pass
            return False
        
        try:
            response = requests.head(BACKEND_URL, timeout=3)
            return response.status_code < 500
        except:
            if not self.offline_mode:
                self.offline_mode = True
                self.offline_since = time.time()
                logger.warning("Backend is offline, falling back to local blacklist")
            return False
    
    def _analyze_url(self, url: str, host: str) -> Tuple[str, str]:
        """Analyze a URL using the backend API or local blacklist."""
        # Check cache first
        cached_decision = self.cache.get(url)
        if cached_decision:
            return cached_decision, "cached"
        
        # Check blacklist if in offline mode or backend is down
        if self.offline_mode or not self._check_backend_health():
            if any(domain in host for domain in self.blacklist):
                self.cache.set(url, "BLOCK")
                return "BLOCK", "blacklist"
            return "ALLOW", "offline_allow"
        
        # Query the backend API
        try:
            response = requests.post(
                BACKEND_URL,
                json={"url": url, "host": host},
                timeout=REQUEST_TIMEOUT
            )
            response.raise_for_status()
            
            data = response.json()
            decision = data.get("decision", "ALLOW").upper()
            
            # Cache the decision
            self.cache.set(url, decision)
            
            return decision, "api"
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Error querying backend: {e}")
            self.offline_mode = True
            self.offline_since = time.time()
            
            # Fall back to blacklist
            if any(domain in host for domain in self.blacklist):
                return "BLOCK", "blacklist"
            return "ALLOW", "error_allow"
    
    def request(self, flow: http.HTTPFlow) -> None:
        """Intercept and analyze HTTP requests."""
        url = flow.request.pretty_url
        host = flow.request.host
        
        # Skip localhost and internal IPs
        if host in ('localhost', '127.0.0.1', '::1') or host.startswith('192.168.'):
            return
        
        # Analyze the URL
        decision, source = self._analyze_url(url, host)
        
        # Log the request
        self.logger.log_request(url, host, decision, source)
        
        # Take action based on the decision
        if decision == "BLOCK":
            self._block_request(flow, "Access to this website has been blocked by your administrator.")
        elif decision == "WARN":
            # We'll handle this in the response
            pass
        # ALLOW: Continue with the request
    
    def response(self, flow: http.HTTPFlow) -> None:
        """Modify responses if needed (e.g., inject warning banner)."""
        url = flow.request.pretty_url
        host = flow.request.host
        
        # Check if this request was previously analyzed as WARN
        cached_decision = self.cache.get(url)
        if cached_decision == "WARN" and "text/html" in flow.response.headers.get("content-type", ""):
            self._inject_warning_banner(flow)
    
    def _block_request(self, flow: http.HTTPFlow, message: str) -> None:
        """Block the request and return a blocking page."""
        flow.response = http.Response.make(
            403,  # Forbidden
            f"""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Access Blocked</title>
                <style>
                    body {{ 
                        font-family: Arial, sans-serif; 
                        text-align: center; 
                        padding: 50px; 
                        background-color: #f8f9fa;
                    }}
                    .container {{ 
                        max-width: 600px; 
                        margin: 0 auto; 
                        background: white; 
                        padding: 30px; 
                        border-radius: 8px; 
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                    }}
                    h1 {{ color: #dc3545; }}
                    .message {{ 
                        margin: 20px 0; 
                        padding: 15px; 
                        background-color: #f8d7da; 
                        border: 1px solid #f5c6cb;
                        border-radius: 4px;
                        color: #721c24;
                    }}
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Access Blocked</h1>
                    <div class="message">
                        {message}
                    </div>
                    <p>URL: {flow.request.pretty_url}</p>
                    <p>If you believe this is an error, please contact your administrator.</p>
                </div>
            </body>
            </html>
            """.format(message=message),
            {"Content-Type": "text/html; charset=utf-8"}
        )
    
    def _inject_warning_banner(self, flow: http.HTTPFlow) -> None:
        """Inject a warning banner into the HTML response."""
        try:
            content = flow.response.content.decode('utf-8', errors='replace')
            
            # Don't inject if we've already done so
            if 'sentinel-warning-banner' in content:
                return
            
            # Find the opening body tag
            body_tag = '<body'
            body_pos = content.lower().find(body_tag)
            
            if body_pos == -1:
                return  # No body tag found
            
            # Find the end of the body tag
            body_end = content.find('>', body_pos) + 1
            
            # Create the warning banner
            banner = """
            <div id="sentinel-warning-banner" style="
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                background-color: #fff3cd;
                color: #856404;
                padding: 10px 20px;
                text-align: center;
                border-bottom: 1px solid #ffeeba;
                z-index: 10000;
                font-family: Arial, sans-serif;
                font-size: 14px;
                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            ">
                ⚠️ <strong>Warning:</strong> This website has been flagged for potential security risks. 
                Proceed with caution.
                <button onclick="this.parentNode.style.display='none'" style="
                    margin-left: 15px;
                    background: #ffc107;
                    border: none;
                    border-radius: 3px;
                    padding: 2px 8px;
                    cursor: pointer;
                    font-weight: bold;
                ">Dismiss</button>
            </div>
            <style>
                body { padding-top: 50px !important; }
            </style>
            """
            
            # Insert the banner after the opening body tag
            new_content = (
                content[:body_end] + 
                banner + 
                content[body_end:]
            )
            
            # Update the response
            flow.response.content = new_content.encode('utf-8')
            
            # Remove content-length header as we've modified the content
            if 'content-length' in flow.response.headers:
                del flow.response.headers['content-length']
                
        except Exception as e:
            logger.error(f"Error injecting warning banner: {e}")


# Create and register the addon
addons = [SentinelAddon()]

if __name__ == "__main__":
    # This allows running the script directly for testing
    from mitmproxy.tools import cmdline
    from mitmproxy.tools.dump import DumpMaster
    
    # Set up argument parser
    parser = cmdline.mitmdump()
    args = parser.parse_args([
        "-s", __file__,  # Load this script as an addon
        "--listen-port", "8081",  # Listen on port 8081
        "--mode", "transparent",  # Transparent proxy mode
        "--showhost"  # Show host in console output
    ])
    
    # Start the proxy
    m = DumpMaster(args)
    m.run()
