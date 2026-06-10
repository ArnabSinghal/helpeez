import sqlite3
import hashlib
from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel
from typing import List, Optional

app = FastAPI(title="Helpeez Cloud Sync Backend", version="1.0")
DB_FILE = "server.db"

# Database connection helper with WAL mode & busy timeout
def get_db_connection():
    conn = sqlite3.connect(DB_FILE, timeout=30)
    conn.execute("PRAGMA journal_mode=WAL;")
    return conn

# Database initialization
def init_db():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT UNIQUE NOT NULL,
            name TEXT NOT NULL,
            phone TEXT NOT NULL,
            password_hash TEXT NOT NULL,
            role TEXT NOT NULL DEFAULT 'owner'
        )
    """)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS homes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            name TEXT NOT NULL,
            rooms INTEGER NOT NULL,
            halls INTEGER NOT NULL,
            balconies INTEGER NOT NULL,
            address TEXT NOT NULL,
            sweeping INTEGER NOT NULL,
            utensils INTEGER NOT NULL,
            cooking INTEGER NOT NULL,
            dishwasher INTEGER NOT NULL,
            washing_machine INTEGER NOT NULL,
            special_balcony INTEGER NOT NULL,
            special_dustbin INTEGER NOT NULL,
            custom_request TEXT,
            timing_slot TEXT NOT NULL,
            sunday_timing_slot TEXT NOT NULL,
            assigned_helper_id INTEGER NOT NULL DEFAULT -1,
            regular_helper_id INTEGER NOT NULL DEFAULT -1,
            otp TEXT NOT NULL DEFAULT '',
            check_in_time INTEGER NOT NULL DEFAULT 0,
            shift_status TEXT NOT NULL DEFAULT 'pending',
            carpet_area INTEGER NOT NULL DEFAULT 1000,
            cleaning_duration INTEGER NOT NULL DEFAULT 60
        )
    """)
    conn.commit()

    # Seed 10 helpers if not exists
    cursor.execute("SELECT id FROM users WHERE role = 'helper'")
    rows = cursor.fetchall()
    if not rows:
        helpers = [
            ("sita@helpeez.com", "Sita", "+91 98765 00001"),
            ("kamla@helpeez.com", "Kamla", "+91 98765 00002"),
            ("shanti@helpeez.com", "Shanti", "+91 98765 00003"),
            ("laxmi@helpeez.com", "Laxmi", "+91 98765 00004"),
            ("radha@helpeez.com", "Radha", "+91 98765 00005"),
            ("sunita@helpeez.com", "Sunita", "+91 98765 00006"),
            ("geeta@helpeez.com", "Geeta", "+91 98765 00007"),
            ("asha@helpeez.com", "Asha", "+91 98765 00008"),
            ("rekha@helpeez.com", "Rekha", "+91 98765 00009"),
            ("savitri@helpeez.com", "Savitri", "+91 98765 00010")
        ]
        pw_hash = hashlib.sha256(b"helper123").hexdigest()
        for email, name, phone in helpers:
            cursor.execute(
                "INSERT INTO users (email, name, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)",
                (email, name, phone, pw_hash, "helper")
            )
        conn.commit()

    conn.close()

init_db()

# Pydantic Schemas
class UserRegister(BaseModel):
    email: str
    name: str
    phone: str
    password_hash: str
    role: str = "owner"

class UserLogin(BaseModel):
    email: str
    password_hash: str

class UserResponse(BaseModel):
    id: int
    email: str
    name: str
    phone: str
    role: str

class HomeSchema(BaseModel):
    id: Optional[int] = 0
    user_id: int
    name: str
    rooms: int
    halls: int
    balconies: int
    address: str
    sweeping: int
    utensils: int
    cooking: int
    dishwasher: int
    washing_machine: int
    special_balcony: int
    special_dustbin: int
    custom_request: Optional[str] = ""
    timing_slot: str
    sunday_timing_slot: str
    assigned_helper_id: Optional[int] = -1
    regular_helper_id: Optional[int] = -1
    otp: Optional[str] = ""
    check_in_time: Optional[int] = 0
    shift_status: Optional[str] = "pending"
    carpet_area: Optional[int] = 1000
    cleaning_duration: Optional[int] = 60

class ShiftStatusUpdate(BaseModel):
    home_id: int
    shift_status: str
    check_in_time: int

# Endpoints
@app.post("/register", status_code=status.HTTP_201_CREATED)
def register(user: UserRegister):
    conn = get_db_connection()
    cursor = conn.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (email, name, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)",
            (user.email.strip().lower(), user.name.strip(), user.phone.strip(), user.password_hash, user.role.strip().lower())
        )
        conn.commit()
    except sqlite3.IntegrityError:
        conn.close()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email address already registered."
        )
    conn.close()
    return {"status": "User registered successfully"}

@app.post("/login", response_model=UserResponse)
def login(credentials: UserLogin):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        "SELECT id, email, name, phone, role FROM users WHERE email = ? AND password_hash = ?",
        (credentials.email.strip().lower(), credentials.password_hash)
    )
    row = cursor.fetchone()
    conn.close()

    if not row:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email address or password."
        )
    
    return {
        "id": row[0],
        "email": row[1],
        "name": row[2],
        "phone": row[3],
        "role": row[4]
    }

