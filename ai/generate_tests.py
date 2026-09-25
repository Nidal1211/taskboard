import re
import sys
from pathlib import Path

import anthropic
from dotenv import load_dotenv

from fetch_story import get_story

load_dotenv()

ROOT = Path(__file__).resolve().parent.parent
CONTRACT_FILE = ROOT / "docs" / "api-vertrag.md"
TEST_DIR = ROOT / "e2e-tests" / "src" / "test" / "java" / "de" / "nidal" / "taskboard" / "api"
MODEL = "claude-sonnet-5"

SYSTEM_PROMPT = """Du bist ein erfahrener Testautomatisierer.
Du bekommst eine User Story mit Szenarien im Format Gegeben/Wenn/Dann und einen API-Vertrag.
Schreibe daraus automatisierte API-Tests.

Regeln:
1. Schreibe genau eine Java-Testklasse mit JUnit 5 und REST Assured.
2. Package: de.nidal.taskboard.api
3. Klassenname: {class_name}
4. Die Klasse bekommt die Annotation @Tag("{story_key}").
5. Jedes Szenario, das sich ueber die API pruefen laesst, wird mindestens ein Test. Jeder Test bekommt @DisplayName mit dem Szenarionamen aus der Story.
6. Szenarien, die nur die Oberflaeche betreffen (Buttons, Weiterleitungen, Dialoge), testest du nicht. Liste sie stattdessen in einem Kommentar am Anfang der Klasse auf.
7. Verwende ausschliesslich Adressen, Statuscodes und Meldungen aus dem API-Vertrag. Erfinde nichts. Fehlt im Vertrag eine Angabe, schreibe einen TODO-Kommentar, statt zu raten.
8. Rufe vor jedem Test POST /test/reset auf (@BeforeEach).
9. Die Basis-URL kommt aus der System-Property baseUrl, Standardwert http://localhost:8080.
10. Jeder Test prueft den Statuscode und die relevanten Felder oder die Meldung.
11. Antworte nur mit dem Java-Code, ohne Erklaerungen und ohne Markdown.
12. Pruefe jede Und-Zeile eines Szenarios. Wenn ein Szenario sagt, dass etwas nicht angelegt oder nicht veraendert wird, beweise das mit einer weiteren Anfrage, zum Beispiel einer erneuten Registrierung oder einem Login.
13. Enthaelt ein Szenario teilweise Oberflaechen-Schritte, teste den API-Teil und nenne die Oberflaechen-Teile im Kommentar am Anfang der Klasse.
14. Erzeuge Request-Bodies mit Map.of statt mit String-Verkettung.
15. Vermeide doppelten Code durch private Hilfsmethoden, zum Beispiel register(email, password).
16. Verwende die konkreten Beispielwerte aus der Story exakt, zum Beispiel die genannte Anzahl Leerzeichen oder die genannten Titel.
17. Nenne im Kommentar zu Oberflaechen-Teilen nur Schritte, die woertlich in der Story stehen. Erfinde keine zusaetzlichen Anforderungen.
18. Jede Pruefung muss fehlschlagen koennen, wenn die App sich falsch verhaelt. Pruefe keine Dinge, die unabhaengig vom getesteten Schritt immer gelten. Beweise zum Beispiel, dass nach einem fehlgeschlagenen Login niemand angemeldet ist, dadurch, dass die Antwort keinen Token enthaelt."""


def generate_test(story_key):
    summary, description = get_story(story_key)
    contract = CONTRACT_FILE.read_text(encoding="utf-8")
    class_name = story_key.replace("-", "") + "ApiTest"

    client = anthropic.Anthropic()
    message = client.messages.create(
        model=MODEL,
        max_tokens=8000,
        system=SYSTEM_PROMPT.format(class_name=class_name, story_key=story_key),
        messages=[{
            "role": "user",
            "content": f"Story {story_key}: {summary}\n\n{description}\n\n--- API-Vertrag ---\n{contract}",
        }],
    )

    if message.stop_reason == "max_tokens":
     raise RuntimeError("Antwort wurde abgeschnitten, max_tokens erhöhen")

    code = "".join(block.text for block in message.content if block.type == "text").strip()
    if not code:
        raise RuntimeError("Claude hat keinen Code zurückgegeben")
    code = re.sub(r"^```\w*\n|```$", "", code).strip()

    TEST_DIR.mkdir(parents=True, exist_ok=True)
    target = TEST_DIR / f"{class_name}.java"
    target.write_text(code, encoding="utf-8")
    return target


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Aufruf: python generate_tests.py TB-1")
        sys.exit(1)
    path = generate_test(sys.argv[1])
    print(f"Test gespeichert: {path}")