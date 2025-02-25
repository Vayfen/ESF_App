import json
import os
from datetime import datetime
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from google.auth.transport.requests import Request
from google_auth_oauthlib.flow import InstalledAppFlow

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

INPUT_FILE = os.path.join(BASE_DIR, "filtered_events.json")
LAST_IMPORT_FILE = os.path.join(BASE_DIR, "dernier_import.json")
ARCHIVE_FOLDER = "archives"
CALENDAR_ID = os.getenv('CALENDAR_ID', 'primary')

def compare_files():
    """Compare les deux fichiers JSON et retourne True s'ils sont différents"""
    if not os.path.exists(LAST_IMPORT_FILE):
        return True
        
    with open(INPUT_FILE, 'r') as f1, open(LAST_IMPORT_FILE, 'r') as f2:
        return json.load(f1) != json.load(f2)

def archive_file():
    """Crée une archive datée et met à jour dernier_import.json"""
    # Création du dossier d'archive si nécessaire
    os.makedirs(ARCHIVE_FOLDER, exist_ok=True)
    
    # Nom de l'archive
    archive_name = f"import_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    archive_path = os.path.join(ARCHIVE_FOLDER, archive_name)
    
    # Copie des fichiers
    os.replace(INPUT_FILE, archive_path)
    os.replace(archive_path, LAST_IMPORT_FILE)
    
    return archive_path

def get_google_calendar_service():
    """Authentification à l'API Google Calendar"""
    # Configuration nécessaire au préalable :
    # 1. Activer l'API Calendar : https://developers.google.com/calendar/api/quickstart/python
    # 2. Créer des identifiants OAuth 2.0
    # 3. Sauvegarder le fichier credentials.json
    
    creds = None
    token_file = "config/token.json"    
    if os.path.exists(token_file):
        creds = Credentials.from_authorized_user_file(token_file)
    
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            flow = InstalledAppFlow.from_client_secrets_file(
                'config/credentials.json', 
                ['https://www.googleapis.com/auth/calendar']
            )
            creds = flow.run_local_server(port=0)
        
        with open(token_file, 'w') as token:
            token.write(creds.to_json())
    
    return build('calendar', 'v3', credentials=creds)

def create_calendar_event(service, event_data):
    """Crée un événement dans Google Calendar"""
    event = {
        'summary': f"Cours {event_data.get('cp', '')}",
        'description': f"Matière: {event_data.get('lp', '')}",
        'start': {
            'dateTime': parse_json_date(event_data['dd']),
            'timeZone': 'Europe/Paris',
        },
        'end': {
            'dateTime': parse_json_date(event_data['df']),
            'timeZone': 'Europe/Paris',
        },
    }
    
    return service.events().insert(
        calendarId=CALENDAR_ID,
        body=event
    ).execute()

def parse_json_date(json_date):
    """Convertit le format de date /Date(...)/ en ISO 8601"""
    timestamp = int(json_date.split('(')[1].split('+')[0]) / 1000
    return datetime.fromtimestamp(timestamp).isoformat()

def main():
    # 1. Vérifier les différences
    if not compare_files():
        print("Aucun changement détecté")
        return
    
    # 2. Archiver
    archive_path = archive_file()
    print(f"Archive créée : {archive_path}")
    
    # 3. Importer sur Google Calendar
    service = get_google_calendar_service()
    
    with open(LAST_IMPORT_FILE, 'r') as f:
        events = json.load(f)['Items']
    
    new_events = []
    for event in events:
        try:
            result = create_calendar_event(service, event)
            new_events.append(event)
            print(f"Événement créé: {result.get('htmlLink')}")
        except Exception as e:
            print(f"Erreur lors de la création: {str(e)}")

if __name__ == "__main__":
    main()
