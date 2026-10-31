# 🚀 Quick Start: Deploy AAYUSH to Vercel

## Option 1: One-Command Deploy (Fastest)

```bash
npx vercel --prod
```

Follow the prompts:
- Login to Vercel (opens browser)
- Confirm project settings (just press Enter)
- Wait ~1 minute
- Get your live URL!

---

## Option 2: GitHub + Vercel (Recommended for Teams)

### Step 1: Push to GitHub
```bash
git init
git add .
git commit -m "Deploy AAYUSH emergency assistance platform"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/aayush.git
git push -u origin main
```

### Step 2: Deploy on Vercel
1. Go to **[vercel.com/new](https://vercel.com/new)**
2. Click **"Import Git Repository"**
3. Select your GitHub repo
4. Click **"Deploy"** (settings auto-detected!)
5. Done! 🎉

---

## ✅ What's Already Configured

- ✅ `vercel.json` created with optimal settings
- ✅ React Router rewrites configured
- ✅ Asset caching headers set
- ✅ Build tested and working (317 KB)
- ✅ `.gitignore` updated for deployment

---

## 🌐 Your Live App Will Have

- **HTTPS** by default (automatic SSL)
- **Global CDN** (fast worldwide)
- **Auto deployments** on every push (if using GitHub)
- **Preview URLs** for pull requests
- **Zero downtime** deployments

---

## 🎯 Test After Deployment

Visit these URLs on your deployed site:
- `/` - Landing page
- `/demo-access` - Role selection
- `/senior-citizen` - Emergency form
- `/emergency-response` - Dashboard
- `/transport-partner` - Transport view
- `/hospital-operations` - Hospital view

---

## 📱 Share Your Demo

Your deployment URL will look like:
```
https://aayush-xyz123.vercel.app
```

Perfect for:
- Hackathon submissions
- Team reviews
- User testing
- Demo presentations

---

## 🔗 Next: Connect Backend

When your Spring Boot backend is ready:

1. Deploy backend to **Render** or **Railway**
2. Add environment variable in Vercel:
   ```
   VITE_API_BASE_URL=https://your-backend.onrender.com
   ```
3. Redeploy frontend
4. Full-stack app ready! 🎊

---

**Ready to deploy? Run:** `npx vercel --prod`
