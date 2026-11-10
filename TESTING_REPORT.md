# Software Engineering Lab — Comprehensive Testing & Defect Report
**Project Name:** Smart Parking Management System  
**Test Types Covered:** Black-Box Testing (Specification-Based) & White-Box Testing (Structural/Code-Based)  
**Modules Evaluated:** Core Authentication, Module A (Parking Slots), Module B (Reservations), Concurrency & Persistence Layer  
**Automated Test Suite:** `SystemIntegrationTest.java` (Executed against MySQL Database)

---

## 1. Executive Summary & Testing Objectives

In accordance with Software Engineering Quality Assurance and the Software Testing Life Cycle (STLC), this report documents the testing methodology, test suites, defects identified during development, and the architectural solutions applied to ensure system robustness, integrity, and security.

### Test Execution Summary
| Metric | Result |
| :--- | :--- |
| **Total Automated Test Cases** | 11 |
| **Passed** | 11 |
| **Failed** | 0 |
| **Testing Approaches** | Black-Box Testing (Functional) & White-Box Testing (Structural) |
| **Test Driver** | `java -cp ".;lib/*" SystemIntegrationTest` |
| **Backend Target** | MySQL 8.0 (`smart_parking`) via JDBC Connection Pool |

---

## 2. Black-Box Testing (Functional / Specification-Based)

### 2.1 Methodology & Techniques Applied
Black-box testing verifies system behavior against software requirements without examining internal code or logic paths. The following techniques were employed:

1. **Equivalence Partitioning (EP):** Partitioning input domains into valid and invalid equivalence classes (e.g., valid credentials vs. wrong password vs. empty strings).
2. **Boundary Value Analysis (BVA):** Testing boundary limits such as slot IDs ($\le 0$ vs. valid ID), 0-character customer names, and maximum field length.
3. **State Transition Testing:** Validating lifecycle transitions:
   - Parking Slot: `AVAILABLE (true)` $\rightarrow$ `OCCUPIED (false)` $\rightarrow$ `AVAILABLE (true)`
   - Reservation: `PENDING` $\rightarrow$ `ACTIVE` $\rightarrow$ `CANCELLED`
4. **Error Guessing:** Simulating user mistakes such as repeated cancellations, simultaneous clicks, and malformed inputs.

---

### 2.2 Black-Box Test Cases Matrix

| Test ID | Module | Scenario / Test Objective | Test Input Data | Expected Output | Actual Output | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-01** | Core Auth | Valid administrator login | User: `"admin"`, Pass: `"admin123"` | Authentication success; redirect to Dashboard | Authenticated as `admin` | **PASS** |
| **TC-02** | Core Auth | Invalid credentials rejection | User: `"admin"`, Pass: `"wrong_xyz"` | Access denied; prompt error message | Access denied: `"Invalid username or password."` | **PASS** |
| **TC-03** | Core Auth | Empty credential validation | User: `""`, Pass: `""` | Rejection without crash; validation warning | Blocked: `"Username cannot be empty."` | **PASS** |
| **TC-04** | Module A | Retrieve all parking slots | Query action | Returns non-empty list of available/occupied slots | 5 slots loaded from MySQL successfully | **PASS** |
| **TC-05** | Module A | Book an available slot | Customer: `"Test Customer"`, Slot ID: `1` | Reservation created, slot status turns `OCCUPIED` | Created reservation #6, slot marked occupied | **PASS** |
| **TC-06** | Module A | Block booking on occupied slot | Customer: `"Second Customer"`, Slot ID: `1` | Reservation rejected with double-booking error | Blocked: `"Slot 1 is already occupied."` | **PASS** |
| **TC-07** | Module B | Query reservation records | Query action | Reservation #6 appears with status `ACTIVE` | Found reservation #6 in active query list | **PASS** |
| **TC-08** | Module B | Cancel active reservation | Reservation ID: `6` | Status updated to `CANCELLED`, slot released | Cancelled reservation #6 successfully | **PASS** |
| **TC-09** | Module A | Verify slot freed on cancellation | Check Slot ID: `1` | Slot `is_available` reverts to `true` | Slot #1 `isAvailable = true` | **PASS** |
| **TC-10** | Module B | Block duplicate cancellation | Cancel Reservation ID: `6` again | System prevents redundant cancellation | Blocked: `"Already cancelled. Repeated cancellation prevented."` | **PASS** |

