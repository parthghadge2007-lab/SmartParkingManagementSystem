/**
 * SmartPark — Backend Server v2
 * New flow: Customer submits request → Admin approves/rejects
 */

const express = require('express');
const mysql   = require('mysql2/promise');
const cors    = require('cors');
const path    = require('path');

const app  = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

const pool = mysql.createPool({
  host: 'localhost', port: 3306,
  user: 'root', password: 'Parth2007',
  database: 'smart_parking',
  waitForConnections: true, connectionLimit: 10,
});

// ═══════════════════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════════════════

app.post('/api/auth/login', async (req, res) => {
  const { username, password } = req.body;
  if (!username?.trim() || !password)
    return res.status(400).json({ success: false, message: 'Credentials required.' });

  // Admin (hardcoded, mirrors AuthService.java)
  if (username.trim().toLowerCase() === 'admin' && password === 'admin123')
    return res.json({ success: true, role: 'admin', username: 'admin', fullName: 'Administrator' });

  // Customer (DB lookup)
  try {
    const [[user]] = await pool.query(
      'SELECT user_id, username, password, full_name FROM users WHERE username = ?',
      [username.trim()]
    );
    if (!user || user.password !== password)
      return res.status(401).json({ success: false, message: 'Invalid username or password.' });

    res.json({ success: true, role: 'customer', userId: user.user_id, username: user.username, fullName: user.full_name });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});

app.post('/api/auth/register', async (req, res) => {
  const { username, password, fullName } = req.body;
  if (!username?.trim() || !password || !fullName?.trim())
    return res.status(400).json({ success: false, message: 'All fields are required.' });
  if (password.length < 6)
    return res.status(400).json({ success: false, message: 'Password must be at least 6 characters.' });
  try {
    const [r] = await pool.query(
      'INSERT INTO users (username, password, full_name) VALUES (?, ?, ?)',
      [username.trim(), password, fullName.trim()]
    );
    res.json({ success: true, userId: r.insertId, message: 'Account created! You can now sign in.' });
  } catch (e) {
    if (e.code === 'ER_DUP_ENTRY')
      return res.status(409).json({ success: false, message: 'Username already taken. Try another.' });
    res.status(500).json({ success: false, message: e.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
// STATUS
// ═══════════════════════════════════════════════════════════════════════════

app.get('/api/status', async (req, res) => {
  try {
    const conn = await pool.getConnection();
    await conn.query('SELECT 1');
    conn.release();
    res.json({ connected: true, message: 'Database Online (MySQL 3306)' });
  } catch (e) {
    res.json({ connected: false, message: e.message });
  }
});

// ═══════════════════════════════════════════════════════════════════════════
// SLOTS (Module A)
// ═══════════════════════════════════════════════════════════════════════════

app.get('/api/slots', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT slot_id, slot_name, is_available FROM parking_slots ORDER BY slot_id');
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.get('/api/slots/available', async (req, res) => {
  try {
    const [rows] = await pool.query('SELECT slot_id, slot_name FROM parking_slots WHERE is_available = TRUE ORDER BY slot_id');
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.get('/api/slots/stats', async (req, res) => {
  try {
    const [[s]] = await pool.query('SELECT COUNT(*) AS total, SUM(is_available=TRUE) AS available, SUM(is_available=FALSE) AS occupied FROM parking_slots');
    const [[p]] = await pool.query("SELECT COUNT(*) AS pending FROM reservation_requests WHERE status='PENDING'");
    res.json({ total: Number(s.total), available: Number(s.available), occupied: Number(s.occupied), pending: Number(p.pending) });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// Admin: direct reserve (keep for backwards compat)
app.post('/api/slots/reserve', async (req, res) => {
  const { customerName, slotId } = req.body;
  if (!customerName?.trim() || !slotId) return res.status(400).json({ error: 'customerName and slotId required.' });
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[slot]] = await conn.query('SELECT is_available FROM parking_slots WHERE slot_id = ? FOR UPDATE', [slotId]);
    if (!slot)          { await conn.rollback(); return res.status(404).json({ error: 'Slot not found.' }); }
    if (!slot.is_available) { await conn.rollback(); return res.status(409).json({ error: 'Slot already occupied. Double-booking prevented.' }); }
    await conn.query('UPDATE parking_slots SET is_available = FALSE WHERE slot_id = ?', [slotId]);
    const [ins] = await conn.query("INSERT INTO reservations (customer_name, slot_id, status) VALUES (?, ?, 'ACTIVE')", [customerName.trim(), slotId]);
    await conn.commit();
    res.json({ success: true, reservationId: ins.insertId });
  } catch (e) { await conn.rollback(); res.status(500).json({ error: e.message }); }
  finally { conn.release(); }
});

// ═══════════════════════════════════════════════════════════════════════════
// RESERVATION REQUESTS  ← NEW
// ═══════════════════════════════════════════════════════════════════════════

// Admin: get ALL requests
app.get('/api/requests', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT rr.request_id, rr.user_id, rr.customer_name, rr.slot_id, p.slot_name,
              rr.requested_at, rr.status, rr.admin_note, rr.reservation_id
       FROM reservation_requests rr
       JOIN parking_slots p ON rr.slot_id = p.slot_id
       ORDER BY rr.requested_at DESC`
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// Customer: get MY requests
app.get('/api/requests/my/:userId', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT rr.request_id, rr.customer_name, rr.slot_id, p.slot_name,
              rr.requested_at, rr.status, rr.admin_note, rr.reservation_id
       FROM reservation_requests rr
       JOIN parking_slots p ON rr.slot_id = p.slot_id
       WHERE rr.user_id = ?
       ORDER BY rr.requested_at DESC`,
      [req.params.userId]
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// Customer: SUBMIT a request
app.post('/api/requests', async (req, res) => {
  const { userId, customerName, slotId } = req.body;
  if (!userId || !customerName?.trim() || !slotId)
    return res.status(400).json({ error: 'userId, customerName and slotId are required.' });
  try {
    // Check slot is available
    const [[slot]] = await pool.query('SELECT is_available FROM parking_slots WHERE slot_id = ?', [slotId]);
    if (!slot)              return res.status(404).json({ error: 'Slot not found.' });
    if (!slot.is_available) return res.status(409).json({ error: 'This slot is currently occupied.' });
    // Prevent duplicate pending request for same user+slot
    const [[dup]] = await pool.query(
      "SELECT request_id FROM reservation_requests WHERE user_id = ? AND slot_id = ? AND status = 'PENDING'",
      [userId, slotId]
    );
    if (dup) return res.status(409).json({ error: 'You already have a pending request for this slot.' });

    const [ins] = await pool.query(
      "INSERT INTO reservation_requests (user_id, customer_name, slot_id, status) VALUES (?, ?, ?, 'PENDING')",
      [userId, customerName.trim(), slotId]
    );
    res.json({ success: true, requestId: ins.insertId, message: 'Request submitted! Awaiting admin approval.' });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// Admin: APPROVE a request → ACID transaction
app.put('/api/requests/:id/approve', async (req, res) => {
  const requestId = parseInt(req.params.id, 10);
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    // 1. Lock request row, check PENDING
    const [[request]] = await conn.query(
      'SELECT * FROM reservation_requests WHERE request_id = ? FOR UPDATE', [requestId]
    );
    if (!request)                      { await conn.rollback(); return res.status(404).json({ error: 'Request not found.' }); }
    if (request.status !== 'PENDING')  { await conn.rollback(); return res.status(409).json({ error: 'Request is no longer pending.' }); }

    // 2. Lock slot row, verify still available
    const [[slot]] = await conn.query('SELECT is_available FROM parking_slots WHERE slot_id = ? FOR UPDATE', [request.slot_id]);
    if (!slot.is_available) {
      // Auto-reject since slot was taken
      await conn.query(
        "UPDATE reservation_requests SET status='REJECTED', admin_note='Slot was taken by another booking.' WHERE request_id=?",
        [requestId]
      );
      await conn.commit();
      return res.status(409).json({ error: 'Slot is no longer available. Request auto-rejected.' });
    }

    // 3. Mark slot occupied
    await conn.query('UPDATE parking_slots SET is_available = FALSE WHERE slot_id = ?', [request.slot_id]);

    // 4. Create reservation record
    const [ins] = await conn.query(
      "INSERT INTO reservations (customer_name, slot_id, status) VALUES (?, ?, 'ACTIVE')",
      [request.customer_name, request.slot_id]
    );

    // 5. Mark request APPROVED, link reservation_id
    await conn.query(
      "UPDATE reservation_requests SET status='APPROVED', reservation_id=? WHERE request_id=?",
      [ins.insertId, requestId]
    );

    await conn.commit();
    res.json({ success: true, reservationId: ins.insertId, message: 'Request approved. Reservation created.' });
  } catch (e) { await conn.rollback(); res.status(500).json({ error: e.message }); }
  finally { conn.release(); }
});

// Admin: REJECT a request
app.put('/api/requests/:id/reject', async (req, res) => {
  const requestId = parseInt(req.params.id, 10);
  const { note } = req.body;
  try {
    const [[r]] = await pool.query('SELECT status FROM reservation_requests WHERE request_id = ?', [requestId]);
    if (!r)                    return res.status(404).json({ error: 'Request not found.' });
    if (r.status !== 'PENDING') return res.status(409).json({ error: 'Request is no longer pending.' });
    await pool.query(
      "UPDATE reservation_requests SET status='REJECTED', admin_note=? WHERE request_id=?",
      [note?.trim() || null, requestId]
    );
    res.json({ success: true, message: 'Request rejected.' });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// Customer or Admin: CANCEL a request
app.put('/api/requests/:id/cancel', async (req, res) => {
  const requestId = parseInt(req.params.id, 10);
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[r]] = await conn.query('SELECT * FROM reservation_requests WHERE request_id = ? FOR UPDATE', [requestId]);
    if (!r) {
      await conn.rollback();
      return res.status(404).json({ error: 'Request not found.' });
    }
    if (r.status === 'CANCELLED' || r.status === 'REJECTED') {
      await conn.rollback();
      return res.status(409).json({ error: `Request is already ${r.status.toLowerCase()}.` });
    }

    // If request was APPROVED and linked to active reservation, free slot and cancel reservation
    if (r.status === 'APPROVED' && r.reservation_id) {
      await conn.query("UPDATE reservations SET status='CANCELLED' WHERE reservation_id=?", [r.reservation_id]);
      await conn.query('UPDATE parking_slots SET is_available=TRUE WHERE slot_id=?', [r.slot_id]);
    }

    await conn.query(
      "UPDATE reservation_requests SET status='CANCELLED', admin_note='Cancelled by customer' WHERE request_id=?",
      [requestId]
    );
    await conn.commit();
    res.json({ success: true, message: 'Request cancelled successfully.' });
  } catch (e) {
    await conn.rollback();
    res.status(500).json({ error: e.message });
  } finally {
    conn.release();
  }
});

// Admin: RELEASE an occupied parking slot
app.put('/api/slots/:id/release', async (req, res) => {
  const slotId = parseInt(req.params.id, 10);
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[slot]] = await conn.query('SELECT slot_id, slot_name, is_available FROM parking_slots WHERE slot_id = ? FOR UPDATE', [slotId]);
    if (!slot) {
      await conn.rollback();
      return res.status(404).json({ error: 'Slot not found.' });
    }
    if (slot.is_available) {
      await conn.rollback();
      return res.status(400).json({ error: `Slot ${slot.slot_name} is already available.` });
    }

    // 1. Mark slot available
    await conn.query('UPDATE parking_slots SET is_available=TRUE WHERE slot_id=?', [slotId]);

    // 2. Mark any active reservations for this slot as CANCELLED
    await conn.query("UPDATE reservations SET status='CANCELLED' WHERE slot_id=? AND status='ACTIVE'", [slotId]);

    // 3. Update any active approved request note
    await conn.query("UPDATE reservation_requests SET admin_note='Slot released by admin' WHERE slot_id=? AND status='APPROVED'", [slotId]);

    await conn.commit();
    res.json({ success: true, message: `Slot ${slot.slot_name} has been released and is now available.` });
  } catch (e) {
    await conn.rollback();
    res.status(500).json({ error: e.message });
  } finally {
    conn.release();
  }
});

// ═══════════════════════════════════════════════════════════════════════════
// RESERVATIONS (Module B)
// ═══════════════════════════════════════════════════════════════════════════

app.get('/api/reservations', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT r.reservation_id, r.customer_name, r.slot_id, p.slot_name, r.reservation_date, r.status
       FROM reservations r JOIN parking_slots p ON r.slot_id = p.slot_id
       ORDER BY r.reservation_id DESC`
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/reservations/:id', async (req, res) => {
  const id = parseInt(req.params.id, 10);
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const [[r]] = await conn.query('SELECT slot_id, status FROM reservations WHERE reservation_id = ? FOR UPDATE', [id]);
    if (!r)              { await conn.rollback(); return res.status(404).json({ error: 'Reservation not found.' }); }
    if (r.status !== 'ACTIVE') { await conn.rollback(); return res.status(409).json({ error: 'Already cancelled.' }); }
    await conn.query("UPDATE reservations SET status='CANCELLED' WHERE reservation_id=?", [id]);
    await conn.query('UPDATE parking_slots SET is_available=TRUE WHERE slot_id=?', [r.slot_id]);
    await conn.commit();
    res.json({ success: true, message: 'Reservation cancelled. Slot is now available.' });
  } catch (e) { await conn.rollback(); res.status(500).json({ error: e.message }); }
  finally { conn.release(); }
});

// Catch-all SPA
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'index.html')));

app.listen(PORT, () => {
  console.log(`\n  SmartPark v2 running at: http://localhost:${PORT}`);
  console.log(`  Customer Portal + Admin Dashboard with Request Approval Flow\n`);
});
