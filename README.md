# Radio Teate On Air

---

## Indice

1.  [Introduzione](#1-introduzione)
2.  [Caratteristiche Principali](#2-caratteristiche-principali)
3.  [Installazione e Uso](#3-installazione-e-uso)
4.  [Tecnologie Utilizzate](#4-tecnologie-utilizzate)
5.  [Architettura](#5-architettura)
6.  [Licenza](#6-licenza)
7.  [Contatti](#7-contatti)

---

## 1. Introduzione

**Radio Teate On Air** è l'applicazione ufficiale ed open-source sviluppata per permettere a tutti gli ascoltatori di seguire il nostro palinsesto in streaming. Il progetto è nato per offrire un'esperienza di ascolto fluida, affidabile e con un'interfaccia utente semplice e intuitiva.

---

## 2. Caratteristiche Principali

* **Streaming Affidabile:** Supporto per flussi audio **[MP3/AAC/HLS]** a bassa latenza e con riconnessione automatica in caso di interruzione.
* **Metadati in Tempo Reale:** Visualizzazione immediata del titolo della canzone, dell'artista o del nome del programma in onda.

---

## 3. Installazione e Uso

### 3.1 Per l'Utente (App Pubblicata)

L'app è disponibile sui seguenti store ufficiali:

| Piattaforma | Link per il Download |
| :--- | :--- |
| Google Play | https://play.google.com/store/apps/details?id=it.teateonair.app |

### 3.2 Per lo Sviluppatore (Build da Sorgente)

Per clonare, configurare e avviare l'applicazione in locale, segui questi passaggi:

1.  **Clonazione:** Clona il repository sulla tua macchina locale:
    ```bash
    git clone https://github.com/lorenzus-jpeg/radioteateonair-app-android.git
    ```
2.  **Navigazione:** Entra nella cartella del progetto:
    ```bash
    cd [IlTuoRepo]
    ```
3.  **Configurazione:** Apri il progetto in Android Studio. Assicurati che il tuo ambiente rispetti i requisiti di SDK e Gradle.
4.  **Esecuzione:** Sincronizza Gradle e avvia l'app su un emulatore o un dispositivo fisico.

---

## 4. Tecnologie Utilizzate

AndroidX Libraries:
- androidx.appcompat:appcompat
- androidx.core:core-ktx
- androidx.constraintlayout:constraintlayout
- androidx.media:media
- androidx.localbroadcastmanager:localbroadcastmanager

Third-Party:
- org.jsoup:jsoup

Android Framework:
- android.animation.ValueAnimator
- android.media.MediaPlayer
- android.media.AudioAttributes
- android.app.Service
- android.app.NotificationManager
- android.app.NotificationChannel
- android.app.Notification
- android.webkit.WebView
- android.webkit.WebViewClient
- android.webkit.WebSettings
- android.graphics.Canvas
- android.graphics.Path
- android.graphics.Paint
- android.graphics.LinearGradient
- android.graphics.Color
- android.graphics.drawable.GradientDrawable
- android.graphics.drawable.RippleDrawable
- android.os.Handler
- android.os.Looper
- android.os.Build
- android.content.Intent
- android.content.BroadcastReceiver
- android.content.IntentFilter
- android.widget (Button, TextView, LinearLayout, ScrollView, ImageView, Toast, ProgressBar)
- android.text.SpannableStringBuilder
- android.text.style.ForegroundColorSpan
- android.text.style.StyleSpan
- org.json.JSONObject
- java.net.URL
- java.util.concurrent.Executors

OS REQUIREMENTS:
- Minimum: Android 5.0 (API 21)
- Target: Android 13 (API 33+)
- Permissions: INTERNET, FOREGROUND_SERVICE, POST_NOTIFICATIONS

IDE REQUIREMENTS:
- Android Studio Arctic Fox 2020.3.1+ (minimum)
- Android Studio Hedgehog 2023.1.1+ (recommended)
- Gradle
- Kotlin Plugin

---

## 5. Architettura

- MVC/MVP Pattern
- MainActivity (UI Controller)
- RadioService (Foreground Service)
- AnimatedBackgroundView (Custom View - 10 waves)
- PlayerAnimatedBackgroundView (Custom View - 6 waves)


---

---

## 6. Licenza

Questo progetto è distribuito sotto la licenza **GNU General Public License (GPL) v3**.

Questa licenza garantisce che il software rimanga libero, assicurando che qualsiasi lavoro derivato o modificato venga anch'esso rilasciato come open-source (Strong Copyleft).

Per i dettagli completi relativi ai diritti di utilizzo, modifica e distribuzione, consulta il file **[LICENSE](LICENSE)** incluso nel repository.

---

## 7. Contatti

Per collaborare al progetto scrivere una mail con oggetto "RICHIESTA COLLABORAZIONE APP ANDROID" all'indirizzo mail: _info@radioteateonair.it_
