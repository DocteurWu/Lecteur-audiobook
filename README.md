<div align="center">

<img src="res/mipmap-xxxhdpi/ic_launcher.png" alt="Icône" width="96" height="96">

# Lecteur audiobook

**Lecteur d'audiobooks et de podcasts par chapitres pour Android.**
Un APK de 100 Ko, hors-ligne, sans compte, sans publicité, sans pistage.

[![Licence MIT](https://img.shields.io/badge/Licence-MIT-8E7CFF?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-2ED3B7?style=flat-square)](https://developer.android.com/)
[![Taille](https://img.shields.io/badge/APK-~100%20Ko-FF6B6B?style=flat-square)](#téléchargement)
[![Release](https://img.shields.io/badge/release-v1.0-8E7CFF?style=flat-square)](../../releases)

</div>

---

## 📖 À propos

Vos audiobooks sont rangés en **plusieurs fichiers** : un par chapitre.
`28 - 10 - AGE D'OR.mp3`, `28 - 11 - LE NOUVEL AGE.mp3`… Chaque application de podcast
les affiche comme un flux d'épisodes radio, avec une pochette par livre, et vous
perdez le fil de l'histoire.

**Lecteur audiobook** fait l'inverse : **un dossier = un livre = une suite de chapitres.**
L'application analyse vos dossiers, reconstitue l'ordre des chapitres, nettoie les
noms de fichiers pour en faire des titres lisibles, et vous laisse écouter
n'importe où — en voiture, en marchant, les yeux fermés.

> 🎧 *Le bon ouvrage, le bon chapitre, la bonne position — même les yeux fermés.*
>
> Le bon livre, le bon chapitre, le bon endroit : tout est là, il n'y a plus qu'à écouter.

### ✨ Ce que ça change

| | Avant (lecteur de podcast) | Lecteur audiobook |
|---|---|---|
| Organisation | Flux d'épisodes | **Livre = dossier**, chapitres = fichiers |
| Titre | `28 - 10 - AGE D'OR` | **`Age d'Or`** |
| Pochette | Par épisode | **Par livre** (lue dans le 1<sup>er</sup> fichier) |
| Navigation | Épisode précédent / suivant | **Chapitre précédent / suivant**, saut de 15 s |
| Reprise | Rare | **Automatique**, à la seconde près |
| Fin d'un chapitre | Passage au suivant | **Pause ou enchaînement** au choix |

---

## 🎯 Fonctionnalités

### Bibliothèque

- **Analyse automatique** des dossiers `Podcasts`, `Podcast`, `Music/Podcasts`, `Download/Podcasts` et `Audiobooks` du stockage interne.
- **Ajout de n'importe quel dossier** via le sélecteur de documents Android (SAF) : carte SD, stockage interne, clé USB OTG…
- **Balayage MediaStore** en complément, pour les fichiers hors des emplacements standards.
- **Regroupement intelligent** : un dossier = un livre, les fichiers triés dans l'**ordre naturel** (`2` avant `10`, pas l'alphabétique).
- **Recherche insensible aux accents et à la casse** sur les livres *et* les chapitres.
- **Durée de chaque chapitre** lue directement dans l'en-tête MP3 (`Mp3Info`) — 71 fichiers en quelques millisecondes, sans `MediaMetadataRetriever`.
- **Pochettes** extraites des tags ID3 et mises en cache mémoire (`ArtCache`).

### Titres intelligents

`Names` transforme un nom de fichier brut en titre de livre lisible :

```
28 - 10 - AGE D'OR.mp3                      →  Age d'Or
01 - Chapitre 1 - Le nouvel ordre.mp3       →  Chapitre 1 : Le Nouvel Ordre
L_ANTHROPOCENE - 12 - ELEMENTS.mp3           →  L'Anthropocène : Éléments
S_IL VOUS PLAIT_Y_A_T_IL.mp3                 →  S'il vous plaît, y a-t-il
```

- **Élisions élaguées** : `L_ANTHROPOCENE` → `L'Anthropocène`.
- **Accents restaurés** à partir d'un dictionnaire français intégré (`AGE` → `ÂGE`, `SYSTEME` → `SYSTÈME`).
- **Title Case intelligent** : les mots courts restent en minuscules, sauf après `:`.

### Lecture

- **Service de premier plan** : la lecture continue écran verrouillé, dans le tiroir de notifications, et avec le casque branché.
- **Notification média** au format Android 12, avec la pochette et les actions précédent / lecture / suivant.
- **`MediaSession`** complète : l'application s'intègre au contrôleur système, à Android Auto, aux assistants, à Wear OS.
- **Reprise de position** automatique, sauvegardée toutes les 2 secondes.
- **Vitesse de lecture** de 0,75× à 2×.
- **Minuterie de sommeil** : 15 / 30 / 45 / 60 minutes, ou **arrêt à la fin du chapitre**.
- **Saut de 15 s** en avant et en arrière.
- **Gestion du focus audio** : la lecture se met en pause quand un autre son démarre, et reprend toute seule ensuite.
- **Détection du casque débranché** (`AUDIO_BECOMING_NOISY`) : la lecture s'arrête.
- **Wake lock** partiel : le CPU ne se met pas en veille pendant la lecture.

### Ergonomie

- **Thème sombre** natif, lisible en voiture la nuit.
- **Carte « Reprendre la lecture »** en haut de la bibliothèque, avec la progression exacte.
- **Barre de progression** sur chaque chapitre déjà entamé.
- **Marquage « écouté »** : appui long sur un chapitre.
- **Tri** des chapitres par ordre du livre, par titre ou par durée.
- **Lecture enchaînée** : un livre entier se lance en un appui.

---

## 📥 Téléchargement

La version stable est publiée dans les **Releases** du dépôt.

👉 **[Télécharger la dernière version](../../releases/latest)**

| Fichier | Détails |
|---|---|
| `Chapitres-v1.0.apk` | APK signé, prêt à installer |

### Installation

1. Télécharger le fichier `.apk`.
2. L'ouvrir sur le téléphone.
3. Autoriser « Installer des applications inconnues » pour l'application de fichiers utilisée.
4. Lancer **Lecteur audiobook**, puis **Ajouter un dossier** pour choisir où sont vos livres.

> **Android 7.0 (Nougat) ou plus récent** · environ **100 Ko** · aucune donnée envoyée, tout reste sur l'appareil.

---

## 🛠️ Compiler

Le projet est en **Java pur**, sans Gradle, sans dépendance : un simple script suffit.

### Prérequis

- **Windows** + **PowerShell** (le script est en `.ps1`)
- **JDK 8** (pour `javac`) et **JDK 11+** (pour `d8` et `apksigner`)
- **Android SDK** : `build-tools 34.0.0` et `platforms/android-34`

### Lancer le build

```powershell
.\build.ps1
```

Le script enchaîne les 7 étapes :

| # | Étape | Outil |
|---|---|---|
| 1 | Compilation des ressources | `aapt2 compile` |
| 2 | Linkage du manifeste + génération de `R.java` | `aapt2 link` |
| 3 | Compilation Java | `javac` |
| 4 | JAR puis DEX | `jar` + `d8` |
| 5 | Assemblage de l'APK | `aapt add` |
| 6 | Alignement | `zipalign` |
| 7 | Signature | `apksigner` |

L'APK signé est écrit dans `Chapitres.apk` à la racine du projet.

> 💡 Le script travaille dans `C:\chapitres-build` via une jonction vers le SDK :
> les outils Android natifs gèrent mal les chemins contenant des caractères non-ASCII.

---

## 📁 Structure du projet

```
.
├── AndroidManifest.xml          # Manifeste : activités, service, permissions
├── build.ps1                    # Script de build (7 étapes)
├── res/                         # Ressources Android
│   ├── drawable/                # Icônes vectorielles & fonds
│   ├── layout/                  # Écrans : bibliothèque, chapitres, lecteur
│   ├── mipmap-*/                # Icône de lancement (PNG multi-densités)
│   └── values/                  # Couleurs, styles, chaînes
├── src/com/chapitres/podcast/
│   ├── MainActivity.java        # Bibliothèque, recherche, carte de reprise
│   ├── EpisodesActivity.java    # Liste des chapitres d'un livre
│   ├── PlayerActivity.java      # Écran « En cours »
│   ├── PlaybackService.java     # Service de lecture (avant-plan, notification, minuterie)
│   ├── MediaButtonReceiver.java # Boutons média du casque
│   ├── Library.java             # Analyse des dossiers, MediaStore, SAF
│   ├── Mp3Info.java             # Lecture de durée MP3 (CBR / VBR / ID3)
│   ├── Names.java               # Titres lisibles, accents, tri naturel
│   ├── AudioItem.java           # Un chapitre
│   ├── Podcast.java             # Un livre
│   ├── ArtCache.java            # Cache mémoire des pochettes
│   └── Prefs.java               # Positions, vitesse, dossiers ajoutés
└── tools/                       # Outils de génération (icônes, drawables)
```

---

## 🧱 Architecture

```
┌──────────────────────────────────────────────┐
│  MainActivity      EpisodesActivity          │  UI (XML + Activity simple)
│  PlayerActivity                                │
└───────────────┬──────────────────────────────┘
                │  PlaybackService.play(...)
                ▼
┌──────────────────────────────────────────────┐
│  PlaybackService   (foreground, MediaSession)│  Moteur de lecture
│  Notification · AudioFocus · Minuterie        │
└───────────────┬──────────────────────────────┘
                │
        ┌───────┴────────┐
        ▼                ▼
┌──────────────┐  ┌──────────────┐
│ MediaPlayer  │  │    Prefs     │  Positions, vitesse, arbres SAF
└──────────────┘  └──────────────┘

┌──────────────────────────────────────────────┐
│  Library ── scan ──► dossiers / MediaStore / SAF │
│  Mp3Info / ArtCache / Names                    │  Analyse & métadonnées
└──────────────────────────────────────────────┘
```

**Choix techniques :**

- **Aucun framework externe** — `Activity`, `ListView`, `MediaPlayer` : le code est lisible de bout en bout.
- **Zéro réseau** — l'application ne demande aucune permission `INTERNET`.
- **Permissions minimales** : `READ_EXTERNAL_STORAGE`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `POST_NOTIFICATIONS`.
- **Service non lié** (`START_NOT_STICKY`) : le système peut arrêter la lecture si l'app est swappée, la position reste sauvegardée.

---

## ❓ FAQ

**L'application ne trouve pas mes livres.**
Ajoutez explicitement le dossier via **Ajouter un dossier** : le scan automatique ne couvre que les emplacements standards.

**Les chapitres sont dans le désordre.**
L'ordre de tri utilise le **premier nombre** du nom de fichier. Renommez vos fichiers en `01 - …`, `02 - …` pour un ordre explicite.

**Comment j'ajoute une pochette ?**
Elle est lue dans les tags ID3 du **premier fichier** du dossier, puis appliquée à tout le livre.

**Est-ce que ça consomme de la batterie ?**
En lecture seule : pas de GPS, pas de réseau, pas de wake lock permanent (un wake lock partiel uniquement pendant la lecture).

---

## 📄 Licence

Code source sous licence **MIT** — voir [LICENSE](LICENSE).

Les fichiers audio de l'utilisateur ne sont jamais lus ni transmis par l'application :
tout est traité localement, hors ligne.
