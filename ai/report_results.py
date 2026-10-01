import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

from jira_status import comment, get_status, transition

DONE = "Fertig"
IN_PROGRESS = "In Arbeit"


def collect_results(report_dir):
    results = {}
    for path in glob.glob(os.path.join(report_dir, "TEST-*.xml")):
        suite = ET.parse(path).getroot()
        match = re.search(r"TB(\d+)(Api|Ui)Test$", suite.get("name", ""))
        if not match:
            continue
        story_key = f"TB-{match.group(1)}"
        cases = suite.findall("testcase")
        failed = [
            f"{match.group(2)}: {case.get('name')}"
            for case in cases
            if case.find("failure") is not None or case.find("error") is not None
        ]
        total, previous_failed = results.get(story_key, (0, []))
        results[story_key] = (total + len(cases), previous_failed + failed)
    return results


def main(report_dir):
    build = f"Jenkins-Lauf #{os.environ.get('BUILD_NUMBER', '?')}: {os.environ.get('BUILD_URL', '')}"

    for story_key, (total, failed) in sorted(collect_results(report_dir).items()):
        status = get_status(story_key)

        if not failed:
            print(f"{story_key}: {total} von {total} bestanden, Status {status}")
            if status.lower() != DONE.lower():
                transition(story_key, DONE)
                comment(story_key, f"Alle {total} API-Tests bestanden. Story automatisch auf {DONE} gesetzt.\n{build}")
        else:
            names = ", ".join(failed)
            print(f"{story_key}: {len(failed)} von {total} fehlgeschlagen: {names}")
            comment(story_key, f"{len(failed)} von {total} API-Tests fehlgeschlagen: {names}\n{build}")
            if status.lower() == DONE.lower():
                transition(story_key, IN_PROGRESS)


if __name__ == "__main__":
    main(sys.argv[1])