# ✅ FINAL PRE-DEMO TEST CHECKLIST

**Run this checklist 5 minutes before the demo!**

---

## 🔧 SETUP CHECK (2 minutes)

### 1. Server Running
```bash
# Check if server is running
# Should see: http://localhost:5173/
```
- [ ] Dev server running
- [ ] No errors in terminal
- [ ] Port 5173 accessible

### 2. Browser Setup
- [ ] Chrome or Edge browser open
- [ ] No browser extensions that might interfere
- [ ] Microphone connected (for voice demo)
- [ ] Multiple tab support enabled

### 3. Open Test Tabs
```
http://localhost:5173/demo-access
```
- [ ] Tab 1: Opened
- [ ] Tab 2: Opened  
- [ ] Tab 3: Opened
- [ ] All tabs loaded correctly
- [ ] No console errors (F12 to check)

---

## 🎤 VOICE INPUT TEST (1 minute)

### Tab 1: Senior Citizen
- [ ] Navigate to Senior Citizen page
- [ ] Language selector visible (EN/HI/MR)
- [ ] Large microphone button visible
- [ ] Click microphone button
- [ ] Permission prompt appears
- [ ] Grant microphone permission
- [ ] Speak: "Testing voice input"
- [ ] Text appears in symptom field
- [ ] No errors displayed

**Fallback if fails:**
- [ ] Quick scenario buttons work (🔴 RED Example)
- [ ] Text input works normally

---

## 🎨 NLP EXTRACTION TEST (1 minute)

### Load Demo Scenario
- [ ] Click "🔴 RED Example" button
- [ ] Symptom field auto-fills
- [ ] Fill: Name "Test Patient", Age 72, Contact "+91-1234567890"
- [ ] Click "Analyze & Submit Emergency"

