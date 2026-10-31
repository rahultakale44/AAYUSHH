# AAYUSH - Vercel Deployment Guide

## ✅ Pre-Deployment Checklist

Your AAYUSH frontend is ready for Vercel deployment!

- ✅ Build successful (317 KB JS, 18 KB CSS, 0 TypeScript errors)
- ✅ Vite configuration optimized
- ✅ React Router configured with client-side routing
- ✅ vercel.json configuration created
- ✅ All assets properly bundled

## 🚀 Deployment Methods

### Method 1: Deploy via Vercel CLI (Recommended)

#### Step 1: Install Vercel CLI
```bash
npm install -g vercel
```

#### Step 2: Login to Vercel
```bash
vercel login
```

#### Step 3: Deploy
```bash
# For preview deployment
vercel

# For production deployment
vercel --prod
```

The CLI will automatically:
- Detect the Vite framework
- Use the build settings from vercel.json
- Deploy to a unique URL

---

### Method 2: Deploy via Vercel Dashboard (Easiest)

#### Step 1: Push to GitHub
First, initialize git and push your code:
```bash
git init
git add .
git commit -m "Initial commit - AAYUSH emergency assistance platform"
git branch -M main
git remote add origin <your-github-repo-url>
git push -u origin main
```

#### Step 2: Import to Vercel
1. Go to [vercel.com](https://vercel.com)
2. Click "Add New Project"
3. Import your GitHub repository
4. Vercel will auto-detect settings:
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
   - **Install Command**: `npm install`

#### Step 3: Deploy
Click "Deploy" and wait ~1-2 minutes.

---

### Method 3: Deploy via Vercel GitHub Integration (Automatic)

Once connected, every push to your main branch will automatically deploy!

1. Connect your repository to Vercel (Method 2)
2. Enable automatic deployments in project settings
3. Every `git push` will trigger a new deployment
4. Pull requests get preview deployments automatically

---

## 🔧 Configuration Details

### vercel.json Explanation
```json
{
  "version": 2,
  "buildCommand": "npm run build",      // Runs TypeScript + Vite build
  "outputDirectory": "dist",            // Vite output folder
  "framework": "vite",                  // Auto-detected
  "rewrites": [                         // Required for React Router SPA
    {
      "source": "/(.*)",
      "destination": "/index.html"      // All routes → index.html
    }
  ],
  "headers": [                          // Cache optimization
    {
      "source": "/assets/(.*)",
      "headers": [
        {
          "key": "Cache-Control",
          "value": "public, max-age=31536000, immutable"
        }
      ]
    }
  ]
}
```

### Why SPA Rewrites?
React Router uses client-side routing. Without rewrites:
- ❌ `/demo-access` → 404 error
- ✅ With rewrites → Correctly loads React app

---

## 🌐 After Deployment

### Your deployed URLs will be:
- **Production**: `https://aayush-<random>.vercel.app`
- **Custom Domain** (optional): `https://aayush.yourdomain.com`

### Test these routes:
1. **Landing Page**: `/`
2. **Demo Access**: `/demo-access`
3. **Senior Citizen**: `/senior-citizen`
4. **Emergency Response**: `/emergency-response`
5. **Transport Partner**: `/transport-partner`
6. **Hospital Operations**: `/hospital-operations`

---

## 🔐 Environment Variables (For Later)

When you integrate the backend, add these in Vercel Dashboard:

**Project Settings → Environment Variables**

```bash
VITE_API_BASE_URL=https://your-backend-api.com
VITE_BACKEND_URL=https://your-spring-boot-api.com
```

Then update your frontend code:
```typescript
const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
```

---

## 🐛 Troubleshooting

### Build Fails
```bash
# Test build locally first
npm run build

# Check for TypeScript errors
npm run build 2>&1 | grep error
```

### 404 on Routes
- ✅ Ensure `vercel.json` has the rewrites configuration
- ✅ Check React Router is using BrowserRouter (not HashRouter)

### Assets Not Loading
- ✅ Verify `vite.config.ts` doesn't have incorrect `base` path
- ✅ Check `dist/` folder contains `assets/` directory

### Deployment Doesn't Update
- Clear build cache in Vercel dashboard: **Project Settings → Advanced → Clear Build Cache**
- Redeploy from Vercel dashboard: **Deployments → ⋯ → Redeploy**

---

## 📊 Performance Expectations

Based on your build:
- **Bundle Size**: 317 KB JS (gzipped: 96 KB)
- **CSS Size**: 18 KB (gzipped: 3.7 KB)
- **First Load**: ~1-2 seconds on 3G
- **Lighthouse Score**: 90+ (expected)

### Optimization Tips (Later)
- Add code splitting for routes
- Lazy load dashboard components
- Optimize images (convert to WebP)
- Enable Vercel Analytics

---

## 🎯 Deployment Command Summary

```bash
# Quick deployment (one command)
npx vercel --prod

# Or step by step
npm install -g vercel
vercel login
vercel --prod
```

---

## 🔮 Next Steps After Deployment

1. **✅ Test all features** on the deployed URL
2. **✅ Share the link** for feedback
3. **🔄 Add custom domain** (optional)
4. **🔒 Deploy backend** (Spring Boot on Render/Railway/Heroku)
5. **🔗 Connect frontend to backend** with environment variables
6. **📊 Enable Vercel Analytics** (free tier available)

---

## 📝 Notes

- **Free Tier Limits**: 100 GB bandwidth/month, unlimited deployments
- **Build Time**: ~30-60 seconds per deployment
- **Cold Start**: None (Vercel Edge Network)
- **SSL**: Automatic HTTPS certificate
- **CDN**: Global edge network included

---

## 🆘 Need Help?

- Vercel Docs: https://vercel.com/docs
- Vite Deployment: https://vitejs.dev/guide/static-deploy.html
- React Router: https://reactrouter.com/en/main

---

**Your AAYUSH platform is production-ready! 🚀**

Built for InnovateX 2.0 Hackathon | October 2026
