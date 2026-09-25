# API-Vertrag TaskBoard

Basis-URL: http://localhost:8080
Alle Anfragen und Antworten sind JSON.
Fehlerantworten haben immer die Form: { "message": "..." }

---

## Bereich: Konto (TB-1, TB-2)

### POST /api/auth/register  (TB-1)

Anfrage: { "email": "nidal@test.de", "password": "geheim123" }

| Status | Wann | Antwort |
|---|---|---|
| 201 | Registrierung erfolgreich | { "id": 1, "email": "nidal@test.de" } |
| 400 | E-Mail ungültig | "Bitte gib eine gültige E-Mail-Adresse ein" |
| 400 | Passwort kürzer als 8 Zeichen | "Passwort muss mindestens 8 Zeichen lang sein" |
| 400 | E-Mail oder Passwort fehlt | "E-Mail und Passwort sind Pflichtfelder" |
| 409 | E-Mail bereits registriert | "Diese E-Mail ist bereits registriert" |

Regel: Keine Antwort enthält jemals das Passwort.

### POST /api/auth/login  (TB-2)

Anfrage: { "email": "nidal@test.de", "password": "geheim123" }

| Status | Wann | Antwort |
|---|---|---|
| 200 | Login erfolgreich | { "token": "..." } |
| 401 | Passwort falsch oder E-Mail unbekannt | "E-Mail oder Passwort ist falsch" |

Regel: Bei falschem Passwort und unbekannter E-Mail kommt exakt dieselbe Antwort.

---

## Bereich: Aufgaben (TB-3, TB-4, TB-5)

Alle Anfragen brauchen den Header: Authorization: Bearer <token>
Ohne gültigen Token: 401 mit "Bitte melde dich an"

Eine Aufgabe sieht so aus:
{ "id": 1, "title": "Einkaufen", "status": "OPEN" }

Mögliche Status: OPEN, IN_PROGRESS, DONE

### POST /api/tasks  (TB-3)

Anfrage: { "title": "Einkaufen" }

| Status | Wann | Antwort |
|---|---|---|
| 201 | Aufgabe angelegt | die neue Aufgabe, Status immer OPEN |
| 400 | Titel leer oder nur Leerzeichen | "Titel darf nicht leer sein" |
| 400 | Titel länger als 100 Zeichen | "Titel darf höchstens 100 Zeichen lang sein" |

### PUT /api/tasks/{id}  (TB-4)

Anfrage: { "title": "Wocheneinkauf" }

| Status | Wann | Antwort |
|---|---|---|
| 200 | Titel geändert | die geänderte Aufgabe, Status unverändert |
| 400 | Titel ungültig | dieselben Meldungen wie bei POST /api/tasks |
| 404 | Aufgabe existiert nicht oder gehört einem anderen Nutzer | "Aufgabe nicht gefunden" |

### PATCH /api/tasks/{id}/status  (TB-4)

Anfrage: { "status": "IN_PROGRESS" }

| Status | Wann | Antwort |
|---|---|---|
| 200 | Status geändert | die geänderte Aufgabe |
| 400 | Unbekannter Status | "Ungültiger Status" |
| 404 | Aufgabe existiert nicht oder gehört einem anderen Nutzer | "Aufgabe nicht gefunden" |

### DELETE /api/tasks/{id}  (TB-4)

| Status | Wann | Antwort |
|---|---|---|
| 204 | Aufgabe gelöscht | keine |
| 404 | Aufgabe existiert nicht oder gehört einem anderen Nutzer | "Aufgabe nicht gefunden" |

### GET /api/tasks  (TB-3, TB-5)

Optionale Parameter:
- status: OPEN, IN_PROGRESS oder DONE
- search: Teil des Titels, Groß- und Kleinschreibung egal

Beispiel: GET /api/tasks?status=IN_PROGRESS&search=einkauf

| Status | Wann | Antwort |
|---|---|---|
| 200 | immer, wenn angemeldet | Liste der eigenen Aufgaben, bei keinem Treffer eine leere Liste [] |

Regel: Ein Nutzer bekommt nie Aufgaben anderer Nutzer zu sehen.

---

## Bereich: Testhilfe

### POST /test/reset

Nur im Test-Profil aktiv. Löscht alle Nutzer und Aufgaben.
Antwort: 204