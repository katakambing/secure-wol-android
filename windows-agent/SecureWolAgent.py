#!/usr/bin/env python3
"""
Secure WOL - Windows Companion Agent
Listens locally on port 9090 to handle Sleep, Restart, and Shutdown commands
from your authenticated Secure WOL Android phone.
"""

import os
import sys
import subprocess
from http.server import HTTPServer, BaseHTTPRequestHandler

PORT = 9090

class PowerCommandHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/status" or self.path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"online"}')
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == "/sleep":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b'{"status":"sleeping"}')
            print("Received SLEEP command. Putting PC to sleep...")
            # Suspend/Sleep
            subprocess.Popen(["rundll32.exe", "powrprof.dll,SetSuspendState", "0,1,0"])

        elif self.path == "/restart":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b'{"status":"restarting"}')
            print("Received RESTART command. Restarting PC in 2 seconds...")
            subprocess.Popen(["shutdown", "/r", "/t", "2"])

        elif self.path == "/shutdown":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b'{"status":"shutting_down"}')
            print("Received SHUTDOWN command. Shutting down PC in 2 seconds...")
            subprocess.Popen(["shutdown", "/s", "/t", "2"])

        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        # Clean logging
        print(f"[SecureWolAgent] {args[0]} - {args[1]}")

def run():
    server = HTTPServer(("0.0.0.0", PORT), PowerCommandHandler)
    print(f"==================================================")
    print(f"   Secure WOL Windows Companion Agent Running")
    print(f"   Listening on port {PORT} for Sleep/Restart/Off")
    print(f"==================================================")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nAgent stopped.")
        server.server_close()

if __name__ == "__main__":
    run()
