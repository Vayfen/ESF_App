import json
import os

# Chemin des fichiers (à adapter)
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # Chemin racine

input_file = os.path.join(BASE_DIR, "events.json")
output_file = os.path.join(BASE_DIR, "filtered_events.json")

# Charger les données
with open(input_file, 'r', encoding='utf-8') as f:
    data = json.load(f)

# Définir les valeurs à exclure
excluded_cp = {"ABSENT", "ABSENCEMONO"}
excluded_lp = {"ABSENT", "ABSENCE MONO"}

# Filtrer les éléments
filtered_items = [
    item for item in data["Items"]
    if (item["cp"] not in excluded_cp) and (item["lp"] not in excluded_lp)
]

# Créer la structure de sortie
result = {
    "Page": data["Page"],
    "Pages": data["Pages"],
    "Total": len(filtered_items),  # Mise à jour du total filtré
    "Items": filtered_items
}

# Sauvegarder les résultats
with open(output_file, 'w', encoding='utf-8') as f:
    json.dump(result, f, indent=4, ensure_ascii=False)

print(f"{len(filtered_items)} éléments filtrés sauvegardés dans {output_file}")
