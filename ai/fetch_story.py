import os
import requests
from dotenv import load_dotenv

load_dotenv()

JIRA_URL = os.environ["JIRA_URL"]
JIRA_EMAIL = os.environ["JIRA_EMAIL"]
JIRA_API_TOKEN = os.environ["JIRA_API_TOKEN"]


def get_story(key):
    url = f"{JIRA_URL}/rest/api/2/issue/{key}"
    response = requests.get(
        url,
        auth=(JIRA_EMAIL, JIRA_API_TOKEN),
        params={"fields": "summary,description"},
        timeout=30,
    )
    response.raise_for_status()
    fields = response.json()["fields"]
    return fields["summary"], fields["description"]


if __name__ == "__main__":
    summary, description = get_story("TB-3")
    print(f"Titel: {summary}\n")
    print(description)
