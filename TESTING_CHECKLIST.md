# AAYUSH Testing Checklist

Use this checklist to verify all features are working correctly.

---

## ✅ Pre-Demo Checks

- [ ] Development server is running: `npm run dev`
- [ ] Browser open at: http://localhost:5174/
- [ ] Console shows no errors (F12 → Console tab)
- [ ] Network is stable

---

## 🏠 Role Selection Page

- [ ] Page loads without errors
- [ ] AAYUSH branding visible
- [ ] All 4 role cards displayed:
  - [ ] Senior Citizen (UserRound icon)
  - [ ] Emergency Response (Radio icon)
  - [ ] Transport Partner (Ambulance icon)
  - [ ] Hospital Operations (Building2 icon)
- [ ] Cards have hover effect
- [ ] Demo disclaimer alert visible
- [ ] All cards clickable and navigate correctly

---

## 👴 Senior Citizen Interface

### Initial Load
- [ ] Page loads with large text
- [ ] "Emergency Assistance" heading visible
- [ ] Language selector present (EN/HI/MR)
- [ ] Form has clear labels

### Language Switching
- [ ] Click "हिंदी" - all labels update to Hindi
- [ ] Click "मराठी" - all labels update to Marathi
- [ ] Click "English" - all labels update back
- [ ] Form functionality unchanged

### Form Validation
- [ ] Submit empty form - shows validation errors
- [ ] Name required - error shows
- [ ] Age required - error shows
- [ ] Age < 1 or > 120 - error shows
- [ ] Emergency contact required - error shows
- [ ] Symptoms required - error shows

### Form Filling
- [ ] Name field accepts text input
- [ ] Age field accepts numbers only
- [ ] Allergies field accepts text (optional)
- [ ] Emergency contact accepts phone format
- [ ] Symptoms textarea accepts multi-line text
- [ ] All fields update state correctly

### Voice Input
- [ ] Click "Use Voice Input" button
- [ ] Browser asks for microphone permission (if supported)
- [ ] Speak: "I have chest pain"
- [ ] Text appears in symptoms field
- [ ] OR shows fallback message if not supported

### Review Step
- [ ] Click "Review Emergency Request"
- [ ] Validation passes with complete form
- [ ] Review page shows all entered information
- [ ] Name, age, allergies, contact visible
- [ ] Symptoms displayed in box
- [ ] Warning disclaimer visible
- [ ] "Back to Home" button works
- [ ] "Confirm Emergency" button visible

### Emergency Submission
- [ ] Click "Confirm Emergency"
- [ ] Success page loads
- [ ] Green checkmark icon visible
- [ ] Session ID generated (EMR-timestamp-random)
- [ ] Urgency badge shows (RED/ORANGE/GREEN)
- [ ] "Illustrative Demo Priority" label present
- [ ] Matched keywords listed (if any)
- [ ] Assessment reasoning shown
- [ ] Handover summary displayed
- [ ] Demo warning present
- [ ] Emergency services info (108, 102, 112) visible
- [ ] "Create New Request" button works

---

## 🚨 Emergency Response Dashboard

### Initial Load
- [ ] Page loads without errors
- [ ] "Emergency Response Dashboard" heading
- [ ] Statistics cards displayed:
  - [ ] Active Requests count
  - [ ] Critical (Demo) count
  - [ ] Urgent (Demo) count
- [ ] "Demo Dashboard" alert visible
- [ ] Session created earlier appears in list

### Session Display
- [ ] Session ID visible and correct
- [ ] Timestamp shows creation time
- [ ] Urgency badge shows (RED/ORANGE/GREEN)
- [ ] Patient name and age displayed
- [ ] Emergency contact shown
- [ ] Allergies displayed (or "None reported")
- [ ] Status badge visible
- [ ] Symptoms shown in box
- [ ] Triage assessment displayed
- [ ] Keywords listed
- [ ] Handover summary visible

### Status Progression
- [ ] "Acknowledge Request" button visible (if CREATED)
- [ ] Click "Acknowledge" - status updates
- [ ] "Assign Transport (Demo)" appears
- [ ] Click "Assign Transport" - status updates
- [ ] "Mark En Route (Demo)" appears
- [ ] Continue through all states
- [ ] Final state: "Complete Session"
- [ ] Completed sessions removed from active list

