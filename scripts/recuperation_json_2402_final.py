from datetime import datetime, timezone
from playwright.sync_api import sync_playwright
import json
from dotenv import load_dotenv
import os

load_dotenv()

username = os.getenv("ESF_USERNAME")
password = os.getenv("ESF_PASSWORD")

def get_esf_events():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()

        # Activation du logging des requêtes
        # page.on("request", lambda req: print(f">> {req.method} {req.url}"))
        # page.on("response", lambda res: print(f"<< {res.status} {res.url}"))

        print("Navigating to login page...")
        # Étape 1: Connexion
        page.goto("https://carnet-rouge-esf.app/")
        page.click('a[title="Connexion"]')
        page.wait_for_url("https://identity.w-esf.com/**")
        page.fill("#LoginVM_Login", username)
        page.fill("#LoginVM_MotDePasse", password)
        page.click('button[type="submit"]')
        page.wait_for_url("https://carnet-rouge-esf.app/**")
        print("Logged in successfully.")

        # Variables pour le tracking des requêtes
        target_url = "AjaxProxyService.svc/InvokeMethod?IPlanningParticulierServicePublic&GetListeHorairesMoniteur"
        response_data = {"body": None}

        # Intercepteur de requêtes
        def intercept_route(route, request):
            if request.method == "POST" and target_url in request.url:
                print("\n--- INTERCEPTION DE LA REQUÊTE POST ---")

                # Construction du payload
                # start_date = datetime(2025, 2, 16, tzinfo=timezone.utc)
                start_date = datetime.now(timezone.utc)
                end_date = datetime(2025, 4, 30, tzinfo=timezone.utc)
                payload = {
                    "serviceContract": "IPlanningParticulierServicePublic",
                    "serviceMethod": "GetListeHorairesMoniteur",
                    "methodParams": json.dumps({
                        "typeLibelle": "1",
                        "language": "1",
                        "IdGenCaisse": "0",
                        "IdGenPosteTechnique": "6862462",
                        "IdComLangue": "1",
                        "IdComSaison": "63",
                        "NoEcole": "356",
                        "CodeUc": "TECH-UC002-M",
                        "CodeApplication": "PLANNING-PARTICULIER-MONITEUR",
                        "idTecHoraire": "0",
                        "idTecMoniteur": "0",
                        "CodeTypePosteTechnique": "MON",
                        "end": "end",
                        "idTecMoniteurList": ["19358136"],
                        "dateHeureDebut": f"/Date({int(start_date.timestamp() * 1000)}+0000)/",
                        "dateHeureFin": f"/Date({int(end_date.timestamp() * 1000)}+0000)/",
                        "dateReferenceDelta": None
                    }).replace(" ", "")
                }

                print("Payload envoyé:", json.dumps(payload, indent=2))


                # Envoi de la requête modifiée
                route.continue_(
                    post_data=json.dumps(payload),
                    headers={
                        **request.headers,
                        "Content-Type": "application/json"
                    }
                )

                # Capture de la réponse associée
                def handle_response(response):
                    if response.request == request:
                        try:
                            response_data["body"] = response.json()
                            print("Réponse capturée avec succès!")
                        except Exception as e:
                            print(f"Erreur de lecture: {e}")
                            response_data["body"] = response.text()

                page.on("response", handle_response)

            else:
                route.continue_()

        # Activation de l'interception
        page.route(f"**/*{target_url}*", intercept_route)

        print("Navigating to target page...")
        # Déclenchement de la navigation
        page.goto("https://esf356.w-esf.com/PlanningParticulierSSO/PlanningParticulier.aspx?NoEcole=356&disable-logout=true")

        # Attente explicite pour la réponse
        page.wait_for_timeout(10000)
        print("Waited for response.")

        # Extraction des données
        events = response_data.get("body")
        #print("Events data:", events)
        if events:
            with open("events.json", "w") as f:
                json.dump(events, f, indent=4)
            print("Événements sauvegardés dans events.json!")
        else:
            print("Aucune donnée récupérée")

        browser.close()
        return events

# Exécution
events = get_esf_events()
if events:
    print("il a y a bien eu la recuperation")
    #print(json.dumps(events, indent=4))
else:
    print("Échec de la récupération")
