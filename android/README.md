# ESF Calendar - Application Android

Application Android moderne pour consulter son emploi du temps ESF hors ligne avec synchronisation automatique.

## 🚀 Fonctionnalités

- ✅ **Authentification sécurisée** via WebView ESF
- ✅ **Synchronisation automatique** toutes les 15-30 minutes (configurable)
- ✅ **Mode hors ligne** avec base de données locale Room
- ✅ **Widget** pour l'écran d'accueil
- ✅ **Notifications** pour les nouveaux événements
- ✅ **Thème futuriste** avec animations fluides
- ✅ **Intégration Google Calendar** (à venir)

## 🛠️ Technologies

- **Kotlin** + **Jetpack Compose** (UI moderne)
- **Architecture MVVM** (ViewModel + Repository)
- **Room Database** (stockage hors ligne)
- **WorkManager** (synchronisation en arrière-plan)
- **Retrofit** (API REST)
- **EncryptedSharedPreferences** (sécurité des credentials)
- **Material Design 3** (design system)

## 📦 Installation

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17
- Android SDK API 34

### Étapes

1. Ouvrir le dossier `android/` dans Android Studio
2. Synchroniser Gradle (Android Studio le fait automatiquement)
3. Brancher un appareil ou lancer un émulateur
4. Appuyer sur Run ▶️

## 🏗️ Structure du projet

```
android/
├── app/
│   ├── src/main/java/com/esf/calendar/
│   │   ├── data/              # Couche données
│   │   │   ├── local/         # Room Database, Preferences
│   │   │   ├── remote/        # API Retrofit
│   │   │   ├── repository/    # Repository pattern
│   │   │   └── model/         # Modèles de données
│   │   ├── ui/                # Interface utilisateur
│   │   │   ├── screens/       # Écrans Compose
│   │   │   ├── components/    # Composants réutilisables
│   │   │   ├── theme/         # Thème Material 3
│   │   │   └── navigation/    # Navigation
│   │   ├── worker/            # WorkManager (synchro)
│   │   ├── widget/            # Widget écran d'accueil
│   │   └── util/              # Utilitaires
│   └── src/main/res/          # Ressources Android
└── build.gradle.kts           # Configuration Gradle
```

## ⚙️ Configuration

L'application utilise les paramètres suivants (modifiables dans `util/Constants.kt`) :

```kotlin
ESF_BASE_URL = "https://carnet-rouge-esf.app/"
ID_COM_SAISON = "63"           // Saison 2024-2025
NO_ECOLE = "356"               // École ESF
```

## 🔐 Sécurité

- **Credentials** : Stockés dans EncryptedSharedPreferences avec AES-256
- **Android Keystore** : Chiffrement matériel
- **Cookies de session** : Chiffrés localement
- **Base de données** : Exclue des backups Android

## 🔄 Synchronisation

La synchronisation s'effectue :
- **Automatiquement** : toutes les 15-30 min (configurable)
- **Heures** : 7h-20h par défaut (hors ligne la nuit)
- **Contraintes** : WiFi optionnel, batterie > 15%

## 📱 Compatibilité

- **Android minimum** : 8.0 (API 26)
- **Android cible** : 14.0 (API 34)

## 🚧 Statut

✅ **Fonctionnel** :
- Architecture MVVM complète
- Base de données Room
- Authentification ESF
- Synchronisation en arrière-plan
- Thème futuriste

⏳ **En développement** :
- Interface calendrier
- Intégration Google Calendar
- Écran paramètres
- Système de notifications

## 📝 TODO

- [ ] Implémenter l'écran du calendrier
- [ ] Ajouter l'intégration Google Calendar
- [ ] Créer l'écran des paramètres
- [ ] Finaliser le système de notifications
- [ ] Tests unitaires et d'intégration

## 📄 Licence

Ce projet est privé et destiné à un usage personnel.
