pip install -r requirements.txt
pip install .
oep2 report --username edx-status-bot -- --org edx --junit-xml=oep2-report.xml