### Auto-Refresh
- [ ] Create new session in another tab
- [ ] Wait 5 seconds
- [ ] New session appears automatically
- [ ] Statistics update

---

## 🚑 Transport Partner Dashboard

### Initial Load
- [ ] Page loads without errors
- [ ] "Transport Partner Dashboard" heading
- [ ] "⚠️ Simulated Transport System" alert visible
- [ ] "Demo Fleet Status" section present
- [ ] 3 mock vehicles displayed:
  - [ ] AMB-001 (Rajesh Kumar, Zone A)
  - [ ] AMB-002 (Priya Sharma, Zone B)
  - [ ] AMB-003 (Mohammed Ali, Zone C)

### Fleet Status Cards
- [ ] Ambulance icon on each card
- [ ] Vehicle ID visible
- [ ] Driver name shown
- [ ] Location displayed
- [ ] Status badge present

### Transport Requests
- [ ] "Transport Requests" section visible
- [ ] Acknowledged sessions appear
- [ ] Session details displayed
- [ ] Urgency badge visible

### Accept Transport
- [ ] "Accept Transport Request (Demo)" button visible
- [ ] Click button - status updates
- [ ] Blue assignment box appears
- [ ] Shows "Demo Assignment: AMB-001 (Simulated)"
- [ ] Mock ETA displayed
- [ ] "Start Route (Demo)" button appears

### Progress Route
- [ ] Click "Start Route"
- [ ] Orange "En Route" box appears
- [ ] Mock ETA shown
- [ ] "Mark Arrived (Demo)" button appears
- [ ] Click "Mark Arrived"
- [ ] Green success alert shows
- [ ] "Arrived at Patient Location" message

---

## 🏥 Hospital Operations Dashboard

### Initial Load
- [ ] Page loads without errors
- [ ] "Hospital Operations Dashboard" heading
- [ ] "⚠️ Mock Hospital Data" alert visible
- [ ] "Demo Facility Status" section present

### Facility Status Cards
- [ ] Emergency Beds card:
  - [ ] Bed icon visible
  - [ ] Shows "7/12 Available (Mock Data)"
- [ ] ICU Beds card:
  - [ ] Bed icon visible
  - [ ] Shows "3/8 Available (Mock Data)"
- [ ] Medical Staff card:
  - [ ] Users icon visible
  - [ ] Shows "43 On Duty (Mock Data)"
  - [ ] Shows "15 Doctors, 28 Nurses"

### Statistics Cards
- [ ] "En Route" count displayed
- [ ] "Arrived" count displayed
- [ ] "In Hospital" count displayed

### Incoming Patients
- [ ] "Incoming Patient Handovers" section visible
- [ ] En route or arrived patients listed
- [ ] Session ID visible
- [ ] Status message shown
- [ ] Urgency badge displayed

### Patient Details
- [ ] Patient name, age, contact visible
- [ ] Known allergies displayed
- [ ] Reported symptoms shown in box
- [ ] Demo triage assessment displayed
- [ ] Keywords listed
- [ ] Handover summary in gray box
- [ ] "⚠️ This is a demonstration" in summary

### Admission Workflow
- [ ] "Prepare for Arrival" or "Admit Patient (Demo)" button visible
- [ ] Click button - status updates to AT_HOSPITAL
- [ ] Green success alert appears
- [ ] "Patient Admitted" message
- [ ] "Complete Session" button appears
- [ ] Click "Complete" - patient removed from list

---

## 📱 Responsive Design

### Mobile (375px width)
- [ ] Open DevTools (F12)
- [ ] Toggle device toolbar
- [ ] Set width to 375px
- [ ] Check all pages:
  - [ ] Role cards stack vertically
  - [ ] Buttons remain large and tappable
  - [ ] Text remains readable
  - [ ] No horizontal scrolling
  - [ ] Forms fit on screen
  - [ ] Dashboard grids stack

### Tablet (768px width)
- [ ] Set width to 768px
- [ ] Check all pages:
  - [ ] Layout adapts appropriately
  - [ ] 2-column grids where appropriate
  - [ ] Spacing comfortable
  - [ ] Navigation clear

### Desktop (1200px width)
- [ ] Set width to 1200px
- [ ] Check all pages:
  - [ ] Content centered with max-width
  - [ ] Multi-column grids
  - [ ] Comfortable spacing
  - [ ] Professional appearance

