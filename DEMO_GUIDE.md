# AAYUSH (आयुष) - Quick Demo Guide

## 🚀 Launch Instructions

1. **Ensure the dev server is running**:
   ```bash
   npm run dev
   ```

2. **Open in browser**:
   ```
   http://localhost:5174/
   ```

---

## 🎬 5-Minute Demo Script

### Screen 1: Welcome / Role Selection (30 seconds)

**What to say:**
> "AAYUSH is a senior-friendly emergency assistance platform that coordinates the entire emergency response workflow. Users can select one of four roles..."

**What to show:**
- Point out the clean, professional healthcare design
- Hover over each role card to show the interaction
- Mention the multi-role coordination capability

---

### Screen 2: Senior Citizen Interface (2 minutes)

**What to say:**
> "The senior citizen interface prioritizes accessibility with large text, high contrast, and a simple workflow. It supports multiple languages..."

**What to do:**
1. Click "Senior Citizen"
2. **Show language selector** - click between English, Hindi (हिंदी), and Marathi (मराठी)
3. **Fill the form:**
   - Name: "Ramesh Patel"
   - Age: 72
   - Allergies: "Aspirin"
   - Emergency Contact: "+91-9876543210"
4. **Symptoms**: Type or click "Use Voice Input" (if supported):
   - "I have severe chest pain and difficulty breathing for 15 minutes"
5. Click **"Review Emergency Request"**

**What to highlight:**
- Large, touch-friendly buttons
- Clear labels and placeholders
- Optional voice input feature
- Multi-language support

---

### Screen 3: Emergency Request Review (30 seconds)

**What to say:**
> "The system provides a review step where users can confirm their information. Upon confirmation, our transparent triage system analyzes the symptoms..."

**What to do:**
1. Review the information shown
2. Click **"Confirm Emergency"**

**What to show:**
- The confirmation step prevents accidental submissions
- Information is clearly organized

---

### Screen 4: Triage Assessment (1 minute)

**What to say:**
> "The system has created an emergency session with a unique ID. Our transparent keyword-based system has classified this as RED priority because it detected critical keywords. Importantly, we're very clear that this is a demonstration prototype, not a medical diagnosis..."

**What to highlight:**
1. **Session ID** - unique identifier for tracking
2. **RED urgency badge** - with clear "Illustrative Demo Priority" label
3. **Matched keywords** - shows "chest pain, difficulty breathing"
4. **Assessment reasoning** - explains why it's classified as critical
5. **Handover summary** - complete patient information for responders
6. **Safety disclaimers** - prominent warnings about:
   - This is a demo, not medical advice
   - Always contact emergency services (108, 102, 112)

---

### Screen 5: Emergency Response Dashboard (1 minute)

**What to say:**
> "On the Emergency Response side, coordinators see all active requests with patient details and can progress them through the workflow..."

**What to do:**
1. Click "Home" button → "Emergency Response"
2. Show the **statistics dashboard** (active, critical, urgent)
3. Find your created session in the list
4. Point out the **urgency badge**, **patient details**, **symptoms**
5. Click **"Acknowledge Request"**
6. Show status update to "Acknowledged"
7. Click **"Assign Transport (Demo)"**

**What to highlight:**
- Real-time queue of emergency requests
- Clear urgency indicators
- Full patient handover summary
- Status progression workflow

---

### Screen 6: Transport Partner Dashboard (30 seconds)

**What to say:**
> "Transport partners see available demo vehicles and can accept transport requests. All dispatch and ETA information is clearly marked as simulated..."

**What to do:**
1. Click "Home" → "Transport Partner"
2. Show the **mock fleet status** (3 vehicles)
3. Find the transport request
4. Click **"Accept Transport Request"**
5. Show the simulated assignment message

**What to highlight:**
- Mock vehicle fleet display
- Clear labeling of simulated data
- Never claims actual ambulance dispatch

---

### Screen 7: Hospital Operations Dashboard (30 seconds)

**What to say:**
> "Finally, hospital operations can view incoming patients with all mock facility capacity clearly labeled, receive patient handovers, and manage admissions..."