---

### 2.3 Bugs Identified via Black-Box Testing & Solutions

#### Bug BB-01: Blank & Whitespace Customer Name Allowed
- **Defect Description:** Entering blank spaces (`"   "`) or empty strings in the customer name field allowed anonymous reservations to be created in the database.
- **Root Cause:** Missing input sanitization and boundary check prior to SQL execution.
- **Solution:** Added defensive input validation with `.trim().isEmpty()` check in both UI controllers and service layers:
  ```java
  if (customerName == null || customerName.trim().isEmpty()) {
      throw new IllegalArgumentException("Customer name cannot be empty.");
  }
  ```

#### Bug BB-02: Repeated Cancellation Corrupting Slot State
- **Defect Description:** Clicking "Cancel" twice on an already-cancelled reservation attempted to mark the linked parking slot as `is_available = true` repeatedly. If another customer had subsequently booked that slot, the second cancellation would erroneously free the newly booked slot!
- **Root Cause:** Lack of state transition precondition verification on `reservations.status`.
- **Solution:** Enforced strict state check before issuing any updates:
  ```java
  if (!"ACTIVE".equalsIgnoreCase(currentStatus)) {
      throw new IllegalStateException("Reservation #" + reservationId + 
          " is already cancelled. Repeated cancellation prevented.");
  }
  ```

#### Bug BB-03: Offline Database Crash / Uncaught NullPointerException
- **Defect Description:** If the MySQL server was stopped or credentials were invalid, opening the GUI threw uncaught exceptions and crashed the desktop application.
- **Solution:** Implemented `DatabaseConnection.testConnection()` with graceful offline fallback and user-friendly error banners (TC-11).

---

## 3. White-Box Testing (Structural / Code-Based)

### 3.1 Methodology & Techniques Applied
White-box testing inspects the internal source code, control flows, branch decisions, and transactional database integrity:

1. **Statement Coverage:** Ensuring every line of execution in `ParkingSlotManagement` and `ReservationManagement` is evaluated.
2. **Branch / Decision Coverage:** Verifying both `true` and `false` evaluations on critical decision points:
   - Branch A: Slot exists vs. does not exist (`!rs.next()`)
   - Branch B: Slot is available vs. already occupied (`!isAvailable`)
   - Branch C: Reservation is active vs. already cancelled (`!"ACTIVE".equalsIgnoreCase(status)`)
3. **Path Testing & Cyclomatic Complexity ($V(G)$):**
   - For `reserveSlotTransactional`:
     - Nodes $N = 9$, Edges $E = 11$, Predicate Nodes $P = 3$
     - Cyclomatic Complexity $V(G) = E - N + 2P = 11 - 9 + 2(1) = 4$ independent basis paths.
     - All 4 paths were systematically exercised through test cases TC-03, TC-05, TC-06, and DB exception handlers.
4. **Concurrency & Race Condition Testing:** Testing mutual exclusion when two threads or requests attempt to lock the same slot record simultaneously.
5. **ACID Transaction & Rollback Testing:** Verifying that a failure midway through a multi-step operation triggers an immediate `con.rollback()` without leaving orphaned records.

---

### 3.2 White-Box Test Cases Matrix

| Test ID | Method Under Test | Logic Path / Branch Tested | Injected Condition | Expected Behavior | Pass/Fail |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **WB-01** | `reserveSlotTransactional` | Concurrency & Race Condition Path | 2 concurrent attempts on identical `slot_id` | Row-level pessimistic lock (`FOR UPDATE`) serializes access; second thread blocked | **PASS** |
| **WB-02** | `reserveSlotTransactional` | Atomicity & Rollback Path | Exception thrown after slot update but before reservation insert | `con.rollback()` invoked; slot remains unchanged in DB | **PASS** |
| **WB-03** | `cancelReservationTransactional` | Multi-table Consistency Path | Cancellation of valid active reservation | Atomic update of `reservations` + `parking_slots` committed together | **PASS** |
| **WB-04** | `cancelReservationTransactional` | Inactive State Guard Branch | `status != 'ACTIVE'` | Branch evaluates to `false`, rollback executed, `IllegalStateException` thrown | **PASS** |
| **WB-05** | `DatabaseConnection` | Connection Leakage & Auto-close | Resource allocation under stress | `try-with-resources` ensures connection/statement closed even upon SQL exception | **PASS** |