### Verify NLP Display
- [ ] Phrases highlighted in colors visible
- [ ] RED phrases: red background (#fee)
- [ ] Original text preserved
- [ ] Urgency level: RED PRIORITY displayed
- [ ] Reasoning text shown
- [ ] JSON output visible
- [ ] Demo warning prominent
- [ ] Click "Confirm Emergency Request"
- [ ] Session ID generated (EMR-xxxxx-xxxxx format)

---

## ⚡ REAL-TIME SYNC TEST (2 minutes)

### Verify Cross-Tab Sync

#### Check Tab 2: Transport Partner
- [ ] Switch to Tab 2 (don't refresh)
- [ ] Emergency appears automatically
- [ ] Same patient name visible
- [ ] Same Session ID visible
- [ ] RED priority badge shown
- [ ] Symptoms displayed
- [ ] Location coordinates shown
- [ ] No console errors

#### Update Status in Tab 2
- [ ] Click emergency card to expand
- [ ] Click "Accept Emergency"
- [ ] Status changes to TRANSPORT_ASSIGNED
- [ ] Vehicle ID assigned (AMB-xxx)
- [ ] Click "Start En Route"
- [ ] Status changes to EN_ROUTE
- [ ] Progress bar updates

#### Verify Tab 1 Updated
- [ ] Switch to Tab 1 (don't refresh)
- [ ] Status shows "EN ROUTE"
- [ ] Last updated timestamp changed
- [ ] No page refresh needed
- [ ] Update appeared within 2 seconds

#### Check Tab 3: Hospital Operations
- [ ] Switch to Tab 3
- [ ] Patient appears in "Incoming Patients" section
- [ ] Same Session ID visible
- [ ] Status: EN_ROUTE displayed
- [ ] Family notification panel visible (green box at top)
- [ ] Notification shows patient name

---

## 🏥 HOSPITAL WORKFLOW TEST (1 minute)

### In Tab 3: Hospital
- [ ] Click patient card to expand
- [ ] "Patient Handover Summary" visible
- [ ] NLP JSON data displayed
- [ ] Triage assessment shown
- [ ] Keywords listed
- [ ] Click "Acknowledge Patient"
- [ ] Status updates to AT_HOSPITAL

### Verify All Tabs Synced
- [ ] Tab 1: Status = "AT HOSPITAL"
- [ ] Tab 2: Status = "AT HOSPITAL"
- [ ] Tab 3: Status = "AT HOSPITAL"
- [ ] Family notification updated
- [ ] All updates automatic (no refresh)

---

## 🎭 DEMO MODE INDICATORS (30 seconds)

### Verify Demo Labels
- [ ] "🎭 DEMO MODE" banner visible on all pages
- [ ] "Simulated location" labels present
- [ ] "DEMONSTRATION ONLY" warnings visible
- [ ] Emergency numbers shown (108, 102, 112)
- [ ] No false claims of real dispatch

---

## 📱 FALLBACK TESTS (if needed)

### If Voice Fails
- [ ] Quick scenario buttons work
- [ ] Text input works
- [ ] Error message is helpful
- [ ] User not stuck

### If Sync Fails
- [ ] Wait 2 seconds (auto-polling)
- [ ] Manual refresh works
- [ ] Data persists in localStorage
- [ ] Session IDs consistent

### If Browser Issues
- [ ] Switch to Chrome/Edge
- [ ] Clear cache and retry
- [ ] Check console for errors
- [ ] Backend not required (frontend-only demo)

---

## 🎯 FINAL VERIFICATION (30 seconds)

### Overall Check
- [ ] All 3 tabs open and ready
- [ ] Demo flow works end-to-end
- [ ] No console errors
- [ ] All features functional
- [ ] Demo takes 2-4 minutes
- [ ] Confident to present

### Demo Materials Ready
- [ ] QUICK_DEMO_CARD.md open for reference
- [ ] Talking points memorized
- [ ] Backup plan ready (2-minute version)
- [ ] Questions & answers prepared

---

## 🚨 EMERGENCY FIXES

### If Server Crashes
```bash
# Stop and restart
Ctrl+C
npm run dev
```
Wait 10 seconds for server to start

### If Build Errors Appear
```bash
npm run build
```
Should show: ✅ SUCCESS

### If Tabs Don't Sync
1. Check console for BroadcastChannel errors
2. Try incognito/private window
3. Use same browser for all tabs
4. Wait 2 seconds for polling fallback

### If Everything Breaks
1. Refresh all tabs
2. Clear localStorage: `localStorage.clear()`
3. Restart from /demo-access
4. Use QUICK_DEMO_CARD.md 2-minute backup version

---

## ✅ PRE-DEMO CONFIDENCE CHECK

**Answer YES to all:**
- [ ] I can open 3 tabs without issues
- [ ] I can submit an emergency in Tab 1
- [ ] It appears automatically in Tab 2 & 3
- [ ] I can update status in Tab 2
- [ ] Changes appear in Tab 1 & 3
- [ ] I can acknowledge in Tab 3
- [ ] All tabs stay synchronized
- [ ] I can explain what's happening
- [ ] I'm ready to demo

---

## 🎬 YOU'RE READY WHEN...

✅ **Technical:**
- Server running, no errors
- All 3 tabs load correctly  
- Voice or scenarios work
- Cross-tab sync confirmed
- End-to-end flow tested

✅ **Presentation:**
- Demo flow memorized
- Talking points ready
- Timing practiced (2-4 min)
- Backup plan ready
- Q&A answers prepared

✅ **Mindset:**
- Confident in the demo
- Proud of what you built
- Ready to explain technical details
- Excited to show judges

---

## 🏆 FINAL WORDS

**You've built something impressive:**
- ✅ Real voice input (not fake)
- ✅ Visual NLP extraction (transparent)
- ✅ Live cross-tab sync (actually works)
- ✅ Complete emergency workflow
- ✅ Professional UI/UX
- ✅ Honest demo labeling

**The hard work is done. Now just show it off!**

**Break a leg! 🌟**

---

**If all checks pass, you're ready to demo!**

**Open:** http://localhost:5173/demo-access

**Reference:** QUICK_DEMO_CARD.md

**Go get that prize! 🏆**
