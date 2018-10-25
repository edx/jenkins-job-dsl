#!/usr/bin/env bash

pip install -r ${WORKSPACE}/analytics-tools/warehouse-docs/requirements.txt
python ${WORKSPACE}/analytics-tools/warehouse-docs/sync_warehouse_docs.py
