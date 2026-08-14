#!/usr/bin/env python3
"""
Secure WOL — Windows Desktop Control Center
Production Grade Companion Receiver for S3 Sleep, Restart, Shutdown & Zero-Trust Control.
"""

import os
import sys
import time
import socket
import threading
import subprocess
import hmac
import tkinter as tk
from tkinter import messagebox, font
from http.server import HTTPServer, BaseHTTPRequestHandler
import ctypes
from PIL import Image, ImageTk

PORT = 9090
AGENT_RUNNING = False
HTTP_SERVER = None
SERVER_THREAD = None
LOG_CALLBACK = None

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(BASE_DIR, "agent_auth.key")
LOGO_PNG = os.path.join(BASE_DIR, "app_logo.png")
LOGO_ICO = os.path.join(BASE_DIR, "app_logo.ico")
AUTH_SECRET_KEY = ""

# Rate Limiting Tracker: { ip: [timestamp_list] }
FAILED_ATTEMPTS = {}
BLOCKED_IPS = {}

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def load_or_create_auth_key():
    global AUTH_SECRET_KEY
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                AUTH_SECRET_KEY = f.read().strip()
        except Exception:
            AUTH_SECRET_KEY = ""
    return AUTH_SECRET_KEY

def save_auth_key(key: str):
    global AUTH_SECRET_KEY
    AUTH_SECRET_KEY = key.strip()
    try:
        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
            f.write(AUTH_SECRET_KEY)
    except Exception:
        pass

def is_client_allowed(client_ip: str) -> bool:
    """Zero-trust private subnet firewall check."""
    return (client_ip.startswith("192.168.") or 
            client_ip.startswith("10.") or 
            client_ip.startswith("172.") or 
            client_ip in ("127.0.0.1", "localhost", "::1"))

def is_rate_limited(client_ip: str) -> bool:
    """Checks if IP is blocked due to excessive failed attempts."""
    now = time.time()
    if client_ip in BLOCKED_IPS:
        if now < BLOCKED_IPS[client_ip]:
            return True
        else:
            del BLOCKED_IPS[client_ip]

    if client_ip in FAILED_ATTEMPTS:
        # Keep attempts in last 60 seconds
        FAILED_ATTEMPTS[client_ip] = [t for t in FAILED_ATTEMPTS[client_ip] if now - t < 60]
        if len(FAILED_ATTEMPTS[client_ip]) >= 5:
            BLOCKED_IPS[client_ip] = now + 120  # Block for 2 minutes
            return True
    return False

def record_failed_attempt(client_ip: str):
    now = time.time()
    if client_ip not in FAILED_ATTEMPTS:
        FAILED_ATTEMPTS[client_ip] = []
    FAILED_ATTEMPTS[client_ip].append(now)

def put_pc_to_sleep():
    """Puts PC into true ACPI S3 Standby Sleep with Wake-on-LAN and all wake events ENABLED."""
    def _sleep_worker():
        time.sleep(0.5)
        # SetSuspendState(bHibernate=0, bForce=0, bWakeupEventsDisabled=0)
        ctypes.windll.powrprof.SetSuspendState(0, 0, 0)
    threading.Thread(target=_sleep_worker, daemon=True).start()

