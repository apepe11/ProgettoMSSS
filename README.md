# HeartMusic 🎵💓

**HeartMusic** è un'applicazione Android innovativa che unisce il mondo della musica al benessere psicofisico. L'obiettivo del progetto è monitorare lo stato emotivo dell'utente attraverso i parametri biometrici rilevati da uno smartwatch e offrire un'esperienza d'ascolto personalizzata che possa influenzare positivamente l'umore.

## 🚀 L'Idea
La musica ha un impatto profondo sulle nostre emozioni. HeartMusic punta a creare un "ponte" tecnologico tra il battito cardiaco e le playlist musicali. Attraverso l'analisi dei dati biometrici (BPM e EDA), l'app è in grado di:
1.  **Analizzare** lo stato emotivo attuale dell'utente.
2.  **Suggerire** playlist mirate per migliorare l'umore o accompagnare un momento di relax.
3.  **Monitorare** l'andamento dei sentimenti nel tempo attraverso grafici e insight personalizzati.

## 👥 Il Team (MSSS Project)
Il progetto è sviluppato con passione da:
*   **Antonio**
*   **Alessandro**
*   **Luca**
*   **Zahra**

---

## 🛠️ Architettura Tecnica
Il progetto segue un approccio **Full-Stack** moderno, separando nettamente le responsabilità tra client e server per garantire scalabilità e robustezza.

### 📱 Mobile (Android)
*   **UI**: Sviluppata interamente con **Jetpack Compose** per una grafica moderna e reattiva.
*   **Architettura**: Segue il pattern **MVVM** (Model-View-ViewModel).
*   **Networking**: Utilizzo di **Retrofit** e **OkHttp** per la comunicazione sicura con il backend.
*   **Persistenza Dati**: Implementazione di **Jetpack DataStore** per mantenere la sessione utente attiva anche dopo la chiusura dell'app.
*   **Linguaggio**: Kotlin.

### ⚙️ Backend (Python & Docker)
*   **Framework**: **Flask** per la gestione delle API REST.
*   **Database**: **PostgreSQL**, un database relazionale robusto per la gestione di utenti, sessioni e playlist.
*   **Containerizzazione**: L'intero backend è pacchettizzato tramite **Docker** e **Docker Compose**, permettendo un'installazione rapida e identica su ogni macchina (Database + Server).
*   **Sicurezza**: Criptazione delle password tramite `werkzeug.security` e gestione delle sessioni.
*   **Automazione**: Sistema di invio email automatico per la conferma della registrazione.

---

## 🛠️ Come avviare il progetto

### Prerequisiti
*   Android Studio (ultima versione)
*   Docker Desktop installato e attivo

### 1. Avviare il Backend
Apri il terminale nella cartella `/backend` e digita:
```bash
docker compose up --build
```
Il server sarà attivo all'indirizzo `http://localhost:5001`.

### 2. Avviare l'App Android
*   Apri la cartella principale del progetto con Android Studio.
*   Assicurati che l'emulatore sia attivo.
*   Premi **Run**. L'app comunicherà automaticamente con il server tramite l'indirizzo speciale `10.0.2.2:5001`.

---

## 📈 Roadmap & Sviluppi Futuri
- [x] Sistema di Login e Registrazione robusto.
- [x] Persistenza della sessione utente.
- [x] Integrazione Docker + PostgreSQL.
- [ ] Implementazione del lettore musicale integrato.
- [ ] Algoritmo di analisi delle emozioni basato sui dati dello smartwatch.
- [ ] Caricamento foto profilo personalizzata.

---
*Progetto realizzato per il corso di MSSS - 2024/2025*
