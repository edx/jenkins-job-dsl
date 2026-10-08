#!/bin/bash -xe

# Use the host python3.12 when it has one; otherwise fetch a standalone build with uv
# into the jenkins user's home (~/.local/share/uv, cached across runs). No root needed.
if command -v python3.12 >/dev/null 2>&1; then
    PYTHON_312="$(command -v python3.12)"
else
    python3 -m pip install --user --quiet "uv==0.12.20"
    export PATH="${HOME}/.local/bin:${PATH}"
    uv python install 3.12
    PYTHON_312="$(uv python find 3.12)"
fi
# stdlib venv: the host virtualenv cannot seed a 3.12 environment.
"${PYTHON_312}" -m venv --clear "$WORKSPACE/venv"
. "$WORKSPACE/venv/bin/activate"

cd $WORKSPACE/tubular
pip install -r requirements.txt

python scripts/message_prs_in_range.py --org "edx" --repo "app-permissions" --base_sha ${GIT_PREVIOUS_COMMIT_1} --head_sha ${GIT_COMMIT_1} --release "jenkins" --extra_text " on ${ENVIRONMENT}-${DEPLOYMENT}-${JOB_TYPE}. ${BUILD_URL}"
