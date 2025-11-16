#!/bin/bash

# Script de debugging pour demain
# Usage: ./debug-tomorrow.sh

echo "=== ESF Calendar App - Debug Script ==="
echo ""

echo "1. Checking current git commit..."
git log --oneline -1
echo ""

echo "2. Vérification de l'état du code..."
echo "   - ESF_BASE_URL actuel:"
grep "ESF_BASE_URL" android/app/src/main/java/com/esf/calendar/util/Constants.kt
echo ""

echo "   - saveCookies() met isLoggedIn=true:"
grep -A 2 "putBoolean(KEY_IS_LOGGED_IN" android/app/src/main/java/com/esf/calendar/data/local/SecurePreferencesManager.kt | head -1
echo ""

echo "   - SSL bypass activé (UnsafeOkHttpClient):"
if grep -q "UnsafeOkHttpClient" android/app/src/main/java/com/esf/calendar/data/remote/RetrofitClient.kt; then
    echo "   ✓ OUI"
else
    echo "   ✗ NON"
fi
echo ""

echo "   - Logs présents dans MainActivity:"
if grep -q "android.util.Log.d" android/app/src/main/java/com/esf/calendar/ui/MainActivity.kt; then
    echo "   ✓ OUI"
else
    echo "   ✗ NON"
fi
echo ""

echo "3. ÉTAPES À SUIVRE:"
echo "   a. Dans Android Studio:"
echo "      - Build → Clean Project"
echo "      - Build → Rebuild Project"
echo ""
echo "   b. Désinstaller l'ancienne version:"
echo "      adb uninstall com.esf.calendar"
echo ""
echo "   c. Lancer l'app et COPIER TOUS LES LOGS"
echo ""
echo "   d. Filtrer Logcat par: package:com.esf.calendar"
echo "      OU chercher ces tags:"
echo "      - MainActivity"
echo "      - LoginViewModel"
echo "      - ESFAuthService"
echo "      - CalendarViewModel"
echo "      - ESFRepository"
echo ""

echo "=== Bon courage ! 🚀 ==="