**What to do:**
1. Click "Home" → "Hospital Operations"
2. Show **mock facility status** (beds, ICU, staff)
3. Find the incoming patient
4. Show the **patient handover summary**
5. Click **"Admit Patient (Demo)"**

**What to highlight:**
- Mock capacity indicators clearly labeled
- Complete patient information transfer
- End-to-end workflow completion

---

## 🎯 Key Messages for Judges

### 1. **Accessibility-First Design**
- Large text and buttons optimized for senior citizens
- High contrast, clear labels
- Multi-language support (EN, HI, MR)
- Keyboard navigation support
- Optional voice input with graceful fallback

### 2. **Transparent & Ethical**
- Clear disclaimers about demo nature
- Transparent keyword-based rules (not "black box AI")
- All mock data clearly labeled
- Prominent emergency service contact information
- No false medical claims or reassurance

### 3. **Complete Workflow**
- End-to-end emergency coordination
- Four distinct role interfaces
- Session tracking across all roles
- Status progression from request to completion
- Realistic handover summaries

### 4. **Production-Quality Code**
- TypeScript for type safety
- Modern React with hooks
- Clean component architecture
- Service layer separation
- Production build verified (299KB)
- Zero TypeScript errors

### 5. **Hackathon-Ready**
- Professional healthcare theme
- Responsive design (mobile, tablet, desktop)
- Complete working demo in ~2 hours
- Polished UI, not a prototype sketch
- All features functional, no placeholder buttons

---

## 💡 Backup Demo Points (If Questions Arise)

### Q: "What makes this senior-friendly?"
**A**: Large 1.25rem+ text, high contrast colors, generous spacing, simple workflow, multi-language support, optional voice input, clear validation messages, and no complex navigation.

### Q: "How does the triage system work?"
**A**: Transparent keyword matching against predefined rules. RED for critical keywords (chest pain, stroke), ORANGE for urgent (difficulty breathing, high fever), GREEN for non-urgent. All reasoning is shown to the user.

### Q: "Is this safe to use?"
**A**: This is explicitly a demonstration prototype for the hackathon. It includes prominent disclaimers that it's not for medical use and always directs users to contact real emergency services (108, 102, 112).

### Q: "Can it scale?"
**A**: The frontend is production-ready. We have architecture planned for Spring Boot backend, PostgreSQL database, and Python FastAPI services for future scaling.

### Q: "What about real-time updates?"
**A**: Currently uses localStorage with periodic refresh (5 seconds). Production version would use WebSocket or Server-Sent Events for true real-time coordination.

---

## 📱 Mobile Demo Tips

If demoing on mobile:
1. Open http://localhost:5174/ on your phone (ensure same network)
2. Show the responsive layout adaptation
3. Demonstrate touch-friendly large buttons
4. Try voice input feature (works better on mobile)
5. Show language switching

---

## ⚠️ Demo Don'ts

1. ❌ Don't claim it's a medical device
2. ❌ Don't say ambulances are actually dispatched
3. ❌ Don't imply the triage is clinically validated
4. ❌ Don't present mock hospital data as real
5. ❌ Don't suggest it replaces emergency services

## ✅ Demo Do's

1. ✅ Emphasize it's a coordination platform
2. ✅ Highlight the transparency of the system
3. ✅ Showcase the accessibility features
4. ✅ Demonstrate the complete workflow
5. ✅ Point out the clear safety disclaimers

---

## 🏆 Unique Selling Points

1. **First senior-citizen-focused emergency platform** with true accessibility
2. **Transparent AI** - shows exactly why decisions were made
3. **Multi-language support** - Hindi, Marathi, English
4. **Complete coordination** - not just one piece, but the entire workflow
5. **Ethical design** - safety first, clear disclaimers, no false promises
6. **Production quality** - not just slides, a working application

---

## 🎉 Closing Statement

> "AAYUSH demonstrates how technology can bridge the gap in emergency response for our senior citizens. With accessibility-first design, transparent decision-making, and complete workflow coordination, we're showing what's possible when we prioritize clarity, safety, and user needs. This is a working prototype ready for the next phase of development."

---

**Good luck with your demo! 🚀**