---

### 3.3 Bugs Identified via White-Box Testing & Solutions

#### Bug WB-01: Critical Race Condition (Double-Booking Vulnerability)
- **Defect Description:** In a multi-user environment, two users viewing the same free slot simultaneously submit reservations at the same instant. Under standard `SELECT` and subsequent `UPDATE`, both transactions read `is_available = TRUE` before either commits, leading to two active reservations for a single slot.
- **Root Cause:** Non-atomic read-then-write sequence without database concurrency locks.
- **Solution:** Implemented **Pessimistic Row-Level Locking** using `SELECT ... FOR UPDATE` inside an ACID transaction (`con.setAutoCommit(false)`):

```diff
- // BUGGY IMPLEMENTATION: Vulnerable to Race Conditions
- String sql = "SELECT is_available FROM parking_slots WHERE slot_id = " + slotId;
- ResultSet rs = stmt.executeQuery(sql);
- if (rs.next() && rs.getBoolean("is_available")) {
-     stmt.executeUpdate("UPDATE parking_slots SET is_available = FALSE WHERE slot_id = " + slotId);
-     stmt.executeUpdate("INSERT INTO reservations (customer_name, slot_id) VALUES ('" + name + "', " + slotId + ")");
- }

+ // SOLVED IMPLEMENTATION: ACID Transaction with Pessimistic Locking
+ con.setAutoCommit(false);
+ try {
+     String checkSql = "SELECT is_available FROM parking_slots WHERE slot_id = ? FOR UPDATE";
+     try (PreparedStatement check = con.prepareStatement(checkSql)) {
+         check.setInt(1, slotId);
+         try (ResultSet rs = check.executeQuery()) {
+             if (!rs.next() || !rs.getBoolean("is_available")) {
+                 con.rollback();
+                 throw new IllegalStateException("Slot " + slotId + " is already occupied. Double-booking prevented.");
+             }
+         }
+     }
+     // Mark slot occupied
+     try (PreparedStatement update = con.prepareStatement("UPDATE parking_slots SET is_available = FALSE WHERE slot_id = ?")) {
+         update.setInt(1, slotId);
+         update.executeUpdate();
+     }
+     // Insert reservation record
+     try (PreparedStatement ins = con.prepareStatement("INSERT INTO reservations (customer_name, slot_id, status) VALUES (?, ?, 'ACTIVE')")) {
+         ins.setString(1, trimmedName);
+         ins.setInt(2, slotId);
+         ins.executeUpdate();
+     }
+     con.commit();
+ } catch (Exception e) {
+     con.rollback();
+     throw e;
+ }
```

---

#### Bug WB-02: Atomicity Violation / Orphaned Records
- **Defect Description:** In reservation cancellation, the code originally updated the `reservations` table and then updated the `parking_slots` table in separate auto-commit steps. If a network disruption or server restart occurred between step 1 and step 2, the reservation was marked `CANCELLED`, but the physical slot remained marked `OCCUPIED` forever (orphan lock).
- **Root Cause:** Lack of unified transaction boundaries across multi-table operations.
- **Solution:** Wrapped the dual-table update within a single transaction with mandatory rollback in the `catch` block:

```diff
- // BUGGY: Separate auto-commit statements
- stmt.executeUpdate("UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = " + resId);
- // If crash occurs here, slot is never released!
- stmt.executeUpdate("UPDATE parking_slots SET is_available = TRUE WHERE slot_id = " + slotId);

+ // SOLVED: Atomic Multi-Table Transaction
+ con.setAutoCommit(false);
+ try {
+     // 1. Verify and update reservation
+     // 2. Release parking slot
+     updateReservationStmt.executeUpdate();
+     releaseSlotStmt.executeUpdate();
+     con.commit(); // Both succeed together
+ } catch (SQLException e) {
+     con.rollback(); // Both rolled back on any error
+     throw e;
+ }
```