@app.post("/homes", status_code=status.HTTP_201_CREATED)
def add_home(home: HomeSchema):
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Auto-assign random helper if -1
    helper_id = home.assigned_helper_id
    if helper_id == -1:
        import random
        cursor.execute("SELECT id FROM users WHERE role = 'helper'")
        rows = cursor.fetchall()
        if rows:
            helper_id = random.choice([r[0] for r in rows])

    # Generate a random OTP if empty
    import random
    otp_code = home.otp if home.otp else str(random.randint(1000, 9999))

    cursor.execute("""
        INSERT INTO homes (
            user_id, name, rooms, halls, balconies, address, sweeping, utensils, cooking, 
            dishwasher, washing_machine, special_balcony, special_dustbin, custom_request, 
            timing_slot, sunday_timing_slot, assigned_helper_id, regular_helper_id, otp, check_in_time, shift_status,
            carpet_area, cleaning_duration
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        home.user_id, home.name, home.rooms, home.halls, home.balconies, home.address,
        home.sweeping, home.utensils, home.cooking, home.dishwasher, home.washing_machine,
        home.special_balcony, home.special_dustbin, home.custom_request, home.timing_slot,
        home.sunday_timing_slot, helper_id, home.regular_helper_id, otp_code, home.check_in_time, home.shift_status,
        home.carpet_area, home.cleaning_duration
    ))
    conn.commit()
    conn.close()
    return {"status": "Home added successfully"}

def auto_complete_expired_shifts(cursor):
    import time
    now_ms = int(time.time() * 1000)
    cursor.execute("SELECT id, check_in_time, cleaning_duration FROM homes WHERE shift_status = 'started'")
    rows = cursor.fetchall()
    for row in rows:
        home_id, check_in_time, cleaning_duration = row
        if check_in_time > 0:
            duration_ms = cleaning_duration * 60 * 1000
            if now_ms >= check_in_time + duration_ms:
                cursor.execute("UPDATE homes SET shift_status = 'completed' WHERE id = ?", (home_id,))

@app.get("/homes", response_model=List[HomeSchema])
def get_homes(user_id: Optional[int] = None, helper_id: Optional[int] = None):
    conn = get_db_connection()
    cursor = conn.cursor()
    auto_complete_expired_shifts(cursor)
    conn.commit()
    
    if user_id is not None:
        cursor.execute("""
            SELECT 
                id, user_id, name, rooms, halls, balconies, address, sweeping, utensils, cooking, 
                dishwasher, washing_machine, special_balcony, special_dustbin, custom_request, 
                timing_slot, sunday_timing_slot, assigned_helper_id, regular_helper_id, otp, check_in_time, shift_status,
                carpet_area, cleaning_duration
            FROM homes WHERE user_id = ?
        """, (user_id,))
    elif helper_id is not None:
        cursor.execute("""
            SELECT 
                id, user_id, name, rooms, halls, balconies, address, sweeping, utensils, cooking, 
                dishwasher, washing_machine, special_balcony, special_dustbin, custom_request, 
                timing_slot, sunday_timing_slot, assigned_helper_id, regular_helper_id, otp, check_in_time, shift_status,
                carpet_area, cleaning_duration
            FROM homes WHERE assigned_helper_id = ? OR regular_helper_id = ?
        """, (helper_id, helper_id))
    else:
        cursor.execute("""
            SELECT 
                id, user_id, name, rooms, halls, balconies, address, sweeping, utensils, cooking, 
                dishwasher, washing_machine, special_balcony, special_dustbin, custom_request, 
                timing_slot, sunday_timing_slot, assigned_helper_id, regular_helper_id, otp, check_in_time, shift_status,
                carpet_area, cleaning_duration
            FROM homes
        """)
        
    rows = cursor.fetchall()
    conn.close()

    homes = []
    for r in rows:
        homes.append(
            HomeSchema(
                id=r[0], user_id=r[1], name=r[2], rooms=r[3], halls=r[4], balconies=r[5], address=r[6],
                sweeping=r[7], utensils=r[8], cooking=r[9], dishwasher=r[10], washing_machine=r[11],
                special_balcony=r[12], special_dustbin=r[13], custom_request=r[14], timing_slot=r[15],
                sunday_timing_slot=r[16], assigned_helper_id=r[17], regular_helper_id=r[18], otp=r[19], check_in_time=r[20],
                shift_status=r[21], carpet_area=r[22], cleaning_duration=r[23]
            )
        )
    return homes

@app.post("/homes/shift-status")
def update_shift_status(payload: ShiftStatusUpdate):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE homes 
        SET shift_status = ?, check_in_time = ? 
        WHERE id = ?
    """, (payload.shift_status, payload.check_in_time, payload.home_id))
    conn.commit()
    
    # Check if record updated
    updated = cursor.rowcount > 0
    conn.close()
    
    if not updated:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Home layout booking record not found."
        )
        
    return {"status": "Shift status updated successfully"}

class HelperUpdate(BaseModel):
    home_id: int
    helper_id: int

@app.post("/homes/update-helper")
def update_home_helper(payload: HelperUpdate):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("UPDATE homes SET assigned_helper_id = ? WHERE id = ?", (payload.helper_id, payload.home_id))
    conn.commit()
    updated = cursor.rowcount > 0
    conn.close()
    if not updated:
        raise HTTPException(status_code=404, detail="Home not found")
    return {"status": "Helper updated successfully"}

class HolidayUpdate(BaseModel):
    home_id: int
    assigned_helper_id: int
    regular_helper_id: int

@app.post("/homes/update-holiday")
def update_holiday(payload: HolidayUpdate):
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE homes 
        SET assigned_helper_id = ?, regular_helper_id = ? 
        WHERE id = ?
    """, (payload.assigned_helper_id, payload.regular_helper_id, payload.home_id))
    conn.commit()
    updated = cursor.rowcount > 0
    conn.close()
    if not updated:
        raise HTTPException(status_code=404, detail="Home not found")
    return {"status": "Holiday status updated successfully"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
