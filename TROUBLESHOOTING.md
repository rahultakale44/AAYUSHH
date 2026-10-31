# 🔧 QUICK TROUBLESHOOTING GUIDE

## 🚨 Common Issues & Instant Fixes

---

### Issue 1: Voice Input Not Working

**Symptoms:**
- Microphone button clicked but nothing happens
- No transcription appearing
- Error message shows

**Quick Fixes:**
1. **Check browser:** Must use Chrome, Edge, or Safari
   - Firefox doesn't support Web Speech API
   
2. **Check permissions:** Click microphone icon in address bar
   - Allow microphone access
   - Reload page if needed

3. **Use fallback:**
   - Click "🔴 RED Example" button instead
   - Or type symptoms manually
   - Demo still works perfectly without voice

**Time to fix:** 10 seconds

---

### Issue 2: Tabs Not Syncing

**Symptoms:**
- Emergency submitted in Tab 1
- Doesn't appear in Tab 2 or Tab 3

**Quick Fixes:**
1. **Wait 2 seconds:** Auto-polling fallback activates
   
2. **Check same browser:** All tabs must be in SAME browser instance
   - Don't mix Chrome + Edge
   - Don't use incognito + normal tabs

3. **Manual refresh:** Press F5 in Tab 2 and Tab 3
   - Data persists in localStorage
   - Session will appear after refresh

4. **Check console:** Press F12, look for errors
   - If BroadcastChannel error: fallback working (OK)
   - If localStorage error: clear and retry

**Time to fix:** 5 seconds

---

### Issue 3: Server Not Running

**Symptoms:**
- "Can't reach this page" error
- localhost:5173 not accessible

**Quick Fix:**
```bash
# Terminal/PowerShell
cd D:\आयुष
npm run dev
```

Wait 10 seconds for server to start

**Time to fix:** 15 seconds

---

### Issue 4: Build Errors

**Symptoms:**
- TypeScript errors shown
- Red error messages

**Quick Fix:**
```bash
npm run build
```

If errors persist:
```bash
# Re-install dependencies
npm install
npm run build
```

**Time to fix:** 30 seconds

---

### Issue 5: Nothing Loads / White Screen

**Symptoms:**
- Page loads but shows nothing
- White or blank screen

**Quick Fixes:**
1. **Hard refresh:** Ctrl+Shift+R (or Cmd+Shift+R on Mac)

2. **Clear cache:**
   - Press F12
   - Right-click refresh button
   - Select "Empty Cache and Hard Reload"

3. **Check console:** F12 → Console tab
   - Look for red errors
   - Share error message if asking for help

**Time to fix:** 10 seconds

---

### Issue 6: Status Not Updating

**Symptoms:**
- Click "Accept Emergency" but nothing happens
- Status stays same

**Quick Fixes:**
1. **Wait 2 seconds:** Updates may be processing

2. **Check all tabs:** Switch between tabs to verify
   - Update may have worked but not reflected yet

3. **Refresh one tab:** F5 on Tab 2 or Tab 3
   - localStorage has latest data

4. **Start new session:** Create new emergency
   - Previous session might be in final state

**Time to fix:** 5 seconds

---

### Issue 7: NLP Highlighting Not Showing

**Symptoms:**
- No colored highlights on phrases
- All text looks plain

**Quick Fixes:**
1. **Use demo scenario:** Click "🔴 RED Example" button
   - Pre-defined text guaranteed to highlight

2. **Check symptoms:** Must contain keywords
   - RED: "chest pain", "difficulty breathing"
   - ORANGE: "high fever", "severe pain"
   - GREEN: "mild fever", "cough"

3. **Type keywords:** Manually add trigger words
   - Even one keyword will highlight

**Time to fix:** 5 seconds

---

### Issue 8: Can't Open Multiple Tabs

**Symptoms:**
- Browser limiting tabs
- Tabs closing automatically

**Quick Fixes:**
1. **Use windows instead:** Open 3 browser windows
   - Same effect as tabs
   - Easier to show side-by-side

2. **Duplicate tab:** 
   - Right-click tab → Duplicate
   - Repeat 2 times

3. **Bookmark and open:**
   - Bookmark: http://localhost:5173/demo-access
   - Click bookmark 3 times

**Time to fix:** 10 seconds

---

### Issue 9: Demo Too Slow

