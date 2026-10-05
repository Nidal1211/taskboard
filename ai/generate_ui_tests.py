import re
import sys
from pathlib import Path

import anthropic
from dotenv import load_dotenv

from fetch_story import get_story

load_dotenv()

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
UI_DIR = ROOT / "e2e-tests" / "src" / "test" / "java" / "de" / "nidal" / "taskboard" / "ui"
MODEL = "claude-sonnet-5"

FILE_MARKER = re.compile(r"^=== FILE: (.+?) ===\s*$", re.MULTILINE)
ALLOWED_PATH = re.compile(r"^(pages/)?[A-Z][A-Za-z0-9]*\.java$")

SYSTEM_PROMPT = """Du bist ein erfahrener Testautomatisierer fuer End-to-End-Tests mit Playwright fuer Java.
Du bekommst eine User Story mit Szenarien im Format Gegeben/Wenn/Dann, einen UI-Vertrag, einen API-Vertrag und die vorhandenen Testdateien als Vorlage.
Schreibe daraus automatisierte UI-Tests.

Aufbau:
1. Schreibe genau eine Testklasse {class_name} im Package de.nidal.taskboard.ui. Sie erbt von UiTestBase und bekommt die Annotation @Tag("{story_key}").
2. Page Objects liegen im Package de.nidal.taskboard.ui.pages und folgen dem Muster von LoginPage: eine Methode pro Element mit page.getByTestId(...) und Aktionsmethoden fuer wiederkehrende Ablaeufe.
3. Ein neues Page Object deckt alle Elemente seiner Seite aus dem UI-Vertrag ab, nicht nur die fuer diese Story benoetigten.
4. Gib vorhandene Dateien nicht erneut aus. Nutze vorhandene Page Objects und die Methoden von UiTestBase.

Testdaten und Anmeldung:
5. Lege Testdaten ausschliesslich ueber die API an, mit register und login aus UiTestBase. Aufgaben und Statuswechsel legst du mit REST Assured ueber die Endpunkte aus dem API-Vertrag an, in privaten Hilfsmethoden der Testklasse.
6. Tests, die nicht den Login selbst pruefen, melden sich mit signInWithToken(login(...)) an, bevor sie eine Seite aufrufen.

Tests:
7. Jedes Szenario, das die Oberflaeche betrifft, wird mindestens ein Test. Jeder Test bekommt @DisplayName mit dem Szenarionamen aus der Story.
8. Szenarien, die sich nur ueber die API pruefen lassen, testest du nicht. Nenne sie in einem Kommentar am Anfang der Klasse.
9. Verwende ausschliesslich data-testid, Texte und Adressen aus dem UI-Vertrag. Erfinde nichts. Fehlt eine Angabe, schreibe einen TODO-Kommentar, statt zu raten.
10. Verwende die Assertions von Playwright (assertThat), die automatisch warten. Verwende niemals Thread.sleep oder waitForTimeout.
11. Verwende die konkreten Beispielwerte aus der Story exakt.
12. Pruefe jede Und-Zeile. Dass etwas nicht angelegt oder nicht veraendert wurde, beweist du ueber die Oberflaeche, zum Beispiel ueber die Anzahl der task-item-Elemente. Bei nach dem Neuladen der Seite verwendest du page.reload().
13. Jede Pruefung muss fehlschlagen koennen, wenn die App sich falsch verhaelt. Pruefe insbesondere eine leere oder unveraenderte Liste erst, nachdem nachweislich ist, dass die Liste geladen wurde, zum Beispiel weil eine andere Aufgabe sichtbar ist oder die Anzahl vorher bestaetigt wurde.

Ausgabe:
14. Gib jede Datei mit einer Zeile === FILE: <Pfad> === aus, gefolgt vom vollstaendigen Java-Code. Der Pfad ist relativ zum Package ui, zum Beispiel {class_name}.java oder pages/TasksPage.java.
15. Antworte nur mit den Dateien, ohne Erklaerungen und ohne Markdown.
16. Ein Locator muss auch nach einer Aktion noch passen. Findet ein Test ein Element ueber einen Text oder ein Kindelement, das sich durch die Aktion aendert oder verschwindet, suche die Folgeelemente auf andere Weise, zum Beispiel direkt auf der Seite, wenn sie dort eindeutig sind.
17. Pruefe bei jeder Aenderung an Daten, also Anlegen, Aendern und Loeschen, zusaetzlich nach page.reload(), dass sie auf dem Server gespeichert wurde. Das gilt auch, wenn die Story das Neuladen nicht ausdruecklich nennt, und ebenso fuer Abbrechen-Szenarien, in denen nichts gespeichert werden darf."""


def read_templates():
    parts = []
    for path in sorted(UI_DIR.rglob("*.java")):
        relative = path.relative_to(UI_DIR).as_posix()
        parts.append(f"=== FILE: {relative} ===\n{path.read_text(encoding='utf-8')}")
    return "\n\n".join(parts)


def generate_ui_test(story_key):
    summary, description = get_story(story_key)
    class_name = story_key.replace("-", "") + "UiTest"

    user_content = (
        f"Story {story_key}: {summary}\n\n{description}\n\n"
        f"--- UI-Vertrag ---\n{(DOCS / 'ui-vertrag.md').read_text(encoding='utf-8')}\n\n"
        f"--- API-Vertrag ---\n{(DOCS / 'api-vertrag.md').read_text(encoding='utf-8')}\n\n"
        f"--- Vorhandene Dateien ---\n{read_templates()}"
    )

    client = anthropic.Anthropic()
    message = client.messages.create(
        model=MODEL,
        max_tokens=16000,
        system=SYSTEM_PROMPT.format(class_name=class_name, story_key=story_key),
        messages=[{"role": "user", "content": user_content}],
    )

    if message.stop_reason == "max_tokens":
        raise RuntimeError("Antwort wurde abgeschnitten, max_tokens erhöhen")

    text = "".join(block.text for block in message.content if block.type == "text")
    pieces = FILE_MARKER.split(text)
    written = []

    for name, content in zip(pieces[1::2], pieces[2::2]):
        name = name.strip()
        if not ALLOWED_PATH.match(name):
            raise RuntimeError(f"Unzulässiger Dateipfad vom Modell: {name}")

        target = UI_DIR / name
        if target.exists() and name != f"{class_name}.java":
            print(f"Übersprungen, existiert bereits: {name}")
            continue

        code = re.sub(r"^```\w*\n|```$", "", content.strip()).strip() + "\n"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(code, encoding="utf-8")
        written.append(name)

    if f"{class_name}.java" not in written:
        raise RuntimeError(f"Claude hat keine Testklasse {class_name}.java geliefert")
    return written


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Aufruf: python generate_ui_tests.py TB-3")
        sys.exit(1)
    for name in generate_ui_test(sys.argv[1]):
        print(f"Gespeichert: {name}")