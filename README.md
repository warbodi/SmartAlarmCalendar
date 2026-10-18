# ⏰ Smart Alarm Calendar (ChronoAlarm)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android_8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.1-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue.svg)](https://developer.android.com/jetpack/compose)

**Smart Alarm Calendar** è un'applicazione Android moderna e privacy-first progettata per chi ha orari flessibili, turni di lavoro variabile, o routine quotidiane non convenzionali.

A differenza delle sveglie tradizionali basate sui giorni della settimana (Lunedì-Venerdì), **Smart Alarm Calendar** ti permette di creare **Set di Sveglie (Preset)** e di assegnarli a **qualsiasi data del calendario** con un solo tocco o tramite selezione multipla.

---

## ✨ Funzionalità Principali

- 📅 **Calendario Interattivo**:
  - Assegnazione rapida dei Preset a singole date o a blocchi multi-giorno.
  - Pre-caricamento istantaneo a 120 FPS dei mesi precedente e successivo.
  - Punti colorati sulle date per identificare al volo i turni ed i preset assegnati.

- ⚙️ **Set di Sveglie Personalizzabili (Preset)**:
  - Crea modelli con sveglie multiple (es. *Turno Mattina*, *Turno Pomeriggio*, *Giorno Libero*).
  - Colori personalizzabili ed opzioni di test immediato (+1 minuto).

- 📱 **Widget Schermata Home (Stile Proton Calendar)**:
  - Lista delle sveglie programmate per **Oggi** e **Domani** divise da intestazioni pulite.
  - **Switch laterali a levetta** per disattivare o riattivare le sveglie direttamente dal widget senza aprire l'app.
  - Supporto nativo al **Tema Chiaro e Tema Scuro** di sistema.

- 🔒 **Schermata Sveglia a Tutto Schermo su Schermo Bloccato**:
  - Funziona su schermo bloccato con accensione automatica dello schermo e supporto Xiaomi/MIUI.
  - Gesture **Swipe-Up** per spegnere la sveglia ed evitare spegnimenti accidentali.
  - Suono in **loop continuo** e vibrazione con frequenza d'allarme.

- 🔔 **Notifica Pre-Sveglia con Tasto Laterale**:
  - Notifica silenziosa 1 ora prima con pulsante **"Disattiva"** ad un tocco in vista compatta.

- 🌗 **Pannello Impostazioni Avanzato**:
  - Tema Chiaro, Tema Scuro e Predefinito di sistema.
  - Colore dello sfondo della schermata sveglia personalizzabile.
  - Messaggio d'allarme a schermo personalizzabile.
  - Scelta della suoneria e durata dello Snooze (5, 10, 15 min).

---

## 🛠️ Architettura e Tecnologia

L'applicazione è sviluppata seguendo le moderne Android Development Best Practices:

- **Linguaggio**: Kotlin 2.1
- **UI Framework**: Jetpack Compose con Material 3 Design
- **Database**: Room Database v2 (con KSP code generation)
- **Allarmi di Sistema**: `AlarmManager.setAlarmClock` + `PowerManager.WakeLock`
- **Widget**: `AppWidgetProvider` con `RemoteViews` adattivi
- **Iniezione & Concorrenza**: Kotlin Coroutines & `StateFlow`

---

## 📦 Compilazione ed Installazione (Build from Source)

### Prerequisiti
- Android Studio Ladybug (2024.2+) o Android Studio 2026.2+
- JDK 17 o superiore
- Android SDK 35 (Android 15)

### Comandi Gradle
```bash
# Clona il repository
git clone https://github.com/your-username/SmartAlarmCalendar.git
cd SmartAlarmCalendar

# Compila l'APK Debug
./gradlew app:assembleDebug

# Esegui i test unitari
./gradlew app:testDebugUnitTest
```

---

## 🌐 Pubblicazione su F-Droid

L'app è **100% Open Source**, priva di tracciamenti, analytics o SDK proprietari (no Google Play Services obbligatori).

### Metadata F-Droid (`fastlane/metadata/android/`)
Le informazioni per l'inclusione nel catalogo F-Droid sono posizionate secondo lo standard Fastlane:
- `title.txt`: Smart Alarm Calendar
- `short_description.txt`: Sveglia flessibile basata su calendario per turni ed orari dinamici.
- `full_description.txt`: Descrizione completa in formato testo.

---

## 📄 Licenza

Questo progetto è rilasciato sotto licenza **GNU General Public License v3.0 (GPL-3.0)**.
Vedi il file [LICENSE](LICENSE) per i dettagli.
