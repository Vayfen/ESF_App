#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Générateur de fichiers ICS à partir d'événements JSON ESF
Ce script lit les événements du fichier filtered_events.json et crée un fichier .ics
compatible avec tous les clients de calendrier (Outlook, Google Calendar, Apple Calendar, etc.)
"""

import json
import os
import re
import logging
from datetime import datetime, timezone, timedelta
from dateutil.parser import parse
import pytz
import hashlib

# Configuration du logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

class ICSGenerator:
    def __init__(self, output_filename="esf_calendar.ics"):
        self.output_filename = output_filename
        self.timezone_paris = pytz.timezone("Europe/Paris")
        
    def parse_esf_date(self, esf_date):
        """Convertit le format de date ESF en datetime Europe/Paris"""
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
            return dt.astimezone(self.timezone_paris)

        except Exception as e:
            logging.error(f"Erreur parsing date {esf_date} : {str(e)}")
            return None

    def convert_esf_to_french_date(self, esf_date):
        """Convertit un format de date ESF en date française formatée pour affichage"""
        try:
            match = re.match(r"/Date\((\d+)([+-]\d{4})?\)/", esf_date)
            if not match:
                raise ValueError(f"Format ESF invalide : {esf_date}")

            timestamp_ms = int(match.group(1))
            offset_str = match.group(2) or "+0000"

            timestamp_sec = timestamp_ms // 1000
            date_utc = datetime.utcfromtimestamp(timestamp_sec).replace(tzinfo=timezone.utc)

            offset_hours = int(offset_str[:3])
            date_adjusted = date_utc + timedelta(hours=offset_hours)

            return date_adjusted.astimezone(self.timezone_paris).strftime("%d/%m/%Y %H:%M:%S")

        except Exception as e:
            raise ValueError(f"Erreur de conversion pour {esf_date} : {str(e)}") from e

    def escape_ics_text(self, text):
        """Échappe le texte pour le format ICS"""
        if not text:
            return ""
        # Échapper les caractères spéciaux ICS
        text = str(text)
        text = text.replace("\\", "\\\\")
        text = text.replace(",", "\\,")
        text = text.replace(";", "\\;")
        text = text.replace("\n", "\\n")
        text = text.replace("\r", "")
        return text

    def format_datetime_ics(self, dt):
        """Formate une datetime pour le format ICS"""
        if dt is None:
            return None
        # Format ICS UTC : YYYYMMDDTHHMMSSZ
        return dt.astimezone(timezone.utc).strftime("%Y%m%dT%H%M%SZ")

    def generate_uid(self, esf_event):
        """Génère un UID unique pour l'événement basé sur l'ih ESF"""
        ih = str(esf_event.get('ih', ''))
        # Créer un UID stable basé sur l'ih ESF
        return f"esf-{ih}@esf-calendar.local"

    def convert_esf_to_ics_event(self, esf_event, server_time):
        """Convertit un événement ESF en format ICS"""
        try:
            start_dt = self.parse_esf_date(esf_event['dd'])
            end_dt = self.parse_esf_date(esf_event['df'])

            if not start_dt or not end_dt:
                logging.warning(f"Dates invalides pour l'événement IH={esf_event.get('ih')}")
                return None

            # Conversion des dates pour la description
            add_esf_date = self.convert_esf_to_french_date(esf_event['dm'])
            add_server_date = self.convert_esf_to_french_date(server_time)

            # Génération de l'UID unique
            uid = self.generate_uid(esf_event)

            # Création de la description
            description = f"Niveau Ski: {esf_event.get('lne', 'Inconnu')}\\n"
            description += f"Niveau Langue: {esf_event.get('lle', 'Non spécifié')} {esf_event.get('nl', '')}\\n"
            description += f"Ajouté le {add_esf_date} par l'ESF\\n"
            description += f"Synchronisé le {add_server_date} via le serveur\\n"
            description += f"Autres Infos: {esf_event.get('cm', 'Inconnu')}"

            # Timestamp de création/modification (maintenant en UTC)
            now_utc = datetime.now(timezone.utc)
            timestamp = now_utc.strftime("%Y%m%dT%H%M%SZ")

            # Construction de l'événement ICS
            ics_event = []
            ics_event.append("BEGIN:VEVENT")
            ics_event.append(f"UID:{uid}")
            ics_event.append(f"DTSTART:{self.format_datetime_ics(start_dt)}")
            ics_event.append(f"DTEND:{self.format_datetime_ics(end_dt)}")
            ics_event.append(f"DTSTAMP:{timestamp}")
            ics_event.append(f"CREATED:{timestamp}")
            ics_event.append(f"LAST-MODIFIED:{timestamp}")
            ics_event.append(f"SUMMARY:{self.escape_ics_text(esf_event.get('lp', 'Cours ESF'))}")
            
            location = esf_event.get('llr', '')
            if location:
                ics_event.append(f"LOCATION:{self.escape_ics_text(location)}")
            
            ics_event.append(f"DESCRIPTION:{self.escape_ics_text(description)}")
            ics_event.append("STATUS:CONFIRMED")
            ics_event.append("TRANSP:OPAQUE")
            ics_event.append("END:VEVENT")

            return "\n".join(ics_event)

        except Exception as e:
            logging.error(f"Erreur conversion événement IH={esf_event.get('ih')} : {str(e)}")
            return None

    def generate_ics_calendar(self, esf_events, server_time):
        """Génère le fichier ICS complet"""
        try:
            # En-tête du calendrier ICS
            ics_lines = [
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//ESF Calendar Generator//ESF Events//FR",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                "X-WR-CALNAME:Calendrier ESF",
                "X-WR-CALDESC:Événements automatiquement synchronisés depuis l'ESF",
                "X-WR-TIMEZONE:Europe/Paris"
            ]

            # Ajout des événements
            events_added = 0
            for esf_event in esf_events:
                ics_event = self.convert_esf_to_ics_event(esf_event, server_time)
                if ics_event:
                    ics_lines.append(ics_event)
                    events_added += 1

            # Fin du calendrier
            ics_lines.append("END:VCALENDAR")

            # Écriture du fichier
            ics_content = "\n".join(ics_lines)
            
            with open(self.output_filename, 'w', encoding='utf-8') as f:
                f.write(ics_content)

            logging.info(f"Fichier ICS généré : {self.output_filename}")
            logging.info(f"Nombre d'événements traités : {events_added}")
            
            return True

        except Exception as e:
            logging.error(f"Erreur génération fichier ICS : {str(e)}")
            return False

    def load_and_process_json(self, json_filename="filtered_events.json"):
        """Charge et traite le fichier JSON des événements"""
        try:
            if not os.path.exists(json_filename):
                logging.error(f"Fichier {json_filename} introuvable")
                return False

            with open(json_filename, "r", encoding='utf-8') as f:
                data = json.load(f)

            esf_events = data.get('Items', [])
            server_time = data.get('ServerTime')

            if not esf_events:
                logging.warning("Aucun événement trouvé dans le fichier JSON")
                return False

            if not server_time:
                logging.warning("ServerTime non trouvé, utilisation de l'heure actuelle")
                server_time = f"/Date({int(datetime.now().timestamp() * 1000)}+0100)/"

            logging.info(f"Chargement de {len(esf_events)} événements depuis {json_filename}")
            
            return self.generate_ics_calendar(esf_events, server_time)

        except Exception as e:
            logging.error(f"Erreur traitement fichier JSON : {str(e)}")
            return False


def main():
    """Fonction principale"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Génère un fichier ICS à partir d'événements ESF")
    parser.add_argument("--input", "-i", default="filtered_events.json", 
                       help="Fichier JSON d'entrée (défaut: filtered_events.json)")
    parser.add_argument("--output", "-o", default="esf_calendar.ics", 
                       help="Fichier ICS de sortie (défaut: esf_calendar.ics)")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Mode verbose")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # Génération du fichier ICS
    generator = ICSGenerator(args.output)
    success = generator.load_and_process_json(args.input)
    
    if success:
        print(f"✅ Fichier ICS généré avec succès : {args.output}")
        print(f"📅 Vous pouvez maintenant vous abonner à ce calendrier via l'URL du fichier")
        return 0
    else:
        print("❌ Erreur lors de la génération du fichier ICS")
        return 1


if __name__ == "__main__":
    exit(main())