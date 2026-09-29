# UI-Vertrag TaskBoard

Basis-URL des Frontends: http://localhost:4200

Dieser Vertrag legt fest, welche Seiten es gibt, welche Elemente sie enthalten und woran automatisierte Tests sie erkennen. Jedes Element, mit dem ein Test arbeitet, hat ein festes `data-testid`. Texte in Anführungszeichen erscheinen exakt so auf der Seite.

Fehlermeldungen des Backends werden unverändert angezeigt, sie stammen aus dem [API-Vertrag](api-vertrag.md).

---

## Allgemeine Regeln

- Nicht angemeldete Besucher werden von `/tasks` sofort auf `/login` umgeleitet.
- Nach erfolgreicher Anmeldung liegt der Token im `localStorage` unter dem Schlüssel `token`.
- Die Adresse `/` leitet auf `/tasks` weiter.

## Hinweise für Tests

- Testdaten werden über die API angelegt, nicht über die Oberfläche: `POST /test/reset`, Registrierung, Login und Aufgaben über die Endpunkte aus dem API-Vertrag. Das ist schneller und macht Tests unabhängig von anderen Seiten.
- Für Tests, die nicht den Login selbst prüfen, wird der Token aus dem API-Login vor dem Seitenaufruf in den `localStorage` unter `token` geschrieben.
- Elemente werden ausschließlich über `data-testid` angesprochen, nicht über CSS-Klassen oder die Position auf der Seite.

---

## Seite: Registrierung (TB-1)

Adresse: `/register`

| data-testid | Element | Verhalten |
|---|---|---|
| `register-email` | Eingabefeld E-Mail | normales Textfeld, keine Prüfung durch den Browser |
| `register-password` | Eingabefeld Passwort | |
| `register-submit` | Button „Registrieren“ | deaktiviert, solange ein Feld leer ist |
| `register-error` | Fehlermeldung | erscheint nur bei einem Fehler, zeigt die Meldung des Backends |
| `link-login` | Link „Anmelden“ | führt zu `/login` |

Nach erfolgreicher Registrierung: Weiterleitung auf `/login?registered=1`.

## Seite: Login (TB-1, TB-2)

Adresse: `/login`

| data-testid | Element | Verhalten |
|---|---|---|
| `login-info` | Erfolgsmeldung | nur nach Registrierung sichtbar: „Registrierung erfolgreich. Bitte melde dich an.“ |
| `login-email` | Eingabefeld E-Mail | |
| `login-password` | Eingabefeld Passwort | |
| `login-submit` | Button „Anmelden“ | deaktiviert, solange ein Feld leer ist |
| `login-error` | Fehlermeldung | erscheint nur bei einem Fehler, zeigt die Meldung des Backends |
| `link-register` | Link „Registrieren“ | führt zu `/register` |

Nach erfolgreicher Anmeldung: Weiterleitung auf `/tasks`.

---

## Seite: Aufgabenliste (TB-3, TB-4, TB-5)

Adresse: `/tasks`, nur für angemeldete Nutzer.

### Kopfbereich

| data-testid | Element | Verhalten |
|---|---|---|
| `tasks-title` | Überschrift „Meine Aufgaben“ | |
| `logout` | Button „Abmelden“ | entfernt den Token und leitet auf `/login` weiter |

### Neue Aufgabe anlegen (TB-3)

| data-testid | Element | Verhalten |
|---|---|---|
| `new-task-title` | Eingabefeld Titel | |
| `new-task-submit` | Button „Hinzufügen“ | immer klickbar, auch bei leerem Feld, damit die Fehlermeldung des Backends erscheinen kann |
| `task-error` | Fehlermeldung | erscheint bei einem Fehler, zeigt die Meldung des Backends |

Nach erfolgreichem Anlegen erscheint die Aufgabe in der Liste, und das Eingabefeld wird geleert.

### Filter und Suche (TB-5)

| data-testid | Element | Verhalten |
|---|---|---|
| `filter-status` | Auswahlliste Status | Optionen mit den Werten `ALL`, `OPEN`, `IN_PROGRESS`, `DONE` und den Beschriftungen „Alle“, „Offen“, „In Arbeit“, „Erledigt“. Voreingestellt: „Alle“ |
| `search-input` | Suchfeld | die Liste wird bei jeder Eingabe neu geladen |
| `tasks-empty` | Hinweis „Keine Aufgaben gefunden“ | erscheint, wenn die Liste leer ist |

Filter und Suche wirken gemeinsam. Ein leeres Suchfeld bedeutet: keine Einschränkung nach Titel.

### Die Liste

Jede Aufgabe ist ein eigenes Element. Ein Test findet eine bestimmte Aufgabe, indem er unter allen `task-item` dasjenige sucht, dessen `task-title` den gesuchten Titel enthält.

| data-testid | Element | Verhalten |
|---|---|---|
| `task-item` | eine Aufgabe in der Liste | Container für die folgenden Elemente |
| `task-title` | Titel der Aufgabe | wird vollständig und ungekürzt angezeigt |
| `task-status` | Auswahlliste Status | Werte `OPEN`, `IN_PROGRESS`, `DONE`, Beschriftungen „Offen“, „In Arbeit“, „Erledigt“. Eine Änderung wird sofort gespeichert |
| `task-edit` | Button „Bearbeiten“ | schaltet die Aufgabe in den Bearbeitungsmodus |
| `task-delete` | Button „Löschen“ | öffnet die Sicherheitsabfrage |

### Bearbeitungsmodus (TB-4)

Im Bearbeitungsmodus ersetzt ein Eingabefeld den Titel.

| data-testid | Element | Verhalten |
|---|---|---|
| `task-edit-input` | Eingabefeld Titel | enthält den bisherigen Titel |
| `task-save` | Button „Speichern“ | speichert den neuen Titel; bei einem Fehler erscheint `task-error`, und der alte Titel bleibt erhalten |
| `task-cancel-edit` | Button „Abbrechen“ | beendet den Bearbeitungsmodus ohne Änderung |
|Es ist immer höchstens eine Aufgabe gleichzeitig im Bearbeitungsmodus.
### Sicherheitsabfrage beim Löschen (TB-4)

| data-testid | Element | Verhalten |
|---|---|---|
| `delete-dialog` | Dialog „Aufgabe wirklich löschen?“ | erscheint nach Klick auf `task-delete` |
| `delete-confirm` | Button „Ja, löschen“ | löscht die Aufgabe und schließt den Dialog |
| `delete-cancel` | Button „Abbrechen“ | schließt den Dialog, die Aufgabe bleibt erhalten |