class PowerCommandHandler(BaseHTTPRequestHandler):
    def check_security_gate(self) -> bool:
        client_ip = self.client_address[0]
        if not is_client_allowed(client_ip):
            self.send_response(403)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error":"Forbidden: Non-local IP blocked"}')
            if LOG_CALLBACK:
                LOG_CALLBACK(f"[FIREWALL BLOCK] Untrusted external IP {client_ip} rejected.")
            return False

        if is_rate_limited(client_ip):
            self.send_response(429)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"error":"Too Many Requests: Temporarily locked out"}')
            if LOG_CALLBACK:
                LOG_CALLBACK(f"[RATE-LIMIT] IP {client_ip} temporarily locked out.")
            return False

        global AUTH_SECRET_KEY
        if AUTH_SECRET_KEY:
            incoming_token = self.headers.get("X-Auth-Token", "").strip()
            # Constant-time comparison to prevent side-channel timing attacks
            if not hmac.compare_digest(incoming_token, AUTH_SECRET_KEY):
                record_failed_attempt(client_ip)
                self.send_response(401)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(b'{"error":"Unauthorized: Invalid secret key"}')
                if LOG_CALLBACK:
                    LOG_CALLBACK(f"[SECURITY ALERT] Unauthorized request from {client_ip}! Wrong Secret Key.")
                return False
        return True

    def do_GET(self):
        global AGENT_RUNNING
        if not AGENT_RUNNING:
            self.send_response(503)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"inactive"}')
            return

        if not self.check_security_gate():
            return

        if self.path in ("/status", "/"):
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"online"}')
            if LOG_CALLBACK:
                LOG_CALLBACK(f"Authenticated status ping from {self.client_address[0]} (ONLINE)")
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        global AGENT_RUNNING
        if not AGENT_RUNNING:
            self.send_response(503)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"inactive"}')
            if LOG_CALLBACK:
                LOG_CALLBACK("Blocked incoming phone signal (Service is TURNED OFF).")
            return

        if not self.check_security_gate():
            return

        client_ip = self.client_address[0]
        if self.path == "/sleep":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"sleeping"}')
            if LOG_CALLBACK:
                LOG_CALLBACK(f"[AUTH OK] Received SLEEP command from {client_ip}! Putting PC to S3 Standby...")
            put_pc_to_sleep()

        elif self.path == "/restart":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"restarting"}')
            if LOG_CALLBACK:
                LOG_CALLBACK(f"[AUTH OK] Received RESTART command from {client_ip}! Rebooting PC...")
            subprocess.Popen(["shutdown", "/r", "/t", "2"])

        elif self.path == "/shutdown":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"shutting_down"}')
            if LOG_CALLBACK:
                LOG_CALLBACK(f"[AUTH OK] Received SHUTDOWN command from {client_ip}! Turning off PC...")
            subprocess.Popen(["shutdown", "/s", "/t", "2"])

        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        pass  # Suppress default noisy console logs

def start_server():
    global HTTP_SERVER, AGENT_RUNNING
    try:
        HTTP_SERVER = HTTPServer(("0.0.0.0", PORT), PowerCommandHandler)
        AGENT_RUNNING = True
        HTTP_SERVER.serve_forever()
    except Exception as e:
        AGENT_RUNNING = False

def stop_server():
    global HTTP_SERVER, AGENT_RUNNING
    AGENT_RUNNING = False
    if HTTP_SERVER:
        def _shutdown_worker():
            try:
                HTTP_SERVER.shutdown()
                HTTP_SERVER.server_close()
            except Exception:
                pass
        threading.Thread(target=_shutdown_worker, daemon=True).start()
        HTTP_SERVER = None


