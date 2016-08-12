#!/usr/bin/env bash

pip install -r acceptance_tests/requirements.txt
xvfb-run --server-args="-screen 0, 1600x1200x24" make accept
