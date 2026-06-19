# 🔑 API Key Setup Guide

## 📋 Overview

This app uses **user-provided API keys** for AI services. Each user must configure their own API key from:

- **Google Gemini** (Recommended - Free tier available)
- **OpenAI GPT** (Paid)
- **Anthropic Claude** (Paid)
- **Other compatible services**

## 🚀 Quick Setup

### Method 1: In-App Configuration (Recommended)

1. Open the app
2. Go to **Settings** → **AI Configuration**
3. Tap **"Set API Key"**
4. Paste your API key
5. Tap **"Save"**

Your API key is stored **encrypted** on your device using AES-256-GCM.

### Method 2: First Launch Setup

When you first open the app:
1. You'll see a prompt to configure your AI
2. Tap **"Configure API Key"**
3. Enter your key
4. You're ready to go!

## 🔐 Getting Your API Key

### Google Gemini (FREE + Generous Limits)

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click **"Create API Key"**
4. Copy the key
5. Paste it in the app

**Gemini Free Tier:**
- ✅ 60 requests per minute
- ✅ 1,500 requests per day
- ✅ No credit card required
- ✅ Best for development and personal use

### OpenAI GPT (PAID)

1. Go to [OpenAI Platform](https://platform.openai.com/api-keys)
2. Sign in / Create account
3. Go to **API Keys** section
4. Click **"Create new secret key"**
5. Copy the key (`sk-...`)
6. Paste it in the app

**Pricing:**
- GPT-3.5-Turbo: ~$0.002 per 1K tokens
- GPT-4: ~$0.03 per 1K tokens

### Anthropic Claude (PAID)

1. Go to [Anthropic Console](https://console.anthropic.com/)
2. Sign in / Create account
3. Navigate to **API Keys**
4. Click **"Create Key"**
5. Copy the key
6. Paste it in the app

**Pricing:**
- Claude Instant: ~$0.80 per million tokens
- Claude 2: ~$11.02 per million tokens

## 🛡️ Security

### How Your Key is Protected

1. **Encrypted Storage**
   - AES-256-GCM encryption
   - Android Keystore backed
   - Never stored in plain text

2. **Local Only**
   - Key never leaves your device
   - Not sent to our servers
   - Only used for direct API calls

3. **Secure Transmission**
   - HTTPS/TLS for all API calls
   - Certificate pinning (if configured)
   - No man-in-the-middle attacks

### Best Practices

✅ **DO:**
- Keep your API key private
- Use separate keys for development/production
- Monitor your API usage on provider dashboard
- Rotate keys periodically
- Use free tier for testing

❌ **DON'T:**
- Share your API key with others
- Commit keys to version control
- Use production keys on untrusted devices
- Expose keys in screenshots/logs

## 🔄 Changing Your API Key

### Update Existing Key

1. Go to **Settings** → **AI Configuration**
2. Tap **"Change API Key"**
3. Enter new key
4. Tap **"Save"**

### Remove API Key

1. Go to **Settings** → **AI Configuration**
2. Tap **"Remove API Key"**
3. Confirm deletion

## ❓ Troubleshooting

### "Invalid API Key" Error

**Problem:** API key not recognized

**Solutions:**
1. Verify the key is correct (copy again)
2. Check for extra spaces
3. Ensure key is activated on provider dashboard
4. Verify your account is in good standing

### "Rate Limit Exceeded" Error

**Problem:** Too many requests

**Solutions:**
1. Wait a few minutes
2. Check your quota on provider dashboard
3. Consider upgrading your tier
4. App has built-in rate limiting to prevent this

### "API Key Not Configured" Warning

**Problem:** No key set up

**Solution:**
1. Follow Quick Setup above
2. Restart the app if needed

### Connection Errors

**Problem:** Can't reach API

**Solutions:**
1. Check internet connection
2. Verify provider service status
3. Try different network (WiFi/Mobile data)
4. Check if API endpoint is blocked by firewall

## 💰 Cost Management

### Monitor Usage

- Check your dashboard regularly:
  - [Gemini Usage](https://makersuite.google.com/app/usage)
  - [OpenAI Usage](https://platform.openai.com/usage)
  - [Claude Usage](https://console.anthropic.com/usage)

### Tips to Save Money

1. **Use Gemini Free Tier**
   - Best value for personal use
   - Generous limits

2. **Optimize Prompts**
   - Be concise in queries
   - Use token optimization features
   - Limit conversation history

3. **Set Spending Limits**
   - Configure on provider dashboard
   - Get alerts before hitting limits

4. **Use Appropriate Models**
   - Don't use GPT-4 for simple queries
   - Gemini free is often sufficient

## 🔧 Advanced Configuration

### Multiple API Keys

You can switch between different providers:

1. Configure multiple keys in settings
2. Select active provider
3. App will use the selected key

### Custom API Endpoints

For advanced users running local AI models:

1. Go to **Settings** → **Advanced**
2. Enable **"Custom Endpoint"**
3. Enter your endpoint URL
4. Configure authentication

## 📊 API Key Information

### What Gets Sent to APIs

When you use the AI features:

✅ **Sent:**
- Your message/query
- Conversation history (recent messages)
- System prompts (for context)

❌ **NOT Sent:**
- Your personal info
- Device identifiers
- Location data
- Contact lists
- Other app data

### Provider Privacy Policies

- [Gemini Privacy](https://policies.google.com/privacy)
- [OpenAI Privacy](https://openai.com/privacy/)
- [Claude Privacy](https://www.anthropic.com/privacy)

## 🆘 Support

### Need Help?

1. Check [Troubleshooting](#-troubleshooting) above
2. Visit GitHub Issues: [Your-Repo]/issues
3. Read provider documentation:
   - [Gemini Docs](https://ai.google.dev/docs)
   - [OpenAI Docs](https://platform.openai.com/docs)
   - [Claude Docs](https://docs.anthropic.com)

### Common Questions

**Q: Do I need to pay for an API key?**  
A: No! Gemini offers a generous free tier. Other providers require payment.

**Q: Can I use multiple keys?**  
A: Yes, you can switch between providers in settings.

**Q: Is my key secure?**  
A: Yes, it's encrypted with military-grade AES-256 encryption.

**Q: What if I lose my key?**  
A: Generate a new one from your provider's dashboard.

**Q: Can I share my key?**  
A: No! Each user should have their own key.

---

## 🚀 Ready to Go!

Once your API key is configured, you can:

1. ✅ Ask technical questions
2. ✅ Get code examples
3. ✅ Research security topics
4. ✅ Analyze vulnerabilities
5. ✅ Learn hacking techniques (legally!)

**Enjoy unrestricted AI assistance!** 🎉

---

**Last Updated:** June 22, 2026  
**Version:** 3.0.0 Ultimate Edition
