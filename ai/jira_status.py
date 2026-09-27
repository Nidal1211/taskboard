import os
import sys

import requests
from dotenv import load_dotenv

load_dotenv()

JIRA_URL = os.environ["JIRA_URL"]
AUTH = (os.environ["JIRA_EMAIL"], os.environ["JIRA_API_TOKEN"])


def get_status(story_key):
    url = f"{JIRA_URL}/rest/api/2/issue/{story_key}"
    response = requests.get(url, auth=AUTH, params={"fields": "status"}, timeout=30)
    response.raise_for_status()
    return response.json()["fields"]["status"]["name"]


def transition(story_key, target_status):
    url = f"{JIRA_URL}/rest/api/2/issue/{story_key}/transitions"
    response = requests.get(url, auth=AUTH, timeout=30)
    response.raise_for_status()
    transitions = response.json()["transitions"]

    for t in transitions:
        if t["to"]["name"].lower() == target_status.lower():
            requests.post(url, auth=AUTH, json={"transition": {"id": t["id"]}}, timeout=30).raise_for_status()
            return

    available = ", ".join(t["to"]["name"] for t in transitions)
    raise RuntimeError(f"Kein Übergang nach '{target_status}' für {story_key}. Möglich: {available}")


def comment(story_key, text):
    url = f"{JIRA_URL}/rest/api/2/issue/{story_key}/comment"
    requests.post(url, auth=AUTH, json={"body": text}, timeout=30).raise_for_status()


if __name__ == "__main__":
    story_key, status = sys.argv[1], sys.argv[2]
    transition(story_key, status)
    if len(sys.argv) > 3:
        comment(story_key, sys.argv[3])
    print(f"{story_key} -> {status}")