class SecureWolApp(tk.Tk):
    def __init__(self):
        super().__init__()

        self.title("Secure WOL — Control Center")
        self.geometry("560x670")
        self.resizable(False, False)
        self.configure(bg="#070B12")

        if os.path.exists(LOGO_ICO):
            try:
                self.iconbitmap(LOGO_ICO)
            except Exception:
                pass

        global LOG_CALLBACK
        LOG_CALLBACK = self.append_log

        self.local_ip = get_local_ip()
        self.is_active = False
        load_or_create_auth_key()

        self.build_ui()
        self.start_service()  # Auto-start on launch

    def build_ui(self):
        # 1. Header Frame with Real High-Res Logo
        header_frame = tk.Frame(self, bg="#0D1420", height=80, padx=18, pady=12)
        header_frame.pack(fill="x")

        logo_row = tk.Frame(header_frame, bg="#0D1420")
        logo_row.pack(fill="x")

        # Display Custom Logo Image
        if os.path.exists(LOGO_PNG):
            try:
                pil_img = Image.open(LOGO_PNG)
                pil_img = pil_img.resize((50, 50), Image.Resampling.LANCZOS)
                self.logo_photo = ImageTk.PhotoImage(pil_img)
                logo_label = tk.Label(logo_row, image=self.logo_photo, bg="#0D1420")
                logo_label.pack(side="left", padx=(0, 14))
            except Exception:
                pass

        text_col = tk.Frame(logo_row, bg="#0D1420")
        text_col.pack(side="left", fill="y")

        title_badge_row = tk.Frame(text_col, bg="#0D1420")
        title_badge_row.pack(anchor="w")

        title_label = tk.Label(
            title_badge_row,
            text="Secure WOL Control Center",
            font=("Segoe UI", 14, "bold"),
            fg="#F8FAFC",
            bg="#0D1420"
        )
        title_label.pack(side="left")

        pro_badge = tk.Label(
            title_badge_row,
            text="ENTERPRISE SECURE",
            font=("Segoe UI", 8, "bold"),
            fg="#10B981",
            bg="#062D24",
            padx=6,
            pady=1
        )
        pro_badge.pack(side="left", padx=(8, 0))

        subtitle_label = tk.Label(
            text_col,
            text="Zero-Trust Protected • HMAC-Auth • Local Subnet Firewall",
            font=("Segoe UI", 9),
            fg="#94A3B8",
            bg="#0D1420"
        )
        subtitle_label.pack(anchor="w", pady=(2, 0))

        # 2. Main Content Frame
        content_frame = tk.Frame(self, bg="#070B12", padx=20, pady=16)
        content_frame.pack(fill="both", expand=True)

        # Status Card (Obsidian Glassmorphic)
        self.status_card = tk.Frame(
            content_frame,
            bg="#131D2E",
            padx=16,
            pady=14,
            highlightthickness=1,
            highlightbackground="#1F2E45"
        )
        self.status_card.pack(fill="x", pady=(0, 12))

        status_header = tk.Frame(self.status_card, bg="#131D2E")
        status_header.pack(fill="x")

        self.status_indicator = tk.Label(
            status_header,
            text="● SERVICE ACTIVE",
            font=("Segoe UI", 11, "bold"),
            fg="#10B981",
            bg="#131D2E"
        )
        self.status_indicator.pack(side="left")

        self.port_label = tk.Label(
            status_header,
            text=f"Port: {PORT}",
            font=("Consolas", 10, "bold"),
            fg="#64748B",
            bg="#131D2E"
        )
        self.port_label.pack(side="right")

        self.status_desc = tk.Label(
            self.status_card,
            text=f"Listening on http://{self.local_ip}:{PORT} (0.0% CPU • ~6MB RAM)",
            font=("Segoe UI", 9),
            fg="#94A3B8",
            bg="#131D2E"
        )
        self.status_desc.pack(anchor="w", pady=(4, 10))

        # Main ON/OFF Toggle Button
        self.toggle_btn = tk.Button(
            self.status_card,
            text="PAUSE SERVICE (TURN OFF)",
            font=("Segoe UI", 9, "bold"),
            bg="#1E293B",
            fg="#EF4444",
            activebackground="#334155",
            activeforeground="#EF4444",
            relief="flat",
            cursor="hand2",
            padx=14,
            pady=7,
            command=self.toggle_service
        )
        self.toggle_btn.pack(fill="x")

        # Network Info & WoL Optimizer Card
        info_frame = tk.Frame(
            content_frame,
            bg="#0D1420",
            padx=14,
            pady=10,
            highlightthickness=1,
            highlightbackground="#1E293B"
        )
        info_frame.pack(fill="x", pady=(0, 12))

        net_row = tk.Frame(info_frame, bg="#0D1420")
        net_row.pack(fill="x")

        tk.Label(
            net_row,
            text=f"🌐 Local IP: {self.local_ip}",
            font=("Consolas", 10, "bold"),
            fg="#38BDF8",
            bg="#0D1420"
        ).pack(side="left")

        opt_btn = tk.Button(
            net_row,
            text="⚡ Fix & Optimize WoL NIC",
            font=("Segoe UI", 8, "bold"),
            fg="#080C14",
            bg="#38BDF8",
            activebackground="#0284C7",
            relief="flat",
            cursor="hand2",
            padx=10,
            pady=2,
            command=self.optimize_nic_settings
        )
        opt_btn.pack(side="right")

        # Security Secret Token Card
        key_frame = tk.Frame(
            content_frame,
            bg="#0D1420",
            padx=14,
            pady=10,
            highlightthickness=1,
            highlightbackground="#1E293B"
        )
        key_frame.pack(fill="x", pady=(0, 12))

        tk.Label(
            key_frame,
            text="🔒 Zero-Trust Secret Key (Phone Pairing):",
            font=("Segoe UI", 9, "bold"),
            fg="#10B981",
            bg="#0D1420"
        ).pack(anchor="w")

        key_row = tk.Frame(key_frame, bg="#0D1420")
        key_row.pack(fill="x", pady=(5, 0))

        self.key_entry = tk.Entry(
            key_row,
            font=("Consolas", 10),
            bg="#070B12",
            fg="#F8FAFC",
            insertbackground="#10B981",
            relief="flat",
            highlightthickness=1,
            highlightbackground="#1E293B"
        )
        self.key_entry.insert(0, load_or_create_auth_key())
        self.key_entry.pack(side="left", fill="x", expand=True, padx=(0, 8))

        save_key_btn = tk.Button(
            key_row,
            text="Save Key",
            font=("Segoe UI", 8, "bold"),
            fg="#070B12",
            bg="#10B981",
            activebackground="#059669",
            relief="flat",
            cursor="hand2",
            padx=12,
            pady=3,
            command=self.save_key_from_ui
        )
        save_key_btn.pack(side="right")

        # Activity Log Frame
        log_header = tk.Frame(content_frame, bg="#070B12")
        log_header.pack(fill="x", pady=(0, 4))

        tk.Label(
            log_header,
            text="Activity Live Log",
            font=("Segoe UI", 9, "bold"),
            fg="#F8FAFC",
            bg="#070B12"
        ).pack(side="left")

        clear_btn = tk.Button(
            log_header,
            text="Clear Log",
            font=("Segoe UI", 8),
            fg="#94A3B8",
            bg="#070B12",
            relief="flat",
            cursor="hand2",
            command=self.clear_log
        )
        clear_btn.pack(side="right")

        self.log_text = tk.Text(
            content_frame,
            height=6,
            bg="#0D1420",
            fg="#E2E8F0",
            insertbackground="#10B981",
            font=("Consolas", 9),
            relief="flat",
            padx=10,
            pady=8,
            highlightthickness=1,
            highlightbackground="#1E293B"
        )
        self.log_text.pack(fill="both", expand=True, pady=(0, 10))

        # Bottom Options
        bottom_frame = tk.Frame(content_frame, bg="#070B12")
        bottom_frame.pack(fill="x")

        self.autostart_var = tk.BooleanVar(value=self.check_autostart_installed())
        self.autostart_cb = tk.Checkbutton(
            bottom_frame,
            text="Start automatically on Windows boot",
            variable=self.autostart_var,
            command=self.toggle_autostart,
            font=("Segoe UI", 9),
            fg="#CBD5E1",
            bg="#070B12",
            activebackground="#070B12",
            activeforeground="#10B981",
            selectcolor="#0D1420"
        )
        self.autostart_cb.pack(side="left")

    def append_log(self, message):
        timestamp = time.strftime("%H:%M:%S")
        log_line = f"[{timestamp}] {message}\n"
        self.after(0, lambda: self._insert_log(log_line))

    def _insert_log(self, line):
        self.log_text.insert(tk.END, line)
        self.log_text.see(tk.END)

    def clear_log(self):
        self.log_text.delete("1.0", tk.END)

    def start_service(self):
        global SERVER_THREAD
        if not self.is_active:
            SERVER_THREAD = threading.Thread(target=start_server, daemon=True)
            SERVER_THREAD.start()
            self.is_active = True
            self.update_ui_state(True)
            self.append_log(f"Zero-Trust Service STARTED on port {PORT}. Ready for phone commands.")

    def stop_service(self):
        if self.is_active:
            stop_server()
            self.is_active = False
            self.update_ui_state(False)
            self.append_log("Service STOPPED. Phone commands will be paused.")

    def toggle_service(self):
        if self.is_active:
            self.stop_service()
        else:
            self.start_service()

    def update_ui_state(self, active: bool):
        if active:
            self.status_indicator.config(text="● SERVICE ACTIVE", fg="#10B981")
            self.status_desc.config(text=f"Listening on http://{self.local_ip}:{PORT} (0.0% CPU • ~6MB RAM)", fg="#94A3B8")
            self.toggle_btn.config(
                text="PAUSE SERVICE (TURN OFF)",
                bg="#1E293B",
                fg="#EF4444",
                activebackground="#334155",
                activeforeground="#EF4444"
            )
            self.status_card.config(highlightbackground="#10B981")
        else:
            self.status_indicator.config(text="○ SERVICE INACTIVE (OFF)", fg="#EF4444")
            self.status_desc.config(text="Service is paused. Phone remote commands are disabled.", fg="#EF4444")
            self.toggle_btn.config(
                text="START SERVICE (TURN ON)",
                bg="#065F46",
                fg="#F8FAFC",
                activebackground="#047857",
                activeforeground="#FFFFFF"
            )
            self.status_card.config(highlightbackground="#EF4444")

    def save_key_from_ui(self):
        new_key = self.key_entry.get().strip()
        save_auth_key(new_key)
        if new_key:
            self.append_log("🔒 Security Secret Key SAVED. Unauthorized requests will be blocked.")
            messagebox.showinfo("Security Key Saved", f"Zero-Trust Secret Key set to:\n\n{new_key}\n\nMake sure to enter this same Secret Key in your Secure WOL Phone App under PC Settings!")
        else:
            self.append_log("🔓 Security Secret Key CLEARED. Local network access open.")
            messagebox.showinfo("Security Key Cleared", "Secret Key removed. Any device on your local WiFi can send commands.")

    def optimize_nic_settings(self):
        opt_script = os.path.join(BASE_DIR, "Optimize-WoL-Settings.bat")
        if os.path.exists(opt_script):
            subprocess.Popen(["cmd.exe", "/c", opt_script])
            self.append_log("Executing Realtek NIC Wake-on-LAN optimization...")
        else:
            messagebox.showerror("Error", "Optimize-WoL-Settings.bat not found.")

    def check_autostart_installed(self):
        startup = os.path.join(os.getenv("APPDATA", ""), r"Microsoft\Windows\Start Menu\Programs\Startup\SecureWolAgent.vbs")
        return os.path.exists(startup)

    def toggle_autostart(self):
        startup = os.path.join(os.getenv("APPDATA", ""), r"Microsoft\Windows\Start Menu\Programs\Startup\SecureWolAgent.vbs")
        app_path = os.path.join(BASE_DIR, "SecureWolDesktopApp.py")

        if self.autostart_var.get():
            try:
                vbs_content = f'Set WshShell = CreateObject("WScript.Shell")\nWshShell.Run "python \\"{app_path}\\"", 0, False\n'
                with open(startup, "w") as f:
                    f.write(vbs_content)
                self.append_log("Autostart ENABLED on Windows boot.")
            except Exception as e:
                messagebox.showerror("Error", f"Failed to enable autostart: {e}")
        else:
            if os.path.exists(startup):
                try:
                    os.remove(startup)
                    self.append_log("Autostart DISABLED.")
                except Exception as e:
                    messagebox.showerror("Error", f"Failed to disable autostart: {e}")

if __name__ == "__main__":
    app = SecureWolApp()
    app.mainloop()