**Symptoms:**
- Running out of time
- Need faster demo

**Quick Fix - 2-MINUTE VERSION:**
1. **Tab 1:** Click "🔴 RED Example" → Fill name → Submit (30 sec)
2. **Tab 2:** Show it appeared → Accept → Update twice (60 sec)
3. **Tab 3:** Show patient → Acknowledge (30 sec)
4. **Pause:** Show all 3 tabs synced

**Key Message:** "Real-time coordination. Zero manual re-entry."

**Time: 2 minutes**

---

### Issue 10: Forgot Demo Flow

**Symptoms:**
- Blanked on what to do next
- Lost track of steps

**Quick Fix:**
**Open:** `QUICK_DEMO_CARD.md` (This file has the complete flow)

**Mini Cheat Sheet:**
1. Tab 1: Submit emergency
2. Tab 2: Accept & update status
3. Tab 3: Acknowledge patient
4. Show: All tabs synced

**Time to fix:** 0 seconds (just glance at card)

---

## 🆘 EMERGENCY RESTART PROCEDURE

**If everything is completely broken:**

### Step 1: Stop Everything
```bash
# Press Ctrl+C in terminal
# Close all browser tabs
```

### Step 2: Clear Data
```javascript
// In browser console (F12):
localStorage.clear()
```

### Step 3: Restart Server
```bash
npm run dev
```

### Step 4: Fresh Start
1. Wait 10 seconds
2. Open: http://localhost:5173/demo-access
3. Open 3 tabs
4. Start demo fresh

**Total time: 30 seconds**

---

## 🎯 PREVENTION TIPS

**Before Demo:**
- [ ] Test voice input in advance
- [ ] Verify 3 tabs open correctly
- [ ] Do one complete test run
- [ ] Have QUICK_DEMO_CARD.md open
- [ ] Know 2-minute backup version

**During Demo:**
- [ ] If voice fails → Use quick scenarios
- [ ] If sync slow → Wait 2 seconds
- [ ] If stuck → Skip to next step
- [ ] If all fails → "Let me show you the code instead"

---

## 💡 JUDGE QUESTIONS - TROUBLESHOOTING

**Q: Why isn't voice working?**
A: "Voice requires Chrome/Edge and microphone permission. Let me show you the text alternative..." [Click RED Example button]

**Q: Are the tabs really syncing?**
A: "Yes - let me do it again. Watch Tab 2..." [Update status, show Tab 1 changes]

**Q: Is this using a real backend?**
A: "For demo, it's localStorage + BroadcastChannel. In production, this would be a Spring Boot backend with WebSocket."

**Q: Can you show the code?**
A: "Absolutely. Here's the coordination service..." [Show coordinationService.ts]

---

## ✅ CONFIDENCE BOOSTERS

**Remember:**
- The demo WORKS - you tested it
- Voice is bonus - scenarios work perfect
- Sync is REAL - BroadcastChannel API
- You have backups - 2-minute version ready
- You have docs - Quick card available

**If something breaks:**
- Stay calm
- Use fallback
- Explain what should happen
- Judges understand demo hiccups

**You've got this! 🌟**

---

## 🎬 OPENING LINE IF TECH FAILS

"Before I show you the live demo, let me quickly explain what AAYUSH does and how it works..."

[Explain concept for 30 seconds while fixing issue]

Then: "Okay, ready - here we go..."

---

## 📞 NEED HELP?

**During Demo:**
1. Use QUICK_DEMO_CARD.md
2. Use 2-minute backup version
3. Show code instead of live demo
4. Explain concept if all else fails

**Before Demo:**
1. Run FINAL_TEST_CHECKLIST.md
2. Do complete test run
3. Have backup browser ready
4. Memorize 2-minute version

---

## 🏆 FINAL REMINDER

**The demo is solid. You're prepared. You've got backups.**

**Even if something glitches, you can:**
- ✅ Explain how it works
- ✅ Show the code
- ✅ Use fallback demos
- ✅ Impress with knowledge

**Now go show them what you built! 🚀**

---

**Quick Access:**
- Demo: http://localhost:5173/demo-access
- Guide: QUICK_DEMO_CARD.md  
- Full Docs: LIVE_DEMO_GUIDE.md
- Tests: FINAL_TEST_CHECKLIST.md
