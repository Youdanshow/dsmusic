# dsmusic
Fonctionnalités de l’application
1. Scan des fichiers locaux
Recherche automatique des fichiers .mp3, .flac, .wav, etc.

Affichage par :

Artiste

Album

Playlist

Tous les titres

2. Lecteur de musique
Play / Pause

Suivant / Précédent

Mode Aléatoire (Shuffle)

Mode Répétition (Titre / Playlist)

Barre de progression

Affichage des métadonnées (titre, artiste, pochette)

3. Égaliseur intégré
Présets : Classique, Rock, Pop, Jazz, etc.

Réglage manuel (5 bandes ou plus)

Contrôle du bass boost et du virtualizer

4. Playlists personnalisées
Création / suppression de playlists

Ajout / retrait de musiques aux playlists

5. Interface utilisateur
Design simple, fluide et responsive

Thème sombre / clair

Affichage dynamique de la pochette de l’album

🛠️ Technologies et bibliothèques recommandées
Langage & SDK
Kotlin (ou Java, mais Kotlin est plus moderne)

Android SDK (API ≥ 24)

Lecteur audio
MediaPlayer ou ExoPlayer (plus performant et flexible)

Scan des fichiers
MediaStore API pour récupérer les musiques locales

Égaliseur audio
android.media.audiofx.Equalizer

BassBoost et Virtualizer pour les effets

Pochette d'album & métadonnées
MediaMetadataRetriever

UI/UX
Jetpack Compose ou XML

Material Components for Android

🗂️ Structure de l’application
bash
Copy
Edit
/musicplayer
├── data/
│   └── MusicRepository.kt
├── ui/
│   ├── MainActivity.kt
│   ├── PlayerFragment.kt
│   ├── PlaylistFragment.kt
│   └── EqualizerFragment.kt
├── model/
│   └── Song.kt
├── utils/
│   └── MusicScanner.kt
└── service/
    └── MusicService.kt (service de fond pour la lecture)
✅ Étapes de développement
Scanner la bibliothèque locale avec MediaStore.

Afficher les chansons dans une liste.

Créer un service de fond pour la lecture audio.

Créer une UI de lecture avec les contrôles de base.

Ajouter l’égaliseur audio.

Gérer les playlists et les favoris.

Ajouter la persistance (SharedPreferences ou Room).

Polir l’interface avec Jetpack Compose ou XML.

mode dev sur téléphone - debugger usb activer auto block desactivé

& "C:\Users\damie\OneDrive\Bureau\gradle-8.7-bin\gradle-8.7\bin\gradle.bat" wrapper --gradle-version 8.7

./gradlew clean build

& "C:\Users\damie\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.dsmusic/.MainActivity

& "C:\Users\damie\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices

./gradlew installDebug
 
