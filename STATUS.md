# État actuel du projet ESF Calendar App

**Date**: 2025-11-17
**Commit actuel**: `3618842` - "debug: Add extensive logging to trace authentication and navigation flow"

---

## ✅ Ce qui fonctionne

1. **Authentification WebView**
   - Le WebView se charge correctement
   - L'utilisateur peut entrer ses identifiants
   - Le WebView se ferme après connexion réussie ✓
   - Les cookies sont capturés depuis `carnet-rouge-esf.app` ✓

2. **Gestion des certificats SSL**
   - `UnsafeOkHttpClient` créé pour accepter les certificats auto-signés
   - `onReceivedSslError` dans WebView pour bypass SSL ✓
   - Configuration réseau sécurisée ajoutée ✓

3. **Stockage sécurisé**
   - `SecurePreferencesManager` utilise EncryptedSharedPreferences ✓
   - **FIX IMPORTANT**: `saveCookies()` met maintenant `isLoggedIn = true` ✓
   - Les cookies et moniteurId sont sauvegardés ✓

4. **Logging étendu**
   - MainActivity logge onCreate et UI init ✓
   - LoginViewModel logge checkLoginStatus et onAuthSuccess ✓
   - CalendarViewModel logge syncEvents ✓
   - ESFRepository logge toute la synchro ✓

---

## ❌ Le problème à résoudre

**Les événements ne s'affichent pas** après connexion réussie.

### Causes possibles

1. **Les logs n'apparaissent pas** (dans les derniers tests utilisateur)
   - Soit l'app n'a pas été rebuild avec la dernière version
   - Soit les ViewModels ne s'initialisent pas

2. **La synchronisation ne se lance pas**
   - Possible que `repository.isLoggedIn()` retourne encore `false`
   - Possible que `repository.syncEvents()` ne soit jamais appelé
   - Possible que l'API retourne une erreur

3. **L'API ne fonctionne pas avec ces cookies**
   - Les cookies de `carnet-rouge-esf.app` (OAuth) ne sont peut-être pas compatibles avec l'API sur `esf356.w-esf.com` (session cookies)

---

## 📋 Plan pour demain

### Étape 1: Vérifier que les logs apparaissent

1. Dans Android Studio: **Clean + Rebuild**
2. Désinstaller complètement l'app: `adb uninstall com.esf.calendar`
3. Installer et lancer la nouvelle version
4. **COPIER TOUS LES LOGS** depuis le lancement

**Logs attendus**:
```
MainActivity: === onCreate appelé ===
LoginViewModel: === init ===
LoginViewModel: checkLoginStatus: isLoggedIn=false
ESFAuthService: Page loaded: https://carnet-rouge-esf.app/...
... (après login) ...
ESFAuthService: Auth success detected!
LoginViewModel: === onAuthSuccess appelé ===
LoginViewModel: Cookies: ...
LoginViewModel: Sauvegarde OK, navigation vers calendrier
CalendarViewModel: === syncEvents() appelé ===
ESFRepository: === Début synchronisation ===
```

Si ces logs n'apparaissent pas → problème de build

### Étape 2: Déboguer la synchro

Si les logs apparaissent mais la synchro échoue, regarder:

1. **ESFRepository logs**:
   - "Credentials non trouvés" → `isLoggedIn` est encore `false`
   - "ID moniteur non trouvé" → moniteurId pas sauvegardé
   - "Erreur API XXX" → problème d'API

2. **Si erreur API**, vérifier:
   - Les cookies sont-ils corrects ?
   - L'URL d'API est-elle correcte ?
   - Les headers sont-ils corrects ?

### Étape 3: Si les cookies OAuth ne fonctionnent pas

**Option A**: Changer l'architecture (DERNIÈRE SOLUTION)
- Au lieu de `carnet-rouge-esf.app`, utiliser `esf356.w-esf.com` directement
- Comme le script Python original
- Nécessite de changer `Constants.ESF_BASE_URL`

**Option B**: Extraire le token depuis l'OAuth
- Parser le code OAuth depuis l'URL de callback
- Échanger le code contre un access token
- Utiliser ce token pour les API calls

---

## 🔧 Fichiers modifiés

### Corrections importantes (à garder):
- `SecurePreferencesManager.kt` → saveCookies() met isLoggedIn=true
- `UnsafeOkHttpClient.kt` → bypass SSL pour dev
- `RetrofitClient.kt` → utilise UnsafeOkHttpClient
- `ESFAuthService.kt` → onReceivedSslError pour WebView
- `MainActivity.kt`, `LoginViewModel.kt`, `CalendarViewModel.kt` → logs étendus

### À NE PAS changer (sauf si nécessaire):
- `Constants.kt` → ESF_BASE_URL = "https://carnet-rouge-esf.app/"
- Détection OAuth dans ESFAuthService (fonctionne)

---

## 🎯 Objectif final

**Que la synchro fonctionne et que les événements s'affichent !**

Une fois que ça marche:
1. Améliorer l'extraction du moniteurId (ne plus hardcoder "19358136")
2. Créer l'écran Settings
3. Implémenter les notifications
4. Finaliser le widget
5. Intégration Google Calendar

---

**Note**: On est TRÈS PROCHE de la solution ! Le login fonctionne, les cookies sont sauvegardés, il reste juste à déboguer pourquoi la synchro ne se lance pas ou échoue.