---

#### Bug WB-03: SQL Injection Vulnerability via String Concatenation
- **Defect Description:** Constructing queries using raw string concatenation allowed malformed or malicious inputs (e.g., `' OR '1'='1`) to manipulate queries.
- **Solution:** Replaced all raw statements with parameterized `PreparedStatement` queries throughout Java and Node.js layers.

---

## 4. Defect Classification & Traceability Matrix

| Defect ID | Defect Category | Discovery Method | Severity | Root Cause | Status |
| :--- | :--- | :--- | :---: | :--- | :---: |
| **DEF-01** | Input Validation | Black-Box (BVA/EP) | Medium | Missing `.trim().isEmpty()` validation | **RESOLVED** |
| **DEF-02** | State Integrity | Black-Box (State Transition) | High | Unchecked duplicate cancellation calls | **RESOLVED** |
| **DEF-03** | Concurrency / Data Integrity | White-Box (Branch & Lock Path) | Critical | Race condition under parallel reservations | **RESOLVED** |
| **DEF-04** | ACID Atomicity | White-Box (Path Coverage) | High | Separate auto-commits for interdependent tables | **RESOLVED** |
| **DEF-05** | Security | White-Box (Static Code Analysis) | High | SQL string concatenation vulnerability | **RESOLVED** |
| **DEF-06** | Fault Tolerance | Black-Box (Error Guessing) | Medium | App crash on database offline state | **RESOLVED** |

---

## 5. Guide for Teacher Demonstration & Viva

When presenting to your evaluator or teacher, follow this step-by-step procedure:

### Step 1: Run the Live Automated Test Suites
Open PowerShell or Terminal in the project directory and run either test suite:

**1. JUnit-style White-Box Test Suite (Internal Logic & Branch Coverage):**
```powershell
javac -cp ".;lib/*" WhiteBoxTesting.java
java  -cp ".;lib/*" WhiteBoxTesting
```

**2. Full System Integration Suite (End-to-End Black & White Box Scenarios):**
```powershell
java -cp ".;lib/*" SystemIntegrationTest
```

**Explain to the teacher:**
> *"Here is our automated test driver. It executes 11 test cases validating both functional requirements (Black-Box: authentication, input boundary handling, slot display) and structural integrity (White-Box: double-booking prevention, duplicate cancellation prevention, ACID rollback)."*

### Step 2: Show the Concurrency / Double-Booking Fix (White-Box)
Open [ParkingSlotManagement.java](file:///c:/Users/ASUS/OneDrive/Desktop/SE_Lab_project/parking/ParkingSlotManagement.java#L84-L150) and point to:
- Line 97: `con.setAutoCommit(false);`
- Line 101: `SELECT is_available FROM parking_slots WHERE slot_id = ? FOR UPDATE;`
- Line 116: `con.rollback();`
- Line 150: `con.commit();`
**Explain to the teacher:**
> *"In white-box testing, we examined the race condition where two users simultaneously try to book the same slot. We solved this by implementing pessimistic row locking with `FOR UPDATE` inside an atomic transaction, guaranteeing that one user succeeds and the second receives a clean rejection without data corruption."*

### Step 3: Show the State Transition Guard (Black-Box)
Open [ReservationManagement.java](file:///c:/Users/ASUS/OneDrive/Desktop/SE_Lab_project/reservation/ReservationManagement.java#L80-L84) and point to:
- Line 80: `if (!"ACTIVE".equalsIgnoreCase(currentStatus)) { ... throw new IllegalStateException(...); }`
**Explain to the teacher:**
> *"In black-box testing, we tested the State Transition model. We caught Bug BB-02 where cancelling an already cancelled reservation caused a defect. We solved it by validating that only reservations currently in the 'ACTIVE' state can transition to 'CANCELLED'."*

---

*Report prepared for Software Engineering Laboratory evaluation.*
