.github/
└── workflows/
    └── calendar-sync.yml
scripts/
    ├── main.py
    └── requirements.txt
config/
    └── credentials.json  # À générer via Google Cloud
.env.sample
README.md

-------------------------------------------------------------------------------------------------------


Récupération JSON → Filtrage → Comparaison → Archivage → Import Google Calendar


├── .env
├── main.py
├── requirements.txt
├── config/
│   ├── credentials.json (Google OAuth)
│   └── token.json (généré automatiquement)
├── scripts/
│   ├── recuperation_json_2402_final.py
│   ├── tri_json_2402_v0.py
│   └── import_cal_et_gen_mail_v0.py
├── archives/ (créé automatiquement)

-------------------------------------------------------------------------------------------------------

name: Calendar Sync

on:
  schedule:
    - cron: '0 8 * * *'  # Tous les jours à 8h
  workflow_dispatch:     # Déclenchement manuel

jobs:
  sync:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.10'

    - name: Install dependencies
      run: |
        python -m pip install --upgrade pip
        pip install -r scripts/requirements.txt

    - name: Configure credentials
      env:
        GCAL_CREDENTIALS: ${{ secrets.GCAL_CREDENTIALS }}
        GCAL_TOKEN: ${{ secrets.GCAL_TOKEN }}
      run: |
        mkdir -p config
        echo "$GCAL_CREDENTIALS" > config/credentials.json
        echo "$GCAL_TOKEN" > config/token.json

    - name: Run sync
      env:
        EMAIL_ADDRESS: ${{ secrets.EMAIL_ADDRESS }}
        EMAIL_PASSWORD: ${{ secrets.EMAIL_PASSWORD }}
        CALENDAR_ID: ${{ secrets.CALENDAR_ID }}
      run: python scripts/main.py

    - name: Commit changes
      uses: stefanzweifel/git-auto-commit-action@v4
      with:
        commit_message: 'chore: Update last import file'
        file_pattern: dernier_import.json
        
        
-------------------------------------------------------------------------------------------------------

python3 scripts/import_cal_et_gen_mail_v0.py
A faire une fois par semaine et copier coller le token.json dans le secrets github
