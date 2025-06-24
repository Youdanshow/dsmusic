# dsmusic

## Fonctionnalités de l'application
- **Scan des fichiers locaux**
  - Recherche automatique des fichiers `.mp3`, `.flac`, `.wav`, etc.
  - Affichage par artiste, album, playlist ou tous les titres.
- **Lecteur de musique**
  - Play / Pause
  - Suivant / Précédent
  - Mode aléatoire (Shuffle)
  - Mode répétition (titre ou playlist)
  - Barre de progression
  - Notification avec contrôle (précédent, pause/lecture, suivant)
  - Affichage de la durée et de la progression dans la notification
  - Affichage des métadonnées (titre, artiste, pochette)
- **Playlists personnalisées**
  - Création et suppression de playlists
  - Ajout ou retrait de musiques aux playlists
- **Recherche de musiques**
  - Bouton de recherche dans le menu principal pour filtrer la liste
  - Recherche sans accent et insensible à la ponctuation
- **Interface utilisateur**
  - Design simple, fluide et responsive
  - Thème sombre ou clair
  - Affichage dynamique de la pochette de l’album

## Technologies et bibliothèques recommandées
- **Langage & SDK**
  - Kotlin (ou Java)
  - Android SDK (API ≥ 24)
- **Lecteur audio** : MediaPlayer ou ExoPlayer
- **Scan des fichiers** : MediaStore API
- **Métadonnées** : `MediaMetadataRetriever`
- **UI/UX**
  - Jetpack Compose ou XML
  - Material Components for Android

## Structure de l’application
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
7. Polir l’interface avec Jetpack Compose ou XML.

## Setup
1. Mode développeur sur téléphone : activer le débogage USB et désactiver le blocage automatique.
2. Utiliser `gradle` version 8.7 :
   ```bash
   & "<path-to-gradle>"/gradle.bat wrapper --gradle-version 8.7
   ```
3. Construire le projet :
   ```bash
   ./gradlew clean build
   ```
4. Lancer l’application sur l’appareil :
   ```bash
   & "<path-to-adb>"/adb shell am start -n com.example.dsmusic/.MainActivity
   ```
5. Vérifier les appareils connectés puis installer l’APK :
   ```bash
   & "<path-to-adb>"/adb devices
   ./gradlew installDebug
   ```
6. En cas de problème de connexion USB :
   ```bash
   & "<path-to-adb>"/adb kill-server
   & "<path-to-adb>"/adb start-server
   & "<path-to-adb>"/adb devices
   ./gradlew installDebug
   ```
6. Tester une branche du projet
    ```bash
    git fetch origin <branch-name>
    git switch <branch-name>
   ./gradlew clean build
   ./gradlew installDebug
   ```
