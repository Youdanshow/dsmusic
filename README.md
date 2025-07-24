# dsmusic

dsmusic est un lecteur audio Android écrit en Kotlin. L'application se concentre sur la lecture de fichiers locaux et la gestion de playlists.

## Fonctionnalités clés
- **Exploration de la bibliothèque**
  - Scan automatique des fichiers `.mp3`, `.flac`, `.wav`…
  - Classement par artiste, album ou playlist
  - Tri des titres (nom, album, artiste, durée ou taille)
  - Mode de sélection multiple pour ajouter plusieurs morceaux à une playlist
- **Lecteur intégré**
  - Play/Pause, Suivant/Précédent
  - Modes aléatoire et répétition
  - Barre de progression et contrôle depuis la notification
  - Affichage des métadonnées et de la pochette de l'album
- **Playlists**
  - Création, renommage et suppression de playlists
  - Ajout ou retrait de plusieurs titres à la fois
  - Détection des doublons
- **Recherche avancée**
  - Champ de recherche sans accent et insensible à la ponctuation
  - Filtre par album ou artiste
- **Interface**
  - Design responsive basé sur Jetpack Compose
  - Thème clair ou sombre sélectionnable depuis les paramètres
  - Image de fond dynamique avec effet de flou

## Technologies utilisées
- Kotlin & Android SDK (API ≥ 24)
- ExoPlayer pour la lecture audio
- MediaStore API pour l'indexation des fichiers
- Jetpack Compose et Material3 pour l'interface

## Structure du projet
```bash
/musicplayer
├── data/
│   └── MusicRepository.kt
├── ui/
│   ├── MainActivity.kt
│   ├── PlayerFragment.kt
│   ├── PlaylistFragment.kt
├── model/
│   └── Song.kt
├── utils/
│   └── MusicScanner.kt
└── service/
    └── MusicService.kt  # service de fond pour la lecture
```

## Étapes de développement
1. Scanner la bibliothèque locale avec MediaStore.
2. Afficher les chansons dans une liste.
3. Créer un service de fond pour la lecture audio.
4. Créer une UI de lecture avec les contrôles de base.
5. Gérer les playlists et les favoris.
6. Ajouter la persistance (SharedPreferences ou Room).
7. Polir l'interface avec Jetpack Compose ou XML.

## Setup
1. Activer le mode développeur sur le téléphone et le débogage USB.
2. Le projet utilise Gradle **8.14** (wrapper fourni) :
   ```bash
   ./gradlew --version
   ```
3. Construire le projet :
   ```bash
   ./gradlew clean build
   ```
4. Lancer l'application sur l'appareil :
   ```bash
   <path-to-adb>/adb shell am start -n com.example.dsmusic/.MainActivity
   ```
5. Vérifier les appareils connectés puis installer l'APK :
   ```bash
   <path-to-adb>/adb devices
   ./gradlew installDebug
   ```
6. En cas de problème de connexion USB :
   ```bash
   & <path-to-adb>/adb kill-server
   & <path-to-adb>/adb start-server
   & <path-to-adb>/adb devices
   ./gradlew installDebug
   ```
6. Tester une branche du projet
    ```bash
    git fetch origin <branch-name>
    git switch <branch-name>
    ./gradlew clean build
    ./gradlew installDebug
    ```