---

## 🔄 Cross-Role Integration

### End-to-End Flow
- [ ] Senior creates emergency (RED priority)
- [ ] Session appears in Emergency Response
- [ ] Emergency acknowledges
- [ ] Session appears in Transport
- [ ] Transport accepts and starts route
- [ ] Session appears in Hospital as "En Route"
- [ ] Transport marks arrived
- [ ] Hospital sees "Arrived" status
- [ ] Hospital admits patient
- [ ] Hospital completes session
- [ ] Session removed from all active lists

### Data Consistency
- [ ] Same session ID across all views
- [ ] Status updates reflect everywhere
- [ ] Patient details consistent
- [ ] Timestamps match
- [ ] Urgency level unchanged

---

## 🛡️ Safety & Ethics

### Disclaimers Present
- [ ] Landing page: "Demonstration Prototype" alert
- [ ] Senior citizen page: Emergency services info (108, 102, 112)
- [ ] Emergency created page: "⚠️ This is a demonstration prototype"
- [ ] Emergency Response: "Demo Dashboard" alert
- [ ] Transport Partner: "⚠️ Simulated Transport System"
- [ ] Hospital Operations: "⚠️ Mock Hospital Data"

### Mock Data Labeled
- [ ] All simulated assignments say "(Demo)" or "(Simulated)"
- [ ] All mock capacity says "(Mock Data)"
- [ ] Urgency badges say "Illustrative Demo Priority"
- [ ] Handover says "⚠️ This is an illustrative demo assessment"

### No False Claims
- [ ] Never says ambulance actually dispatched
- [ ] Never says hospital beds actually reserved
- [ ] Never claims to contact emergency services
- [ ] Never provides medical diagnosis
- [ ] Always directs to real emergency numbers

---

## 🏗️ Build & Production

### Development Build
- [ ] `npm run dev` starts without errors
- [ ] Server running on port 5173 or 5174
- [ ] Hot module replacement working
- [ ] Changes reflect immediately
- [ ] No console errors

### Production Build
- [ ] Run `npm run build`
- [ ] TypeScript compilation succeeds (0 errors)
- [ ] Vite build succeeds
- [ ] dist/ folder created
- [ ] Files generated:
  - [ ] dist/index.html
  - [ ] dist/assets/*.css (~7KB)
  - [ ] dist/assets/*.js (~299KB)

### Preview Production
- [ ] Run `npm run preview`
- [ ] Preview server starts
- [ ] Open preview URL
- [ ] Application works identically
- [ ] No console errors

---

## 📊 Performance

### Load Times
- [ ] Initial page load < 2 seconds
- [ ] Navigation between pages instant
- [ ] Form inputs responsive < 100ms
- [ ] Button clicks responsive < 100ms

### No Memory Leaks
- [ ] Navigate between all pages multiple times
- [ ] Memory usage stable (check DevTools → Memory)
- [ ] No growing arrays in console
- [ ] localStorage not growing indefinitely

---

## 🐛 Error Handling

### Form Errors
- [ ] Empty required fields show errors
- [ ] Invalid age shows error
- [ ] Error messages clear and helpful
- [ ] Errors clear when corrected

### Missing Data
- [ ] Empty session list shows empty state
- [ ] Empty state has icon and message
- [ ] No transport requests shows empty state
- [ ] No incoming patients shows empty state

### Browser Compatibility
- [ ] Chrome: All features work
- [ ] Firefox: All features work (voice may not work)
- [ ] Edge: All features work
- [ ] Safari: Core features work (voice varies)

---

## 📝 Final Verification

- [ ] All pages accessible from role selection
- [ ] Home button returns to role selection
- [ ] No broken links
- [ ] No dead buttons
- [ ] No console errors
- [ ] No console warnings (except expected ones)
- [ ] TypeScript: 0 errors
- [ ] Build: Success
- [ ] Documentation: Complete

---

## ✅ Sign-Off

**Tested By**: _________________
**Date**: _________________
**Result**: ☐ PASS  ☐ FAIL  ☐ NEEDS WORK
**Notes**: ___________________________________________
_____________________________________________________
_____________________________________________________

---

**Status**: ☐ Ready for Demo  ☐ Issues Found  ☐ Re-test Required
