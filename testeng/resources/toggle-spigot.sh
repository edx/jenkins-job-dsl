#!/bin/bash
set -e

virtualenv toggle_venv -q
source toggle_venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements.txt

echo "Running toggle script..."
python jenkins/toggle-spigot.py --spigot_state $SPIGOT_STATE
