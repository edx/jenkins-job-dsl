#!/usr/bin/env bash

pip install -r e2e/requirements.txt
xvfb-run --server-args="-screen 0, 1600x1200x24" make e2e
