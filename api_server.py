#!/usr/bin/env python3
"""
Simple API server for TA Recruitment System
Provides endpoints to read from data files and handle job applications
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import os
from urllib.parse import urlparse, parse_qs
import sys
from datetime import datetime
import time

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, 'data')

class APIHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        query_params = parse_qs(parsed_path.query)
        
        if path == '/api/login':
            self.handle_login()
        elif path == '/api/users':
            self.handle_get_users()
        elif path.startswith('/api/user/'):
            user_id = path.split('/')[-1]
            self.handle_get_user(user_id)
        elif path == '/api/positions':
            self.handle_get_positions(query_params)
        elif path.startswith('/api/position/'):
            position_id = path.split('/')[-1]
            self.handle_get_position(position_id)
        elif path == '/api/applications':
            self.handle_get_applications(query_params)
        else:
            self.wfile.write(json.dumps({"success": False, "message": "Not found"}).encode())
    
    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode()
        
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        
        # Parse form data or JSON
        content_type = self.headers.get('Content-Type', '')
        if 'application/x-www-form-urlencoded' in content_type:
            data = {k: v[0] for k, v in parse_qs(body).items()}
        else:
            try:
                data = json.loads(body) if body else {}
            except:
                data = {}
        
        if path == '/api/login':
            self.handle_login_post(data)
        elif path == '/api/register':
            self.handle_register(data)
        elif path == '/api/applications':
            self.handle_create_application(data)
        else:
            self.wfile.write(json.dumps({"success": False, "message": "Not found"}).encode())
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
    
    def handle_login_post(self, data):
        """Handle login POST request"""
        user_id = data.get('userId', '') or data.get('username', '')
        password = data.get('password', '')
        role = data.get('role', '')
        
        users = self.load_users()
        for user in users:
            parts = user.split('|')
            if len(parts) >= 5:
                file_user_id, name, email = parts[0], parts[1], parts[2]
                user_pass = parts[3] if len(parts) > 3 else ''
                user_type = parts[4] if len(parts) > 4 else ''
                
                # Match by userId or email
                if (file_user_id == user_id or email == user_id) and user_pass == password:
                    # Check role matches if role is provided
                    if role and user_type != role:
                        self.wfile.write(json.dumps({"success": False, "message": "User role does not match selected role"}).encode())
                        return
                    
                    response = {
                        "success": True,
                        "user": {
                            "userId": file_user_id,
                            "userName": name,
                            "userEmail": email,
                            "userRole": user_type
                        }
                    }
                    self.wfile.write(json.dumps(response).encode())
                    return
        
        self.wfile.write(json.dumps({"success": False, "message": "Invalid credentials"}).encode())
    
    def handle_login(self):
        """Handle GET login (for testing)"""
        self.wfile.write(json.dumps({"success": False, "message": "Use POST for login"}).encode())
    
    def handle_register(self, data):
        """Handle registration"""
        response = {
            "success": True,
            "message": "Registration not yet implemented"
        }
        self.wfile.write(json.dumps(response).encode())
    
    def handle_get_users(self):
        """Get all users"""
        users = self.load_users()
        users_list = []
        for user in users:
            parts = user.split('|')
            if len(parts) >= 5:
                users_list.append({
                    "id": parts[0],
                    "name": parts[1],
                    "email": parts[2],
                    "type": parts[4]
                })
        
        self.wfile.write(json.dumps({"success": True, "users": users_list}).encode())
    
    def handle_get_user(self, user_id):
        """Get specific user"""
        users = self.load_users()
        for user in users:
            parts = user.split('|')
            if len(parts) >= 5 and parts[0] == user_id:
                response = {
                    "success": True,
                    "user": {
                        "id": parts[0],
                        "name": parts[1],
                        "email": parts[2],
                        "type": parts[4]
                    }
                }
                self.wfile.write(json.dumps(response).encode())
                return
        
        self.wfile.write(json.dumps({"success": False, "message": "User not found"}).encode())
    
    def handle_get_positions(self, query_params):
        """Get positions that are open"""
        positions = self.load_positions()
        ta_id = query_params.get('taId', [None])[0]
        
        positions_list = []
        for pos in positions:
            parts = pos.split('|')
            if len(parts) >= 11:
                pos_id, title, major, salary, description, req, mo_id, expected, accepted, rejected, status = parts[:11]
                
                # Only return open positions
                if status.lower() == 'open':
                    position_data = {
                        "id": pos_id,
                        "title": title,
                        "major": major,
                        "salary": salary,
                        "description": description,
                        "requirements": req,
                        "moId": mo_id,
                        "expectedCount": int(expected) if expected else 0,
                        "acceptedCount": int(accepted) if accepted else 0,
                        "rejectedCount": int(rejected) if rejected else 0,
                        "status": status
                    }
                    
                    # Check if TA already applied
                    if ta_id:
                        has_applied = self.check_application_exists(pos_id, ta_id)
                        position_data["taApplied"] = has_applied
                    
                    positions_list.append(position_data)
        
        self.wfile.write(json.dumps({"success": True, "positions": positions_list}).encode())
    
    def handle_get_position(self, position_id):
        """Get specific position"""
        positions = self.load_positions()
        for pos in positions:
            parts = pos.split('|')
            if len(parts) >= 11 and parts[0] == position_id:
                pos_id, title, major, salary, description, req, mo_id, expected, accepted, rejected, status = parts[:11]
                
                response = {
                    "success": True,
                    "position": {
                        "id": pos_id,
                        "title": title,
                        "major": major,
                        "salary": salary,
                        "description": description,
                        "requirements": req,
                        "moId": mo_id,
                        "expectedCount": int(expected) if expected else 0,
                        "acceptedCount": int(accepted) if accepted else 0,
                        "rejectedCount": int(rejected) if rejected else 0,
                        "status": status
                    }
                }
                self.wfile.write(json.dumps(response).encode())
                return
        
        self.wfile.write(json.dumps({"success": False, "message": "Position not found"}).encode())
    
    def handle_get_applications(self, query_params):
        """Get applications for a TA"""
        ta_id = query_params.get('taId', [None])[0]
        
        if not ta_id:
            self.wfile.write(json.dumps({"success": False, "message": "taId required"}).encode())
            return
        
        applications = self.load_applications()
        apps_list = []
        
        for app in applications:
            parts = app.split('|')
            if len(parts) >= 9 and parts[3] == ta_id:
                app_id, pos_id, pos_title, app_ta_id, ta_name, mo_id, priority, status, apply_date = parts[:9]
                apps_list.append({
                    "id": app_id,
                    "positionId": pos_id,
                    "positionTitle": pos_title,
                    "taId": app_ta_id,
                    "taName": ta_name,
                    "moId": mo_id,
                    "priority": priority,
                    "status": status,
                    "appliedDate": apply_date
                })
        
        self.wfile.write(json.dumps({"success": True, "applications": apps_list}).encode())
    
    def handle_create_application(self, data):
        """Create a new application"""
        ta_id = data.get('taId', '')
        position_id = data.get('positionId', '')
        
        if not ta_id or not position_id:
            self.wfile.write(json.dumps({"success": False, "message": "taId and positionId required"}).encode())
            return
        
        # Check if TA already applied for this position
        if self.check_application_exists(position_id, ta_id):
            self.wfile.write(json.dumps({"success": False, "message": "You already applied for this position"}).encode())
            return
        
        # Get position info
        positions = self.load_positions()
        position = None
        for pos in positions:
            parts = pos.split('|')
            if len(parts) >= 11 and parts[0] == position_id:
                if parts[10].lower() != 'open':
                    self.wfile.write(json.dumps({"success": False, "message": "This position is no longer open"}).encode())
                    return
                position = parts
                break
        
        if not position:
            self.wfile.write(json.dumps({"success": False, "message": "Position not found"}).encode())
            return
        
        # Get TA info
        users = self.load_users()
        ta_name = ''
        for user in users:
            parts = user.split('|')
            if len(parts) >= 5 and parts[0] == ta_id and parts[4].lower() == 'ta':
                ta_name = parts[1]
                break
        
        if not ta_name:
            self.wfile.write(json.dumps({"success": False, "message": "Invalid TA ID"}).encode())
            return
        
        # Create application
        app_id = f"app{int(time.time() * 1000)}"
        mo_id = position[6]
        pos_title = position[1]
        today = datetime.now().strftime("%Y-%m-%d")
        
        application_record = f"{app_id}|{position_id}|{pos_title}|{ta_id}|{ta_name}|{mo_id}|first|pending|{today}|"
        
        # Save to applications.txt
        apps_file = os.path.join(DATA_DIR, 'applications.txt')
        try:
            with open(apps_file, 'a', encoding='utf-8') as f:
                f.write(application_record + '\n')
            
            response = {
                "success": True,
                "message": "Application submitted successfully",
                "applicationId": app_id,
                "application": {
                    "id": app_id,
                    "positionId": position_id,
                    "positionTitle": pos_title,
                    "taId": ta_id,
                    "taName": ta_name,
                    "moId": mo_id,
                    "priority": "first",
                    "status": "pending",
                    "appliedDate": today
                }
            }
            self.wfile.write(json.dumps(response).encode())
        except Exception as e:
            self.wfile.write(json.dumps({"success": False, "message": f"Failed to save application: {str(e)}"}).encode())
    
    def check_application_exists(self, position_id, ta_id):
        """Check if TA has already applied for this position"""
        applications = self.load_applications()
        for app in applications:
            parts = app.split('|')
            if len(parts) >= 4 and parts[0].startswith('app') and parts[1] == position_id and parts[3] == ta_id:
                return True
        return False
    
    def load_users(self):
        """Load users from data file"""
        users_file = os.path.join(DATA_DIR, 'users.txt')
        if os.path.exists(users_file):
            with open(users_file, 'r', encoding='utf-8') as f:
                return [line.strip() for line in f if line.strip()]
        return []
    
    def load_positions(self):
        """Load positions from data file"""
        positions_file = os.path.join(DATA_DIR, 'positions.txt')
        if os.path.exists(positions_file):
            with open(positions_file, 'r', encoding='utf-8') as f:
                return [line.strip() for line in f if line.strip()]
        return []
    
    def load_applications(self):
        """Load applications from data file"""
        applications_file = os.path.join(DATA_DIR, 'applications.txt')
        if os.path.exists(applications_file):
            with open(applications_file, 'r', encoding='utf-8') as f:
                return [line.strip() for line in f if line.strip()]
        return []
    
    def log_message(self, format, *args):
        """Suppress logging"""
        pass

def run_server(port=9091):
    """Run the API server"""
    server_address = ('localhost', port)
    httpd = HTTPServer(server_address, APIHandler)
    print(f"API Server running on http://localhost:{port}")
    httpd.serve_forever()

if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9091
    run_server(port)
