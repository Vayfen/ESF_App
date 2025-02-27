import json
import os
import pytz
import logging
import re
import sys
from dotenv import load_dotenv
from datetime import datetime, timezone, timedelta
from dateutil.parser import parse
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from google.auth.transport.requests import Request
from google_auth_oauthlib.flow import InstalledAppFlow

# Configuration des scopes
SCOPES = ['https://www.googleapis.com/auth/calendar']
load_dotenv()
CALENDAR_ID = os.getenv("CALENDAR_ID")

def parse_esf_date(esf_date):
    """Convertit le format de date ESF en datetime UTC"""
    try:
        # Extraction du timestamp avec une regex plus robuste
        match = re.match(r"/Date\((\d+)([+-]\d{4})\)/", esf_date)
        if not match:
            raise ValueError(f"Format de date invalide : {esf_date}")
        
        timestamp = int(match.group(1)) // 1000  # Convertit en secondes
        offset = int(match.group(2)[:3])  # Prend seulement les heures
        
        dt = datetime.fromtimestamp(timestamp, tz=timezone.utc)
        return dt.astimezone(timezone(timedelta(hours=offset)))
        
    except Exception as e:
        logging.error(f"Erreur parsing date {esf_date} : {str(e)}")
        return None

def get_calendar_events(service, CALENDAR_ID, time_min, time_max):
    """Récupère les événements du calendrier"""
    events_result = service.events().list(
        calendarId=CALENDAR_ID,
        timeMin=time_min,
        timeMax=time_max,
        singleEvents=True,
        orderBy='startTime'
    ).execute()
    return events_result.get('items', [])

def compare_with_calendar(service, new_events):
    """Compare les événements ESF avec le calendrier existant et retourne les nouveaux événements"""
    # Vérification du calendar_id
    calendar_id = os.environ.get("CALENDAR_ID")
    if not CALENDAR_ID:
        raise ValueError("CALENDAR_ID non défini dans les variables d'environnement")

    # Récupération des événements existants avec plage de dates
    now = datetime.now(timezone.utc).isoformat()
    existing_events = service.events().list(
        calendarId=CALENDAR_ID,
        timeMin=now,
        timeMax=(datetime.now(timezone.utc) + timedelta(days=60)).isoformat(),
        singleEvents=True,
        orderBy="startTime",
        maxResults=2500
    ).execute().get('items', [])

    # Création d'un set de comparaison
    existing_times = set()
    for event in existing_events:
        start = event['start'].get('dateTime') or event['start'].get('date')
        if start:
            existing_times.add(parse(start).timestamp())

    # Filtrage des nouveaux événements
    to_add = []
    for esf_event in new_events:
        try:
            # Conversion de l'événement ESF au format Google
            gevent = convert_esf_to_google_event(esf_event)  # Renommage de la fonction
            if not gevent:
                continue

            # Extraction du timestamp de début
            start_time = parse(gevent['start']['dateTime']).timestamp()

            # Vérification de l'existence
            if start_time not in existing_times:
                to_add.append(gevent)
                logging.info(f"Nouvel événement détecté: {gevent['summary']}")
            else:
                logging.debug(f"Événement existant: {gevent['summary']}")

        except Exception as e:
            logging.error(f"Erreur traitement événement {esf_event.get('ih')}: {str(e)}")
            continue

    return to_add

def get_google_calendar_service():
    """Authentification Google"""
    creds = None
    token_file = "config/token.json"
    
    if os.path.exists(token_file):
        creds = Credentials.from_authorized_user_file(token_file, SCOPES)
    
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            flow = InstalledAppFlow.from_client_secrets_file(
                'config/credentials.json', 
                SCOPES
            )
            creds = flow.run_local_server(port=0)
        
        with open(token_file, 'w') as token:
            token.write(creds.to_json())
    
    return build('calendar', 'v3', credentials=creds)

def create_calendar_event(service, event_data):
    """Crée un événement calendrier à partir du format ESF"""
    # Récupérer le calendar_id depuis les variables globales
    calendar_id = os.environ.get("CALENDAR_ID")
    
    if not CALENDAR_ID:
        raise ValueError("CALENDAR_ID non configuré")
    
    try:
        # Appel corrigé avec le calendar_id
        event = service.events().insert(
            calendarId=CALENDAR_ID,  # <-- Correction ici
            body=event_data
        ).execute()
        return event
        
    except Exception as e:
        logging.error(f"Échec de création d'événement : {str(e)}")
        return None

def convert_esf_to_google_event(esf_event):
    """Convertit le format ESF en structure Google Calendar"""
    try:
        # Extraction des dates
        start_utc = parse_esf_date(esf_event['dd'])
        end_utc = parse_esf_date(esf_event['df'])
        
        return {
            'summary': f"{esf_event.get('lne', 'Cours ESF')} - {esf_event.get('llr', '')}",
            'location': esf_event.get('llr', ''),
            'description': f"""\
Niveau: {esf_event.get('lne', 'Inconnu')}
Moniteur: {esf_event.get('im', 'Non spécifié')}
Code: {esf_event.get('cp', '')}""",
            'start': {
                'dateTime': start_utc.isoformat(),
                'timeZone': 'Europe/Paris'
            },
            'end': {
                'dateTime': end_utc.isoformat(),
                'timeZone': 'Europe/Paris'
            }
        }
    except KeyError as e:
        logging.error(f"Champ manquant dans l'événement ESF: {str(e)}")
        return None
    except Exception as e:
        logging.error(f"Erreur conversion événement: {str(e)}")
        return None

def main():
    if not CALENDAR_ID:
        logging.critical("CALENDAR_ID non configuré")
        sys.exit(1)

    service = get_google_calendar_service()
    
    # Charger les nouveaux événements
    try:
        with open("filtered_events.json", "r") as f:
            esf_events = json.load(f).get('Items', [])
    except Exception as e:
        logging.error(f"Erreur chargement données: {str(e)}")
        return

    new_events = compare_with_calendar(service, esf_events)

    for event_data in new_events:
        try:
            service.events().insert(
                calendarId=CALENDAR_ID,
                body=event_data
            ).execute()
        except Exception as e:
            logging.error(f"Échec ajout événement: {str(e)}")

if __name__ == "__main__":
    main()
