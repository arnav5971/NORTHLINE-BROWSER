#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "North Browser GitHub push helper"
read -p "Paste your GitHub repository HTTPS URL: " REPO_URL

git init
git add .
git commit -m "Initial professional North Browser" || true
git branch -M main
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"
git push -u origin main

echo "Done. Check GitHub Actions to build the APK."
