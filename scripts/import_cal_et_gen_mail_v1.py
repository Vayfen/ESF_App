# Cette version comprend la modification des choses récupérées pour créer un évènement et l'ajout de la date du
# serveur lors de l'ajout de l'évènement. Pour cela j'ai ajouté la ligne 29 :
# "ServerTime": data["ServerTime"],  # Ajout de la valeur ServerTime
#dans tri_json_2402_v0.py
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
logging.basicConfig(level=logging.DEBUG)  # Ajoutez ceci au début du script
# Configuration des scopes
SCOPES = ['https://www.googleapis.com/auth/calendar']
load_dotenv()
CALENDAR_ID = os.getenv("CALENDAR_ID")

def convert_esf_to_french_date(esf_date):
    """
    Convertit un format de date ESF (avec ou sans offset) en date française formatée.
    """
    try:
        # Nouvelle regex plus robuste
        match = re.match(r"/Date\((\d+)([+-]\d{4})?\)/", esf_date)
        if not match:
            raise ValueError(f"Format ESF invalide : {esf_date}")

        timestamp_ms = int(match.group(1))
        offset_str = match.group(2) or "+0000"  # Gestion de l'offset manquant

        # Conversion du timestamp en datetime UTC
        timestamp_sec = timestamp_ms // 1000
        date_utc = datetime.utcfromtimestamp(timestamp_sec).replace(tzinfo=timezone.utc)

        # Ajustement avec l'offset
        offset_hours = int(offset_str[:3])  # Extraction des heures (+/-XX)
        date_adjusted = date_utc + timedelta(hours=offset_hours)

        # Conversion finale en Europe/Paris
        timezone_paris = pytz.timezone("Europe/Paris")
        return date_adjusted.astimezone(timezone_paris).strftime("%d/%m/%Y %H:%M:%S")

    except Exception as e:
        raise ValueError(f"Erreur de conversion pour {esf_date} : {str(e)}") from e

def parse_esf_date(esf_date):
    """Convertit le format de date ESF en datetime Europe/Paris (sans double offset)"""
    try:
        match = re.match(r"/Date\((\d+)([+-]\d{4})?\)/", esf_date)
        if not match:
            raise ValueError(f"Format de date ESF invalide : {esf_date}")

        timestamp_ms = int(match.group(1))
        offset_str = match.group(2) or "+0000"

        # Conversion de l'offset (+/-HHMM) en délai
        offset_h = int(offset_str[:3])
        offset_m = int(offset_str[3:5]) if len(offset_str) > 3 else 0
        tz_offset = timezone(timedelta(hours=offset_h, minutes=offset_m))

        # Création du datetime AVEC l'offset d'origine
        dt = datetime.fromtimestamp(timestamp_ms // 1000, tz=tz_offset)

        # Conversion en Europe/Paris (gère les DST automatiquement)
        tz_paris = pytz.timezone("Europe/Paris")
        return dt.astimezone(tz_paris)

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

def compare_with_calendar(service, new_events, server_time):
    """Compare les événements via le champ 'ih' et retourne les nouveaux"""
    # Récupérer tous les 'ih' existants dans le calendrier
    existing_ih = set()
    page_token = None
    
    try:
        while True:
            # Récupération par pages (max 2500 événements/page)
            events_batch = service.events().list(
                calendarId=CALENDAR_ID,
                pageToken=page_token,
                maxResults=2500,
                fields="items(id,extendedProperties/private)"
            ).execute()
            
            # Extraction des 'ih' depuis extendedProperties
            for event in events_batch.get('items', []):
                private_props = event.get('extendedProperties', {}).get('private', {})
                if 'esf_ih' in private_props:
                    existing_ih.add(private_props['esf_ih'])
            
            page_token = events_batch.get('nextPageToken')
            if not page_token:
                break

    except Exception as e:
        # logging.error(f"Erreur récupération calendrier: {str(e)}")
        return []

    # Filtrer les nouveaux événements
    to_add = []
    for esf_event in new_events:
        esf_ih = str(esf_event.get('ih'))  # Conversion forcée en string
        if not esf_ih:
            continue

        if esf_ih not in existing_ih:
            to_add.append(esf_event)
            # logging.info(f"Nouvel événement détecté (IH: {esf_ih}")
        # else:
            # logging.debug(f"Événement déjà existant (IH: {esf_ih}")

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
            calendarId=CALENDAR_ID,
            body=event_data
        ).execute()
        return event
        
    except Exception as e:
        logging.error(f"Échec de création d'événement : {str(e)}")
        return None

def convert_esf_to_google_event(esf_event, server_time):
    """Convertit le format ESF en structure Google Calendar"""
    try:
        start = parse_esf_date(esf_event['dd'])
        end = parse_esf_date(esf_event['df'])

        # Conversion des dates ESF pour la description
        add_ESF_utc = convert_esf_to_french_date(esf_event['dm'])
        add_cal_via_serv_utc = convert_esf_to_french_date(server_time)

        if not start or not end:
            return None

        return {
            'extendedProperties': {
                'private': {'esf_ih': str(esf_event['ih'])}
            },
            'summary': esf_event.get('lp', 'Cours ESF'),
            'location': esf_event.get('llr', ''),
            'description': f"""\
Niveau Ski:      {esf_event.get('lne', 'Inconnu')}
Niveau Langue:   {esf_event.get('lle', 'Non spécifié')} {esf_event.get('nl', '')}
Ajouté le {add_ESF_utc} par l'ESF
Synchronisé le {add_cal_via_serv_utc} via le serveur
Autres Infos:    {esf_event.get('cm', 'Inconnu')}""",
            'start': {
                'dateTime': start.isoformat(),
                'timeZone': 'Europe/Paris'
            },
            'end': {
                'dateTime': end.isoformat(),
                'timeZone': 'Europe/Paris'
            }
        }

    except Exception as e:
        logging.error(f"Erreur conversion : {str(e)}")
        return None


def main():
    if not CALENDAR_ID:
    	logging.critical("CALENDAR_ID non configuré")
    	sys.exit(1)

    service = get_google_calendar_service()
    
    # Charger les nouveaux événements ET ServerTime
    try:
        with open("filtered_events.json", "r") as f:
            data = json.load(f)
            esf_events = data.get('Items', [])
            server_time = data.get('ServerTime')  # Récupération de ServerTime
    except Exception as e:
        logging.error(f"Erreur chargement données: {str(e)}")
        return

    # Passer server_time à compare_with_calendar
    new_events = compare_with_calendar(service, esf_events, server_time)

    for event_data in new_events:
        try:
            # Affichez les données brutes AVANT conversion
            # logging.debug(f"Données ESF brutes (IH={event_data.get('ih')}) : {json.dumps(event_data, indent=2)}")
            
            # Conversion et ajout
            gevent = convert_esf_to_google_event(event_data, server_time)
            if not gevent:
                continue

            # Affichez les dates APRÈS conversion
            # logging.debug(f"Dates converties : Début={gevent['start']['dateTime']}, Fin={gevent['end']['dateTime']}")
            
            # Appel à l'API
            service.events().insert(
                calendarId=CALENDAR_ID,
                body=gevent
            ).execute()

        except Exception as e:
            logging.error(f"Échec ajout événement IH={event_data.get('ih')} : {str(e)}")

if __name__ == "__main__":
    main()
