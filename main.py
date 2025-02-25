import subprocess
from pathlib import Path

def main():
    # Créer le dossier config si nécessaire
    Path("config").mkdir(exist_ok=True)
    
    # Exécution des étapes dans l'ordre
    steps = [
        "python3 scripts/recuperation_json_2402_final.py",
        "python3 scripts/tri_json_2402_v0.py",
        "python3 scripts/import_cal_et_gen_mail_v0.py"
    ]
    
    for step in steps:
        try:
            subprocess.run(step, shell=True, check=True)
        except subprocess.CalledProcessError as e:
            print(f"Erreur lors de l'étape {step}: {e}")
            break

if __name__ == "__main__":
    main()
